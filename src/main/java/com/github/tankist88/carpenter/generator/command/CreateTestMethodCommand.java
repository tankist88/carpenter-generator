package com.github.tankist88.carpenter.generator.command;

import com.github.tankist88.carpenter.core.dto.argument.GeneratedArgument;
import com.github.tankist88.carpenter.core.dto.unit.field.FieldProperties;
import com.github.tankist88.carpenter.core.dto.unit.method.MethodBaseInfo;
import com.github.tankist88.carpenter.core.dto.unit.method.MethodCallInfo;
import com.github.tankist88.carpenter.core.property.GenerationProperties;
import com.github.tankist88.carpenter.core.property.GenerationPropertiesFactory;
import com.github.tankist88.carpenter.generator.dto.PreparedMock;
import com.github.tankist88.carpenter.generator.dto.ProviderNextPartInfo;
import com.github.tankist88.carpenter.generator.dto.SeparatedInners;
import com.github.tankist88.carpenter.generator.dto.SpyMaps;
import com.github.tankist88.carpenter.generator.dto.unit.ClassExtInfo;
import com.github.tankist88.carpenter.generator.dto.unit.imports.ImportInfo;
import com.github.tankist88.carpenter.generator.dto.unit.method.MethodExtInfo;
import com.github.tankist88.carpenter.generator.extension.assertext.AssertExtension;
import com.github.tankist88.object2source.dto.ProviderInfo;
import com.github.tankist88.object2source.dto.ProviderResult;

import java.lang.reflect.Modifier;
import java.util.*;

