package com.github.tankist88.carpenter.generator.command;

import com.github.tankist88.carpenter.core.dto.argument.GeneratedArgument;
import com.github.tankist88.carpenter.core.dto.unit.field.FieldProperties;
import com.github.tankist88.carpenter.core.dto.unit.method.MethodBaseInfo;
import com.github.tankist88.carpenter.core.dto.unit.method.MethodCallInfo;
import com.github.tankist88.carpenter.core.property.GenerationProperties;
import com.github.tankist88.carpenter.core.property.GenerationPropertiesFactory;
import com.github.tankist88.carpenter.generator.TestGenerator;
import com.github.tankist88.carpenter.generator.dto.PreparedMock;
import com.github.tankist88.carpenter.generator.dto.ProviderNextPartInfo;
import com.github.tankist88.carpenter.generator.dto.SeparatedInners;
import com.github.tankist88.carpenter.generator.dto.unit.ClassExtInfo;
import com.github.tankist88.carpenter.generator.dto.unit.imports.ImportInfo;
import com.github.tankist88.carpenter.generator.dto.unit.method.MethodExtInfo;
import com.github.tankist88.carpenter.generator.extension.assertext.AssertExtension;
import com.github.tankist88.carpenter.generator.util.ConvertUtil;
import com.github.tankist88.carpenter.generator.util.TypeHelper;
import com.github.tankist88.object2source.dto.ProviderInfo;
import com.github.tankist88.object2source.dto.ProviderResult;

import java.lang.reflect.Modifier;
import java.util.*;

import static com.github.tankist88.carpenter.core.property.AbstractGenerationProperties.COMMON_UTIL_POSTFIX;
import static com.github.tankist88.carpenter.core.property.AbstractGenerationProperties.TAB;
import static com.github.tankist88.object2source.util.GenerationUtil.*;
import static java.util.Collections.singletonList;

public class CreateTestMethodCommand extends AbstractReturnClassInfoCommand<ClassExtInfo> {
    private static final int DATA_PROVIDER_MAX_LENGTH_IN_METHODS = 40;

    public static final String TEST_ANNOTATION = "@Test";
    public static final String HASH_CODE_SEPARATOR = "_";
    public static final String TEST_METHOD_PREFIX = "test";
    public static final String ARRAY_PROVIDER_PREFIX = "getArrProv";

    private StringBuilder builder;

    private List<AssertExtension> assertExtensions;

    private MethodCallInfo callInfo;
    private Map<String, Set<String>> providerSignatureMap;
    private GenerationProperties props;

    private List<MethodExtInfo> methods;
    private Map<String, Set<ClassExtInfo>> dataProviders;
    private Set<ImportInfo> imports;

    private Set<MethodExtInfo> arrayProviders;

    private Set<FieldProperties> testClassHierarchy;

    public CreateTestMethodCommand(MethodCallInfo callInfo, Map<String, Set<String>> providerSignatureMap, Map<String, Set<ClassExtInfo>> dataProviders, List<AssertExtension> assertExtensions) {
        this.callInfo = callInfo;
        this.providerSignatureMap = providerSignatureMap;
        this.props = GenerationPropertiesFactory.loadProps();
        this.builder = new StringBuilder();
        this.imports = new HashSet<>();
        this.dataProviders = dataProviders;
        if(assertExtensions != null) {
            this.assertExtensions = assertExtensions;
        } else {
            this.assertExtensions = new ArrayList<>();
        }
        this.arrayProviders = new HashSet<>();
        this.testClassHierarchy = new HashSet<>();
    }

    @Override
    public void execute() {
        super.execute();
        createTestMethod();
    }

    @Override
    public List<ClassExtInfo> returnResult() {
        List<ClassExtInfo> result = new ArrayList<>();
        result.addAll(methods);
        result.addAll(arrayProviders);
        result.addAll(imports);
        for(Set<ClassExtInfo> set : dataProviders.values()) {
            result.addAll(set);
        }
        return result;
    }

