/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package HttpHttpsProxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Proxy creates a Server Socket which will wait for connections on the
 * specified port. Once a connection arrives and a socket is accepted, the Proxy
 * creates a RequestHandler object on a new thread and passes the socket to it
 * to be handled. This allows the Proxy to continue accept further connections
 * while others are being handled.
 *
 * The Proxy class is also responsible for providing the dynamic management of
 * the proxy through the console and is run on a separate thread in order to not
 * interrupt the acceptance of socket connections. This allows the administrator
 * to dynamically block web sites in real time.
 *
 * The Proxy server is also responsible for maintaining cached copies of the any
 * websites that are requested by clients and this includes the HTML markup,
 * images, css and js files associated with each webpage.
 *
 * Upon closing the proxy server, the HashMaps which hold cached items and
 * blocked sites are serialized and written to a file and are loaded back in
 * when the proxy is started once more, meaning that cached and blocked sites
 * are maintained.
 *
 */
public class HttpHttpsProxy implements Runnable {

    public static Log log = new Log();
    static String fs = System.getProperty("file.separator");
    private RequestHandler runnable = null;

    List<File> list_files = new ArrayList();

    //Файл с данными последнего апдейта
    static String latest_update_BL = "latestUpdate.xml";
    static Properties luBL = new Properties();
    static String latestRemoteTarballUpdate;
    static String latestFullUpdate;

    public static void main(String[] args) {

        log.logWrite("log");

        if (new File(latest_update_BL).exists()) {
            try {

                luBL.loadFromXML(new FileInputStream(latest_update_BL));

                latestRemoteTarballUpdate = luBL.getProperty("latestRemoteTarballUpdate");
                latestFullUpdate = luBL.getProperty("latestFullUpdate");

            } catch (FileNotFoundException ex) {
                log.add(log, getStackTrace(ex));
            } catch (IOException ex) {
                log.add(log, getStackTrace(ex));
            }
        } else {
            try {
                luBL.setProperty("latestRemoteTarballUpdate", "0");
                luBL.setProperty("latestFullUpdate", "0");
                luBL.storeToXML(new FileOutputStream(latest_update_BL), "store to xml file");
            } catch (FileNotFoundException ex) {
                log.add(log, getStackTrace(ex));
            } catch (IOException ex) {
                log.add(log, getStackTrace(ex));
            }
        }

        //Создаём папку cached чтобы не было экспешнов
        File cached = new File("cached");
        if (!cached.exists()) {
            if (!cached.mkdir()) {
                log.add(log, "Cannot create folder \"cached\"");
            }
        }
        File downloads = new File("downloads");
        if (!downloads.exists()) {
            if (!downloads.mkdir()) {
                log.add(log, "Cannot create folder \"downloads\"");
            }
        }
        //Создаём папку blacklists для хранения списков блокировки
        File blacklists = new File("blacklists");
        if (!blacklists.exists()) {
            if (!blacklists.mkdir()) {
                log.add(log, "Cannot create folder \"blacklists\"");
            }
        }
        File logs = new File("logs");
        if (!logs.exists()) {
            if (!logs.mkdir()) {
                log.add(log, "Cannot create folder \"logs\"");
            }
        }

        // Create an instance of Proxy and begin listening for connections
        HttpHttpsProxy myProxy = new HttpHttpsProxy(8085);
        myProxy.listen();
    }

    private ServerSocket serverSocket;

    /**
     * Semaphore for Proxy and Consolee Management System.
     */
    public static volatile boolean running = true;

    /**
     * Data structure for constant order lookup of cache items. Key: URL of
     * page/image requested. Value: File in storage associated with this key.
     */
    static HashMap<String, File> cache;

    /**
     * Data structure for constant order lookup of blocked sites. Key: URL of
     * page/image requested. Value: URL of page/image requested.
     */
    static HashMap<String, String> ipv4;
    static HashMap<String, String> ipv6;
    static HashMap<String, String> http_url;
    static HashMap<String, String> https_url;
    static HashMap<String, String> blockedSites;

