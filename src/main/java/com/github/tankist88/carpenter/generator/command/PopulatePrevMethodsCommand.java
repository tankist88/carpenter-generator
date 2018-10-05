package com.github.tankist88.carpenter.generator.command;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.tankist88.carpenter.core.property.GenerationProperties;
import com.github.tankist88.carpenter.core.property.GenerationPropertiesFactory;
import com.github.tankist88.carpenter.generator.TestGenerator;
import com.github.tankist88.carpenter.generator.dto.unit.ClassExtInfo;
import com.github.tankist88.carpenter.generator.dto.unit.field.FieldExtInfo;
import com.github.tankist88.carpenter.generator.dto.unit.imports.ImportInfo;
import com.github.tankist88.carpenter.generator.dto.unit.method.MethodExtInfo;
import com.github.tankist88.carpenter.generator.util.ConvertUtils;
import com.github.tankist88.carpenter.generator.util.GenerateUtils;
import com.github.tankist88.carpenter.generator.util.TypeHelper;
import com.github.tankist88.object2source.dto.ProviderInfo;

import java.io.File;
import java.util.*;

import static com.github.tankist88.carpenter.core.property.AbstractGenerationProperties.COMMON_UTIL_POSTFIX;
import static com.github.tankist88.carpenter.core.property.AbstractGenerationProperties.TAB;
import static com.github.tankist88.object2source.util.AssigmentUtil.getCommonMethods;
import static com.github.tankist88.object2source.util.GenerationUtil.getClassShort;

public class PopulatePrevMethodsCommand extends AbstractReturnClassInfoCommand<ClassExtInfo> {

    private GenerationProperties props;

    private Map<String, Set<String>> providerSignatureMap;
    private Map<String, Set<ClassExtInfo>> existsMethodsMap;

    public PopulatePrevMethodsCommand(Map<String, Set<String>> providerSignatureMap) {
        this.props = GenerationPropertiesFactory.loadProps();
        this.providerSignatureMap = providerSignatureMap != null ? providerSignatureMap : new HashMap<String, Set<String>>();
    }

    @Override
    public void execute() {
        super.execute();
        populateMethodsMaps();
    }

    @Override
    public List<ClassExtInfo> returnResult() {
        List<ClassExtInfo> result = new ArrayList<>();
        for(Set<ClassExtInfo> set : existsMethodsMap.values()) {
            result.addAll(set);
        }
        return result;
    }