    private void createTestMethod() {
        String testMethodName = (
                TEST_METHOD_PREFIX + upFirst(callInfo.getUnitName()) +
                HASH_CODE_SEPARATOR + callInfo.getArguments().hashCode() + "()"
                ).replaceAll("-", "_");

        FieldProperties testProp = new FieldProperties(callInfo.getClassName(), TestGenerator.TEST_INST_VAR_NAME);
        testProp.setClassHierarchy(callInfo.getClassHierarchy());
        testProp.setInterfacesHierarchy(callInfo.getInterfacesHierarchy());

        Set<FieldProperties> serviceClasses = new HashSet<>();
        serviceClasses.add(testProp);
        serviceClasses.addAll(callInfo.getServiceFields());

        testClassHierarchy.add(testProp);

        builder.append(TAB + TEST_ANNOTATION + "\n")
               .append(TAB + "public void ").append(testMethodName).append(" throws Exception {\n");

        List<PreparedMock> mocks = createMocks(callInfo, serviceClasses, testClassHierarchy);

        appendInitMethod();
        appendMocks(mocks);
        appendTestCall();
        appendMethodCallVerification(mocks);
        appendResultCheckAssert();

        builder.append(TAB + "}\n");

        methods = singletonList(new MethodExtInfo(callInfo.getClassName(), testMethodName, builder.toString()));
    }

    private void appendInitMethod() {
        // TODO Implement init method from com.github.tankist88.carpenter.core.dto.unit.method.MethodCallInfo.targetObj
    }

    private AssertExtension findAssertExtension(MethodCallInfo callInfo) {
        AssertExtension assertExtension = null;
        for (AssertExtension ext : assertExtensions) {
            if (ext.isTypeSupported(callInfo.getReturnArg())) {
                assertExtension = ext;
                break;
            }
        }
        return assertExtension;
    }

    private void appendResultCheckAssert() {
        AssertExtension assertExtension = findAssertExtension(callInfo);
        if (assertExtension != null) {
            String varName = "control";
            builder.append(createVariableAssigment(callInfo.getReturnArg(), varName));
            builder.append(assertExtension.getAssertBlock(varName));
        }
    }

    private void appendMocks(List<PreparedMock> mocks) {
        for (PreparedMock mock : mocks) {
            builder.append(mock.getMock());
        }
    }

    private void appendMethodCallVerification(List<PreparedMock> mocks) {
        for (PreparedMock mock : mocks) {
            builder.append(mock.getVerify());
        }
    }

    private String createVariableAssigment(GeneratedArgument generatedArgument, String varName) {
        String varType = generatedArgument.getNearestInstantAbleClass();
        if(!isPrimitive(varType) && !isWrapper(varType) && !varType.equals(String.class.getName())) {
            imports.add(TypeHelper.createImportInfo(varType, callInfo.getClassName()));
        }
        return TAB + TAB + getLastClassShort(TypeHelper.typeOfGenArg(generatedArgument)) + " " + varName + " = " + createDataProvider(generatedArgument) + ";\n";
    }

    private void appendTestCall() {
        int i = 0;
        StringBuilder argBuilder = new StringBuilder();
        StringBuilder providerBuilder = new StringBuilder();
        Iterator<GeneratedArgument> iterator = callInfo.getArguments().iterator();
        while (iterator.hasNext()) {
            String argName = "_arg" + i;
            providerBuilder.append(createVariableAssigment(iterator.next(), argName));
            argBuilder.append(argName);
            if(iterator.hasNext()) argBuilder.append(", ");
            i++;
        }
        builder.append(providerBuilder.toString());
        builder.append(TAB + TAB);
        if (findAssertExtension(callInfo) != null) {
            String varType = callInfo.getReturnArg().getNearestInstantAbleClass();
            if(!isPrimitive(varType) && !isWrapper(varType) && !varType.equals(String.class.getName())) {
                imports.add(TypeHelper.createImportInfo(varType, callInfo.getClassName()));
            }
            builder.append(getLastClassShort(TypeHelper.typeOfGenArg(callInfo.getReturnArg()))).append(" result = ");
        }
        if(Modifier.isStatic(callInfo.getMethodModifiers())) {
            builder.append(getClassShort(callInfo.getClassName())).append(".");
        } else {
            builder.append(TestGenerator.TEST_INST_VAR_NAME + ".");
        }
        builder.append(callInfo.getUnitName()).append("(").append(argBuilder.toString()).append(");\n");
    }