    /**
     * ArrayList of threads that are currently running and servicing requests.
     * This list is required in order to join all threads on closing of server
     */
    static HashMap<Thread, RequestHandler> servicingThreads;
    File blockedSitesHashMapFile = new File("blacklists" + fs + "blockedSites.txt");

    /**
     * Create the Proxy Server
     *
     * @param port Port number to run proxy server from.
     */
    public HttpHttpsProxy(int port) {

        // Load in hash map containing previously cached sites and blocked Sites
        cache = new HashMap<>();

        //Эти 4 строки - задел на будущее, для фильтрации ipv4,ipv6 адресов и http/https urlов
        ipv4 = new HashMap<>();
        ipv6 = new HashMap<>();
        http_url = new HashMap<>();
        https_url = new HashMap<>();

        blockedSites = new HashMap<>();
        // Create array list to hold servicing threads
        servicingThreads = new HashMap<>();

        // Start dynamic manager on a separate thread.
        new Thread(this).start();	// Starts overriden run() method at bottom

        try {
            // Load in cached sites from file
            File cachedSites = new File("cachedSites.txt");
            if (!cachedSites.exists()) {
                log.add(log, "No cached sites found - creating new file");
                cachedSites.createNewFile();
            } else {
                ObjectInputStream objectInputStream;
                try ( FileInputStream fileInputStream = new FileInputStream(cachedSites)) {
                    objectInputStream = new ObjectInputStream(fileInputStream);
                    cache = (HashMap<String, File>) objectInputStream.readObject();
                }
                objectInputStream.close();
            }

            // Load in blocked sites from file
            if (!blockedSitesHashMapFile.exists()) {
                log.add(log, "No blocked sites found - creating new file");
                blockedSitesHashMapFile.createNewFile();
            } else {
                ObjectInputStream objectInputStream;
                try ( FileInputStream fileInputStream = new FileInputStream(blockedSitesHashMapFile)) {
                    objectInputStream = new ObjectInputStream(fileInputStream);
                    blockedSites = (HashMap<String, String>) objectInputStream.readObject();
                    //Очистим файл, чтобы не было эксепшена при следующем запуске
                    long lstMdfd = blockedSitesHashMapFile.lastModified();
                    blockedSitesHashMapFile.delete();
                    blockedSitesHashMapFile.createNewFile();
                    blockedSitesHashMapFile.setLastModified(lstMdfd);

                }
                objectInputStream.close();
            }
        } catch (IOException e) {
            log.add(log, "Error loading previously cached sites file");
            log.add(log, getStackTrace(e));
        } catch (ClassNotFoundException e) {
            log.add(log, "Class not found loading in preivously cached sites file");
            log.add(log, getStackTrace(e));
        }

        //Скачиваем blacklist
//        Download.getblacklists();
        //Проверка на существование файла
        log.add(log, "Проверка последнего обновления файла " + blockedSitesHashMapFile.getName());
        if (blockedSitesHashMapFile.exists()) {
            if (blockedSites.size() > 0) {
                //Если последняя модификация файла проходила больше 6 часов назад
                if ((System.currentTimeMillis() - blockedSitesHashMapFile.lastModified()) > 6 * 60 * 60 * 1000) {
                    getblacklists();
                } else {
                    log.add(log, "Файл недавно обновлялся");
                }
            } else {
                getblacklists();
            }

        } else {
            log.add(log, "Отсутствует файл для загрузки черных списков!!!");
            System.exit(1);
        }

        try {
            // Create the Server Socket for the Proxy 
            serverSocket = new ServerSocket(port);

            // Set the timeout
            //serverSocket.setSoTimeout(100000);	// debug
            log.add(log, "Waiting for client on port " + serverSocket.getLocalPort() + "..");
            running = true;
        } // Catch exceptions associated with opening socket
        catch (SocketException se) {
            log.add(log, "Socket Exception when connecting to client");
            log.add(log, getStackTrace(se));
        } catch (SocketTimeoutException ste) {
            log.add(log, "Timeout occured while connecting to client");
            log.add(log, getStackTrace(ste));
        } catch (IOException io) {
            log.add(log, "IO exception when connecting to client");
            log.add(log, getStackTrace(io));
        }

    }

