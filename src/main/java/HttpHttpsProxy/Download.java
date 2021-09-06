/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package HttpHttpsProxy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.net.URLConnection;
import java.util.HashMap;

/**
 *
 * @author Dry1d
 */
public class Download {

    static Log log = HttpHttpsProxy.log;

    //Т.к. сайт минъюста кривой и страничка формируется неприлично долго, требуется
    // качать страницу через jsoup, возможно это можно сделать по другому, но пока не знаю как
    public static HashMap download_php(String url) {

        HashMap<String, String> urls = new HashMap();

        try {
            //Маскируемся под хром
            Document doc = Jsoup.connect(url)
                    //Страница огромная, загружается страсть как долго
                    .timeout(180*1000)
                    .userAgent("Chrome/4.0.249.0 Safari/532.5")
                    .referrer("http://www.google.com")
                    .get();
            Element body = doc.body();
            String[] bd = body.text().split(" ");
            for (String b : bd) {
                urls.put(b,b);
            }
//            log.add(log,"Blocked_sites: ");
//            for (String key : blocked_sites.keySet()) {
//                    log.add(log,key);
//                }
            return (urls);
        } catch (IOException ex) {
            log.add(log, ex.toString());
            return null;
        }
    }

    public static boolean download(File file, URL url) {
        try ( ReadableByteChannel rbc = Channels.newChannel(url.openStream());  FileOutputStream fos = new FileOutputStream(file)) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            return true;
//            sleep(10300);
        } catch (Exception e) {
            log.add(log, e.toString());
//            log.add(log, "Download", e.toString());
            return false;
        }
    }
public static long date_url(URL url) {
        
        try {
            URLConnection conn = url.openConnection();
            long date = conn.getDate();
            return(date);
        } catch (Exception e) {
            System.out.println(e);
        }
        return 0;
        
    }
}
