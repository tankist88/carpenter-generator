package org.carpenter.generator.service;

import org.carpenter.core.dto.unit.method.MethodCallInfo;
import org.carpenter.generator.command.LoadObjectDumpCommand;
import org.carpenter.generator.command.ReturnCommand;

import java.util.List;

public class LoadDataService {
    @SuppressWarnings("unchecked")
    public List<MethodCallInfo> loadObjectDump() {
        ReturnCommand<MethodCallInfo> command = new LoadObjectDumpCommand();
        command.execute();
        return command.returnResult();
    }
}