    private List<PreparedMock> createMocks(MethodCallInfo callInfo, Set<FieldProperties> serviceClasses, Set<FieldProperties> testClassHierarchy) {
        Set<PreparedMock> allMocks = new HashSet<>();
        if(!Modifier.isStatic(callInfo.getMethodModifiers())) {
            SeparatedInners separatedInners = separateInners(callInfo.getInnerMethods());
            for (MethodCallInfo inner : separatedInners.getSingleInners()) {
                Set<PreparedMock> mocks = createSingleMock(inner, serviceClasses, testClassHierarchy);
                if(mocks != null) allMocks.addAll(mocks);
            }
            for (Set<MethodCallInfo> multiInner : separatedInners.getMultipleInners()) {
                Set<PreparedMock> mocks = createMultipleMock(multiInner, serviceClasses, testClassHierarchy);
                if(mocks != null) allMocks.addAll(mocks);
            }
        }
        List<PreparedMock> resultList = new ArrayList<>(allMocks);
        Collections.sort(resultList, new Comparator<PreparedMock>() {
            @Override
            public int compare(PreparedMock o1, PreparedMock o2) {
                return o1.getMock().compareTo(o2.getMock());
            }
        });
        return resultList;
    }

    private boolean skipMock(MethodCallInfo callInfo, Set<FieldProperties> serviceClasses) {
        boolean staticMethod = Modifier.isStatic(callInfo.getMethodModifiers());
        return (staticMethod || (!TypeHelper.isSameTypes(callInfo, serviceClasses) && !TypeHelper.isSameTypes(callInfo, testClassHierarchy)));
    }

    private String createArrayProvider(Set<MethodCallInfo> innerSet) {
        MethodCallInfo innerFirst = innerSet.iterator().next();
        String retType = innerFirst.getReturnArg().getClassName();

        List<MethodCallInfo> methodCallInfoList = new ArrayList<>(innerSet);
        Collections.sort(methodCallInfoList, new Comparator<MethodCallInfo>() {
            @Override
            public int compare(MethodCallInfo o1, MethodCallInfo o2) {
                return (o1.getCallTime() > o2.getCallTime()) ? 1 : -1;
            }
        });
        StringBuilder bodyBuilder = new StringBuilder();
        Iterator<MethodCallInfo> methodCallInfoIterator = methodCallInfoList.iterator();
        while(methodCallInfoIterator.hasNext()) {
            MethodCallInfo m = methodCallInfoIterator.next();
            bodyBuilder.append(TAB + TAB + TAB).append(createDataProvider(m.getReturnArg()));
            if(methodCallInfoIterator.hasNext()) bodyBuilder.append(",");
            bodyBuilder.append("\n");
        }

        String hashCodeStr = String.valueOf(bodyBuilder.toString().hashCode()).replace("-", "_");
        String unitName = ARRAY_PROVIDER_PREFIX + getClassShort(retType) + "_" + hashCodeStr + "()";
        String arrayProvider =
                TAB + "private " + getClassShort(retType) + "[] " + unitName + " throws Exception {\n" +
                 TAB + TAB + getClassShort(retType) + "[] values = {\n" + bodyBuilder.toString() +
                TAB + TAB + "};\n" + TAB + TAB + "return values;\n" + TAB + "}\n";

        arrayProviders.add(new MethodExtInfo(callInfo.getClassName(), unitName, arrayProvider));
        return unitName;
    }

