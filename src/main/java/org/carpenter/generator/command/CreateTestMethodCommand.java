package org.carpenter.generator.command;

import org.carpenter.core.dto.argument.GeneratedArgument;
import org.carpenter.core.dto.unit.field.FieldProperties;
import org.carpenter.core.dto.unit.method.MethodBaseInfo;
import org.carpenter.core.dto.unit.method.MethodCallInfo;
import org.carpenter.core.property.GenerationProperties;
import org.carpenter.core.property.GenerationPropertiesFactory;
import org.carpenter.generator.dto.PreparedMock;
import org.carpenter.generator.dto.ProviderNextPartInfo;
import org.carpenter.generator.dto.SeparatedInners;
import org.carpenter.generator.dto.source.MethodLine;
import org.carpenter.generator.dto.source.MethodSource;
import org.carpenter.generator.dto.source.Variable;
import org.carpenter.generator.dto.unit.ClassExtInfo;
import org.carpenter.generator.dto.unit.imports.ImportInfo;
import org.carpenter.generator.dto.unit.method.MethodExtInfo;
import org.carpenter.generator.extension.assertext.AssertExtension;
import org.carpenter.generator.util.ConvertUtil;
import org.carpenter.generator.util.TypeHelper;
import org.object2source.dto.ProviderInfo;
import org.object2source.dto.ProviderResult;

import java.lang.reflect.Modifier;
import java.util.*;

import static java.util.Collections.singletonList;
import static org.carpenter.core.property.AbstractGenerationProperties.TAB;
import static org.carpenter.generator.TestGenerator.TEST_INST_VAR_NAME;
import static org.carpenter.generator.dto.source.MethodLine.PLACE_HOLDER;
import static org.carpenter.generator.util.TypeHelper.typeOfGenArg;
import static org.object2source.util.GenerationUtil.*;

public class CreateTestMethodCommand extends AbstractReturnClassInfoCommand<ClassExtInfo> {
    private static final int DATA_PROVIDER_MAX_LENGTH_IN_METHODS = 30;

    private List<AssertExtension> assertExtensions;

    private MethodCallInfo callInfo;
    private Map<String, Set<String>> providerSignatureMap;
    private GenerationProperties props;

    private List<MethodExtInfo> methods;
    private Map<String, Set<ClassExtInfo>> dataProviders;
    private Set<ImportInfo> imports;

    public CreateTestMethodCommand(MethodCallInfo callInfo, Map<String, Set<String>> providerSignatureMap, Map<String, Set<ClassExtInfo>> dataProviders, List<AssertExtension> assertExtensions) {
        this.callInfo = callInfo;
        this.providerSignatureMap = providerSignatureMap;
        this.props = GenerationPropertiesFactory.loadProps();
        this.imports = new HashSet<>();
        this.dataProviders = dataProviders;
        if(assertExtensions != null) {
            this.assertExtensions = assertExtensions;
        } else {
            this.assertExtensions = new ArrayList<>();
        }
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
        result.addAll(imports);
        for(Set<ClassExtInfo> set : dataProviders.values()) {
            result.addAll(set);
        }
        return result;
    }

