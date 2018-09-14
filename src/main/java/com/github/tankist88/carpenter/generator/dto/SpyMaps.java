package com.github.tankist88.carpenter.generator.dto;

import com.github.tankist88.carpenter.core.dto.unit.method.MethodCallInfo;

import java.util.HashMap;
import java.util.Map;

public class SpyMaps {
    private Map<MethodCallInfo, String> returnSpyMap;
    private Map<MethodCallInfo, String> targetSpyMap;
    
    public Map<MethodCallInfo, String> getReturnSpyMap() {
        if (returnSpyMap == null) {
            returnSpyMap = new HashMap<>();
        }
        return returnSpyMap;
    }

    public Map<MethodCallInfo, String> getTargetSpyMap() {
        if (targetSpyMap == null) {
            targetSpyMap = new HashMap<>();
        }
        return targetSpyMap;
    }
}
