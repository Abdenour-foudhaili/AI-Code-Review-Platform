package com.aicodereview.analysis.model;

import java.util.List;

/** A type declared in a source file (class, interface, enum, record, Angular component/service...). */
public record CodeElement(String name, String qualifiedName, String kind, String file, String packageName,
                          int startLine, int endLine, List<String> annotations, List<String> extendsTypes,
                          List<String> implementsTypes, List<FieldInfo> fields, List<String> constructorTypes,
                          List<MethodInfo> methods, String basePath, List<String> endpoints) {}
