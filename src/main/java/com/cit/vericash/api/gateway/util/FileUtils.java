package com.cit.vericash.api.gateway.util;

import java.io.*;

public class FileUtils {
    public static boolean writeToFile(File file, String content){
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile()))) {
            bw.write(content);
        }
        catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static String getFileContents (String path){
        StringBuilder content = new StringBuilder();
        File fileToRead = new File(path);
        if(fileToRead.exists()) {
            try (FileReader fileStream = new FileReader(fileToRead);
                 BufferedReader bufferedReader = new BufferedReader(fileStream)) {
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    content.append(line);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
        return content.toString();
    }
}
