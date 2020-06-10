package org.abstracthorizon.extend.repo.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class IOUtils {

    public static String asString(InputStream stream) throws IOException {
        StringBuilder res = new StringBuilder();
        try {
            byte[] buffer = new byte[10240];
            int r = stream.read(buffer);
            while (r > 0) {
                res.append(new String(buffer, 0, r));
                r = stream.read(buffer);
            }
        } finally {
            stream.close();
        }
        return res.toString();
    }
    
    public static String asString(File file) throws IOException {
        return asString(new FileInputStream(file));
    }
       
    public static List<String> streamToStringArray(InputStream stream) throws IOException {
        BufferedReader bufferedStream = new BufferedReader(new InputStreamReader(stream));
        List<String> res = new ArrayList<String>();
        String s = bufferedStream.readLine();
        while (s != null) {
            res.add(s);
            s = bufferedStream.readLine();
        }
        return res;
    }
    
    public List<String> fileToStringArray(File file) throws IOException {
        return streamToStringArray(new FileInputStream(file));
    }
}
