package com.github.tankist88.carpenter.generator.extension.assertext;

import com.github.tankist88.carpenter.core.dto.argument.GeneratedArgument;

public interface AssertExtension {
    boolean isTypeSupported(GeneratedArgument returnValue);
    String getAssertBlock(String actual, String expected);
}
