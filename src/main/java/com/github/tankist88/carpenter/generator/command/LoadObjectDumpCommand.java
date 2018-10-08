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
import java.util.*;

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
                int byteReaded = dis.read(data);
                dis.close();
                if (byteReaded != length) {
                    throw new DeserializationException("Can't deserialize object dump " + filename);
                }
                callTraceInfo = SerializationUtils.deserialize(data);
            } catch (Exception ex) {
                throw new DeserializationException(ex.getMessage(), ex);
            }

            if (callTraceInfo.getKey().contains("sendClientNotification")) {
                int a = 2;
            }

            List<MethodCallTraceInfo> methodSet = commonDataMap.get(callTraceInfo.getKey());
            if (methodSet == null) {
                methodSet = new ArrayList<>();
                commonDataMap.put(callTraceInfo.getKey(), methodSet);
            }
            methodSet.add(callTraceInfo);
        }

        List<MethodCallTraceInfo> sortedList = new ArrayList<>();
        for (Set<MethodCallTraceInfo> values : commonDataMap.values()) {
            sortedList.addAll(values);
        }
        sortMethodCallInfos(sortedList);
        for (MethodCallTraceInfo value : sortedList) {
            String upLevelKey = value.getTraceAnalyzeData().getUpLevelElementKey();
            if (upLevelKey.contains("sendClientNotification")) {
                int a = 2;
            }
            if (upLevelKey == null) {
                throw new CallerNotFoundException("FATAL ERROR!!! Can't determine caller for " + value);
            }
            Set<MethodCallTraceInfo> callers = commonDataMap.get(upLevelKey);
            if (callers != null) {
                for (MethodCallTraceInfo m : callers) {
                    if (m != null && !m.getKey().equals(value.getKey()) && value.getStartTime() >= m.getStartTime() && value.getEndTime() <= m.getEndTime()) {
                        m.getInnerMethods().add(value);
                    }
                }
            }
        }

        return new ArrayList<MethodCallInfo>(sortedList);
    }
}
