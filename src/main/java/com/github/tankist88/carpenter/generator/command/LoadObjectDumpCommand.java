package com.github.tankist88.carpenter.generator.command;

import com.github.tankist88.carpenter.core.dto.unit.method.MethodCallInfo;
import com.github.tankist88.carpenter.core.dto.unit.method.MethodCallTraceInfo;
import com.github.tankist88.carpenter.core.exception.CallerNotFoundException;
import com.github.tankist88.carpenter.core.property.GenerationProperties;
import com.github.tankist88.carpenter.core.property.GenerationPropertiesFactory;
import com.github.tankist88.carpenter.generator.exception.DeserializationException;
import com.github.tankist88.carpenter.generator.util.GenerateUtil;
import org.apache.commons.lang3.SerializationUtils;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import static com.github.tankist88.carpenter.core.property.AbstractGenerationProperties.OBJ_FILE_EXTENSION;

public class LoadObjectDumpCommand extends AbstractReturnClassInfoCommand<MethodCallInfo> {

    private GenerationProperties props;

    private List<MethodCallInfo> objectDataList;

    public LoadObjectDumpCommand() {
        this.props = GenerationPropertiesFactory.loadProps();
    }

    @Override
    public void execute() {
        super.execute();
        try {
            objectDataList = loadObjectData();
        } catch (Exception e) {
            objectDataList = new ArrayList<>();
            e.printStackTrace();
        }
    }

    @Override
    public List<MethodCallInfo> returnResult() {
        return objectDataList;
    }

    private List<MethodCallInfo> loadObjectData() throws DeserializationException, IOException, CallerNotFoundException {
        List<MethodCallInfo> result = new ArrayList<>();

        Map<String, Set<MethodCallTraceInfo>> commonDataMap = new HashMap<>();

        for (File objDump : GenerateUtil.getFileList(new File(props.getObjectDumpDir()), OBJ_FILE_EXTENSION)) {
            DataInputStream dis = new DataInputStream(new FileInputStream(objDump));
            String filename = objDump.getName();
            int length = dis.readInt();
            byte[] data = new byte[length];
            int byteReaded = dis.read(data);
            dis.close();

            MethodCallTraceInfo callTraceInfo;
            try {
                if(byteReaded != length) throw new DeserializationException("Can't deserialize object dump " + filename);
                callTraceInfo = SerializationUtils.deserialize(data);
            } catch (Exception ioex) {
                System.err.println("WARNING!!! Can't deserialize file " + filename + ". " + ioex.getMessage());
                continue;
            }

            Set<MethodCallTraceInfo> methodSet = commonDataMap.get(callTraceInfo.getKey());
            if (methodSet == null) {
                methodSet = new HashSet<>();
                commonDataMap.put(callTraceInfo.getKey(), methodSet);
            }
            methodSet.add(callTraceInfo);
        }
        for (Set<MethodCallTraceInfo> values : commonDataMap.values()) {
            for (MethodCallTraceInfo value : values) {
                String upLevelKey = value.getTraceAnalyzeData().getUpLevelElementKey();
                if (upLevelKey == null) throw new CallerNotFoundException("FATAL ERROR. Can't determine caller for " + value);
                Set<MethodCallTraceInfo> callers = commonDataMap.get(upLevelKey);
                if (callers != null) {
                    for (MethodCallTraceInfo m : callers) {
                        if (m != null && !m.getKey().equals(value.getKey())) m.getInnerMethods().add(value);
                    }
                }
            }
        }
        for(Set<MethodCallTraceInfo> values : commonDataMap.values()) {
            result.addAll(values);
        }
        return result;
    }
}
