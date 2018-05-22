package org.carpenter.generator.dto;

import org.carpenter.core.dto.unit.method.MethodCallInfo;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class SeparatedInners implements Serializable {
    private Set<MethodCallInfo> singleInners;
    private Set<Set<MethodCallInfo>> multipleInners;

    public Set<MethodCallInfo> getSingleInners() {
        if(singleInners == null) {
            singleInners = new HashSet<>();
        }
        return singleInners;
    }

    public Set<Set<MethodCallInfo>> getMultipleInners() {
        if(multipleInners == null) {
            multipleInners =  new HashSet<>();
        }
        return multipleInners;
    }
}
