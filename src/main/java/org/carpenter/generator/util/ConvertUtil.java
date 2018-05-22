package org.carpenter.generator.util;

import org.carpenter.generator.dto.unit.method.MethodExtInfo;
import org.object2source.dto.ProviderInfo;

public class ConvertUtil extends org.carpenter.core.util.ConvertUtil {
    public static MethodExtInfo toMethodExtInfo(String className, ProviderInfo providerInfo) {
        if(providerInfo == null) return null;
        MethodExtInfo result = new MethodExtInfo();
        result.setUnitName(providerInfo.getMethodName());
        result.setBody(providerInfo.getMethodBody());
        result.setClassName(className);
        return result;
    }
}