import static com.github.tankist88.carpenter.core.property.AbstractGenerationProperties.COMMON_UTIL_POSTFIX;
import static com.github.tankist88.carpenter.core.property.AbstractGenerationProperties.TAB;
import static com.github.tankist88.carpenter.generator.TestGenerator.TEST_INST_VAR_NAME;
import static com.github.tankist88.carpenter.generator.TestGenerator.isCreateMockFields;
import static com.github.tankist88.carpenter.generator.command.CreateMockFieldCommand.CREATE_INST_METHOD;
import static com.github.tankist88.carpenter.generator.util.ConvertUtil.toMethodExtInfo;
import static com.github.tankist88.carpenter.generator.util.GenerateUtil.*;
import static com.github.tankist88.carpenter.generator.util.TypeHelper.*;
import static com.github.tankist88.object2source.util.AssigmentUtil.VAR_NAME_PLACEHOLDER;
import static com.github.tankist88.object2source.util.GenerationUtil.*;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class CreateTestMethodCommand extends AbstractReturnClassInfoCommand<ClassExtInfo> {
    private static final int DATA_PROVIDER_MAX_LENGTH_IN_METHODS = 40;

    public static final String TEST_ANNOTATION = "@Test";
    public static final String HASH_CODE_SEPARATOR = "_";
    public static final String TEST_METHOD_PREFIX = "test";
    public static final String ARRAY_PROVIDER_PREFIX = "getArrProv";

    private static final String RESULT_VAR = "result";
    private static final String CONTROL_VAR = "control";
    private static final String SPY_VAR_NAME = "spyObj";

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

    public CreateTestMethodCommand(
            MethodCallInfo callInfo,
            Map<String, Set<String>> providerSignatureMap,
            Map<String, Set<ClassExtInfo>> dataProviders,
            List<AssertExtension> assertExtensions
    ) {
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
        this.testClassHierarchy = createTestClassHierarchy(callInfo);
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

        builder.append(TAB + TEST_ANNOTATION + "\n")
               .append(TAB + "public void ").append(testMethodName).append(" throws Exception {\n");

        List<PreparedMock> mocks = createMocks(callInfo, createServiceFields(callInfo));

        if (!isCreateMockFields()) {
            appendTestInstance();
        }
        if (props.isFillTestClassInstance()) {
            appendInitMethod();
        }
        appendMocks(mocks);
        appendTestCall();
        appendMethodCallVerification(mocks);
        appendResultCheckAssert();

        builder.append(TAB + "}\n");

        methods = singletonList(new MethodExtInfo(callInfo.getClassName(), testMethodName, builder.toString()));
    }

    private void appendInitMethod() {
        GeneratedArgument arg = callInfo.getTargetObj();
        if (arg != null && arg.getGenerated() != null && !arg.getGenerated().getEndPoint().isEmpty()) {
            builder.append(TAB + TAB).append(createDataProvider(callInfo.getTargetObj())).append(";\n");
        }
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
            builder.append(createVariableAssigment(callInfo.getReturnArg(), CONTROL_VAR));
            builder.append(assertExtension.getAssertBlock(RESULT_VAR, CONTROL_VAR));
        }
    }

    private void appendMocks(List<PreparedMock> mocks) {
        for (PreparedMock mock : mocks) {
            if (isBlank(mock.getMock())) continue;
            builder.append(mock.getMock());
        }
    }

    private void appendMethodCallVerification(List<PreparedMock> mocks) {
        for (PreparedMock mock : mocks) {
            if (isBlank(mock.getVerify())) continue;
            builder.append(mock.getVerify());
        }
    }

    private String createVariableAssigment(GeneratedArgument generatedArgument, String varName) {
        return createVariableAssigment(generatedArgument, varName, false);
    }
    
    private String createVariableAssigment(GeneratedArgument generatedArgument, String varName, boolean spy) {
        String varType = generatedArgument.getNearestInstantAbleClass();
        if(!isPrimitive(varType) && !isWrapper(varType) && !varType.equals(String.class.getName())) {
            imports.add(createImportInfo(varType, callInfo.getClassName()));
        }
        StringBuilder result = new StringBuilder();
        result  .append(TAB + TAB)
                .append(getClassShort(typeOfGenArg(generatedArgument)))
                .append(" ").append(varName).append(" = ");
        if (spy) {
            result.append("spy(");
        }
        result.append(createDataProvider(generatedArgument));
        if (spy) {
            result.append(")");
        }
        result.append(";\n");
        return result.toString();
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
                imports.add(createImportInfo(varType, callInfo.getClassName()));
            }
            builder.append(getLastClassShort(typeOfGenArg(callInfo.getReturnArg()))).append(" " + RESULT_VAR + " = ");
        }
        if(Modifier.isStatic(callInfo.getMethodModifiers())) {
            builder.append(getClassShort(callInfo.getClassName())).append(".");
        } else {
            builder.append(TEST_INST_VAR_NAME + ".");
        }
        builder.append(callInfo.getUnitName()).append("(").append(argBuilder.toString()).append(");\n");
    }

    private List<PreparedMock> createMocks(MethodCallInfo callInfo, Set<FieldProperties> serviceClasses) {
        Set<PreparedMock> allMocks = new HashSet<>();
        if (!Modifier.isStatic(callInfo.getMethodModifiers())) {
            SeparatedInners separatedInners = separateInners(callInfo.getInnerMethods(), serviceClasses);
            Set<FieldProperties> fieldProperties;
            if (isCreateMockFields()) {
                fieldProperties = serviceClasses;
            } else {
                Set<MethodCallInfo> allMethods = new HashSet<>(separatedInners.getSingleInners());
                for (Set<MethodCallInfo> multiInner : separatedInners.getMultipleInners()) {
                    allMethods.addAll(multiInner);
                }
                fieldProperties = createServiceFields(allMethods, serviceClasses);
                allMocks.addAll(createMockClasses(fieldProperties));
            }
            SpyMaps spyMaps = createSpyMap(separatedInners);
            for (MethodCallInfo inner : separatedInners.getSingleInners()) {
                PreparedMock mock = createSingleMock(inner, fieldProperties, spyMaps);
                if (mock != null) allMocks.add(mock);
            }
            for (Set<MethodCallInfo> multiInner : separatedInners.getMultipleInners()) {
                PreparedMock mock = createMultipleMock(multiInner, fieldProperties, spyMaps);
                if (mock != null) allMocks.add(mock);
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

    private void appendTestInstance() {
        builder .append(TAB + TAB)
                .append(getLastClassShort(callInfo.getClassName()))
                .append(" ")
                .append(TEST_INST_VAR_NAME)
                .append(" = spy(");
        if (!callInfo.isClassHasZeroArgConstructor()) {
            String createInstMethod = props.getDataProviderClassPattern() + COMMON_UTIL_POSTFIX + "." + CREATE_INST_METHOD;
            imports.add(createImportInfo(createInstMethod, callInfo.getClassName(), true));
            builder.append("createInstance(").append(callInfo.getClassName()).append(".class)");
        } else {
            builder.append("new ").append(getLastClassShort(callInfo.getClassName())).append("()");
        }
        builder.append(");\n");
    }

    private Set<PreparedMock> createMockClasses(Set<FieldProperties> serviceClasses) {
        Set<PreparedMock> result = new HashSet<>();
        for (FieldProperties f : serviceClasses) {
            boolean testClass = f.getClassName().equals(callInfo.getClassName());
            
            if (testClass) continue;
            
            StringBuilder mockBuilder = new StringBuilder();
            String varType = f.getClassName();
            if(!isPrimitive(varType) && !isWrapper(varType) && !varType.equals(String.class.getName())) {
                imports.add(createImportInfo(varType, callInfo.getClassName()));
            }
            String depInjectMethod = props.getDataProviderClassPattern() + COMMON_UTIL_POSTFIX + ".notPublicAssignment";
            imports.add(createImportInfo(depInjectMethod, callInfo.getClassName(), true));
            
            mockBuilder
                    .append(TAB + TAB)
                    .append(getLastClassShort(f.getClassName()))
                    .append(" ")
                    .append(f.getUnitName())
                    .append(" = mock(")
                    .append(getLastClassShort(f.getClassName()))
                    .append(".class);\n")
                    .append(TAB + TAB)
                    .append("notPublicAssignment(")
                    .append(TEST_INST_VAR_NAME).append(", ")
                    .append("\"").append(f.getUnitName()).append("\"").append(", ")
                    .append(f.getUnitName())
                    .append(");\n");
            result.add(new PreparedMock(mockBuilder.toString(), null));
        }
        return result;
    }
    
    private SpyMaps createSpyMap(SeparatedInners separatedInners) {
        SpyMaps result = new SpyMaps();
        List<MethodCallInfo> allMethods = new ArrayList<>(separatedInners.getSingleInners());
        for (Set<MethodCallInfo> multiInner : separatedInners.getMultipleInners()) {
            allMethods.addAll(multiInner);
        }
        Collections.sort(allMethods, new Comparator<MethodCallInfo>() {
            @Override
            public int compare(MethodCallInfo o1, MethodCallInfo o2) {
                return (o1.getCallTime() > o2.getCallTime()) ? 1 : -1;
            }
        });
        int varCounter = 0;
        for (MethodCallInfo inner : allMethods) {
            for (MethodCallInfo current : allMethods) {
                if (inner.getReturnArg() == null || inner.getReturnArg().getGenerated() == null) continue;
                String returnType = inner.getReturnArg().getClassName();
                if (
                        inner.getReturnArg().getClassName().equals(current.getClassName()) ||
                        current.getClassHierarchy().contains(returnType) ||
                        current.getInterfacesHierarchy().contains(returnType)
                ) {
                    result.getReturnSpyMap().put(inner, SPY_VAR_NAME + varCounter);
                    result.getTargetSpyMap().put(current, SPY_VAR_NAME + varCounter);
                }
            }
            varCounter++;
        }
        return result;
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

    private String getMockVarName(MethodCallInfo inner, Set<FieldProperties> serviceClasses) {
        boolean testClass = inner.getClassName().equals(callInfo.getClassName());
        if (testClass) {
            return TEST_INST_VAR_NAME;
        } else {
           return createMockVarName(inner, serviceClasses);
        }
    }

    private PreparedMock createMultipleMock(Set<MethodCallInfo> innerSet, Set<FieldProperties> serviceClasses, SpyMaps spyMaps) {
        MethodCallInfo innerFirst = innerSet.iterator().next();

        if (skipMock(innerFirst, serviceClasses, testClassHierarchy) || forwardMock(innerFirst, testClassHierarchy)) return null;

        // TODO Implement using spyMaps.getReturnSpyMap() for spy() return values in array provider
        String varName;
        if (spyMaps.getTargetSpyMap().containsKey(innerFirst)) {
            varName = spyMaps.getTargetSpyMap().get(innerFirst);
        } else {
            varName = getMockVarName(innerFirst, serviceClasses);
        }
        String retType = innerFirst.getReturnArg().getClassName();
        if (!isPrimitive(retType) && !isWrapper(retType) && !retType.equals(String.class.getName())) {
            imports.add(createImportInfo(retType, callInfo.getClassName()));
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

        appendMockArguments(mockBuilder, innerFirst, imports);
        mockBuilder.append(";\n");
        return new PreparedMock(mockBuilder.toString(), getVerificationBlock(innerFirst, varName, serviceClasses));
    }

    private PreparedMock createSingleMock(MethodCallInfo inner, Set<FieldProperties> serviceClasses, SpyMaps spyMaps) {
        if (skipMock(inner, serviceClasses, testClassHierarchy) || forwardMock(inner, testClassHierarchy)) return null;

        boolean voidMethod = inner.isVoidMethod();
        StringBuilder mockBuilder = new StringBuilder();
        if (voidMethod) {
            mockBuilder.append(TAB + TAB + "doNothing().when(");
        } else {
            String dpVar;
            if (spyMaps.getReturnSpyMap().containsKey(inner)) {
                dpVar = spyMaps.getReturnSpyMap().get(inner);
                mockBuilder.append(createVariableAssigment(inner.getReturnArg(), dpVar, true));
            } else {
                dpVar = createDataProvider(inner.getReturnArg());
            }
            mockBuilder.append(TAB + TAB + "doReturn(").append(dpVar).append(").when(");
        }
        String varName;
        if (spyMaps.getTargetSpyMap().containsKey(inner)) {
            varName = spyMaps.getTargetSpyMap().get(inner);
        } else {
            varName = getMockVarName(inner, serviceClasses);
        }
        mockBuilder.append(varName).append(").").append(inner.getUnitName());
        appendMockArguments(mockBuilder, inner, imports);
        mockBuilder.append(";\n");
        return new PreparedMock(mockBuilder.toString(), getVerificationBlock(inner, varName, serviceClasses));
    }
    
    private String getVerificationBlock(MethodCallInfo inner, String varName, Set<FieldProperties> serviceClasses) {
        if (determineVarName(inner, serviceClasses) == null) return null;
        StringBuilder verifyBuilder = new StringBuilder();
        verifyBuilder.append(TAB + TAB + "verify(");
        verifyBuilder.append(varName).append(", atLeastOnce()).").append(inner.getUnitName());
        appendMockArguments(verifyBuilder, inner, imports);
        verifyBuilder.append(";\n");
        return verifyBuilder.toString();
    }

    private void appendMockArguments(StringBuilder sb, MethodCallInfo inner, Set<ImportInfo> imports) {
        sb.append("(");
        Iterator<GeneratedArgument> iterator = inner.getArguments().iterator();
        while (iterator.hasNext()) {
            GeneratedArgument arg = iterator.next();
            if (arg.getGenerated() != null && arg.getGenericString() != null && arg.getInterfacesHierarchy().contains("java.util.List")) {
                sb.append("ArgumentMatchers.<").append(getClassShort(arg.getGenericString())).append(">anyList()");
                if(!isPrimitive(arg.getGenericString()) && !isWrapper(arg.getGenericString()) && !arg.getGenericString().equals(String.class.getName())) {
                    imports.add(createImportInfo(arg.getGenericString(), callInfo.getClassName()));
                }
            } else if (arg.getGenerated() != null && arg.getGenericString() != null && arg.getInterfacesHierarchy().contains("java.util.Set")) {
                sb.append("ArgumentMatchers.<").append(getClassShort(arg.getGenericString())).append(">anySet()");
                if(!isPrimitive(arg.getGenericString()) && !isWrapper(arg.getGenericString()) && !arg.getGenericString().equals(String.class.getName())) {
                    imports.add(createImportInfo(arg.getGenericString(), callInfo.getClassName()));
                }
            } else {
                String clearedType = getClearedClassName(arg.getNearestInstantAbleClass());
                sb.append("nullable(").append(getClassShort(clearedType)).append(".class").append(")");
                if(!isPrimitive(clearedType) && !isWrapper(clearedType) && !clearedType.equals(String.class.getName())) {
                    imports.add(createImportInfo(clearedType, callInfo.getClassName()));
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
                String importClass = dp.getClassName()+ "." + dp.getUnitName().substring(0, dp.getUnitName().indexOf("("));
                imports.add(createImportInfo(importClass, callInfo.getClassName(), true));
            }
        } else {
            String dpType = arg.getNearestInstantAbleClass();
            result = "(" + getClassShort(convertPrimitiveToWrapper(dpType)) + ") null";
            if(!isPrimitive(dpType) && !isWrapper(dpType) && !dpType.equals(String.class.getName())) {
                imports.add(createImportInfo(dpType, callInfo.getClassName()));
            }
        }
        return result;
    }
    
    private String createInnerKey(MethodCallInfo inner) {
        return  inner.getClassName() + "-" + 
                inner.getUnitName() + "-" + 
                inner.getMethodModifiers() + "-" + 
                inner.getArguments().size();
    }
    
    private Map<String, Set<MethodCallInfo>> createSetOfSetInners(Set<MethodCallInfo> inners, Set<FieldProperties> serviceClasses) {
        Map<String, Set<MethodCallInfo>> innerMap = new HashMap<>();
        for(MethodCallInfo inner : inners) {
            if (skipMock(inner, serviceClasses, testClassHierarchy)) continue;
            for(MethodCallInfo current : inners) {
                if (createInnerKey(current).equals(createInnerKey(inner))) {
                    Set<MethodCallInfo> innerSet = innerMap.get(createInnerKey(current));
                    if (innerSet == null) {
                        innerSet = new HashSet<>();
                        innerMap.put(createInnerKey(current), innerSet);
                    }
                    innerSet.add(current);
                }
            }
            if (forwardMock(inner, testClassHierarchy)) {
                Map<String, Set<MethodCallInfo>> recMap = createSetOfSetInners(inner.getInnerMethods(), serviceClasses);
                for (Map.Entry<String, Set<MethodCallInfo>> entry : recMap.entrySet()) {
                    Set<MethodCallInfo> innerSet = innerMap.get(entry.getKey());
                    if (innerSet != null) {
                        innerSet.addAll(entry.getValue());
                    } else {
                        innerMap.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        return innerMap;
    }

    private SeparatedInners separateInners(Set<MethodCallInfo> inners, Set<FieldProperties> serviceClasses) {
        SeparatedInners result = new SeparatedInners();
        for (Set<MethodCallInfo> set : createSetOfSetInners(inners, serviceClasses).values()) {
            if (set.size() > 1) {
                boolean allReturnNulls = true;
                for (MethodCallInfo m : set) {
                    if (m.getReturnArg().getGenerated() != null) {
                        allReturnNulls = false;
                        break;
                    }
                }
                boolean allReturnEquals = true;
                ProviderResult prevProvider = set.iterator().next().getReturnArg().getGenerated();
                for (MethodCallInfo m : set) {
                    if (m.getReturnArg().getGenerated() != null && !m.getReturnArg().getGenerated().equals(prevProvider)) {
                        allReturnEquals = false;
                        break;
                    } else {
                        prevProvider = m.getReturnArg().getGenerated();
                    }
                }
                MethodCallInfo mc = set.iterator().next();
                if (mc.isVoidMethod() || allReturnNulls || allReturnEquals) {
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
        String methodName = providerResult.getEndPoint().getMethodName().replace(VAR_NAME_PLACEHOLDER, TEST_INST_VAR_NAME);
        String existsClassName = getExistsProviderClassName(methodName);
        if(existsClassName != null) {
            return new MethodBaseInfo(existsClassName, methodName);
        } else {
            ProviderNextPartInfo dataProviderInfo = getNextProviderClassName();
            Set<String> methodsSig = this.providerSignatureMap.get(dataProviderInfo.getClassName());
            if (methodsSig == null) {
                methodsSig = new HashSet<>();
                this.providerSignatureMap.put(dataProviderInfo.getClassName(), methodsSig);
            }
            for (ProviderInfo provider : providerResult.getProviders()) {
                String currProviderName = provider.getMethodName().replace(VAR_NAME_PLACEHOLDER, TEST_INST_VAR_NAME);
                if (!methodsSig.contains(currProviderName)) {
                    dataProviderInfo.getMethods().add(toMethodExtInfo(dataProviderInfo.getClassName(), provider));
                    methodsSig.add(currProviderName);
                }
            }
            return new MethodBaseInfo(dataProviderInfo.getClassName(), methodName);
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
