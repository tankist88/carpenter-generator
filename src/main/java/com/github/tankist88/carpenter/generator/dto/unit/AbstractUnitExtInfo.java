package com.github.tankist88.carpenter.generator.dto.unit;

import com.github.tankist88.carpenter.core.dto.unit.AbstractUnitBaseInfo;

public abstract class AbstractUnitExtInfo extends AbstractUnitBaseInfo implements ClassExtInfo {
    private String body;
    private boolean newUnit;

    public AbstractUnitExtInfo() {
        this(null, null, null);
    }

    public AbstractUnitExtInfo(String className, String unitName, String body) {
        this(className, unitName, body, true);
    }

    public AbstractUnitExtInfo(String className, String unitName, String body, boolean newUnit) {
        super(className, unitName);
        this.body = body;
        this.newUnit = newUnit;
    }

    @Override
    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public boolean newUnit() {
        return newUnit;
    }

    public void setNewUnit(boolean newUnit) {
        this.newUnit = newUnit;
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
