package org.carpenter.generator.command;

import org.apache.commons.lang3.SerializationUtils;
import org.carpenter.core.dto.argument.GeneratedArgument;
import org.carpenter.core.dto.unit.method.MethodCallInfo;
import org.carpenter.core.dto.unit.method.MethodCallTraceInfo;
import org.carpenter.core.exception.CallerNotFoundException;
import org.carpenter.core.property.GenerationProperties;
import org.carpenter.core.property.GenerationPropertiesFactory;
import org.carpenter.generator.exception.DeserializationException;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import static org.carpenter.core.property.AbstractGenerationProperties.OBJ_FILE_EXTENSION;
import static org.carpenter.generator.util.GenerateUtil.getFileList;

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

        Map<String, MethodCallTraceInfo> inDataMap = new HashMap<>();
        Map<String, GeneratedArgument> outDataMap = new HashMap<>();

        Map<String, Set<MethodCallTraceInfo>> commonDataMap = new HashMap<>();

        for (File objDump : getFileList(new File(props.getObjectDumpDir()), OBJ_FILE_EXTENSION)) {
            DataInputStream dis = new DataInputStream(new FileInputStream(objDump));
            String filename = objDump.getName();
            int length = dis.readInt();
            byte[] data = new byte[length];
            int byteReaded = dis.read(data);
            if(byteReaded != length) throw new DeserializationException("Can't deserialize object dump " + filename);
            if(filename.contains("_IN")) {
                MethodCallTraceInfo callTraceInfo = SerializationUtils.deserialize(data);
                String key = filename.substring(0, filename.indexOf("_IN"));
                inDataMap.put(key, callTraceInfo);
            } else if(filename.contains("_OUT")) {
                GeneratedArgument returnValue = SerializationUtils.deserialize(data);
                String key = filename.substring(0, filename.indexOf("_OUT"));
                outDataMap.put(key, returnValue);
            }
            dis.close();
        }
        for(Map.Entry<String, MethodCallTraceInfo> entry : inDataMap.entrySet()) {
            GeneratedArgument retVal = outDataMap.get(entry.getKey());
            if(retVal != null) {
                MethodCallTraceInfo value = entry.getValue();
                value.setReturnArg(retVal);
                Set<MethodCallTraceInfo> methodSet = commonDataMap.get(value.getKey());
                if (methodSet == null) {
                    methodSet = new HashSet<>();
                    commonDataMap.put(value.getKey(), methodSet);
                }
                methodSet.add(value);
            }
        }
        for(Set<MethodCallTraceInfo> values : commonDataMap.values()) {
            for(MethodCallTraceInfo value : values) {
                String upLevelKey = value.getTraceAnalyzeData().getUpLevelElementKey();
                if(upLevelKey == null) throw new CallerNotFoundException("FATAL ERROR. Can't determine caller for " + value);
                Set<MethodCallTraceInfo> callers = commonDataMap.get(upLevelKey);
                if(callers != null) {
                    for (MethodCallTraceInfo m : callers) {
                        if(m != null && !m.getKey().equals(value.getKey())) m.getInnerMethods().add(value);
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
