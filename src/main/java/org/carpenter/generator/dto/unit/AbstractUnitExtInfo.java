package org.carpenter.generator.dto.unit;

import org.carpenter.core.dto.unit.AbstractUnitBaseInfo;

public abstract class AbstractUnitExtInfo extends AbstractUnitBaseInfo implements ClassExtInfo {
    private String body;

    public AbstractUnitExtInfo() {
    }

    public AbstractUnitExtInfo(String className, String unitName, String body) {
        super(className, unitName);
        this.body = body;
    }

    @Override
    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
