package org.carpenter.generator.util;

import org.carpenter.core.dto.unit.field.FieldProperties;
import org.carpenter.core.dto.unit.method.MethodCallInfo;
import org.carpenter.generator.dto.unit.imports.ImportInfo;
import org.carpenter.generator.enums.TypeCategory;

import java.util.*;

import static org.object2source.util.GenerationUtil.getInstName;
import static org.object2source.util.GenerationUtil.isPrimitive;

public class TypeHelper {
    public static String determineVarName(MethodCallInfo callInfo, Set<FieldProperties> serviceClasses) {
        Map<FieldProperties, String> serviceMap = new HashMap<>();
        for(FieldProperties f : serviceClasses) {
            serviceMap.put(f, f.getUnitName());
        }
        FieldProperties sc = TypeHelper.getSameType(callInfo, serviceMap.keySet());
        return sc != null ? serviceMap.get(sc) : getInstName(callInfo.getClassName());
    }

    public static boolean isSameTypes(MethodCallInfo callInfo, Set<FieldProperties> availableTypes) {
        return getSameType(callInfo, availableTypes) != null;
    }

    private static int matchCountInServices(MethodCallInfo callInfo, FieldProperties serviceClass, TypeCategory typeCategory) {
        if(isPrimitive(callInfo.getClassName()) && isPrimitive(serviceClass.getClassName())) {
            return callInfo.getClassName().equals(serviceClass.getClassName()) ? 1 : 0;
        } else if(isPrimitive(callInfo.getClassName()) && !isPrimitive(serviceClass.getClassName())) {
            return 0;
        } else if(!isPrimitive(callInfo.getClassName()) && isPrimitive(serviceClass.getClassName())) {
            return 0;
        }
        int matchCount = 0;
        List<String> classesAndInterfaces = new ArrayList<>();
        classesAndInterfaces.addAll(callInfo.getClassHierarchy());
        classesAndInterfaces.addAll(callInfo.getInterfacesHierarchy());
        Set<String> extendedServices = new HashSet<>();
        extendedServices.add(serviceClass.getClassName());
        if(TypeCategory.CLASS.equals(typeCategory)) {
            extendedServices.addAll(serviceClass.getClassHierarchy());
        } else if(TypeCategory.INTERFACE.equals(typeCategory)) {
            extendedServices.addAll(serviceClass.getInterfacesHierarchy());
        }
        for(String c : classesAndInterfaces) {
            if(extendedServices.contains(c)) {
                matchCount++;
            }
        }
        return matchCount;
    }

    private static FieldProperties getSameType(MethodCallInfo callInfo, Set<FieldProperties> availableTypes) {
        FieldProperties result = null;
        int currentMatch = 0;
        for(FieldProperties sc : availableTypes) {
            int match = matchCountInServices(callInfo, sc, TypeCategory.CLASS);
            if(match > currentMatch){
                currentMatch = match;
                result = sc;
            }
        }
        if(result != null) return result;
        currentMatch = 0;
        for(FieldProperties sc : availableTypes) {
            int match = matchCountInServices(callInfo, sc, TypeCategory.INTERFACE);
            if(match > currentMatch){
                currentMatch = match;
                result = sc;
            }
        }
        return result;
    }

    public static String clearClassName(String className) {
        return className.replaceAll("\\$", ".");
    }

    public static ImportInfo createImportInfo(String importClass, String ownerClass) {
        ImportInfo importInfo = new ImportInfo();
        importInfo.setClassName(ownerClass);
        importInfo.setUnitName(importClass);
        importInfo.setBody("import " + clearClassName(importClass) + ";\n");
        return importInfo;
    }
}
