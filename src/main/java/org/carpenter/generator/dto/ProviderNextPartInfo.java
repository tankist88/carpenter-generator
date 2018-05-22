package org.carpenter.generator.dto;

import org.carpenter.generator.dto.unit.ClassExtInfo;

import java.io.Serializable;
import java.util.Set;

public class ProviderNextPartInfo implements Serializable {
    private String className;
    private Set<ClassExtInfo> methods;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Set<ClassExtInfo> getMethods() {
        return methods;
    }

    public void setMethods(Set<ClassExtInfo> methods) {
        this.methods = methods;
    }
}
