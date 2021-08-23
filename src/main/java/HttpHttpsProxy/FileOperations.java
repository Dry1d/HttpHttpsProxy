/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package HttpHttpsProxy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Dry1d
 */
public class FileOperations {

    static Log log = HttpHttpsProxy.log;

    public static String readMd5File(File md5) {
        FileReader ir = null;
        String line = null;
        try {
            ir = new FileReader(md5);
            BufferedReader reader = new BufferedReader(ir);
            line = reader.readLine();
//            while (line != null) {
//                line = reader.readLine();
//            }
        } catch (FileNotFoundException ex) {
            log.add(log, ex.toString());
        } catch (IOException ex) {
            log.add(log, ex.toString());
        } finally {
            try {
                ir.close();
            } catch (IOException ex) {
                log.add(log, ex.toString());
            }
        }
        return (line);
    }

    public static List<String> readfile(File f) {
        List<String> list = new ArrayList();
        FileReader ir = null;
        try {
            ir = new FileReader(f);
            BufferedReader reader = new BufferedReader(ir);
            String line = reader.readLine();
            while (line != null) {
                list.add(line);
                line = reader.readLine();
            }
        } catch (FileNotFoundException ex) {
            log.add(log, ex.toString());
        } catch (IOException ex) {
            log.add(log, ex.toString());
        } finally {
            try {
                ir.close();
            } catch (IOException ex) {
                log.add(log, ex.toString());
            }
        }
        return (list);
    }

    public static void appendToFile(String path, String msg) {
        File file = new File(path);
        FileWriter fr = null;
        try {

//            if(!file.exists()){
//                file.createNewFile();
//            }
            fr = new FileWriter(file, true);
            fr.write(msg);
            fr.close();
//            Files.write(Paths.get(path), msg.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException ex) {
            log.add(log, ex.toString());
        } 

    }
}
