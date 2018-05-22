package org.carpenter.generator.command;

import org.carpenter.core.dto.unit.ClassInfo;

import java.util.List;

public class AbstractReturnClassInfoCommand<T extends ClassInfo> implements ReturnCommand {
    @Override
    public void execute() {

    }

    @Override
    public List<T> returnResult() {
        return null;
    }
}
