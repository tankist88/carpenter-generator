package com.github.tankist88.carpenter.generator.command;

import com.github.tankist88.carpenter.core.dto.unit.method.MethodCallInfo;
import com.github.tankist88.carpenter.core.dto.unit.method.MethodCallTraceInfo;
import com.github.tankist88.carpenter.core.exception.CallerNotFoundException;
import com.github.tankist88.carpenter.core.property.GenerationProperties;
import com.github.tankist88.carpenter.core.property.GenerationPropertiesFactory;
import com.github.tankist88.carpenter.generator.exception.DeserializationException;
import org.apache.commons.lang3.SerializationUtils;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tankist88.carpenter.core.property.AbstractGenerationProperties.OBJ_FILE_EXTENSION;
import static com.github.tankist88.carpenter.generator.util.GenerateUtils.getFileList;
import static com.github.tankist88.carpenter.generator.util.GenerateUtils.sortMethodCallInfos;

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

    private List<MethodCallInfo> loadObjectData() throws CallerNotFoundException, DeserializationException {
        Map<String, List<MethodCallTraceInfo>> commonDataMap = loadMethodsFromFiles();
        linkingMethods(commonDataMap);
        return applyDuplicationFilter(commonDataMap);
    }

    private List<MethodCallInfo> applyDuplicationFilter(Map<String, List<MethodCallTraceInfo>> commonDataMap) {
        List<MethodCallInfo> filteredList = new ArrayList<>();
        for (List<MethodCallTraceInfo> values : commonDataMap.values()) {
            sortMethodCallInfos(values);
            MethodCallTraceInfo maxInnersCall = values.iterator().next();
            for (MethodCallTraceInfo value : values) {
                if (value.getInnerMethods().size() > maxInnersCall.getInnerMethods().size()) {
                    maxInnersCall = value;
                }
            }
            filteredList.add(maxInnersCall);
        }
        return filteredList;
    }

    private void linkingMethods(Map<String, List<MethodCallTraceInfo>> commonDataMap) throws CallerNotFoundException {
        for (List<MethodCallTraceInfo> values : commonDataMap.values()) {
            for (MethodCallTraceInfo value : values) {
                String upLevelKey = value.getTraceAnalyzeData().getUpLevelElementKey();
                if (upLevelKey == null) {
                    throw new CallerNotFoundException("FATAL ERROR!!! Can't determine caller for " + value);
                }
                List<MethodCallTraceInfo> callers = commonDataMap.get(upLevelKey);
                if (callers == null) continue;
                for (MethodCallTraceInfo m : callers) {
                    if (m == null) continue;
                    if (m.getKey().equals(value.getKey())) continue;
                    if (value.getStartTime() >= m.getStartTime() && value.getEndTime() <= m.getEndTime()) {
                        m.getInnerMethods().add(value);
                    }
                }
            }
        }
    }

    private Map<String, List<MethodCallTraceInfo>> loadMethodsFromFiles() throws DeserializationException {
        Map<String, List<MethodCallTraceInfo>> commonDataMap = new HashMap<>();

        for (File objDump : getFileList(new File(props.getObjectDumpDir()), OBJ_FILE_EXTENSION)) {
            String filename = objDump.getName();
            MethodCallTraceInfo callTraceInfo;
            try {
                DataInputStream dis = new DataInputStream(new FileInputStream(objDump));
                if (dis.available() <= 0) {
                    System.out.println("WARNING! Empty file " + filename + ".");
                    continue;
                }
                int length = dis.readInt();
                byte[] data = new byte[length];
                int byteRead = dis.read(data);
                dis.close();
                if (byteRead != length) {
                    throw new DeserializationException("Can't deserialize object dump " + filename);
                }
                callTraceInfo = SerializationUtils.deserialize(data);
            } catch (Exception ex) {
                throw new DeserializationException(ex.getMessage(), ex);
            }

            List<MethodCallTraceInfo> methodList = commonDataMap.get(callTraceInfo.getKey());
            if (methodList == null) {
                methodList = new ArrayList<>();
                commonDataMap.put(callTraceInfo.getKey(), methodList);
            }
            methodList.add(callTraceInfo);
        }
        return commonDataMap;
    }
}