    private void populateMethodsMaps() {
        try {
            existsMethodsMap = new HashMap<>();
            String dataProviderClassPattern = props.getDataProviderClassPattern();
            for (File f : GenerateUtils.getFileList(new File(props.getUtGenDir()), "java")) {
                boolean allowedFiles = (f.getName().endsWith(TestGenerator.GENERATED_TEST_CLASS_POSTFIX + ".java") ||
                        f.getName().startsWith(getClassShort(dataProviderClassPattern)));
                if (!allowedFiles) continue;
                CompilationUnit compilationUnit = JavaParser.parse(f);
                TypeDeclaration type = compilationUnit.getTypes().get(0);
                String fullClassName = compilationUnit.getPackage().getName() + "." + type.getName();
                if (fullClassName.contains(TestGenerator.GENERATED_TEST_CLASS_POSTFIX)) {
                    fullClassName = fullClassName.substring(0, fullClassName.indexOf(TestGenerator.GENERATED_TEST_CLASS_POSTFIX));
                }
                Set<ClassExtInfo> units = existsMethodsMap.get(fullClassName);
                if (units == null) {
                    units = new HashSet<>();
                    existsMethodsMap.put(fullClassName, units);
                }

                for (ImportDeclaration imp : compilationUnit.getImports()) {
                    String name = imp.getName().toString();
                    String impClass = imp.isAsterisk() ? name + ".*" : name;
                    ImportInfo importInfo = TypeHelper.createImportInfo(impClass, fullClassName, imp.isStatic());
                    importInfo.setNewUnit(false);
                    units.add(importInfo);
                }

                for (Node node : type.getChildrenNodes()) {
                    if (node instanceof FieldDeclaration) {
                        FieldExtInfo field = new FieldExtInfo();
                        field.setClassName(fullClassName);
                        for (Node v : node.getChildrenNodes()) {
                            if (v instanceof VariableDeclarator) {
                                field.setUnitName(v.toString());
                                break;
                            }
                        }
                        field.setBody(TAB + node.toString()
                                .replace("\r\n", "\n")
                                .replace("\n",  "\n" + TAB) + "\n");
                        field.setNewUnit(false);
                        units.add(field);
                    } else if (node instanceof MethodDeclaration) {
                        MethodExtInfo method = new MethodExtInfo();
                        method.setClassName(fullClassName);
                        StringBuilder paramStrBuilder = new StringBuilder();
                        Iterator<Parameter> iterator = ((MethodDeclaration) node).getParameters().iterator();
                        while (iterator.hasNext()) {
                            paramStrBuilder.append(iterator.next().toString());
                            if (iterator.hasNext()) paramStrBuilder.append(", ");
                        }
                        method.setUnitName(((MethodDeclaration) node).getName() + "(" + paramStrBuilder.toString() + ")");

                        String body = TAB + node.toString()
                                .replace("\r\n", "\n")
                                .replace("\n", "\n" + TAB) + "\n";
                        // TODO Fix format for multi row expressions
                        // START temp code
                        if (method.isArrayProvider()) {
                            body = body.replace("= { ", "= {\n" + TAB + TAB + TAB);
                            body = body.replace("(), ", "(),\n" + TAB + TAB + TAB);
                            body = body.replace("() };", "()\n" + TAB + TAB + "};");
                        } else if (method.isDataProvider()) {
                            body = body.replace("[][] { ", "[][] {\n" + TAB + TAB + TAB);
                            body = body.replace("}, {", "}, \n" + TAB + TAB + TAB + "{");
                            body = body.replace("} };", "}\n" + TAB + TAB + "};");
                        }
                        // END temp code
                        method.setBody(body);
                        method.setNewUnit(false);
                        units.add(method);
                        if (fullClassName.startsWith(dataProviderClassPattern)) {
                            Set<String> providersSignatures = providerSignatureMap.get(fullClassName);
                            if (providersSignatures == null) {
                                providersSignatures = new HashSet<>();
                                providerSignatureMap.put(fullClassName, providersSignatures);
                            }
                            providersSignatures.add(method.getUnitName());
                        }
                    }
                }
            }

            String commonClassName = dataProviderClassPattern + COMMON_UTIL_POSTFIX;
            Set<ClassExtInfo> commonMethods = new HashSet<>();
            for (ProviderInfo p : getCommonMethods(TAB)) {
                commonMethods.add(ConvertUtils.toMethodExtInfo(commonClassName, p));
            }

            Set<ClassExtInfo> existsCommonMethods = existsMethodsMap.get(commonClassName);

            if (existsCommonMethods == null || !methodsEquals(existsCommonMethods, commonMethods)) {
                existsMethodsMap.put(commonClassName, commonMethods);
            }
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private boolean methodsEquals(Set<ClassExtInfo> set1, Set<ClassExtInfo> set2) {
        if (set1 == null && set2 == null) {
            return true;
        } else if (set1 != null && set2 == null) {
            return false;
        } else if (set1 == null) {
            return false;
        } else {
            return containsMethos(set1, set2) && containsMethos(set2, set1);
        }
    }

    private boolean containsMethos(Set<ClassExtInfo> set1, Set<ClassExtInfo> set2) {
        for (ClassExtInfo extInfo1 : set1) {
            if (!(extInfo1 instanceof MethodExtInfo)) continue;
            boolean contains = false;
            String preparedBody1 = getClearedMethodBody(extInfo1.getBody());
            for (ClassExtInfo extInfo2 : set2) {
                String preparedBody2 = getClearedMethodBody(extInfo2.getBody());
                if (preparedBody1.equals(preparedBody2)) {
                    contains = true;
                    break;
                }
            }
            if (!contains) return false;
        }
        return true;
    }

    private String getClearedMethodBody(String body) {
        return body
                .replace(TAB, "")
                .replace(" ", "")
                .replace("\t", "")
                .replace("\n", "")
                .replace("\r", "")
                .trim();
    }
}
