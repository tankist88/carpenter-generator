package org.carpenter.generator.command;

import org.carpenter.core.dto.unit.method.MethodBaseInfo;
import org.carpenter.core.property.GenerationProperties;
import org.carpenter.core.property.GenerationPropertiesFactory;
import org.carpenter.generator.dto.source.MethodLine;
import org.carpenter.generator.dto.source.MethodSource;
import org.carpenter.generator.dto.source.Variable;
import org.carpenter.generator.dto.unit.ClassExtInfo;
import org.carpenter.generator.dto.unit.field.FieldExtInfo;
import org.carpenter.generator.dto.unit.imports.ImportInfo;
import org.carpenter.generator.dto.unit.method.MethodExtInfo;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.write;
import static org.carpenter.core.property.AbstractGenerationProperties.TAB;
import static org.carpenter.generator.TestGenerator.GENERATED_TEST_CLASS_POSTFIX;
import static org.carpenter.generator.command.CreateTestMethodCommand.TEST_ANNOTATION;
import static org.carpenter.generator.util.GenerateUtil.createAndReturnPathName;
import static org.object2source.util.GenerationUtil.getLastClassShort;
import static org.object2source.util.GenerationUtil.getPackage;

public class CreateJavaClassesCommand extends AbstractCommand {
    public static final String PROVIDER_POSTFIX = "_Provider";
    public static final String DATA_PROVIDER_ANNOTATION = "@DataProvider";
    public static final String DATA_PROVIDER_PARAMETER = "dataProvider";

    private GenerationProperties props;
    private Map<String, Set<ClassExtInfo>> collectedTests;

    public CreateJavaClassesCommand(Map<String, Set<ClassExtInfo>> collectedTests) {
        this.props = GenerationPropertiesFactory.loadProps();
        this.collectedTests = collectedTests;
    }

    @Override
    public void execute() {
        try {
            saveJavaClassesAndPackages();
        } catch (IOException ioex) {
            throw new IllegalStateException(ioex);
        }
    }

    private void saveJavaClassesAndPackages() throws IOException {
        String pathname = createAndReturnPathName(props);
        String dataProviderClassPattern = props.getDataProviderClassPattern();
        for(String fullClassName : collectedTests.keySet()) {
            String className = getLastClassShort(fullClassName);
            String packageName = getPackage(fullClassName);
            String packageFileStruct = pathname + "/" + packageName.replaceAll("\\.", "/");
            forceMkdir(new File(packageFileStruct));
            Set<ClassExtInfo> units = collectedTests.get(fullClassName);
            List<ClassExtInfo> groupList = new ArrayList<>(units);
            Collections.sort(groupList, new Comparator<ClassExtInfo>() {
                @Override
                public int compare(ClassExtInfo o1, ClassExtInfo o2) {
                    return o1.getUnitName().compareTo(o2.getUnitName());
                }
            });
            if(units.size() != groupList.size()) {
                throw new RuntimeException("Error while grouping units!");
            }
            StringBuilder classBuilder = new StringBuilder();
            classBuilder.append("package ").append(packageName).append(";\n\n");

            if(!fullClassName.startsWith(dataProviderClassPattern)) {
                classBuilder.append("import org.testng.annotations.*;\n\n");
                classBuilder.append("import org.mockito.ArgumentMatchers;\n");
                classBuilder.append("import org.mockito.InjectMocks;\n");
                classBuilder.append("import org.mockito.invocation.InvocationOnMock;\n");
                classBuilder.append("import org.mockito.stubbing.Answer;\n");
                classBuilder.append("import org.mockito.Spy;\n");
                classBuilder.append("import org.mockito.Mock;\n\n");
                classBuilder.append("import static org.testng.Assert.*;\n\n");
                classBuilder.append("import static org.mockito.ArgumentMatchers.*;\n");
                classBuilder.append("import static org.mockito.Mockito.*;\n");
                classBuilder.append("import static org.mockito.MockitoAnnotations.initMocks;\n\n");
            }

            classBuilder.append("import javax.annotation.Generated;\n\n");

            for(ClassExtInfo unit : groupList) {
                if(unit instanceof ImportInfo) {
                    classBuilder.append(unit.getBody());
                }
            }
            classBuilder.append("\n");

            String postfix = fullClassName.startsWith(dataProviderClassPattern) ? "" : GENERATED_TEST_CLASS_POSTFIX;

            classBuilder.append("@Generated(value = \"org.carpenter.generator.TestGenerator\")\n");
            classBuilder.append("public class ").append(className).append(postfix).append(" {\n\n");

            for(ClassExtInfo unit : groupList) {
                if(unit instanceof FieldExtInfo) {
                    classBuilder.append(unit.getBody()).append("\n");
                }
            }
            for(MethodExtInfo unit : createDataProviders(extractMethods(groupList))) {
                classBuilder.append(unit.getBody()).append("\n");
            }

            classBuilder.append("}");

            File utClass = new File(packageFileStruct + "/" + className + postfix + ".java");
            write(utClass, classBuilder.toString());
        }
    }

