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
import com.github.tankist88.carpenter.generator.service.MockCreator;
import com.github.tankist88.object2source.dto.ProviderInfo;
import com.github.tankist88.object2source.dto.ProviderResult;

import java.util.*;

import static com.github.tankist88.carpenter.core.property.AbstractGenerationProperties.COMMON_UTIL_POSTFIX;
import static com.github.tankist88.carpenter.core.property.AbstractGenerationProperties.TAB;
import static com.github.tankist88.carpenter.generator.TestGenerator.*;
import static com.github.tankist88.carpenter.generator.command.CreateMockFieldCommand.CREATE_INST_METHOD;
import static com.github.tankist88.carpenter.generator.util.ConvertUtils.toMethodExtInfo;
import static com.github.tankist88.carpenter.generator.util.GenerateUtils.*;
import static com.github.tankist88.carpenter.generator.util.MockCreateUtils.createMock;
import static com.github.tankist88.carpenter.generator.util.TypeHelper.*;
import static com.github.tankist88.carpenter.generator.util.classfields.FilterUtils.filterFieldPropByServiceClasses;
import static com.github.tankist88.carpenter.generator.util.classfields.FilterUtils.filterFieldPropBySpyMaps;
import static com.github.tankist88.object2source.util.AssigmentUtil.VAR_NAME_PLACEHOLDER;
import static com.github.tankist88.object2source.util.GenerationUtil.*;
import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class CreateTestMethodCommand extends AbstractReturnClassInfoCommand<ClassExtInfo> {
    private static final int DATA_PROVIDER_MAX_LENGTH_IN_METHODS = 40;

    private static final String GOOD_MOCKITO_VERSION = "2.8.9";

    public static final String TEST_ANNOTATION = "@Test";
    public static final String HASH_CODE_SEPARATOR = "_";
    public static final String TEST_METHOD_PREFIX = "test";
    public static final String ARRAY_PROVIDER_PREFIX = "getArrProv";
    public static final String SPY_VAR_NAME_SEPARATOR = ",";

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
    private Set<String> mockStaticClassNames;

    private Set<MethodExtInfo> arrayProviders;

    private Set<FieldProperties> testClassHierarchy;
    // serviceFields
    private Set<FieldProperties> testClassFields;

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
        this.mockStaticClassNames = new HashSet<>();
        this.testClassFields = createServiceFields(callInfo);
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

        if (callInfo.getUnitName().contains("sendClientNotification")) {
            int a = 2;
        }

        List<PreparedMock> mocks = createMocks();

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

        MethodExtInfo testMethod = new MethodExtInfo(callInfo.getClassName(), testMethodName, builder.toString());
        testMethod.getMockStaticClassNames().addAll(mockStaticClassNames);
        methods = singletonList(testMethod);
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
            builder.append(createVariableAssignment(callInfo.getReturnArg(), CONTROL_VAR));
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

    private String createVariableAssignment(GeneratedArgument generatedArgument, String varName) {
        return createVariableAssignment(generatedArgument, varName, false);
    }
    
    private String createVariableAssignment(GeneratedArgument generatedArgument, String varName, boolean spy) {
        String varType = generatedArgument.getNearestInstantAbleClass();
        if(!isPrimitive(varType) && !isWrapper(varType) && !varType.equals(String.class.getName())) {
            imports.add(createImportInfo(varType, callInfo.getClassName()));
        }
        StringBuilder result = new StringBuilder();
        result  .append(TAB + TAB)
                .append(getClassShort(typeOfGenArg(generatedArgument)))
                .append(" ").append(varName).append(" = ");
        if (spy) {
            result.append("spy(").append(createDataProvider(generatedArgument)).append(")");
        } else {
            result.append(createDataProvider(generatedArgument));
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
            providerBuilder.append(createVariableAssignment(iterator.next(), argName));
            argBuilder.append(argName);
            if (iterator.hasNext()) argBuilder.append(", ");
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
        if(isStatic(callInfo.getMethodModifiers())) {
            builder.append(getClassShort(callInfo.getClassName())).append(".");
        } else {
            builder.append(TEST_INST_VAR_NAME + ".");
        }
        builder.append(callInfo.getUnitName()).append("(").append(argBuilder.toString()).append(");\n");
    }

    private List<PreparedMock> createMocks() {
        Set<PreparedMock> methodMocks = new HashSet<>();
        List<PreparedMock> fieldMocks = new ArrayList<>();
        if (!isStatic(callInfo.getMethodModifiers()) || isUsePowermock()) {
            SeparatedInners separatedInners = separateInners(callInfo.getInnerMethods());
            SpyMaps spyMaps = createSpyMap(separatedInners);
            Set<FieldProperties> fieldProperties;
            if (isCreateMockFields()) {
                fieldProperties = testClassFields;
            } else {
                Set<MethodCallInfo> allMethods = new HashSet<>(separatedInners.getSingleInners());
                for (Set<MethodCallInfo> multiInner : separatedInners.getMultipleInners()) {
                    allMethods.addAll(multiInner);
                }
                Set<FieldProperties> serviceFields = createServiceFields(allMethods, testClassFields);
                fieldProperties = filterFieldPropBySpyMaps(serviceFields, spyMaps, testClassFields);
                if (!isStatic(callInfo.getMethodModifiers())) {
                    fieldMocks.addAll(createMockInstances(filterFieldPropByServiceClasses(fieldProperties, testClassFields)));
                }
                fieldMocks.addAll(createMockStatic(fieldProperties));
            }
            for (MethodCallInfo inner : separatedInners.getSingleInners()) {
                methodMocks.addAll(createMock(inner, fieldProperties, spyMaps, new MockCreator() {
                    @Override
                    public PreparedMock createMock(Set<MethodCallInfo> multiInner, Set<FieldProperties> fieldProperties, SpyMaps spyMaps) {
                        return createSingleMock(multiInner.iterator().next(), fieldProperties, spyMaps);
                    }
                }));
            }
            for (Set<MethodCallInfo> multiInner : separatedInners.getMultipleInners()) {
                methodMocks.addAll(createMock(multiInner, fieldProperties, spyMaps, new MockCreator() {
                    @Override
                    public PreparedMock createMock(Set<MethodCallInfo> multiInner, Set<FieldProperties> fieldProperties, SpyMaps spyMaps) {
                        return createMultipleMock(multiInner, fieldProperties, spyMaps);
                    }
                }));
            }
        }
        List<PreparedMock> methodList = new ArrayList<>(methodMocks);
        Collections.sort(methodList, new Comparator<PreparedMock>() {
            @Override
            public int compare(PreparedMock o1, PreparedMock o2) {
                return o1.getMock().compareTo(o2.getMock());
            }
        });
        List<PreparedMock> resultList = new ArrayList<>();
        resultList.addAll(fieldMocks);
        resultList.addAll(methodList);
        return resultList;
    }

    private void appendTestInstance() {
        if (isStatic(callInfo.getMethodModifiers())) return;
        String clearedClassName = getClearedClassName(callInfo.getNearestInstantAbleClass());
        imports.add(createImportInfo(clearedClassName, callInfo.getClassName()));
        builder .append(TAB + TAB)
                .append(getClassShort(clearedClassName))
                .append(" ")
                .append(TEST_INST_VAR_NAME)
                .append(" = spy(");
        if (isAbstract(callInfo.getClassModifiers())) {
            builder.append(getClassShort(clearedClassName)).append(".class");
        } else if (!callInfo.isClassHasZeroArgConstructor()) {
            String createInstMethod = props.getDataProviderClassPattern() + COMMON_UTIL_POSTFIX + "." + CREATE_INST_METHOD;
            imports.add(createImportInfo(createInstMethod, callInfo.getClassName(), true));
            builder.append("createInstance(").append(getClassShort(clearedClassName)).append(".class)");
        } else {
            builder.append("new ").append(getClassShort(clearedClassName)).append("()");
        }
        builder.append(");\n");
    }

    private Set<PreparedMock> createMockStatic(Set<FieldProperties> serviceClasses) {
        Set<PreparedMock> result = new HashSet<>();
        for (FieldProperties f : serviceClasses) {
            boolean anonymousClass = getLastClassShort(f.getClassName()).matches("\\d+");

            if (!isUsePowermock() || !isStatic(f.getModifiers()) || anonymousClass) continue;

            String varType = f.getClassName();
            if(!isPrimitive(varType) && !isWrapper(varType) && !varType.equals(String.class.getName())) {
                imports.add(createImportInfo(varType, callInfo.getClassName()));
            }
            mockStaticClassNames.add(getClassShort(f.getClassName()));
            String mock = TAB + TAB + "PowerMockito.mockStatic(" + getClassShort(f.getClassName()) + ".class);\n";
            result.add(new PreparedMock(mock, null));
        }
        return result;
    }

    private Set<PreparedMock> createMockInstances(Set<FieldProperties> serviceClasses) {
        Set<PreparedMock> result = new HashSet<>();
        for (FieldProperties f : serviceClasses) {
            boolean testClass = f.getClassName().equals(callInfo.getClassName());
            boolean anonymousClass = getLastClassShort(f.getClassName()).matches("\\d+");
            
            if (testClass || anonymousClass) continue;

            String varType = f.getClassName();
            if(!isPrimitive(varType) && !isWrapper(varType) && !varType.equals(String.class.getName())) {
                imports.add(createImportInfo(varType, callInfo.getClassName()));
            }

            String depInjectMethod = props.getDataProviderClassPattern() + COMMON_UTIL_POSTFIX + ".notPublicAssignment";
            imports.add(createImportInfo(depInjectMethod, callInfo.getClassName(), true));
            
            String mock = 
                    TAB + TAB + getClassShort(f.getClassName()) + " " + f.getUnitName() + 
                    " = mock(" + getClassShort(f.getClassName()) + ".class);\n";
            String assign = 
                    TAB + TAB + "notPublicAssignment(" + TEST_INST_VAR_NAME + ", \"" + f.getUnitName() + 
                    "\", " + f.getUnitName() + ");\n";
            
            result.add(new PreparedMock(mock + assign, null));
        }
        return result;
    }

    private SpyMaps createSpyMap(SeparatedInners separatedInners) {
        SpyMaps result = new SpyMaps();
        List<MethodCallInfo> allMethods = new ArrayList<>(separatedInners.getSingleInners());
        for (Set<MethodCallInfo> multiInner : separatedInners.getMultipleInners()) {
            allMethods.addAll(multiInner);
        }
        sortMethodCallInfos(allMethods);
        int varCounter = 0;
        for (MethodCallInfo inner : allMethods) {
            List<MethodCallInfo> foundSpyMethods = new ArrayList<>();
            foundSpyMethods.addAll(findSpyLinks(inner, allMethods, true));
            foundSpyMethods.addAll(findSpyLinks(inner, allMethods, false));
            for (MethodCallInfo current : foundSpyMethods) {
                result.getReturnSpyMap().put(inner, SPY_VAR_NAME + varCounter);
                String fieldVarName = determineVarName(current, testClassFields);
                String spyVarName = fieldVarName != null ? fieldVarName + SPY_VAR_NAME_SEPARATOR : "";
                if (result.getTargetSpyMap().containsKey(current)) {
                    String varName = result.getTargetSpyMap().get(current);
                    spyVarName += varName + SPY_VAR_NAME_SEPARATOR + SPY_VAR_NAME + varCounter;
                } else {
                    spyVarName += SPY_VAR_NAME + varCounter;
                }
                result.getTargetSpyMap().put(current, spyVarName);
            }
            varCounter++;
        }
        return result;
    }

    private List<MethodCallInfo> findSpyLinks(MethodCallInfo inner, List<MethodCallInfo> allMethods, boolean hashCodeEqualsCheck) {
        List<MethodCallInfo> result = new ArrayList<>();
        for (MethodCallInfo current : allMethods) {
            if (inner.getReturnArg() == null || inner.getReturnArg().getGenerated() == null) continue;
            boolean hashCodeCheckPassed =
                    (inner.getReturnArg().getClassHashCode() == current.getClassHashCode() && hashCodeEqualsCheck) ||
                    !hashCodeEqualsCheck;

            if (hashCodeCheckPassed && calledObjectFromReturn(inner, current)) {
                result.add(current);
            }
        }
        return result;
    }

    private boolean calledObjectFromReturn(MethodCallInfo inner, MethodCallInfo current) {
        String returnType = inner.getReturnArg().getClassName();
        return (inner.getReturnArg().getClassName().equals(current.getClassName()) ||
                current.getClassHierarchy().contains(returnType) ||
                current.getInterfacesHierarchy().contains(returnType));
    }

    private String createArrayProvider(Set<MethodCallInfo> innerSet) {
        MethodCallInfo innerFirst = innerSet.iterator().next();
        String retType = innerFirst.getReturnArg().getClassName();
        List<MethodCallInfo> methodCallInfoList = new ArrayList<>(innerSet);
        sortMethodCallInfos(methodCallInfoList);
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

    private String getMockVarName(MethodCallInfo inner, Set<FieldProperties> serviceClasses, SpyMaps spyMaps) {
        if (spyMaps.getTargetSpyMap().containsKey(inner)) return spyMaps.getTargetSpyMap().get(inner);
        boolean testClass = inner.getClassName().equals(callInfo.getClassName());
        if (testClass) {
            return TEST_INST_VAR_NAME;
        } else {
           return createMockVarName(inner, serviceClasses);
        }
    }

    private PreparedMock createMultipleMock(Set<MethodCallInfo> innerSet, Set<FieldProperties> serviceClasses, SpyMaps spyMaps) {
        MethodCallInfo innerFirst = innerSet.iterator().next();

        if (skipMock(innerFirst, callInfo, serviceClasses, testClassHierarchy, spyMaps) || forwardMock(innerFirst, callInfo, testClassHierarchy)) return null;

        // TODO Implement using spyMaps.getReturnSpyMap() for spy() return values in array provider

        String retType = innerFirst.getReturnArg().getClassName();
        if (!isPrimitive(retType) && !isWrapper(retType) && !retType.equals(String.class.getName())) {
            imports.add(createImportInfo(retType, callInfo.getClassName()));
        }
        String retShortType = getClassShort(retType);
        String arrVarName = "values" + retShortType;
        StringBuilder mockBuilder = new StringBuilder();
        mockBuilder
                .append(createDoExpression(innerFirst, "", true))
                .append("(new Answer() {\n" + TAB + "\n")
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
                .append(TAB + TAB + "}).when(");

        mockBuilder.append(createWhen(innerFirst, serviceClasses, spyMaps));
        String varName = getMockVarName(innerFirst, serviceClasses, spyMaps);
        return new PreparedMock(mockBuilder.toString(), getVerificationBlock(innerFirst, varName, serviceClasses));
    }

    private PreparedMock createSingleMock(MethodCallInfo inner, Set<FieldProperties> serviceClasses, SpyMaps spyMaps) {
        if (skipMock(inner, callInfo, serviceClasses, testClassHierarchy, spyMaps) || forwardMock(inner, callInfo, testClassHierarchy)) return null;
        boolean voidMethod = inner.isVoidMethod();
        StringBuilder mockBuilder = new StringBuilder();
        if (voidMethod) {
            mockBuilder.append(createDoExpression(inner, "", false));
        } else {
            String dpVar;
            if (spyMaps.getReturnSpyMap().containsKey(inner)) {
                dpVar = spyMaps.getReturnSpyMap().get(inner);
                mockBuilder.append(createVariableAssignment(inner.getReturnArg(), dpVar, true));
            } else {
                dpVar = createDataProvider(inner.getReturnArg());
            }
            mockBuilder.append(createDoExpression(inner, dpVar, false));
        }
        mockBuilder.append(createWhen(inner, serviceClasses, spyMaps));
        String varName = getMockVarName(inner, serviceClasses, spyMaps);
        return new PreparedMock(mockBuilder.toString(), getVerificationBlock(inner, varName, serviceClasses));
    }

    private String createWhen(MethodCallInfo inner, Set<FieldProperties> serviceClasses, SpyMaps spyMaps) {
        StringBuilder mockBuilder = new StringBuilder();
        if (isUsePowermock() && isStatic(inner.getMethodModifiers())) {
            mockBuilder
                    .append(getClassShort(inner.getClassName())).append(".class, ")
                    .append("\"").append(inner.getUnitName()).append("\"");
            if (inner.getArguments().size() > 0) {
                mockBuilder.append(", ");
                appendMockArguments(mockBuilder, inner, imports);
            }
            mockBuilder.append(");\n");
        } else {
            String varName = getMockVarName(inner, serviceClasses, spyMaps);
            mockBuilder.append(varName).append(").").append(inner.getUnitName()).append("(");
            appendMockArguments(mockBuilder, inner, imports);
            mockBuilder.append(");\n");
        }
        return mockBuilder.toString();
    }
    
    private String createDoExpression(MethodCallInfo inner, String returnExpression, boolean multiple) {
        StringBuilder doBuilder = new StringBuilder();
        if (isUsePowermock() && isStatic(inner.getMethodModifiers())) {
            doBuilder.append(TAB + TAB + "PowerMockito.");
        } else {
            doBuilder.append(TAB + TAB);
        }
        if (multiple) {
            doBuilder.append("doAnswer");
        } else if (isBlank(returnExpression)) {
            doBuilder.append("doNothing().when(");
        } else {
            doBuilder.append("doReturn(").append(returnExpression).append(").when(");
        }
        return doBuilder.toString();
    }
    
    private String getVerificationBlock(MethodCallInfo inner, String varName, Set<FieldProperties> serviceClasses) {
        if (determineVarName(inner, serviceClasses) == null || isStatic(inner.getMethodModifiers())) return null;
        StringBuilder verifyBuilder = new StringBuilder();
        verifyBuilder.append(TAB + TAB + "verify(");
        verifyBuilder.append(varName).append(", atLeastOnce()).").append(inner.getUnitName());
        verifyBuilder.append("(");
        appendMockArguments(verifyBuilder, inner, imports);
        verifyBuilder.append(");\n");
        return verifyBuilder.toString();
    }

    private void appendMockArguments(StringBuilder sb, MethodCallInfo inner, Set<ImportInfo> imports) {
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
            } else if (arg.getGenerated() == null && props.getTargetMockitoVersion().equals(GOOD_MOCKITO_VERSION)) {
                sb.append("null");
            } else {
                String clearedType = getClearedClassName(arg.getNearestInstantAbleClass());
                sb.append("nullable(").append(getClassShort(clearedType)).append(".class").append(")");
                if(!isPrimitive(clearedType) && !isWrapper(clearedType) && !clearedType.equals(String.class.getName())) {
                    imports.add(createImportInfo(clearedType, callInfo.getClassName()));
                }
            }
            if (iterator.hasNext()) sb.append(", ");
        }
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
        StringBuilder argTypes = new StringBuilder();
        Iterator<GeneratedArgument> iterator = inner.getArguments().iterator();
        while (iterator.hasNext()) {
            argTypes.append(iterator.next().getNearestInstantAbleClass());
            if (iterator.hasNext()) argTypes.append(", ");
        }
        return  inner.getClassName() + "-" +
                inner.getUnitName() + "-" + 
                inner.getMethodModifiers() + "-" +
                argTypes.toString();
    }
    
    private Map<String, Set<MethodCallInfo>> createSetOfSetInners(Set<MethodCallInfo> inners) {
        Map<String, Set<MethodCallInfo>> innerMap = new HashMap<>();
        for(MethodCallInfo inner : inners) {
            if (skipMock(inner, callInfo, testClassFields, testClassHierarchy, null)) continue;
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
            if (forwardMock(inner, callInfo, testClassHierarchy)) {
                Map<String, Set<MethodCallInfo>> recMap = createSetOfSetInners(inner.getInnerMethods());
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

    private SeparatedInners separateInners(Set<MethodCallInfo> inners) {
        SeparatedInners result = new SeparatedInners();
        for (Set<MethodCallInfo> set : createSetOfSetInners(inners).values()) {
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
