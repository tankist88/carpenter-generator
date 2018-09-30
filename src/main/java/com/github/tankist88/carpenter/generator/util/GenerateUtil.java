package com.github.tankist88.carpenter.generator.util;

import com.github.tankist88.carpenter.core.dto.unit.field.FieldProperties;
import com.github.tankist88.carpenter.core.dto.unit.method.MethodCallInfo;
import com.github.tankist88.carpenter.core.property.GenerationProperties;
import com.github.tankist88.carpenter.generator.dto.SpyMaps;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.github.tankist88.carpenter.generator.TestGenerator.TEST_INST_VAR_NAME;
import static com.github.tankist88.carpenter.generator.TestGenerator.isCreateMockFields;
import static com.github.tankist88.carpenter.generator.TestGenerator.isUsePowermock;
import static com.github.tankist88.carpenter.generator.util.TypeHelper.determineVarName;
import static com.github.tankist88.carpenter.generator.util.TypeHelper.isSameTypes;
import static com.github.tankist88.object2source.util.GenerationUtil.*;
import static java.lang.reflect.Modifier.*;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class GenerateUtil {
    public static boolean allowedPackage(String classname, GenerationProperties props) {
        for(String p : props.getAllowedPackagesForTests()) {
            if(classname.startsWith(p)) return true;
        }
        return false;
    }

    public static String createAndReturnPathName(GenerationProperties props) throws IOException {
        String pathname = props.getUtGenDir();
        FileUtils.forceMkdir(new File(pathname));
        return pathname;
    }

    public static List<File> getFileList(File inDir, String extension) {
        List<File> result = new ArrayList<>();
        if(!inDir.exists()) return result;
        if(inDir.isDirectory()) {
            File[] listFiles = inDir.listFiles();
            if(listFiles != null) {
                for (File f : listFiles) {
                    if (f.isDirectory()) {
                        result.addAll(getFileList(f, extension));
                    } else if (f.getName().endsWith(extension)) {
                        result.add(f);
                    }
                }
            }
        } else {
            throw new IllegalArgumentException(inDir.getName() + " not a directory");
        }
        return result;
    }

    public static String createVarNameFromMethod(String methodSig) {
        if (isBlank(methodSig)) return null;
        String methodName = methodSig.contains("(") ? methodSig.substring(0, methodSig.indexOf("(")) : methodSig;
        return downFirst(methodName);
    }

    /**
     * Return true if need skip mock for method in argument, but need create mocks for their inner calls
     * @param inner method call info
     * @return true if need skip mock for method in argument, but need create mocks for their inner calls
     */
    public static boolean forwardMock(MethodCallInfo inner, MethodCallInfo callInfo, Set<FieldProperties> testClassHierarchy) {
        boolean sameTypeWithTest = isSameTypes(inner, testClassHierarchy);
        boolean sameMethodWithTest = callInfo.getUnitName().equals(inner.getUnitName());
        boolean voidMethod = inner.isVoidMethod();
        boolean privateMethod = isPrivate(inner.getMethodModifiers());
        boolean protectedMethod = isProtected(inner.getMethodModifiers());
        boolean anonymousClass = getLastClassShort(inner.getClassName()).matches("\\d+");
        return
                (sameMethodWithTest && sameTypeWithTest) ||
                (voidMethod && sameTypeWithTest) ||
                privateMethod ||
                protectedMethod ||
                anonymousClass;
    }

    /**
     * Return true if need skip create mock for method in argument
     * @param inner inner method to check
     * @param callInfo method call info
     * @param serviceClasses service fields of test class
     * @return true if need skip create mock for method in argument
     */
    public static boolean skipMock(MethodCallInfo inner, MethodCallInfo callInfo, Set<FieldProperties> serviceClasses, Set<FieldProperties> testClassHierarchy, SpyMaps spyMaps) {
        boolean staticMethod = isStatic(inner.getMethodModifiers()) && !isUsePowermock();
        boolean notStaticCallInStaticCtx = isStatic(callInfo.getMethodModifiers()) && !isStatic(inner.getMethodModifiers());
        boolean varNotFound = 
                spyMaps != null && 
                !isCreateMockFields() && 
                !isStatic(inner.getMethodModifiers()) && 
                determineVarName(inner, serviceClasses) == null
                && !spyMaps.getTargetSpyMap().containsKey(inner);
        return  staticMethod ||
                varNotFound || 
                notStaticCallInStaticCtx || 
                (
                        !isSameTypes(inner, serviceClasses) &&
                        !isSameTypes(inner, testClassHierarchy) &&
                        !inner.isMaybeServiceClass()
                );
    }

    public static String createMockVarName(MethodCallInfo inner, Set<FieldProperties> serviceClasses) {
        String varName = determineVarName(inner, serviceClasses);
        if (varName != null) {
            return varName;
        } else if (inner.isMaybeServiceClass()) {
            return createVarNameFromMethod(inner.getUnitName());
        } else {
            return getInstName(inner.getClassName());
        }
    }

    public static Set<FieldProperties> createTestClassHierarchy(MethodCallInfo callInfo) {
        FieldProperties testProp = new FieldProperties(callInfo.getClassName(), TEST_INST_VAR_NAME);
        testProp.setClassHierarchy(callInfo.getClassHierarchy());
        testProp.setInterfacesHierarchy(callInfo.getInterfacesHierarchy());
        Set<FieldProperties> testClassHierarchy = new HashSet<>();
        testClassHierarchy.add(testProp);
        return testClassHierarchy;
    }

    public static Set<FieldProperties> createServiceFields(MethodCallInfo callInfo) {
        Set<FieldProperties> serviceClasses = new HashSet<>();
        serviceClasses.addAll(createTestClassHierarchy(callInfo));
        serviceClasses.addAll(callInfo.getServiceFields());
        return serviceClasses;
    }
    
    public static Set<FieldProperties> createServiceFields(Set<MethodCallInfo> callInfoSet, Set<FieldProperties> serviceClasses) {
        Set<FieldProperties> result = new HashSet<>();
        for (MethodCallInfo call : callInfoSet) {
            FieldProperties f = new FieldProperties();
            f.setClassName(call.getClassName());
            f.setUnitName(createMockVarName(call, serviceClasses));
            f.setClassHierarchy(call.getClassHierarchy());
            f.setInterfacesHierarchy(call.getInterfacesHierarchy());
            f.setGenericString(call.getGenericString());
            f.setFieldTypeModifiers(call.getClassModifiers());
            f.setModifiers(call.getMethodModifiers());
            result.add(f);
        }
        return result;
    }
}
