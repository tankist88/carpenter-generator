package com.github.tankist88.carpenter.generator.util.classfields;

import com.github.tankist88.carpenter.core.dto.unit.field.FieldProperties;
import com.github.tankist88.carpenter.core.dto.unit.method.MethodCallInfo;
import com.github.tankist88.carpenter.generator.dto.SpyMaps;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.reflect.Modifier.isStatic;

public class FilterUtils {
    public static Set<FieldProperties> filterFieldPropByServiceClasses(Set<FieldProperties> fieldProperties, Set<FieldProperties> testClassFields) {
        Set<FieldProperties> result = new HashSet<>();
        for (FieldProperties f : fieldProperties) {
            List<String> typeHierarchy = new ArrayList<>();
            typeHierarchy.addAll(f.getClassHierarchy());
            typeHierarchy.addAll(f.getInterfacesHierarchy());
            boolean contains = false;
            for (FieldProperties s : testClassFields) {
                if (typeHierarchy.contains(s.getClassName())) {
                    contains = true;
                    break;
                }
            }
            if (contains) result.add(f);
        }
        return result;
    }

    public static Set<FieldProperties> filterFieldPropBySpyMaps(Set<FieldProperties> fieldProperties, SpyMaps spyMaps, Set<FieldProperties> testClassFields) {
        Set<FieldProperties> result = new HashSet<>();
        Set<FieldProperties> filteredByServices = filterFieldPropByServiceClasses(fieldProperties, testClassFields);
        for (FieldProperties f : fieldProperties) {
            if (filteredByServices.contains(f) || isStatic(f.getModifiers())) {
                result.add(f);
            } else {
                boolean contains = false;
                for (MethodCallInfo m : spyMaps.getTargetSpyMap().keySet()) {
                    if (m.getClassName().equals(f.getClassName()) && m.getUnitName().startsWith(f.getUnitName())) {
                        contains = true;
                        break;
                    }
                }
                if (contains) result.add(f);
            }
        }
        return result;
    }
}
