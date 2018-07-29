package com.github.tankist88.carpenter.generator.command;

import com.github.tankist88.carpenter.core.dto.unit.ClassInfo;

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
