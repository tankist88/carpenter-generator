package com.github.tankist88.carpenter.generator;

import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static com.github.tankist88.carpenter.generator.util.zip.FileExtractUtils.extractZip;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.testng.Assert.assertEquals;

public class TestGeneratorTest {
    @Test
    public void generateTest() throws IOException {
        String tmpDir = System.getProperty("java.io.tmpdir");
        deleteDirectory(new File(tmpDir + "/trace_dump"));
        deleteDirectory(new File(tmpDir + "/ut_gen"));
        InputStream zipFileIs = getClass().getClassLoader().getResourceAsStream("trace_dump.zip");
        extractZip(zipFileIs, new File(tmpDir));
        int generatedTests = TestGenerator.runGenerator();
        assertEquals(generatedTests, 262);
//        deleteDirectory(new File(tmpDir + "/trace_dump"));
//        deleteDirectory(new File(tmpDir + "/ut_gen"));
    }
}
