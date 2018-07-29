package com.github.tankist88.carpenter.generator.service;

import com.github.tankist88.carpenter.core.dto.unit.method.MethodCallInfo;
import com.github.tankist88.carpenter.generator.command.LoadObjectDumpCommand;
import com.github.tankist88.carpenter.generator.command.ReturnCommand;

import java.util.List;

public class LoadDataService {
    @SuppressWarnings("unchecked")
    public List<MethodCallInfo> loadObjectDump() {
        ReturnCommand<MethodCallInfo> command = new LoadObjectDumpCommand();
        command.execute();
        return command.returnResult();
    }
}
