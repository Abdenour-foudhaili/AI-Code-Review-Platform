package com.aicodereview.analysis.parser;

import com.aicodereview.analysis.model.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Minimal Python structure extraction (imports, classes, methods by indentation). */
@Component
public class PythonCodeParser implements CodeParser {
    private static final Pattern IMPORT = Pattern.compile("(?m)^(?:from\\s+([\\w.]+)\\s+import|import\\s+([\\w.]+))");
    private static final Pattern CLASS = Pattern.compile("(?m)^class\\s+(\\w+)\\s*(?:\\(([^)]*)\\))?\\s*:");
    private static final Pattern DEF = Pattern.compile("(?m)^([ \\t]*)(?:async\\s+)?def\\s+(\\w+)\\s*\\(([^)]*)\\)");
    private static final Pattern BRANCH = Pattern.compile("\\b(if|elif|for|while|except|and|or)\\b");

    @Override
    public boolean supports(String path) { return path.endsWith(".py"); }

    @Override
    public ParsedFile parse(String path, String content) {
        String[] lines = content.split("\n", -1);
        List<String> imports = new ArrayList<>();
        Matcher im = IMPORT.matcher(content);
        while (im.find()) imports.add(im.group(1) != null ? im.group(1) : im.group(2));

        List<MethodInfo> topLevel = new ArrayList<>();
        List<CodeElement> elements = new ArrayList<>();
        Matcher cm = CLASS.matcher(content);
        List<int[]> classRanges = new ArrayList<>();
        List<String[]> classMeta = new ArrayList<>();
        while (cm.find()) {
            int startLine = lineIndex(content, cm.start());
            int endLine = blockEnd(lines, startLine, 0);
            classRanges.add(new int[]{startLine, endLine});
            classMeta.add(new String[]{cm.group(1), cm.group(2) == null ? "" : cm.group(2)});
        }
        Matcher dm = DEF.matcher(content);
        Map<Integer, List<MethodInfo>> methodsByClass = new HashMap<>();
        while (dm.find()) {
            int startLine = lineIndex(content, dm.start());
            int indent = dm.group(1).length();
            int endLine = blockEnd(lines, startLine, indent);
            StringBuilder body = new StringBuilder();
            for (int i = startLine; i <= endLine && i < lines.length; i++) body.append(lines[i]).append('\n');
            int complexity = 1;
            Matcher bm = BRANCH.matcher(body);
            while (bm.find()) complexity++;
            String params = dm.group(3).trim();
            MethodInfo mi = new MethodInfo(dm.group(2), "", params.isEmpty() ? 0 : params.split(",").length,
                    startLine + 1, endLine + 1, complexity, List.of());
            int owner = -1;
            for (int c = 0; c < classRanges.size(); c++) {
                if (indent > 0 && startLine > classRanges.get(c)[0] && startLine <= classRanges.get(c)[1]) owner = c;
            }
            if (owner >= 0) methodsByClass.computeIfAbsent(owner, k -> new ArrayList<>()).add(mi);
            else if (indent == 0) topLevel.add(mi);
        }
        String folder = TypeScriptCodeParser.folder(path);
        for (int c = 0; c < classMeta.size(); c++) {
            List<String> ext = new ArrayList<>();
            for (String b : classMeta.get(c)[1].split(",")) {
                String t = b.trim();
                if (!t.isEmpty() && !t.contains("=")) ext.add(t.substring(t.lastIndexOf('.') + 1));
            }
            elements.add(new CodeElement(classMeta.get(c)[0], path + "#" + classMeta.get(c)[0], "CLASS", path, folder,
                    classRanges.get(c)[0] + 1, classRanges.get(c)[1] + 1, List.of(), ext, List.of(), List.of(), List.of(),
                    methodsByClass.getOrDefault(c, List.of()), "", List.of()));
        }
        if (!topLevel.isEmpty()) {
            String module = path.substring(path.lastIndexOf('/') + 1).replace(".py", "");
            elements.add(new CodeElement(module, path + "#module", "MODULE", path, folder, 1, lines.length, List.of(),
                    List.of(), List.of(), List.of(), List.of(), topLevel, "", List.of()));
        }
        return new ParsedFile(path, "Python", folder, imports, elements, List.of(), JavaCodeParser.countLoc(content));
    }

    private static int lineIndex(String s, int index) {
        int n = 0;
        for (int i = 0; i < index; i++) if (s.charAt(i) == '\n') n++;
        return n;
    }

    private static int blockEnd(String[] lines, int startLine, int indent) {
        int end = startLine;
        for (int i = startLine + 1; i < lines.length; i++) {
            String l = lines[i];
            if (l.isBlank()) continue;
            int ind = 0;
            while (ind < l.length() && (l.charAt(ind) == ' ' || l.charAt(ind) == '\t')) ind++;
            if (ind <= indent) break;
            end = i;
        }
        return end;
    }
}
