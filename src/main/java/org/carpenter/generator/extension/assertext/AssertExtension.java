package org.carpenter.generator.extension.assertext;

import org.carpenter.core.dto.argument.GeneratedArgument;
import org.carpenter.generator.dto.source.MethodLine;

public interface AssertExtension {
    boolean isTypeSupported(GeneratedArgument returnValue);
    MethodLine getAssertBlock(String dataProviderMethod);
}
