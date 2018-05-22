package org.carpenter.generator.util;

import org.apache.commons.io.FileUtils;
import org.carpenter.core.property.GenerationProperties;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
}
