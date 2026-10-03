package com.aicodereview.analysis.parser;

import com.aicodereview.analysis.model.*;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** Real AST based parser (JavaParser) - classes, interfaces, enums, records, fields, methods, complexity, calls, REST endpoints. */
@Component
public class JavaCodeParser implements CodeParser {

    private static final Set<String> MAPPING_ANNOTATIONS = Set.of(
            "GetMapping", "PostMapping", "PutMapping", "DeleteMapping", "PatchMapping", "RequestMapping");

    @Override
    public boolean supports(String path) { return path.endsWith(".java"); }

    @Override
    public ParsedFile parse(String path, String content) {
        ParserConfiguration cfg = new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
        ParseResult<CompilationUnit> result = new JavaParser(cfg).parse(content);   // new instance: JavaParser is not thread-safe
        if (!result.isSuccessful() || result.getResult().isEmpty()) {
            throw new IllegalArgumentException("Java parse failed for " + path);
        }
        CompilationUnit cu = result.getResult().get();
        String pkg = cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");

        List<String> imports = new ArrayList<>();
        for (ImportDeclaration imp : cu.getImports()) {
            if (!imp.isStatic()) imports.add(imp.getNameAsString() + (imp.isAsterisk() ? ".*" : ""));
        }

        List<CodeElement> elements = new ArrayList<>();
        for (TypeDeclaration<?> td : cu.findAll(TypeDeclaration.class)) {
            elements.add(toElement(path, pkg, td));
        }
        return new ParsedFile(path, "Java", pkg, imports, elements, List.of(), countLoc(content));
    }

    private CodeElement toElement(String path, String pkg, TypeDeclaration<?> td) {
        String name = td.getNameAsString();
        String qualified = pkg.isEmpty() ? name : pkg + "." + name;
        String kind = "CLASS";
        List<String> extendsTypes = new ArrayList<>();
        List<String> implementsTypes = new ArrayList<>();
        List<FieldInfo> fields = new ArrayList<>();

        if (td instanceof ClassOrInterfaceDeclaration c) {
            kind = c.isInterface() ? "INTERFACE" : "CLASS";
            c.getExtendedTypes().forEach(t -> extendsTypes.add(t.getNameAsString()));
            c.getImplementedTypes().forEach(t -> implementsTypes.add(t.getNameAsString()));
        } else if (td instanceof EnumDeclaration e) {
            kind = "ENUM";
            e.getImplementedTypes().forEach(t -> implementsTypes.add(t.getNameAsString()));
        } else if (td instanceof RecordDeclaration r) {
            kind = "RECORD";
            r.getImplementedTypes().forEach(t -> implementsTypes.add(t.getNameAsString()));
            r.getParameters().forEach(p -> fields.add(new FieldInfo(p.getNameAsString(), p.getType().asString())));
        } else if (td instanceof AnnotationDeclaration) {
            kind = "ANNOTATION";
        }

        List<String> annotations = new ArrayList<>();
        td.getAnnotations().forEach(a -> annotations.add(a.getName().getIdentifier()));

        for (FieldDeclaration fd : td.getFields()) {
            for (VariableDeclarator v : fd.getVariables()) {
                fields.add(new FieldInfo(v.getNameAsString(), v.getType().asString()));
            }
        }

        List<String> ctorTypes = new ArrayList<>();
        for (ConstructorDeclaration cd : td.getConstructors()) {
            cd.getParameters().forEach(p -> ctorTypes.add(p.getType().asString()));
        }

        String basePath = "";
        for (AnnotationExpr a : td.getAnnotations()) {
            if (a.getName().getIdentifier().equals("RequestMapping")) basePath = extractPath(a);
        }

        List<MethodInfo> methods = new ArrayList<>();
        List<String> endpoints = new ArrayList<>();
        for (MethodDeclaration m : td.getMethods()) {
            methods.add(toMethod(m));
            for (AnnotationExpr a : m.getAnnotations()) {
                if (MAPPING_ANNOTATIONS.contains(a.getName().getIdentifier())) {
                    String verb = a.getName().getIdentifier().replace("Mapping", "").toUpperCase(Locale.ROOT);
                    if (verb.equals("REQUEST")) verb = "ANY";
                    endpoints.add(verb + " " + joinPath(basePath, extractPath(a)));
                }
            }
        }

        int start = td.getRange().map(r -> r.begin.line).orElse(1);
        int end = td.getRange().map(r -> r.end.line).orElse(start);
        return new CodeElement(name, qualified, kind, path, pkg, start, end, annotations, extendsTypes, implementsTypes,
                fields, ctorTypes, methods, basePath, endpoints);
    }

    private MethodInfo toMethod(MethodDeclaration m) {
        int start = m.getRange().map(r -> r.begin.line).orElse(0);
        int end = m.getRange().map(r -> r.end.line).orElse(start);
        int complexity = 1
                + m.findAll(IfStmt.class).size()
                + m.findAll(ForStmt.class).size()
                + m.findAll(ForEachStmt.class).size()
                + m.findAll(WhileStmt.class).size()
                + m.findAll(DoStmt.class).size()
                + m.findAll(CatchClause.class).size()
                + m.findAll(SwitchEntry.class).size()
                + m.findAll(ConditionalExpr.class).size();
        for (BinaryExpr b : m.findAll(BinaryExpr.class)) {
            if (b.getOperator() == BinaryExpr.Operator.AND || b.getOperator() == BinaryExpr.Operator.OR) complexity++;
        }
        Set<String> calls = new LinkedHashSet<>();
        for (MethodCallExpr call : m.findAll(MethodCallExpr.class)) {
            if (call.getScope().isEmpty() || calls.size() >= 200) continue;
            Expression scope = call.getScope().get();
            if (scope instanceof NameExpr n) {
                calls.add(n.getNameAsString() + "." + call.getNameAsString());
            } else if (scope instanceof FieldAccessExpr fa && fa.getScope() instanceof ThisExpr) {
                calls.add(fa.getNameAsString() + "." + call.getNameAsString());
            }
        }
        return new MethodInfo(m.getNameAsString(), m.getType().asString(), m.getParameters().size(), start, end,
                complexity, new ArrayList<>(calls));
    }

    /** First string value of value=/path= (or the single member) of a mapping annotation. */
    private static String extractPath(AnnotationExpr a) {
        Expression value = null;
        if (a instanceof SingleMemberAnnotationExpr sm) {
            value = sm.getMemberValue();
        } else if (a instanceof NormalAnnotationExpr n) {
            for (MemberValuePair p : n.getPairs()) {
                String key = p.getNameAsString();
                if (key.equals("value") || key.equals("path")) value = p.getValue();
            }
        }
        if (value instanceof ArrayInitializerExpr arr && !arr.getValues().isEmpty()) value = arr.getValues().get(0);
        if (value instanceof StringLiteralExpr s) return s.asString();
        return "";
    }

    private static String joinPath(String base, String sub) {
        String b = base == null ? "" : base;
        String s = sub == null ? "" : sub;
        String joined = (b + "/" + s).replaceAll("/+", "/");
        if (joined.length() > 1 && joined.endsWith("/")) joined = joined.substring(0, joined.length() - 1);
        return joined.startsWith("/") ? joined : "/" + joined;
    }

    static int countLoc(String content) {
        int n = 0;
        for (String line : content.split("\n")) if (!line.isBlank()) n++;
        return n;
    }
}