    private Set<PreparedMock> createMultipleMock(Set<MethodCallInfo> innerSet, Set<FieldProperties> serviceClasses, Set<FieldProperties> testClassHierarchy) {
        MethodCallInfo innerFirst = innerSet.iterator().next();

        if (skipMock(innerFirst, serviceClasses)) return null;

        Set<PreparedMock> mocks = new HashSet<>();

        boolean sameTypeWithTest = TypeHelper.isSameTypes(innerFirst, testClassHierarchy);
        boolean voidMethod = innerFirst.isVoidMethod();
        boolean privateMethod = Modifier.isPrivate(innerFirst.getMethodModifiers());
        boolean protectedMethod = Modifier.isProtected(innerFirst.getMethodModifiers());
        boolean anonymousClass = getLastClassShort(innerFirst.getClassName()).matches("\\d+");
        if ((voidMethod && sameTypeWithTest) || privateMethod || protectedMethod || anonymousClass) {
            List<PreparedMock> innerMocks = createMocks(innerFirst, serviceClasses, testClassHierarchy);
            if(innerMocks != null) {
                mocks.addAll(innerMocks);
            }
        } else {
            boolean testClass = innerFirst.getClassName().equals(callInfo.getClassName());
            String varName = testClass ? TestGenerator.TEST_INST_VAR_NAME : TypeHelper.determineVarName(innerFirst, serviceClasses);

            String retType = innerFirst.getReturnArg().getClassName();
            if (!isPrimitive(retType) && !isWrapper(retType) && !retType.equals(String.class.getName())) {
                imports.add(TypeHelper.createImportInfo(retType, callInfo.getClassName()));
            }

            String retShortType = getClassShort(retType);
            String arrVarName = "values" + retShortType;
            StringBuilder mockBuilder = new StringBuilder();
            mockBuilder.append(TAB + TAB + "doAnswer(new Answer() {\n" + TAB + "\n")
                    .append(TAB + TAB + TAB + "private int count = 0;\n" + TAB + "\n")
                    .append(TAB + TAB + TAB + "private ")
                    .append(retShortType).append("[] ").append(arrVarName).append(" = ").append(createArrayProvider(innerSet)).append(";\n")
                    .append(TAB + TAB + TAB + "@Override\n")
                    .append(TAB + TAB + TAB + "public Object answer(InvocationOnMock invocationOnMock) throws Throwable {\n")
                    .append(TAB + TAB + TAB + TAB).append(retShortType).append(" result = ").append(arrVarName).append("[count];\n")
                    .append(TAB + TAB + TAB + TAB + "if (count + 1 < ").append(arrVarName).append(".length)\n")
                    .append(TAB + TAB + TAB + TAB + TAB + "count++;\n")
                    .append(TAB + TAB + TAB + TAB + "return result;\n")
                    .append(TAB + TAB + TAB + "}\n")
                    .append(TAB + TAB + "}).when(").append(varName).append(").").append(innerFirst.getUnitName());

            StringBuilder verifyBuilder = new StringBuilder();
            verifyBuilder.append(TAB + TAB + "verify(");
            verifyBuilder.append(varName).append(", atLeastOnce()).").append(innerFirst.getUnitName());
            appendMockArguments(mockBuilder, innerFirst, imports);
            appendMockArguments(verifyBuilder, innerFirst, imports);
            mockBuilder.append(";\n");
            verifyBuilder.append(";\n");
            mocks.add(new PreparedMock(mockBuilder.toString(), verifyBuilder.toString()));
        }
        return mocks;
    }

    private Set<PreparedMock> createSingleMock(MethodCallInfo inner, Set<FieldProperties> serviceClasses, Set<FieldProperties> testClassHierarchy) {
        if (skipMock(inner, serviceClasses)) return null;

        Set<PreparedMock> mocks = new HashSet<>();

        boolean sameTypeWithTest = TypeHelper.isSameTypes(inner, testClassHierarchy);
        boolean voidMethod = inner.isVoidMethod();
        boolean privateMethod = Modifier.isPrivate(inner.getMethodModifiers());
        boolean protectedMethod = Modifier.isProtected(inner.getMethodModifiers());
        boolean anonymousClass = getLastClassShort(inner.getClassName()).matches("\\d+");
        if ((voidMethod && sameTypeWithTest) || privateMethod || protectedMethod || anonymousClass) {
            List<PreparedMock> innerMocks = createMocks(inner, serviceClasses, testClassHierarchy);
            if(innerMocks != null) {
                mocks.addAll(innerMocks);
            }
        } else {
            boolean testClass = inner.getClassName().equals(callInfo.getClassName());
            String varName = testClass ? TestGenerator.TEST_INST_VAR_NAME : TypeHelper.determineVarName(inner, serviceClasses);
            StringBuilder mockBuilder = new StringBuilder();
            StringBuilder verifyBuilder = new StringBuilder();
            verifyBuilder.append(TAB + TAB + "verify(");
            if (voidMethod) {
                mockBuilder.append(TAB + TAB + "doNothing().when(");
            } else {
                mockBuilder.append(TAB + TAB + "doReturn(")
                           .append(createDataProvider(inner.getReturnArg()))
                           .append(").when(");
            }
            mockBuilder.append(varName).append(").").append(inner.getUnitName());
            verifyBuilder.append(varName).append(", atLeastOnce()).").append(inner.getUnitName());
            appendMockArguments(mockBuilder, inner, imports);
            appendMockArguments(verifyBuilder, inner, imports);
            mockBuilder.append(";\n");
            verifyBuilder.append(";\n");
            mocks.add(new PreparedMock(mockBuilder.toString(), verifyBuilder.toString()));
        }
        return mocks;
    }

