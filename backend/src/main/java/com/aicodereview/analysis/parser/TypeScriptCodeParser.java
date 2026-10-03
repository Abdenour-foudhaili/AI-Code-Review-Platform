package com.aicodereview.analysis.parser;

import com.aicodereview.analysis.model.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight structural parser for TypeScript / JavaScript (Angular aware): decorators, classes, interfaces, enums,
 * constructor / inject() dependencies, methods (with approximate complexity), HTTP API paths.
 */
@Component
public class TypeScriptCodeParser implements CodeParser {

    private static final Pattern IMPORT = Pattern.compile("import\\s+(?:type\\s+)?(?:[\\w*\\s{},$]+?)\\s+from\\s+['\"]([^'\"]+)['\"]");
    private static final Pattern DECLARATION = Pattern.compile(
            "(?m)^[ \\t]*(?:export\\s+)?(?:default\\s+)?(?:abstract\\s+)?(class|interface|enum)\\s+(\\w+)(?:<[^>{]*>)?"
                    + "(?:\\s+extends\\s+([\\w.]+)(?:<[^>{]*>)?)?(?:\\s+implements\\s+([\\w.,\\s<>]+?))?\\s*\\{");
    private static final Pattern DECORATOR = Pattern.compile("@(Component|Injectable|Directive|Pipe|NgModule)\\s*\\(");
    private static final Pattern CTOR = Pattern.compile("constructor\\s*\\(([^)]*)\\)");
    private static final Pattern CTOR_PARAM = Pattern.compile("(?:private|public|protected|readonly)?\\s*\\w+\\??\\s*:\\s*([A-Za-z_][\\w.]*)");
    private static final Pattern INJECT = Pattern.compile("\\binject\\(\\s*([A-Z]\\w*)");
    private static final Pattern METHOD = Pattern.compile(
            "(?m)^[ \\t]+(?:(?:public|private|protected|static|async|override|readonly)\\s+)*(\\w+)\\s*\\(([^)]*)\\)\\s*(?::\\s*([^{;=]+?))?\\s*\\{");
    private static final Pattern THIS_CALL = Pattern.compile("this\\.(\\w+)\\.(\\w+)\\s*\\(");
    private static final Pattern API_LITERAL = Pattern.compile("['\"`]([^'\"`]*?/api/[^'\"`]*)['\"`]");
    private static final Pattern API_URL_VAR = Pattern.compile("apiUrl\\}?(/[A-Za-z0-9_\\-/{}$.]*)");
    private static final Pattern COMPLEXITY = Pattern.compile("\\bif\\b|\\bfor\\b|\\bwhile\\b|\\bcase\\b|\\bcatch\\b|&&|\\|\\||\\?(?![.?:])");
    private static final Set<String> KEYWORDS = Set.of("if", "for", "while", "switch", "catch", "constructor", "function", "return", "else", "super");

    @Override
    public boolean supports(String path) {
        return path.endsWith(".ts") || path.endsWith(".tsx") || path.endsWith(".js") || path.endsWith(".jsx") || path.endsWith(".mjs");
    }

