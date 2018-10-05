package com.github.tankist88.carpenter.generator.util;

import com.github.tankist88.carpenter.core.dto.unit.field.FieldProperties;
import com.github.tankist88.carpenter.core.dto.unit.method.MethodCallInfo;
import com.github.tankist88.carpenter.generator.dto.PreparedMock;
import com.github.tankist88.carpenter.generator.dto.SpyMaps;
import com.github.tankist88.carpenter.generator.service.MockCreator;

import java.util.*;

import static com.github.tankist88.carpenter.generator.command.CreateTestMethodCommand.SPY_VAR_NAME_SEPARATOR;

public class MockCreateUtils {
    public static Set<PreparedMock> createMock(MethodCallInfo inner, Set<FieldProperties> fieldProperties, SpyMaps spyMaps, MockCreator mockCreator) {
        Set<MethodCallInfo> singletonSet = new HashSet<>();
        singletonSet.add(inner);
        return createMock(singletonSet, fieldProperties, spyMaps, mockCreator);
    }

    public static Set<PreparedMock> createMock(Set<MethodCallInfo> multiInner, Set<FieldProperties> fieldProperties, SpyMaps spyMaps, MockCreator mockCreator) {
        if (multiInner == null || multiInner.isEmpty()) return new HashSet<>();
        Set<PreparedMock> mockSet = new HashSet<>();
        MethodCallInfo inner = multiInner.iterator().next();
        String spyVarName = spyMaps.getTargetSpyMap().get(inner);
        if (spyVarName != null && spyVarName.contains(SPY_VAR_NAME_SEPARATOR)) {
            for (String varName : spyVarName.split(SPY_VAR_NAME_SEPARATOR)) {
                SpyMaps tmpSpyMaps = spyMaps.copy();
                Map<MethodCallInfo, String> tmpTargetSpyMap = new HashMap<>();
                tmpTargetSpyMap.put(inner, varName);
                tmpSpyMaps.getTargetSpyMap().putAll(tmpTargetSpyMap);
                PreparedMock mock = mockCreator.createMock(multiInner, fieldProperties, tmpSpyMaps);
                if (mock != null) mockSet.add(mock);
            }
        } else {
            PreparedMock mock = mockCreator.createMock(multiInner, fieldProperties, spyMaps);
            if (mock != null) mockSet.add(mock);
        }
        return mockSet;
    }
}