    private void appendMockArguments(StringBuilder sb, MethodCallInfo inner, Set<ImportInfo> imports) {
        sb.append("(");
        Iterator<GeneratedArgument> iterator = inner.getArguments().iterator();
        while (iterator.hasNext()) {
            GeneratedArgument arg = iterator.next();
            if (arg.getGenerated() != null && arg.getGenericString() != null && arg.getInterfacesHierarchy().contains("java.util.List")) {
                sb.append("ArgumentMatchers.<").append(getLastClassShort(arg.getGenericString())).append(">anyList()");
                if(!isPrimitive(arg.getGenericString()) && !isWrapper(arg.getGenericString()) && !arg.getGenericString().equals(String.class.getName())) {
                    imports.add(TypeHelper.createImportInfo(arg.getGenericString(), callInfo.getClassName()));
                }
            } else if (arg.getGenerated() != null && arg.getGenericString() != null && arg.getInterfacesHierarchy().contains("java.util.Set")) {
                sb.append("ArgumentMatchers.<").append(getLastClassShort(arg.getGenericString())).append(">anySet()");
                if(!isPrimitive(arg.getGenericString()) && !isWrapper(arg.getGenericString()) && !arg.getGenericString().equals(String.class.getName())) {
                    imports.add(TypeHelper.createImportInfo(arg.getGenericString(), callInfo.getClassName()));
                }
            } else {
                String clearedType = getClearedClassName(arg.getNearestInstantAbleClass());
                sb.append("nullable(").append(getLastClassShort(clearedType)).append(".class").append(")");
                if(!isPrimitive(clearedType) && !isWrapper(clearedType) && !clearedType.equals(String.class.getName())) {
                    imports.add(TypeHelper.createImportInfo(clearedType, callInfo.getClassName()));
                }
            }
            if (iterator.hasNext()) sb.append(", ");
        }
        sb.append(")");
    }

    private String createDataProvider(GeneratedArgument arg) {
        String result;
        ProviderResult providerResult = arg.getGenerated();
        if (providerResult != null) {
            MethodBaseInfo dp = getProviderNameAndUpdateState(providerResult);
            result = dp.getUnitName();
            if(!isPrimitive(dp.getClassName()) && !isWrapper(dp.getClassName()) && !dp.getClassName().equals(String.class.getName())) {
                String importClass = dp.getClassName()+ "." + (dp.getUnitName()).replace("()", "");
                imports.add(TypeHelper.createImportInfo(importClass, callInfo.getClassName(), true));
            }
        } else {
            String dpType = arg.getNearestInstantAbleClass();
            result = "(" + getLastClassShort(dpType) + ") null";
            if(!isPrimitive(dpType) && !isWrapper(dpType) && !dpType.equals(String.class.getName())) {
                imports.add(TypeHelper.createImportInfo(dpType, callInfo.getClassName()));
            }
        }
        return result;
    }