    private void createTestMethod() {
        String testMethodName = (
                "test" + upFirst(callInfo.getUnitName()) +
                "_" + callInfo.getArguments().hashCode() +
                "()").replaceAll("-", "_");

        FieldProperties testProp = new FieldProperties(callInfo.getClassName(), TEST_INST_VAR_NAME);
        testProp.setClassHierarchy(callInfo.getClassHierarchy());
        testProp.setInterfacesHierarchy(callInfo.getInterfacesHierarchy());

        Set<FieldProperties> serviceClasses = new HashSet<>();
        serviceClasses.add(testProp);
        serviceClasses.addAll(callInfo.getServiceFields());

        Set<FieldProperties> testClassHierarchy = new HashSet<>();
        testClassHierarchy.add(testProp);

        MethodSource methodSource = new MethodSource();
        methodSource.setUnitName(callInfo.getUnitName());
        methodSource.setTestMethodName(testMethodName);
        methodSource.setTestMethodDefinition(
                TAB + "@Test\n" +
                TAB + "public void " +
                testMethodName +
                " throws java.lang.Exception {\n");

        Set<PreparedMock> mocks = createMocks(callInfo, serviceClasses, testClassHierarchy);

        for (PreparedMock mock : mocks) {
            methodSource.getLines().add(mock.getMock());
        }

        appendTestCall(callInfo, methodSource);
        appendMethodCallCheckAssert(mocks, methodSource);
        appendResultCheckAssert(callInfo, methodSource);

        methodSource.setTestMethodEnd(TAB + "}\n");

        methods = singletonList(new MethodExtInfo(callInfo.getClassName(), testMethodName, methodSource));
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

    private void appendResultCheckAssert(MethodCallInfo callInfo, MethodSource methodSource) {
        AssertExtension assertExtension = findAssertExtension(callInfo);
        if (assertExtension != null) {
            StringBuilder dataProviderBuilder = new StringBuilder();
            appendDataProvider(dataProviderBuilder, callInfo.getReturnArg());
            String dataProviderMethod = dataProviderBuilder.toString();
            methodSource.getLines().add(assertExtension.getAssertBlock(dataProviderMethod));
        }
    }

    private void appendMethodCallCheckAssert(Set<PreparedMock> mocks, MethodSource methodSource) {
        for (PreparedMock mock : mocks) {
            methodSource.getLines().add(mock.getVerify());
        }
    }

    private void appendTestCall(MethodCallInfo callInfo, MethodSource methodSource) {
        MethodLine line = new MethodLine();
        StringBuilder builder = new StringBuilder();
        builder.append(TAB + TAB);
        if (findAssertExtension(callInfo) != null) {
            builder.append(typeOfGenArg(callInfo.getReturnArg())).append(" result = ");
        }
        if(Modifier.isStatic(callInfo.getMethodModifiers())) {
            builder.append(getClassShort(callInfo.getClassName())).append(".");
        } else {
            builder.append(TEST_INST_VAR_NAME + ".");
        }
        builder.append(callInfo.getUnitName()).append("(");
        Iterator<GeneratedArgument> iterator = callInfo.getArguments().iterator();
        int i = 1;
        while (iterator.hasNext()) {
            StringBuilder sb = new StringBuilder();
            appendDataProvider(sb, iterator.next());
            line.getVariables().add(new Variable(i, sb.toString()));
            builder.append(PLACE_HOLDER.replace("?", String.valueOf(i)));
            if(iterator.hasNext()) builder.append(", ");
            i++;
        }
        builder.append(");\n");
        line.setExpression(builder.toString());
        methodSource.getLines().add(line);
    }

    private Set<PreparedMock> createMocks(MethodCallInfo callInfo, Set<FieldProperties> serviceClasses, Set<FieldProperties> testClassHierarchy) {
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
        return allMocks;
    }

    private boolean skipMock(MethodCallInfo callInfo, Set<FieldProperties> serviceClasses, boolean multiple) {
        boolean staticMethod = Modifier.isStatic(callInfo.getMethodModifiers());
        boolean privateMethod = Modifier.isPrivate(callInfo.getMethodModifiers());
        boolean protectedMethod = Modifier.isProtected(callInfo.getMethodModifiers());
        return (staticMethod || (multiple && (privateMethod || protectedMethod)) || !TypeHelper.isSameTypes(callInfo, serviceClasses));
    }

    private Set<PreparedMock> createMultipleMock(Set<MethodCallInfo> innerSet, Set<FieldProperties> serviceClasses, Set<FieldProperties> testClassHierarchy) {
        MethodCallInfo innerFirst = innerSet.iterator().next();

        if(skipMock(innerFirst, serviceClasses, true)) return null;

        boolean sameTypeWithTest = TypeHelper.isSameTypes(innerFirst, testClassHierarchy);
        String varName = sameTypeWithTest ? TEST_INST_VAR_NAME : TypeHelper.determineVarName(innerFirst, serviceClasses);
        StringBuilder mockBuilder = new StringBuilder();
        MethodLine mockLine = new MethodLine();
        mockBuilder.append(TAB + TAB + "doAnswer(new Answer() {\n")
                .append(TAB + TAB + TAB + "private int count = 0;\n")
                .append(TAB + TAB + TAB + "private ");
        if(!isPrimitive(innerFirst.getReturnArg().getClassName())) {
            imports.add(TypeHelper.createImportInfo(innerFirst.getReturnArg().getClassName(), callInfo.getClassName()));
        }
        mockBuilder.append(getClassShort(innerFirst.getReturnArg().getClassName())).append("[] values = {\n");
        List<MethodCallInfo> methodCallInfoList = new ArrayList<>(innerSet);
        Collections.sort(methodCallInfoList, new Comparator<MethodCallInfo>() {
            @Override
            public int compare(MethodCallInfo o1, MethodCallInfo o2) {
                return (o1.getCallTime() > o2.getCallTime()) ? -1 : 1;
            }
        });
        int i = 1;
        Iterator<MethodCallInfo> methodCallInfoIterator = methodCallInfoList.iterator();
        while(methodCallInfoIterator.hasNext()) {
            MethodCallInfo m = methodCallInfoIterator.next();
            mockBuilder.append(TAB + TAB + TAB + TAB + TAB);
            StringBuilder sb = new StringBuilder();
            appendDataProvider(sb, m.getReturnArg());
            mockLine.getVariables().add(new Variable(i, sb.toString()));
            mockBuilder.append(PLACE_HOLDER.replace("?", String.valueOf(i)));
            if(methodCallInfoIterator.hasNext()) mockBuilder.append(",");
            mockBuilder.append("\n");
            i++;
        }
        mockBuilder.append(TAB + TAB + TAB + "};\n")
                .append(TAB + TAB + TAB + "@Override\n")
                .append(TAB + TAB + TAB + "public Object answer(InvocationOnMock invocationOnMock) throws Throwable {\n")
                .append(TAB + TAB + TAB + TAB).append(getClassShort(innerFirst.getReturnArg().getClassName())).append(" result = values[count];\n")
                .append(TAB + TAB + TAB + TAB + "if(count + 1 < values.length) count++;\n")
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
        Set<PreparedMock> mocks = new HashSet<>();

        MethodLine verifyLine = new MethodLine();
        verifyLine.setExpression(verifyBuilder.toString());

        mockLine.setExpression(mockBuilder.toString());

        mocks.add(new PreparedMock(mockLine, verifyLine));
        return mocks;
    }

    private Set<PreparedMock> createSingleMock(MethodCallInfo inner, Set<FieldProperties> serviceClasses, Set<FieldProperties> testClassHierarchy) {
        if(skipMock(inner, serviceClasses, false)) return null;

        Set<PreparedMock> mocks = new HashSet<>();

        boolean sameTypeWithTest = TypeHelper.isSameTypes(inner, testClassHierarchy);
        boolean voidMethod = inner.isVoidMethod();
        boolean privateMethod = Modifier.isPrivate(inner.getMethodModifiers());
        boolean protectedMethod = Modifier.isProtected(inner.getMethodModifiers());
        if((voidMethod && sameTypeWithTest) || privateMethod || protectedMethod) {
            Set<PreparedMock> innerMocks = createMocks(inner, serviceClasses, testClassHierarchy);
            if(innerMocks != null) {
                mocks.addAll(innerMocks);
            }
        } else {
            String varName = sameTypeWithTest ? TEST_INST_VAR_NAME : TypeHelper.determineVarName(inner, serviceClasses);
            StringBuilder mockBuilder = new StringBuilder();
            StringBuilder verifyBuilder = new StringBuilder();

            MethodLine mockLine = new MethodLine();

            if (voidMethod) {
                mockBuilder.append(TAB + TAB + "doNothing().when(");
            } else {
                ProviderResult providerResult = inner.getReturnArg().getGenerated();
                String placeHolder = PLACE_HOLDER.replace("?", "1");
                if (providerResult != null) {
                    MethodBaseInfo mn = getProviderNameAndUpdateState(providerResult);
                    String dataProvider = getClassShort(mn.getClassName()) + "." + mn.getUnitName();
                    mockLine.getVariables().add(new Variable(1, dataProvider));
                    mockBuilder.append(TAB + TAB + "doReturn(").append(placeHolder).append(").when(");
                    if(!isPrimitive(mn.getClassName())) {
                        imports.add(TypeHelper.createImportInfo(mn.getClassName(), callInfo.getClassName()));
                    }
                } else {
                    mockBuilder.append(TAB + TAB + "doReturn(").append(placeHolder).append(").when(");
                    mockLine.getVariables().add(new Variable(1, "null"));
                }
            }
            mockBuilder.append(varName).append(").").append(inner.getUnitName());
            verifyBuilder.append(TAB + TAB + "verify(")
                         .append(varName)
                         .append(", atLeastOnce()).")
                         .append(inner.getUnitName());
            appendMockArguments(mockBuilder, inner, imports);
            appendMockArguments(verifyBuilder, inner, imports);
            mockBuilder.append(";\n");
            verifyBuilder.append(";\n");

            MethodLine verifyLine = new MethodLine();
            verifyLine.setExpression(verifyBuilder.toString());

            mockLine.setExpression(mockBuilder.toString());

            mocks.add(new PreparedMock(mockLine, verifyLine));
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
                if(!isPrimitive(arg.getGenericString())) {
                    imports.add(TypeHelper.createImportInfo(arg.getGenericString(), callInfo.getClassName()));
                }
            } else if (arg.getGenerated() != null && arg.getGenericString() != null && arg.getInterfacesHierarchy().contains("java.util.Set")) {
                sb.append("ArgumentMatchers.<").append(getLastClassShort(arg.getGenericString())).append(">anySet()");
                if(!isPrimitive(arg.getGenericString())) {
                    imports.add(TypeHelper.createImportInfo(arg.getGenericString(), callInfo.getClassName()));
                }
            } else if (arg.getGenerated() != null) {
                String clearedType = getClearedClassName(arg.getClassName());
                sb.append("any(").append(getLastClassShort(clearedType)).append(".class").append(")");
                if(!isPrimitive(clearedType)) {
                    imports.add(TypeHelper.createImportInfo(clearedType, callInfo.getClassName()));
                }
            } else {
                String clearedType = getClearedClassName(arg.getClassName());
                sb.append("nullable(").append(getLastClassShort(clearedType)).append(".class").append(")");
                if(!isPrimitive(clearedType)) {
                    imports.add(TypeHelper.createImportInfo(clearedType, callInfo.getClassName()));
                }
            }
            if (iterator.hasNext()) sb.append(", ");
        }
        sb.append(")");
    }

    private void appendDataProvider(StringBuilder sb, GeneratedArgument arg) {
        ProviderResult providerResult = arg.getGenerated();
        if (providerResult != null) {
            MethodBaseInfo mn = getProviderNameAndUpdateState(providerResult);
            sb.append(getClassShort(mn.getClassName())).append(".").append(mn.getUnitName());
            if(!isPrimitive(mn.getClassName())) {
                imports.add(TypeHelper.createImportInfo(mn.getClassName(), callInfo.getClassName()));
            }
        } else {
            sb.append("(").append(getLastClassShort(arg.getNearestInstantAbleClass())).append(") null");
            imports.add(TypeHelper.createImportInfo(arg.getNearestInstantAbleClass(), callInfo.getClassName()));
        }
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
                MethodCallInfo mc = set.iterator().next();
                if(mc.isVoidMethod() || allReturnNulls) {
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
        for(Map.Entry<String, Set<String>> entry : this.providerSignatureMap.entrySet()) {
            for(String method : entry.getValue()) {
                if(method.equals(currentMethodName)) {
                    methodClass = entry.getKey();
                    break;
                }
            }
            if(methodClass != null) break;
        }
        return methodClass;
    }
}
