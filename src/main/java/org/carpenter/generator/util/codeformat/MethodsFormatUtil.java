package org.carpenter.generator.util.codeformat;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.carpenter.core.dto.unit.method.MethodBaseInfo;
import org.carpenter.generator.dto.source.MethodLine;
import org.carpenter.generator.dto.source.MethodSource;
import org.carpenter.generator.dto.source.Variable;
import org.carpenter.generator.dto.unit.ClassExtInfo;
import org.carpenter.generator.dto.unit.method.MethodExtInfo;

import java.util.*;

import static org.carpenter.core.property.AbstractGenerationProperties.TAB;
import static org.carpenter.generator.command.CreateJavaClassesCommand.DATA_PROVIDER_ANNOTATION;
import static org.carpenter.generator.command.CreateJavaClassesCommand.DATA_PROVIDER_PARAMETER;
import static org.carpenter.generator.command.CreateJavaClassesCommand.PROVIDER_POSTFIX;
import static org.carpenter.generator.command.CreateTestMethodCommand.TEST_ANNOTATION;

public class MethodsFormatUtil {
    public static Set<MethodExtInfo> extractMethods(List<ClassExtInfo> groupList) {
        Set<MethodExtInfo> methods = new HashSet<>();
        for (ClassExtInfo unit : groupList) {
            if (unit instanceof MethodExtInfo) {
                methods.add((MethodExtInfo) unit);
            }
        }
        return methods;
    }

    private static Set<MethodSource> createCommonMethods(List<MethodExtInfo> allMethods) {
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

    public static List<MethodExtInfo> createDataProviders(Set<MethodExtInfo> allMethods) {
        List<MethodExtInfo> allMethodsSortedList = new ArrayList<>(allMethods);
        Collections.sort(allMethodsSortedList, new Comparator<MethodExtInfo>() {
            @Override
            public int compare(MethodExtInfo o1, MethodExtInfo o2) {
                return o1.getUnitName().compareTo(o2.getUnitName());
            }
        });
        return createDataProviders(allMethodsSortedList);
    }

    private static List<MethodExtInfo> createDataProviders(List<MethodExtInfo> allMethodsSortedList) {
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
            List<Pair<String, String>> varChangeList = new ArrayList<>();
            List<MethodLine> removeLines = new ArrayList<>();
            for (MethodLine line : methodSource.getLines()) {
                for (Variable var : line.getVariables()) {
                    if (var.getName() != null) {
                        varChangeList.add(new MutablePair<>(var.getName(), var.getValue()));
                        removeLines.add(line);
                    }
                }
            }
            for (MethodLine rl : removeLines) {
                methodSource.getLines().remove(rl);
            }
            for (MethodLine line : methodSource.getLines()) {
                String expression = line.getExpression();
                for (Pair<String, String> pair : varChangeList) {
                    expression = expression.replace(pair.getKey() + ",", pair.getValue() + ",");
                    expression = expression.replace(pair.getKey() + ")", pair.getValue() + ")");
                    expression = expression.replace(pair.getKey() + "[", pair.getValue() + "[");
                    expression = expression.replace(pair.getKey() + ".", pair.getValue() + ".");
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
