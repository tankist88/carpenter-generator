package com.github.tankist88.carpenter.generator.util;

import com.github.tankist88.carpenter.core.property.GenerationProperties;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.github.tankist88.object2source.util.GenerationUtil.downFirst;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class GenerateUtil {
    public static boolean allowedPackage(String classname, GenerationProperties props) {
        for(String p : props.getAllowedPackagesForTests()) {
            if(classname.startsWith(p)) return true;
        }
        return false;
    }

    public static String createAndReturnPathName(GenerationProperties props) throws IOException {
        String pathname = props.getUtGenDir();
        FileUtils.forceMkdir(new File(pathname));
        return pathname;
    }

    public static List<File> getFileList(File inDir, String extension) {
        List<File> result = new ArrayList<>();
        if(!inDir.exists()) return result;
        if(inDir.isDirectory()) {
            File[] listFiles = inDir.listFiles();
            if(listFiles != null) {
                for (File f : listFiles) {
                    if (f.isDirectory()) {
                        result.addAll(getFileList(f, extension));
                    } else if (f.getName().endsWith(extension)) {
                        result.add(f);
                    }
                }
            }
        } else {
            throw new IllegalArgumentException(inDir.getName() + " not a directory");
        }
        return result;
    }

    public static String createVarNameFromMethod(String methodSig) {
        if (isBlank(methodSig)) return null;
        String methodName = methodSig.contains("(") ? methodSig.substring(0, methodSig.indexOf("(")) : methodSig;
        return downFirst(methodName);
    }
}
