package org.carpenter.generator.dto.unit.method;

import org.carpenter.generator.dto.unit.AbstractUnitExtInfo;

public class MethodExtInfo extends AbstractUnitExtInfo {
    public MethodExtInfo() {
    }
    public MethodExtInfo(String className, String methodName, String body) {
        super(className, methodName, body);
    }
}