    /**
     * Listens to port and accepts new socket connections. Creates a new thread
     * to handle the request and passes it the socket connection and continues
     * listening.
     */
    public void listen() {

        while (running) {
            try {
                // serverSocket.accpet() Blocks until a connection is made
                Socket socket = serverSocket.accept();

                runnable = new RequestHandler(socket);
                // Create new Thread and pass it Runnable RequestHandler
                Thread thread = new Thread(runnable);

                // Key a reference to each thread so they can be joined later if necessary
                servicingThreads.put(thread, runnable);

                thread.start();
            } catch (SocketException e) {
                // Socket exception is triggered by management system to shut down the proxy 
                log.add(log, "Server closed");
                log.add(log, getStackTrace(e));
            } catch (IOException e) {
                log.add(log, getStackTrace(e));
            }
        }
    }

    /**
     * Saves the blocked and cached sites to a file so they can be re loaded at
     * a later time. Also joins all of the RequestHandler threads currently
     * servicing requests.
     */
    private void closeServer() {
        log.add(log, "\nClosing Server..");
        running = false;
        try {
            try ( FileOutputStream fileOutputStream = new FileOutputStream("cachedSites.txt");  ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {

                objectOutputStream.writeObject(cache);
            }
            log.add(log, "Cached Sites written");
//            System.out.println("Cached Sites written");

            try ( FileOutputStream fileOutputStream2 = new FileOutputStream("blockedSites.txt");  ObjectOutputStream objectOutputStream2 = new ObjectOutputStream(fileOutputStream2)) {
                objectOutputStream2.writeObject(blockedSites);
            }
            log.add(log, "Blocked Site list saved");
//            System.out.println("Blocked Site list saved");

            try {
                // Close all servicing threads
                for (Map.Entry<Thread, RequestHandler> threadRequestHandler : servicingThreads.entrySet()) {
                    if (threadRequestHandler.getKey().isAlive()) {
                        System.out.println("Waiting on " + threadRequestHandler.getKey().getId() + " to close..");

                        //Завершаем поток
                        threadRequestHandler.getValue().terminate();

                        threadRequestHandler.getKey().join();
                        log.add(log, " closed");
                        System.out.println(threadRequestHandler.getKey().getId() + " closed");
                    }
                }
            } catch (InterruptedException e) {
                log.add(log, getStackTrace(e));
            }

        } catch (IOException e) {
            log.add(log, "Error saving cache/blocked sites");
            log.add(log, getStackTrace(e));
        }

        // Close Server Socket
        try {
//            System.out.println("Terminating Connection");
            log.add(log, "Terminating Connection..");
            serverSocket.close();
//            System.out.println("Terminated");
            System.exit(0);
        } catch (IOException e) {
            log.add(log, "Exception closing proxy's server socket");
            log.add(log, getStackTrace(e));
        }

    }

    /**
     * Looks for File in cache
     *
     * @param url of requested file
     * @return File if file is cached, null otherwise
     */
    public static File getCachedPage(String url) {
        return cache.get(url);
    }

    /**
     * Adds a new page to the cache
     *
     * @param urlString URL of webpage to cache
     * @param fileToCache File Object pointing to File put in cache
     */
    public static void addCachedPage(String urlString, File fileToCache) {
        cache.put(urlString, fileToCache);
    }

    /**
     * Check if a URL is blocked by the proxy
     *
     * @param url URL to check
     * @return true if URL is blocked, false otherwise
     */
    public static boolean isBlocked(String url) {
//        if(url.contains("pornhub")){
//            System.err.println("\t\t!!!PORNHUB!!!");
//            return true;
//        }

        //Если url это http адрес
        if (url.contains("http://")) {
            if (http_url.get(url) != null) {
                return true;
            }
            //Проверяем домен на вхождение в blockedSites
            if (blockedSites.get(parseDomain(url)) != null) {
                return true;
            }
        }
        //На будущее, для фильтрации https
        //Если url это https адрес
        if (url.contains("https://")) {
            if (https_url.get(url) != null) {
                return true;
            }
            //Проверяем домен на вхождение в blockedSites
            if (blockedSites.get(parseDomain(url)) != null) {
                return true;
            }
        }
        //Вхождение в список ipv4
        if (ipv4.get(parseDomain(url)) != null) {
            return true;
        }
        //Вхождение в список ipv6
        if (ipv6.get(parseDomain(url)) != null) {
            return true;
        }
        //Вхождение в список blockedSites
        if (blockedSites.get(url) != null) {
//            log.add(log, blockedSites.get(url));
            return true;
//        } else if (urls.get(url) != null) {
////            log.add(log, urls.get(url));
//            return true;
        }
        return false;

    }

    /**
     * Creates a management interface which can dynamically update the proxy
     * configurations blocked : Lists currently blocked sites cached	: Lists
     * currently cached sites close	: Closes the proxy server *	: Adds * to the
     * list of blocked sites
     */
    @Override
    public void run() {
        try /*( Scanner scanner = new Scanner(System.in))*/ {
//            String command;
            while (running) {
//                System.out.println("Enter new site to block, or type \"blocked\" to see blocked sites, \"cached\" to see cached sites, or \"close\" to close server.");
//                command = scanner.nextLine();
//                if (command.toLowerCase().equals("blocked")) {
//                    log.add(log, "\nCurrently Blocked Sites");
//                   
//                    if(blockedSites.size()>1000){
////                        blockedSites.keySet().forEach(key -> {//Тут выскакивает concurrentModificationException
////                            FileOperations.appendToFile("blacklist.txt", key +"\n");
////                        });
////                        System.out.println("Слишком много доменов с черном списке!\nПожалуйста посмотрите в файл blacklist.txt");
//                        System.out.println("Слишком много доменов с черном списке!");
//                    } else {
//                        for (Map.Entry<String, String> entry : blockedSites.entrySet()) {
//                            System.out.println(entry.getValue());
//                        }
//                    }
////                    log.add(log, "\nCurrently Blocked Urls");
////                    urls.keySet().forEach(key -> {
////                        log.add(log, key);
////                    });
//
//                } else if (command.toLowerCase().equals("cached")) {
//                    log.add(log, "\nCurrently Cached Sites");
//                    cache.keySet().forEach(key -> {
//                        System.out.println(key);
//                    });
//                } else if (command.equals("close")) {
//                    running = false;
//                    closeServer();
//                } else {
//                    parseDomain(command);
//                    log.add(log, "\n" + command + " blocked successfully \n");
//                }
                //Скачиваем blocklist по таймеру
                //В 8:00 и 16:00 ежедневно, без остановки сервиса
                LocalTime current_time = LocalTime.now();
                if ((current_time.getHour() == 0 || current_time.getHour() == 8 || current_time.getHour() == 16)
                        && current_time.getMinute() == 0
                        && current_time.getSecond() == 0
                        && current_time.getNano() == 0) {
                    getblacklists();
                }
            }
        } catch (Exception e) {
            log.add(log, getStackTrace(e));
        }
    }

    public static void processFilesFromFolder(File tarbal_folder) {

        File[] folderEntries = tarbal_folder.listFiles();
        for (File entry : folderEntries) {

            if (entry.isDirectory()) {
                processFilesFromFolder(entry);
                continue;
            }
            // иначе попался файл, обрабатываем его!
            if (entry.getName().equals("urls")) {
                List<String> url_list = FileOperations.readfile(entry);
                url_list.forEach(new Consumer<String>() {
                    @Override
                    public void accept(String ur) {
                        String dn = parseDomain(ur);
                        String result_add_dn = blockedSites.put(dn, dn);
                        if (result_add_dn != null) {
                            log.add(log, "Адрес " + dn + " успешно добавлен в список blockedSites");
                        } else {
                            log.add(log, "Адрес " + dn + " был добавлен ранее");
                        }
                        //т.к. у нас есть еще списки http_url и https_url, добавляем и в них
                        switch (whatThis(ur)) {
                            case (2) -> {
                                String result_add_http_url = http_url.put(dn, dn);
                                if (result_add_http_url != null) {
                                    log.add(log, "Адрес " + ur + " успешно добавлен в список http_urls");
                                } else {
                                    log.add(log, "Адрес " + ur + " был добавлен ранее");
                                }
                            }
                            case (3) -> {
                                String result_add_https_url = https_url.put(dn, dn);
                                if (result_add_https_url != null) {
                                    log.add(log, "Адрес " + ur + " успешно добавлен в список https_urls");
                                } else {
                                    log.add(log, "Адрес " + ur + " был добавлен ранее");
                                }
                            }
                        }
                    }
                });
            }
            if (entry.getName().equals("domains")) {
                List<String> domains_list = FileOperations.readfile(entry);
                domains_list.forEach(domain -> {
                    String dn = parseDomain(domain);
                    String result_add_dn = blockedSites.put(dn, dn);
                    if (result_add_dn != null) {
                        log.add(log, "Адрес " + dn + " успешно добавлен в список blockedSites");
                    } else {
                        log.add(log, "Адрес " + dn + " был добавлен ранее");
                    }
                });
            }
        }
    }

    /*
    Метод определяет тип строки
    0 ipv4 адрес
    1 ipv6 адрес
    2 http url
    3 https url
    4 domain name
     */
    public static int whatThis(String str) {
        String IPV4_PATTERN
                = "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
//                        = "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
        String IPV6_PATTERN
                = "((^|:)([0-9a-fA-F]{0,4})){1,8}";
        String date_pattern
                = "[0-9]{4}-[0-9]{2}-[0-9]{2}";
        Pattern pattern_ipv4 = Pattern.compile(IPV4_PATTERN);
        Matcher matcher_ipv4 = pattern_ipv4.matcher(str);
        Pattern pattern_ipv6 = Pattern.compile(IPV6_PATTERN);
        Matcher matcher_ipv6 = pattern_ipv6.matcher(str);
        Pattern pattern_date = Pattern.compile(IPV6_PATTERN);
        Matcher matcher_date = pattern_date.matcher(str);
        //Ищем среди элементов ipv4 адреса 
        if (matcher_ipv4.find()) {
            return (0);
        } else if (matcher_ipv6.find()) {
            //Ищем среди элементов ipv6 адреса 
            //Отсеиваем дату
            if (matcher_date.find()) {
                return (5);
            }
            return (1);
        } else if (str.contains("http://")) {
            return (2);
        } else if (str.contains("https://")) {
            return (3);
        }
        return (4);
    }

    private static ArrayList splitStr(String input_str) {

        ArrayList<String> output = new ArrayList();
        String[] urls = ((String) input_str).split(";");
        for (String ur : urls) {
            if (!ur.equals("")) {
                //Если в строке присутствует запятая, значит нужно еще разделить строку по запятой
                if (!ur.contains(",")) {
                    //Пихаем все в один список
                    output.add(ur);
                } else {
                    String[] u = ur.split(",");
                    for (String us : u) {
                        output.add(us);
                    }
                }
            }
        }
        return (output);
    }

    //Парсит доменное имя из url
    private static String parseDomain(String url) {

        String domain_name = url.replace("http://", "");
        domain_name = domain_name.replace("https://", "");
        if (domain_name.contains("/")) {
            domain_name = domain_name.substring(0, domain_name.indexOf("/"));
            if (domain_name.contains(":")) {
                domain_name = domain_name.substring(0, domain_name.indexOf(":"));
            }
        }

        return (domain_name);
    }

    private void getblacklists() {

        //Качаем список запрещенных сайтов с минъюста
        //Получение текущего содержания реестра: URL: http://api.antizapret.info/all.php
        ArrayList urls_all = Download.download_php("http://api.antizapret.info/all.php");

        log.add(log, "Добавляем сайты в blacklists c url http://api.antizapret.info/all.php");
//==>Поле reason        
        addAntizapretToHashMaps(urls_all);

        //Получение текущего содержания базы Минюста: URL: http://api.antizapret.info/minjust.php
        ArrayList urls_minjust = Download.download_php("http://api.antizapret.info/minjust.php");
        log.add(log, "Добавляем сайты в blacklists c url http://api.antizapret.info/minjust.php");

        //Почему то не добавляются домены с этой страницы
//==>Поле reason
        addAntizapretToHashMaps(urls_minjust);
        log.add(log, "Качаем архив https://www.shallalist.de/Downloads/shallalist.tar.gz");

        //Качаем архив с shallalist.de
        //                https://www.shallalist.de/Downloads/shallalist.tar.gz.md5
        File tarbal = new File("downloads" + fs + "shallalist.tar.gz");
        File md5 = new File("downloads" + fs + "shallalist.tar.gz.md5");

        long lRM = 0;
        try {
            lRM = Download.date_url(new URL("https://www.shallalist.de/Downloads/shallalist.tar.gz"));
        } catch (MalformedURLException ex) {
            log.add(log, getStackTrace(ex));
        }

        //Если архив уже скачан
        if (tarbal.exists()) {
            log.add(log, "Архив был скачан ранее");
            long lRTU = Long.parseLong(latestRemoteTarballUpdate);
            //Если дата удаленной модификации файла изменилась
            if (lRTU != lRM) {
                log.add(log, "Дата удаленной модификации поменялась");
                //Если дата скачанного файла > записанной даты
                //Что маловероятно
                if (lRTU > tarbal.lastModified()) {
                    //перекачиваем
                    log.add(log, "Перекачиваем");
                    //Если файлы существуют, удаляем
                    if (tarbal.exists()) {
                        tarbal.delete();
                    }
                    if (md5.exists()) {
                        md5.delete();
                    }

                }
                getTarball(tarbal, md5);
            }
        } else {
            getTarball(tarbal, md5);
        }
//==>        //Потом требуется перебрать все файлы и папки и добавить домены и url к нашим спискам
        File tarbal_folder = new File("blacklists" + fs + "BL");
        processFilesFromFolder(tarbal_folder);
        try {
            luBL.storeToXML(new FileOutputStream(latest_update_BL), "store to xml file");

        } catch (FileNotFoundException ex) {
            log.add(log, getStackTrace(ex));
        } catch (IOException ex) {
            log.add(log, getStackTrace(ex));
        }
        if (blockedSitesHashMapFile.exists()) {
            try {
                blockedSitesHashMapFile.delete();
                blockedSitesHashMapFile.createNewFile();
            } catch (IOException ex) {
                log.add(log, "Не удалось очистить файл " + blockedSitesHashMapFile.getName());
                log.add(log, getStackTrace(ex));
            }
        }
        //Сохраняем getblacklists
        try ( FileOutputStream fileOutputStream2 = new FileOutputStream("blacklists" + fs + "blockedSites.txt");  ObjectOutputStream objectOutputStream2 = new ObjectOutputStream(fileOutputStream2)) {
            objectOutputStream2.writeObject(blockedSites);
        } catch (FileNotFoundException ex) {
            log.add(log, getStackTrace(ex));
        } catch (IOException ex) {
            log.add(log, getStackTrace(ex));
        }
    }

    //метод заполняет списки с антизапрета
    private void addAntizapretToHashMaps(ArrayList urls) {
        //Очень медленный перебор, надо думать как избавиться от ArrayList
        for (Object url : urls) {

            String urls_str = (String) url;
            //Режем строку на части
            ArrayList<String> splitted_str = splitStr(urls_str);
            splitted_str.forEach(u -> {
                //Проверяем что это за строка
                switch (whatThis(u)) {
                    case (0) -> {
                        String result_add_ipv4 = ipv4.put(u, u);
                        if (result_add_ipv4 != null) {
                            log.add(log, "Адрес " + u + " успешно добавлен в список ipv4");
                        } else {
                            log.add(log, "Адрес " + u + " был добавлен ранее");
                        }
//==>                        //На первое время добавляем еще и в blockedSites
                        String result_add_blacklist = blockedSites.put(u, u);
                        if (result_add_blacklist != null) {
                            log.add(log, "Адрес " + u + " успешно добавлен в список blockedSites");
                        } else {
                            log.add(log, "Адрес " + u + " был добавлен ранее");
                        }
                    }
                    case (1) -> {
                        String result_add_ipv6 = ipv6.put(u, u);
                        if (result_add_ipv6 != null) {
                            log.add(log, "Адрес " + u + " успешно добавлен в список ipv6");
                        } else {
                            log.add(log, "Адрес " + u + " был добавлен ранее");
                        }
//==>                        //На первое время добавляем еще и в blockedSites
                        String result_add_blacklist = blockedSites.put(u, u);
                        if (result_add_blacklist != null) {
                            log.add(log, "Адрес " + u + " успешно добавлен в список blockedSites");
                        } else {
                            log.add(log, "Адрес " + u + " был добавлен ранее");
                        }
                    }
                    case (2) -> {
                        String result_add_http = http_url.put(u, u);
                        if (result_add_http != null) {
                            log.add(log, "Url " + u + " успешно добавлен в список http_url");
                        } else {
                            log.add(log, "Адрес " + u + " был добавлен ранее");
                        }
                        //На всякий случай парсим домен и пихаем в blockedSites
                        String dn = parseDomain(u);
                        String result_add_blacklist = blockedSites.put(dn, dn);
                        if (result_add_blacklist != null) {
                            log.add(log, "Адрес " + dn + " успешно добавлен в список blockedSites");
                        } else {
                            log.add(log, "Адрес " + u + " был добавлен ранее");
                        }
                    }
                    case (3) -> {
                        //Тут нужно добавить в список https и спарсить домен
                        String result_add_https = https_url.put(u, u);
                        if (result_add_https != null) {
                            log.add(log, "Url " + u + " успешно добавлен в список https_url");
                        } else {
                            log.add(log, "Адрес " + u + " был добавлен ранее");
                        }
                        //На всякий случай парсим домен и пихаем в blockedSites
                        String dn = parseDomain(u);
                        String result_add_blacklist = blockedSites.put(dn, dn);
                        if (result_add_blacklist != null) {
                            log.add(log, "Адрес " + dn + " успешно добавлен в список blockedSites");
                        } else {
                            log.add(log, "Адрес " + u + " был добавлен ранее");
                        }
                    }
                    case (4) -> {
                        String result_add_blacklist = blockedSites.put(u, u);
                        if (result_add_blacklist != null) {
                            log.add(log, "Адрес " + u + " успешно добавлен в список blockedSites");
                        } else {
                            log.add(log, "Адрес " + u + " был добавлен ранее");
                        }
                    }
                }
                //ipv4
                //ipv6
                //http
                //https
                //domain_name
            });

        }

    }

    private void getTarball(File tarbal, File md5) {
        //Качаем файлы архива и md5 суммы
        try {
            Download.download(tarbal, new URL("https://www.shallalist.de/Downloads/shallalist.tar.gz"));
            Download.download(md5, new URL("https://www.shallalist.de/Downloads/shallalist.tar.gz.md5"));
        } catch (MalformedURLException ex) {
            log.add(log, getStackTrace(ex));
        }

        try {
            luBL.setProperty("latestRemoteTarballUpdate", String.valueOf(Download.date_url(new URL("https://www.shallalist.de/Downloads/shallalist.tar.gz"))));
        } catch (MalformedURLException ex) {
            log.add(log, getStackTrace(ex));
        }

        try {
            String md5summ_file = FileSecret.getMd5Summ(tarbal.getAbsolutePath());
            String file_md5 = FileOperations.readMd5File(md5);
            String md5Summ_str = file_md5.substring(0, file_md5.indexOf(" "));
            if (md5summ_file.equals(md5Summ_str)) {
                if (file_md5.substring(file_md5.indexOf(" ") + 2, file_md5.length()).equals(tarbal.getName())) {

                    File destTar = new File(tarbal.getParentFile().getAbsolutePath() + fs + "tarball");
                    File destDir = new File("blacklists");

                    if (destDir.exists()) {
                        destDir.delete();
                    }
                    if (destTar.exists()) {
                        destTar.delete();
                    }
                    destTar.mkdirs();

                    //распаковываем
                    Archive.unTar(Archive.unGzip(tarbal, destTar), destDir);

                }
            } else {
                //Выводим ошибку
                log.add(log, "Не совпадает сумма md5");
            }
        } catch (Exception ex) {
            log.add(log, getStackTrace(ex));
        }
    }

    public static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

}
