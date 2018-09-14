package com.github.tankist88.carpenter.generator.util;

import com.github.tankist88.carpenter.core.dto.argument.GeneratedArgument;
import com.github.tankist88.carpenter.core.dto.unit.field.FieldProperties;
import com.github.tankist88.carpenter.core.dto.unit.method.MethodCallInfo;
import com.github.tankist88.carpenter.generator.dto.unit.imports.ImportInfo;
import com.github.tankist88.carpenter.generator.enums.TypeCategory;

import java.util.*;

import static com.github.tankist88.object2source.util.GenerationUtil.*;

public class TypeHelper {
    public static String determineVarName(MethodCallInfo callInfo, Set<FieldProperties> serviceClasses) {
        Map<FieldProperties, String> serviceMap = new HashMap<>();
        for(FieldProperties f : serviceClasses) {
            serviceMap.put(f, f.getUnitName());
        }
        FieldProperties sc = TypeHelper.getSameType(callInfo, serviceMap.keySet());
        return sc != null ? serviceMap.get(sc) : null;
    }

    public static boolean isSameTypes(MethodCallInfo callInfo, Set<FieldProperties> availableTypes) {
        return getSameType(callInfo, availableTypes) != null;
    }

    private static int matchCountInServices(MethodCallInfo callInfo, FieldProperties serviceClass, TypeCategory typeCategory) {
        if (isPrimitive(callInfo.getClassName()) && isPrimitive(serviceClass.getClassName())) {
            return callInfo.getClassName().equals(serviceClass.getClassName()) ? 1 : 0;
        } else if (isPrimitive(callInfo.getClassName()) && !isPrimitive(serviceClass.getClassName())) {
            return 0;
        } else if (!isPrimitive(callInfo.getClassName()) && isPrimitive(serviceClass.getClassName())) {
            return 0;
        }
        int matchCount = 0;
        List<String> classesAndInterfaces = new ArrayList<>();
        classesAndInterfaces.addAll(callInfo.getClassHierarchy());
        classesAndInterfaces.addAll(callInfo.getInterfacesHierarchy());
        Set<String> extendedServices = new HashSet<>();
        extendedServices.add(serviceClass.getClassName());
        if (TypeCategory.CLASS.equals(typeCategory)) {
            extendedServices.addAll(serviceClass.getClassHierarchy());
        } else if (TypeCategory.INTERFACE.equals(typeCategory)) {
            extendedServices.addAll(serviceClass.getInterfacesHierarchy());
        }
        for (String c : classesAndInterfaces) {
            if (extendedServices.contains(isAnonymousClass(c) ? getAnonymousCallerClass(c) : c)) {
                matchCount++;
            }
        }
        return matchCount;
    }

    private static FieldProperties getSameType(MethodCallInfo callInfo, Set<FieldProperties> availableTypes) {
        FieldProperties fp = getSameType(callInfo, availableTypes, TypeCategory.CLASS);
        return fp != null ? fp : getSameType(callInfo, availableTypes, TypeCategory.INTERFACE);
    }

    private static FieldProperties getSameType(MethodCallInfo callInfo, Set<FieldProperties> availableTypes, TypeCategory typeCategory) {
        Map<FieldProperties, Integer> rangeMap = new HashMap<>();
        int currentMatch = 0;
        for (FieldProperties sc : availableTypes) {
            int match = matchCountInServices(callInfo, sc, typeCategory);
            if(match > 0 && match >= currentMatch) {
                currentMatch = match;
                rangeMap.put(sc, match);
            }
        }
        List<FieldProperties> candidates = new ArrayList<>();
        for (Map.Entry<FieldProperties, Integer> entry : rangeMap.entrySet()) {
            if (entry.getValue() == currentMatch) candidates.add(entry.getKey());
        }
        if (candidates.size() == 1) return candidates.get(0);
        for (FieldProperties fp : candidates) {
            for(GeneratedArgument ga : callInfo.getArguments()) {
                if(fp.getGenericString() == null) continue;
                if(fp.getGenericString().contains(ga.getNearestInstantAbleClass())) {
                    return fp;
                }
            }
        }
        return null;
    }

    public static ImportInfo createImportInfo(String importClass, String ownerClass) {
        return createImportInfo(importClass, ownerClass, false);
    }

    public static ImportInfo createImportInfo(String importClass, String ownerClass, boolean isStatic) {
        ImportInfo importInfo = new ImportInfo();
        importInfo.setClassName(ownerClass);
        if (!isStatic && !getClassShort(importClass).equals(getLastClassShort(importClass))) {
            importInfo.setUnitName(importClass.substring(0, importClass.indexOf(getLastClassShort(importClass)) - 1));
        } else {
            importInfo.setUnitName(importClass);
        }
        if (isStatic) {
            importInfo.setBody("import static " + getClearedClassName(importInfo.getUnitName()) + ";\n");
        } else {
            importInfo.setBody("import " + getClearedClassName(importInfo.getUnitName()) + ";\n");
        }
        return importInfo;
    }

    public static String typeOfGenArg(GeneratedArgument arg) {
        if (arg == null) return null;
        if (isPrimitive(arg.getNearestInstantAbleClass()) || isWrapper(arg.getNearestInstantAbleClass())) {
            return getLastClassShort(convertPrimitiveToWrapper(arg.getNearestInstantAbleClass()));
        } else {
            return arg.getNearestInstantAbleClass();
        }
    }

    public static boolean simpleType(GeneratedArgument arg) {
        if (arg == null) return false;
        String type = arg.getNearestInstantAbleClass();
        return isPrimitive(type) || isWrapper(type) || type.equals(String.class.getName());
    }
}