    @Override
    public ParsedFile parse(String path, String content) {
        String language = path.endsWith(".ts") || path.endsWith(".tsx") ? "TypeScript" : "JavaScript";
        List<String> imports = new ArrayList<>();
        Matcher im = IMPORT.matcher(content);
        while (im.find()) imports.add(im.group(1));

        List<String> apiPaths = new ArrayList<>();
        Matcher am = API_LITERAL.matcher(content);
        while (am.find() && apiPaths.size() < 50) apiPaths.add(normalizeApi(am.group(1).substring(am.group(1).indexOf("/api/"))));
        Matcher av = API_URL_VAR.matcher(content);
        while (av.find() && apiPaths.size() < 50) apiPaths.add(normalizeApi("/api" + av.group(1)));

        List<CodeElement> elements = new ArrayList<>();
        Matcher dm = DECLARATION.matcher(content);
        int previousEnd = 0;
        while (dm.find()) {
            String kind = dm.group(1).toUpperCase(Locale.ROOT);
            String name = dm.group(2);
            int open = dm.end() - 1;
            int close = matchBrace(content, open);
            String body = content.substring(open, Math.min(close + 1, content.length()));
            int startLine = lineOf(content, dm.start());
            int endLine = lineOf(content, Math.min(close, content.length() - 1));

            List<String> annotations = new ArrayList<>();
            String window = content.substring(Math.max(previousEnd, Math.max(0, dm.start() - 2500)), dm.start());
            Matcher dec = DECORATOR.matcher(window);
            String lastDecorator = null;
            while (dec.find()) lastDecorator = dec.group(1);
            if (lastDecorator != null) annotations.add(lastDecorator);

            List<String> ext = new ArrayList<>();
            List<String> impl = new ArrayList<>();
            if (dm.group(3) != null) ext.add(simple(dm.group(3)));
            if (dm.group(4) != null) {
                for (String s : dm.group(4).split(",")) {
                    String t = simple(s.replaceAll("<.*", "").trim());
                    if (!t.isEmpty()) impl.add(t);
                }
            }

            List<String> ctorTypes = new ArrayList<>();
            Matcher cm = CTOR.matcher(body);
            if (cm.find()) {
                Matcher pm = CTOR_PARAM.matcher(cm.group(1));
                while (pm.find()) ctorTypes.add(simple(pm.group(1)));
            }
            Matcher injm = INJECT.matcher(body);
            while (injm.find()) ctorTypes.add(injm.group(1));

            List<MethodInfo> methods = "CLASS".equals(kind) ? parseMethods(content, body, open) : List.of();
            elements.add(new CodeElement(name, stripExt(path) + "#" + name, kind, path, folder(path), startLine, endLine,
                    annotations, ext, impl, List.of(), ctorTypes, methods, "", List.of()));
            previousEnd = close > dm.start() ? close : dm.end();
            dm.region(Math.max(dm.end(), previousEnd), content.length());
        }
        return new ParsedFile(path, language, folder(path), imports, elements, apiPaths, JavaCodeParser.countLoc(content));
    }

    private List<MethodInfo> parseMethods(String whole, String classBody, int classOffset) {
        List<MethodInfo> methods = new ArrayList<>();
        Matcher mm = METHOD.matcher(classBody);
        while (mm.find()) {
            String name = mm.group(1);
            if (KEYWORDS.contains(name)) continue;
            int open = mm.end() - 1;
            int close = matchBrace(classBody, open);
            String body = classBody.substring(open, Math.min(close + 1, classBody.length()));
            int start = lineOf(whole, classOffset + mm.start());
            int end = lineOf(whole, classOffset + Math.min(close, classBody.length() - 1));
            int params = mm.group(2).isBlank() ? 0 : mm.group(2).split(",").length;
            int complexity = 1;
            Matcher cx = COMPLEXITY.matcher(body);
            while (cx.find()) complexity++;
            Set<String> calls = new LinkedHashSet<>();
            Matcher tc = THIS_CALL.matcher(body);
            while (tc.find() && calls.size() < 200) calls.add(tc.group(1) + "." + tc.group(2));
            methods.add(new MethodInfo(name, mm.group(3) == null ? "" : mm.group(3).trim(), params, start, end, complexity, new ArrayList<>(calls)));
            mm.region(Math.min(close + 1, classBody.length()), classBody.length());
        }
        return methods;
    }

    private static int matchBrace(String s, int open) {
        int depth = 0;
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return i;
        }
        return s.length() - 1;
    }

    private static int lineOf(String s, int index) {
        int line = 1;
        for (int i = 0; i < index && i < s.length(); i++) if (s.charAt(i) == '\n') line++;
        return line;
    }

    private static String normalizeApi(String p) {
        String q = p.replaceAll("\\$\\{[^}]*}", "{}").replaceAll("\\?.*$", "");
        return q.length() > 1 && q.endsWith("/") ? q.substring(0, q.length() - 1) : q;
    }

    private static String simple(String t) {
        int i = t.lastIndexOf('.');
        return i >= 0 ? t.substring(i + 1) : t;
    }

    private static String stripExt(String path) {
        int i = path.lastIndexOf('.');
        return i > 0 ? path.substring(0, i) : path;
    }

    static String folder(String path) {
        int i = path.lastIndexOf('/');
        return i >= 0 ? path.substring(0, i) : "";
    }
}