    private Set<MethodExtInfo> extractMethods(List<ClassExtInfo> groupList) {
        Set<MethodExtInfo> methods = new HashSet<>();
        for(ClassExtInfo unit : groupList) {
            if(unit instanceof MethodExtInfo) {
                methods.add((MethodExtInfo) unit);
            }
        }
        return methods;
    }

    private Set<MethodSource> createCommonMethods(List<MethodExtInfo> allMethods) {
        Set<MethodExtInfo> duplicateMethods = new HashSet<>();
        Set<MethodSource> checkMethodSet = new HashSet<>();
        for (MethodExtInfo extInfo : allMethods) {
            if (!checkMethodSet.add(extInfo.createMethodSource())) {
                duplicateMethods.add(extInfo);
            }
        }
        Set<MethodSource> commonMethods = new HashSet<>();
        for (MethodExtInfo extInfo : duplicateMethods) {
            if (!extInfo.isTestMethod()) continue;
            MethodSource methodSource = extInfo.createMethodSource();

            StringBuilder argDefBuilder = new StringBuilder();
            argDefBuilder.append("(");
            int i = 0;
            for (MethodLine line : methodSource.getLines()) {
                for (Variable var : line.getVariables()) {
                    String argDef = "arg" + i;
                    var.setValue(argDef);
                    if (var.isArray()) {
                        argDefBuilder.append("final ");
                    }
                    argDefBuilder.append(var.getType()).append(" ").append(argDef).append(", ");
                    i++;
                }
            }
            if (i == 0) continue;
            argDefBuilder.delete(argDefBuilder.length() - 2, argDefBuilder.length());
            argDefBuilder.append(")");

            methodSource.setUnitName(extInfo.createCommonMethodName() + argDefBuilder.toString());
            String definition = methodSource.getTestMethodDefinition();
            definition = definition.replace(extInfo.getUnitName(), methodSource.getUnitName());
            String providerName = extInfo.createCommonMethodName() + PROVIDER_POSTFIX;
            definition = definition.replace(TEST_ANNOTATION, TEST_ANNOTATION + "(" + DATA_PROVIDER_PARAMETER + " = \"" + providerName + "\")");
            methodSource.setTestMethodDefinition(definition);
            commonMethods.add(methodSource);
        }
        return commonMethods;
    }