    private SeparatedInners separateInners(Set<MethodCallInfo> inners) {
        SeparatedInners result = new SeparatedInners();
        Set<Set<MethodCallInfo>> tmpMultipleInners = new HashSet<>();
        for(MethodCallInfo inner : inners) {
            Set<MethodCallInfo> innerSet = new HashSet<>();
            for(MethodCallInfo current : inners) {
                if( inner.getClassName().equals(current.getClassName())
                    && inner.getUnitName().equals(current.getUnitName())
                    && inner.getMethodModifiers() == current.getMethodModifiers()
                    && inner.getArguments().size() == current.getArguments().size())
                {
                    innerSet.add(current);
                }
            }
            tmpMultipleInners.add(innerSet);
        }
        for(Set<MethodCallInfo> set : tmpMultipleInners) {
            if(set.size() > 1) {
                boolean allReturnNulls = true;
                for(MethodCallInfo m : set) {
                    if(m.getReturnArg().getGenerated() != null) {
                        allReturnNulls = false;
                        break;
                    }
                }
                boolean allReturnEquals = true;
                ProviderResult prevProvider = set.iterator().next().getReturnArg().getGenerated();
                for(MethodCallInfo m : set) {
                    if (m.getReturnArg().getGenerated() != null && !m.getReturnArg().getGenerated().equals(prevProvider)) {
                        allReturnEquals = false;
                        break;
                    } else {
                        prevProvider = m.getReturnArg().getGenerated();
                    }
                }
                MethodCallInfo mc = set.iterator().next();
                if(mc.isVoidMethod() || allReturnNulls || allReturnEquals) {
                    result.getSingleInners().add(mc);
                } else {
                    result.getMultipleInners().add(set);
                }
            } else {
                result.getSingleInners().add(set.iterator().next());
            }
        }
        return result;
    }

    private MethodBaseInfo getProviderNameAndUpdateState(ProviderResult providerResult) {
        String existsClassName = getExistsProviderClassName(providerResult.getEndPoint().getMethodName());
        if(existsClassName != null) {
            return new MethodBaseInfo(existsClassName, providerResult.getEndPoint().getMethodName());
        } else {
            ProviderNextPartInfo dataProviderInfo = getNextProviderClassName();
            Set<String> methodsSig = this.providerSignatureMap.get(dataProviderInfo.getClassName());
            if (methodsSig == null) {
                methodsSig = new HashSet<>();
                this.providerSignatureMap.put(dataProviderInfo.getClassName(), methodsSig);
            }
            for (ProviderInfo provider : providerResult.getProviders()) {
                if (!methodsSig.contains(provider.getMethodName())) {
                    dataProviderInfo.getMethods().add(ConvertUtil.toMethodExtInfo(dataProviderInfo.getClassName(), provider));
                    methodsSig.add(provider.getMethodName());
                }
            }
            return new MethodBaseInfo(dataProviderInfo.getClassName(), providerResult.getEndPoint().getMethodName());
        }
    }

    private ProviderNextPartInfo getNextProviderClassName() {
        ProviderNextPartInfo result = new ProviderNextPartInfo();
        int dataProviderCount = providerSignatureMap.size();
        String dataProviderClassName =
                this.props.getDataProviderClassPattern() +
                String.valueOf(dataProviderCount);
        Set<ClassExtInfo> dataProviderMethods = this.dataProviders.get(dataProviderClassName);
        if(dataProviderMethods == null || dataProviderMethods.size() >= DATA_PROVIDER_MAX_LENGTH_IN_METHODS) {
            dataProviderMethods = new HashSet<>();
            dataProviderClassName =
                    this.props.getDataProviderClassPattern() +
                    String.valueOf(dataProviderCount + 1);
            this.dataProviders.put(dataProviderClassName, dataProviderMethods);
        }
        result.setClassName(dataProviderClassName);
        result.setMethods(dataProviderMethods);
        return result;
    }

    private String getExistsProviderClassName(String currentMethodName) {
        String methodClass = null;
        List<Map.Entry<String, Set<String>>> entryList = new ArrayList<>(providerSignatureMap.entrySet());
        Collections.sort(entryList, new Comparator<Map.Entry<String, Set<String>>>() {
            @Override
            public int compare(Map.Entry<String, Set<String>> o1, Map.Entry<String, Set<String>> o2) {
                int num1 = extractNumber(o1.getKey());
                int num2 = extractNumber(o2.getKey());
                return num1 - num2;
            }
            private int extractNumber(String classname) {
                String dpPattern = props.getDataProviderClassPattern();
                String num = classname.substring(classname.indexOf(dpPattern) + dpPattern.length());
                if (num.equals(COMMON_UTIL_POSTFIX)) {
                    return Integer.MAX_VALUE;
                } else {
                    return Integer.parseInt(num);
                }
            }
        });
        for (Map.Entry<String, Set<String>> entry : entryList) {
            for (String method : entry.getValue()) {
                if (method.equals(currentMethodName)) {
                    methodClass = entry.getKey();
                    break;
                }
            }
            if (methodClass != null) break;
        }
        return methodClass;
    }
}
