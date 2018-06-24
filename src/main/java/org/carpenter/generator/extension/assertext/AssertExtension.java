package org.carpenter.generator.extension.assertext;

import org.carpenter.core.dto.argument.GeneratedArgument;

public interface AssertExtension {
    boolean isTypeSupported(GeneratedArgument returnValue);
    String getAssertBlock(String dataProviderMethod);
}
