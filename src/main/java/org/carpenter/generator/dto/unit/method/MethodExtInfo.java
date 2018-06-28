package org.carpenter.generator.dto.unit.method;

import org.carpenter.generator.dto.source.MethodSource;
import org.carpenter.generator.dto.unit.AbstractUnitExtInfo;

public class MethodExtInfo extends AbstractUnitExtInfo {
    private MethodSource methodSource;

    public MethodExtInfo() {
    }
    public MethodExtInfo(String className, String methodName, MethodSource methodSource) {
        super(className, methodName, null);
        this.methodSource = methodSource;
    }

    @Override
    public String getBody() {
        return methodSource.toString();
    }

    @Override
    public void setBody(String body) {
        throw new IllegalStateException("Wrong use! In MethodExtInfo need call setMethodSource()");
    }

    public MethodSource getMethodSource() {
        return methodSource;
    }

    public void setMethodSource(MethodSource methodSource) {
        this.methodSource = methodSource;
    }
}
