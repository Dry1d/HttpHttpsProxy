/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package HttpHttpsProxy;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Dry1d
 */
public class Log {

    private static Thread writer;
    private static final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue();
    static String fs = System.getProperty("file.separator");

    void logWrite(String logfile) {
        writer = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean flag = true;
                while (flag) {
                    try {
                        if (queue.isEmpty() && !HttpHttpsProxy.running) {
                            flag = false;
                        }
                        String msg = queue.poll(1, TimeUnit.SECONDS);

                        if (msg != null) {
                            LocalDateTime dateTime = LocalDateTime.now();
                            String date = dateTime.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                            msg = msg.replace("\n\n", "\n");
                            msg = msg.replace("\n", ";");
                            msg = date + ";" + msg;

                            File logs_folder = new File("logs");
                            File[] files = logs_folder.listFiles();
                            for (File file : files) {
                                //Если время последнего изменения файла больше 30 суток
                                long timestamp = System.currentTimeMillis();
                                if (file.lastModified() > (timestamp - 1000 * 60 * 60 * 24 * 30)) {
                                    file.delete();
                                }
                            }

                            String path = "logs" + fs + logfile + "-" + dateTime.format(DateTimeFormatter.ISO_DATE) + ".log";
                            FileOperations.appendToFile(path, msg+"\n");
                        }
                    } catch (InterruptedException ex) {
                        queue.add(ex.toString());
                    }
                }
            }
        });
        writer.start();
    }

    public void add(Log log, String msg) {
        synchronized (log) {
            queue.add(msg);
        }
    }

}
