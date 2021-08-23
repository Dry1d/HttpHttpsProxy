/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package HttpHttpsProxy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import static HttpHttpsProxy.HttpHttpsProxy.fs;
import static HttpHttpsProxy.HttpHttpsProxy.blockedSites;

/**
 *
 * @author Dry1d
 */
public class Download {

    static Log log = HttpHttpsProxy.log;

    //Т.к. сайт минъюста кривой и страничка формируется неприлично долго, требуется
    // качать страницу через jsoup, возможно это можно сделать по другому, но пока не знаю как
    public static boolean download_php(String url) {

        blockedSites = new HashMap<>();

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Chrome/4.0.249.0 Safari/532.5")
                    .referrer("http://www.google.com")
                    .get();
            Element body = doc.body();
            String[] bd = body.text().split(" ");
            for (String b : bd) {
                String[] urlDomain = b.split(";");

                if (urlDomain.length == 2) {//для строки, длинной в 2 ячейки
                    if (!urlDomain[1].equals("")) {
                        if (urlDomain[1].contains(",")) {
                            String[] splitted_domain = urlDomain[1].split(",");
                            for (String s_domain : splitted_domain) {
                                HttpHttpsProxy.parseDomain(s_domain);
                            }
                        } else {
                            HttpHttpsProxy.parseDomain(urlDomain[1]);
                        }
                    }
                }
                if (urlDomain.length >= 3) {//для строки, длинной >=3 ячеек

//                    log.add(log,urlDomain[1]+";"+urlDomain[2]);
                    if (!urlDomain[1].equals("")) {

//                        log.add(log,urlDomain[1]);
                        if (urlDomain[1].contains(",")) {
                            String[] splitted_domain = urlDomain[1].split(",");
                            for (String s_domain : splitted_domain) {
                                HttpHttpsProxy.parseDomain(s_domain);
//                                String url_s = blockedSites.put(s_domain, s_domain);
//                                if (url_s == null) {
//                                    log.add(log,"Домен был успешно добавлен: " + s_domain);
//                                }

                            }
                        } else {

                            HttpHttpsProxy.parseDomain(urlDomain[1]);
//                            String site = blockedSites.put(urlDomain[1], urlDomain[1]);
//                            if (site == null) {
//                                log.add(log,"Домен был успешно добавлен: " + site);
//                            }
                        }
                    }
                    if (!urlDomain[2].equals("")) {
                        if (urlDomain[2].contains(",")) {
                            String[] splitted_url = urlDomain[2].split(",");
                            for (String s_url : splitted_url) {
                                String url_s = blockedSites.put(s_url, s_url);
                                if (url_s == null) {
                                    log.add(log, "Домен был успешно добавлен: " + url_s);
                                }
                                //Попробуем еще воткнуть домен в blockedSites
                                HttpHttpsProxy.parseDomain(s_url);
                            }
                        } else {
                            String url_s = blockedSites.put(urlDomain[2], urlDomain[2]);
                            if (url_s != null) {
                                log.add(log, "Домен был успешно добавлен: " + url_s);
                            }
                            HttpHttpsProxy.parseDomain(urlDomain[2]);
                        }
                    }

                    //Можно еще добавлять в списки IP адреса, но пока не актуально
                }
//                log.add(log,urlDomain.length);
            }
//            log.add(log,"Blocked_sites: ");
//            for (String key : blocked_sites.keySet()) {
//                    log.add(log,key);
//                }

            return true;
        } catch (IOException ex) {
            log.add(log, ex.toString());
            return false;
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

    public static void getblacklists() {
        //Качаем список запрещенных сайтов с минъюста
        //Получение текущего содержания реестра: URL: http://api.antizapret.info/all.php
        Download.download_php("http://api.antizapret.info/all.php");
        //Получение текущего содержания базы Минюста: URL: http://api.antizapret.info/minjust.php
        Download.download_php("http://api.antizapret.info/minjust.php");

        //Не помешает еще скачать архив https://www.shallalist.de/Downloads/shallalist.tar.gz
//                https://www.shallalist.de/Downloads/shallalist.tar.gz.md5
        File tarbal = new File("downloads" + fs + "shallalist.tar.gz");
        File md5 = new File("downloads" + fs + "shallalist.tar.gz.md5");
        //Если файлы существуют, удаляем
        if (tarbal.exists()) {
            tarbal.delete();
        }
        if (md5.exists()) {
            md5.delete();
        }
        //Качаем файлы архива и md5 суммы
        try {
            Download.download(tarbal, new URL("https://www.shallalist.de/Downloads/shallalist.tar.gz"));
            Download.download(md5, new URL("https://www.shallalist.de/Downloads/shallalist.tar.gz.md5"));
        } catch (MalformedURLException ex) {
            log.add(log, ex.toString());
        }
        try {
            String md5summ_file = FileSecret.getMd5Summ(tarbal.getAbsolutePath());
            String file_md5 = FileOperations.readMd5File(md5);
            String md5Summ_str = file_md5.substring(0, file_md5.indexOf(" "));
            if (md5summ_file.equals(md5Summ_str)) {
                if (file_md5.substring(file_md5.indexOf(" ") + 2, file_md5.length()).equals(tarbal.getName())) {

                    File destTar = new File(tarbal.getParentFile().getAbsolutePath() + fs + "tarball");
                    File destDir = new File(tarbal.getParentFile().getAbsolutePath() + fs + "tarball");

                    if (destDir.exists()) {
                        destDir.delete();
                    }
                    if (destTar.exists()) {
                        destTar.delete();
                    }
                    destTar.mkdirs();

                    //распаковываем
                    Archive.unTar(Archive.unGzip(tarbal, destTar), destDir);
                    //Потом требуется перебрать все файлы и папки и добавить домены и url к нашим спискам
                    File tarbal_folder = new File(tarbal.getParentFile().getAbsolutePath() + fs + "tarball" + fs + "BL");

                    HttpHttpsProxy.processFilesFromFolder(tarbal_folder);

                }
            } else {
                //Выводим ошибку
                log.add(log, "Не совпадает сумма md5");
            }
//            System.exit(0);
        } catch (Exception ex) {
            log.add(log, ex.toString());
        }
    }
}
