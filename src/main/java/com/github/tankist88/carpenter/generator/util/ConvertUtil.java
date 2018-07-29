package com.github.tankist88.carpenter.generator.util;

import com.github.tankist88.carpenter.generator.dto.unit.method.MethodExtInfo;
import com.github.tankist88.object2source.dto.ProviderInfo;

public class ConvertUtil extends com.github.tankist88.carpenter.core.util.ConvertUtil {
    public static MethodExtInfo toMethodExtInfo(String className, ProviderInfo providerInfo) {
        if(providerInfo == null) return null;
        MethodExtInfo result = new MethodExtInfo();
        result.setUnitName(providerInfo.getMethodName());
        result.setBody(providerInfo.getMethodBody());
        result.setClassName(className);
        return result;
    }
}
