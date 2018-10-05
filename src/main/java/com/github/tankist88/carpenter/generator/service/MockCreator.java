package com.github.tankist88.carpenter.generator.service;

import com.github.tankist88.carpenter.core.dto.unit.field.FieldProperties;
import com.github.tankist88.carpenter.core.dto.unit.method.MethodCallInfo;
import com.github.tankist88.carpenter.generator.dto.PreparedMock;
import com.github.tankist88.carpenter.generator.dto.SpyMaps;

import java.util.Set;

public interface MockCreator {
    PreparedMock createMock(Set<MethodCallInfo> multiInner, Set<FieldProperties> serviceClasses, SpyMaps spyMaps);
}