    private List<MethodExtInfo> createDataProviders(Set<MethodExtInfo> allMethods) {
        List<MethodExtInfo> allMethodsSortedList = new ArrayList<>(allMethods);
        Collections.sort(allMethodsSortedList, new Comparator<MethodExtInfo>() {
            @Override
            public int compare(MethodExtInfo o1, MethodExtInfo o2) {
                return o1.getUnitName().compareTo(o2.getUnitName());
            }
        });

        Set<MethodSource> commonMethods = createCommonMethods(allMethodsSortedList);
        List<MethodSource> commonMethodsSortedList = new ArrayList<>(commonMethods);
        Collections.sort(commonMethodsSortedList, new Comparator<MethodSource>() {
            @Override
            public int compare(MethodSource o1, MethodSource o2) {
                return o1.getUnitName().compareTo(o2.getUnitName());
            }
        });

        Set<MethodExtInfo> resultSet = new HashSet<>();

        Map<MethodBaseInfo, List<MethodExtInfo>> groupingMap = new HashMap<>();
        for (MethodExtInfo extInfo : allMethodsSortedList) {
            if (commonMethodsSortedList.contains(extInfo.createMethodSource())) {
                MethodBaseInfo key = new MethodBaseInfo(extInfo.getClassName(), extInfo.createCommonMethodName());
                List<MethodExtInfo> methodGroup = groupingMap.get(key);
                if (methodGroup == null) {
                    methodGroup = new ArrayList<>();
                    groupingMap.put(key, methodGroup);
                }
                methodGroup.add(extInfo);
            } else {
                resultSet.add(extInfo);
            }
        }
        for (Map.Entry<MethodBaseInfo, List<MethodExtInfo>> entry : groupingMap.entrySet()) {
            StringBuilder dataProviderBuilder = new StringBuilder();
            String methodName = entry.getKey().getUnitName() + PROVIDER_POSTFIX + "()";
            dataProviderBuilder
                    .append(TAB + DATA_PROVIDER_ANNOTATION +"\n")
                    .append(TAB + "public Object[][] ")
                    .append(methodName).append(" throws Exception {\n")
                    .append(TAB + TAB + "return new Object[][] {\n");
            Collections.sort(entry.getValue(), new Comparator<MethodExtInfo>() {
                @Override
                public int compare(MethodExtInfo o1, MethodExtInfo o2) {
                    return o1.getUnitName().compareTo(o2.getUnitName());
                }
            });
            Iterator<MethodExtInfo> iterator = entry.getValue().iterator();
            while (iterator.hasNext()) {
                MethodExtInfo extInfo = iterator.next();
                MethodSource methodSource = extInfo.createMethodSource();
                dataProviderBuilder.append(TAB + TAB + TAB + "{ ");
                for (MethodLine line : methodSource.getLines()) {
                    for (Variable var : line.getVariables()) {
                        dataProviderBuilder.append(var.getValue()).append(", ");
                    }
                }
                dataProviderBuilder.delete(dataProviderBuilder.length() - 2, dataProviderBuilder.length()).append(" }");
                if (iterator.hasNext()) dataProviderBuilder.append(", ");
                dataProviderBuilder.append("\n");
            }
            dataProviderBuilder.append(TAB + TAB + "};\n").append(TAB + "}\n");
            resultSet.add(new MethodExtInfo(entry.getKey().getClassName(), methodName, dataProviderBuilder.toString()));
        }
        for (MethodSource methodSource : commonMethodsSortedList) {
            Map<String, String> varChangeMap = new HashMap<>();
            List<MethodLine> removeLines = new ArrayList<>();
            for (MethodLine line : methodSource.getLines()) {
                for (Variable var : line.getVariables()) {
                    if (var.getName() != null) {
                        varChangeMap.put(var.getName(), var.getValue());
                        removeLines.add(line);
                    }
                }
            }
            for (MethodLine rl : removeLines) {
                methodSource.getLines().remove(rl);
            }
            for (MethodLine line : methodSource.getLines()) {
                String expression = line.getExpression();
                for (Map.Entry<String, String> entry : varChangeMap.entrySet()) {
                    expression = expression.replace(entry.getKey(), entry.getValue());
                }
                line.setExpression(expression);
            }
            resultSet.add(methodSource.createMethodExtInfo());
        }

        int allIndexCounter = 1;
        int commonIndexCounter = 5000;
        int arrayProvIndexCounter = 10000;
        int dataProvIndexCounter = 15000;
        for (MethodExtInfo extInfo : resultSet) {
            if (extInfo.isInitMethod()) {
                extInfo.setIndex(0);
            } else if (extInfo.isArrayProvider()) {
                extInfo.setIndex(arrayProvIndexCounter);
                arrayProvIndexCounter++;
            } else if (extInfo.isDataProvider()) {
                extInfo.setIndex(dataProvIndexCounter);
                dataProvIndexCounter++;
            } else if (extInfo.isCommonMethod()) {
                extInfo.setIndex(commonIndexCounter);
                commonIndexCounter++;
            } else {
                extInfo.setIndex(allIndexCounter);
                allIndexCounter++;
            }
        }

        List<MethodExtInfo> result = new ArrayList<>(resultSet);
        Collections.sort(result, new Comparator<MethodExtInfo>() {
            @Override
            public int compare(MethodExtInfo o1, MethodExtInfo o2) {
                return o1.getIndex() - o2.getIndex();
            }
        });
        return result;
    }
}
