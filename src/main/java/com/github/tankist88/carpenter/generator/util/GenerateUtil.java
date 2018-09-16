package com.github.tankist88.carpenter.generator.util;

import com.github.tankist88.carpenter.core.dto.unit.field.FieldProperties;
import com.github.tankist88.carpenter.core.dto.unit.method.MethodCallInfo;
import com.github.tankist88.carpenter.core.property.GenerationProperties;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.github.tankist88.carpenter.generator.TestGenerator.TEST_INST_VAR_NAME;
import static com.github.tankist88.carpenter.generator.util.TypeHelper.isSameTypes;
import static com.github.tankist88.object2source.util.GenerationUtil.downFirst;
import static com.github.tankist88.object2source.util.GenerationUtil.getLastClassShort;
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
     * @param callInfo method call info
     * @return true if need skip mock for method in argument, but need create mocks for their inner calls
     */
    public static boolean forwardMock(MethodCallInfo callInfo, Set<FieldProperties> testClassHierarchy) {
        boolean sameTypeWithTest = isSameTypes(callInfo, testClassHierarchy);
        boolean voidMethod = callInfo.isVoidMethod();
        boolean privateMethod = Modifier.isPrivate(callInfo.getMethodModifiers());
        boolean protectedMethod = Modifier.isProtected(callInfo.getMethodModifiers());
        boolean anonymousClass = getLastClassShort(callInfo.getClassName()).matches("\\d+");
        return  (voidMethod && sameTypeWithTest) ||
                privateMethod ||
                protectedMethod ||
                anonymousClass;
    }

    /**
     * Return true if need skip create mock for method in argument
     * @param callInfo method call info
     * @param serviceClasses service fields of test class
     * @return true if need skip create mock for method in argument
     */
    public static boolean skipMock(MethodCallInfo callInfo, Set<FieldProperties> serviceClasses, Set<FieldProperties> testClassHierarchy) {
        boolean staticMethod = Modifier.isStatic(callInfo.getMethodModifiers());
        return  staticMethod ||
                (
                        !isSameTypes(callInfo, serviceClasses) &&
                        !isSameTypes(callInfo, testClassHierarchy) &&
                        !callInfo.isMaybeServiceClass()
                );
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
}
