/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package HttpHttpsProxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

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
// Main method for the program

    public static Log log = new Log();
    static String fs = System.getProperty("file.separator");
    private RequestHandler runnable = null;

    List<File> list_files = new ArrayList();

    static String[] first_domain_levels = {
        ".academy",
        ".accountant",
        ".accountants",
        ".active",
        ".actor",
        ".adult",
        ".aero",
        ".agency",
        ".airforce",
        ".apartments",
        ".app",
        ".archi",
        ".army",
        ".associates",
        ".asia",
        ".attorney",
        ".auction",
        ".audio",
        ".autos",
        ".biz",
        ".cat",
        ".com",
        ".coop",
        ".dance",
        ".edu",
        ".eus",
        ".family",
        ".gov",
        ".info",
        ".int",
        ".jobs",
        ".mil",
        ".mobi",
        ".museum",
        ".name",
        ".net",
        ".one",
        ".ong",
        ".onl",
        ".online",
        ".ooo",
        ".org",
        ".organic",
        ".partners",
        ".parts",
        ".party",
        ".pharmacy",
        ".photo",
        ".photography",
        ".photos",
        ".physio",
        ".pics",
        ".pictures",
        ".feedback",
        ".pink",
        ".pizza",
        ".place",
        ".plumbing",
        ".plus",
        ".poker",
        ".porn",
        ".post",
        ".press",
        ".pro",
        ".productions",
        ".prof",
        ".properties",
        ".property",
        ".qpon",
        ".racing",
        ".recipes",
        ".red",
        ".rehab",
        ".ren",
        ".rent",
        ".rentals",
        ".repair",
        ".report",
        ".republican",
        ".rest",
        ".review",
        ".reviews",
        ".rich",
        ".site",
        ".tel",
        ".trade",
        ".travel",
        ".xxx",
        ".xyz",
        ".yoga",
        ".zone",
        ".ninja",
        ".art",
        ".moe",
        ".dev",
        ".ac",
        ".ad",
        ".ae",
        ".af",
        ".ag",
        ".ai",
        ".al",
        ".am",
        ".an",
        ".ao",
        ".aq",
        ".ar",
        ".as",
        ".at",
        ".au",
        ".aw",
        ".ax",
        ".az",
        ".ba",
        ".bb",
        ".bd",
        ".be",
        ".bf",
        ".bg",
        ".bh",
        ".bi",
        ".bj",
        ".bm",
        ".bn",
        ".bo",
        ".br",
        ".bs",
        ".bt",
        ".bv",
        ".bw",
        ".by",
        ".бел",
        ".bz",
        ".ca",
        ".cc",
        ".cd",
        ".cf",
        ".cg",
        ".ch",
        ".ci",
        ".ck",
        ".cl",
        ".cm",
        ".cn",
        ".co",
        ".cr",
        ".cu",
        ".cv",
        ".cx",
        ".cy",
        ".cz",
        ".de",
        ".dj",
        ".dk",
        ".dm",
        ".do",
        ".dz",
        ".ec",
        ".ee",
        ".eg",
        ".er",
        ".es",
        ".et",
        ".eu",
        ".fi",
        ".fj",
        ".fk",
        ".fm",
        ".fo",
        ".fr",
        ".ga",
        ".gb",
        ".gd",
        ".ge",
        ".gf",
        ".gg",
        ".gh",
        ".gi",
        ".gl",
        ".gm",
        ".gn",
        ".gp",
        ".gq",
        ".gr",
        ".gs",
        ".gt",
        ".gu",
        ".gw",
        ".gy",
        ".hk",
        ".hm",
        ".hn",
        ".hr",
        ".ht",
        ".hu",
        ".id",
        ".ie",
        ".il",
        ".im",
        ".in",
        ".io",
        ".iq",
        ".ir",
        ".is",
        ".it",
        ".je",
        ".jm",
        ".jo",
        ".jp",
        ".ke",
        ".kg",
        ".kh",
        ".ki",
        ".km",
        ".kn",
        ".kp",
        ".kr",
        ".kd",
        ".kw",
        ".ky",
        ".kz",
        ".la",
        ".lb",
        ".lc",
        ".li",
        ".lk",
        ".lr",
        ".ls",
        ".lt",
        ".lu",
        ".lv",
        ".ly",
        ".ma",
        ".mc",
        ".md",
        ".me",
        ".mg",
        ".mh",
        ".mk",
        ".ml",
        ".mm",
        ".mn",
        ".мон",
        ".mo",
        ".mp",
        ".mq",
        ".mr",
        ".ms",
        ".mt",
        ".mu",
        ".mv",
        ".mw",
        ".mx",
        ".my",
        ".mz",
        ".na",
        ".nc",
        ".ne",
        ".nf",
        ".ng",
        ".ni",
        ".nl",
        ".no",
        ".np",
        ".nr",
        ".nu",
        ".nz",
        ".om",
        ".pa",
        ".pe",
        ".pf",
        ".pg",
        ".ph",
        ".pk",
        ".pl",
        ".pm",
        ".pn",
        ".pr",
        ".ps",
        ".pt",
        ".pw",
        ".py",
        ".qa",
        ".re",
        ".ro",
        ".rs",
        ".срб",
        ".ru",
        ".рф",
        ".rw",
        ".sa",
        ".sb",
        ".sc",
        ".sd",
        ".se",
        ".sg",
        ".sh",
        ".si",
        ".sj",
        ".sk",
        ".sl",
        ".sm",
        ".sn",
        ".so",
        ".sr",
        ".st",
        ".su",
        ".sv",
        ".sy",
        ".sz",
        ".tc",
        ".td",
        ".tf",
        ".tg",
        ".th",
        ".tj",
        ".tk",
        ".tl",
        ".tm",
        ".tn",
        ".to",
        ".tp",
        ".tr",
        ".tt",
        ".tv",
        ".tw",
        ".tz",
        ".ua",
        ".укр",
        ".ug",
        ".uk",
        ".us",
        ".uy",
        ".uz",
        ".va",
        ".vc",
        ".ve",
        ".vg",
        ".vi",
        ".vn",
        ".vu",
        ".wf",
        ".ws",
        ".ye",
        ".yt",
        ".yu",
        ".za",
        ".zm",
        ".zw",};

    static String[] third_domain_level = {"rt",
        "us",
        "www"};

    public static void main(String[] args) {
        log.logWrite("log");
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
    static HashMap<String, String> blockedSites;

    /**
     * ArrayList of threads that are currently running and servicing requests.
     * This list is required in order to join all threads on closing of server
     */
    static HashMap<Thread, RequestHandler> servicingThreads;

    /**
     * Create the Proxy Server
     *
     * @param port Port number to run proxy server from.
     */
    public HttpHttpsProxy(int port) {

        // Load in hash map containing previously cached sites and blocked Sites
        cache = new HashMap<>();
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
                try (FileInputStream fileInputStream = new FileInputStream(cachedSites)) {
                    objectInputStream = new ObjectInputStream(fileInputStream);
                    cache = (HashMap<String, File>) objectInputStream.readObject();
                }
                objectInputStream.close();
            }

            // Load in blocked sites from file
            File blockedSitesTxtFile = new File("blockedSites.txt");
            if (!blockedSitesTxtFile.exists()) {
                log.add(log, "No blocked sites found - creating new file");
                blockedSitesTxtFile.createNewFile();
            } else {
                ObjectInputStream objectInputStream;
                try (FileInputStream fileInputStream = new FileInputStream(blockedSitesTxtFile)) {
                    objectInputStream = new ObjectInputStream(fileInputStream);
                    blockedSites = (HashMap<String, String>) objectInputStream.readObject();
                    //Очистим файл, чтобы не было эксепшена при следующем запуске
                    blockedSitesTxtFile.delete();
                    blockedSitesTxtFile.createNewFile();
                }
                objectInputStream.close();
            }
        } catch (IOException e) {
            log.add(log, "Error loading previously cached sites file");
            log.add(log, e.toString());
        } catch (ClassNotFoundException e) {
            log.add(log, "Class not found loading in preivously cached sites file");
            log.add(log, e.toString());
        }

        //Скачиваем blacklist
        Download.getblacklists();

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
            log.add(log, se.toString());
        } catch (SocketTimeoutException ste) {
            log.add(log, "Timeout occured while connecting to client");
        } catch (IOException io) {
            log.add(log, "IO exception when connecting to client");
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
            } catch (IOException e) {
                log.add(log, e.toString());
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
            try (FileOutputStream fileOutputStream = new FileOutputStream("cachedSites.txt"); ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {

                objectOutputStream.writeObject(cache);
            }
            log.add(log, "Cached Sites written");
            System.out.println("Cached Sites written");

            try (FileOutputStream fileOutputStream2 = new FileOutputStream("blockedSites.txt"); ObjectOutputStream objectOutputStream2 = new ObjectOutputStream(fileOutputStream2)) {
                objectOutputStream2.writeObject(blockedSites);
            }
            log.add(log, "Blocked Site list saved");
            System.out.println("Blocked Site list saved");

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
                log.add(log, e.toString());
            }

        } catch (IOException e) {
            log.add(log, "Error saving cache/blocked sites");
            log.add(log, e.toString());
        }

        // Close Server Socket
        try {
            System.out.println("Terminating Connection");
            log.add(log, "Terminating Connection..");
            serverSocket.close();
            System.out.println("Terminated");
            System.exit(0);
        } catch (IOException e) {
            log.add(log, "Exception closing proxy's server socket");
            log.add(log, e.toString());
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

        if (blockedSites.get(url) != null) {
//            log.add(log, blockedSites.get(url));
            return true;
//        } else if (urls.get(url) != null) {
////            log.add(log, urls.get(url));
//            return true;
        } else if (url.substring(0, 7).equals("http://")) {
            url = url.replace("http://", "https://");
            if (blockedSites.get(url) != null) {
//                log.add(log, blockedSites.get(url));
                return true;
//            } else if (urls.get(url) != null) {
////                log.add(log, urls.get(url));
//                return true;
            }
            url = url.replace("https://", "");
            if (blockedSites.get(url) != null) {
//                log.add(log, blockedSites.get(url));
                return true;
//            } else if (urls.get(url) != null) {
////                log.add(log, urls.get(url));
//                return true;
            }
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
        try (Scanner scanner = new Scanner(System.in)) {
            String command;
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
                    Download.getblacklists();
                }
            }
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
                url_list.forEach(ur -> {
                    parseDomain(ur);
                });
            }
            if (entry.getName().equals("domains")) {
                List<String> domains_list = FileOperations.readfile(entry);
                domains_list.forEach(domain -> {
                    parseDomain(domain);
                });
            }
        }
    }

    public static void parseDomain(String domain_url) {

        domain_url = domain_url.replace("http://", "");

        domain_url = domain_url.replace("https://", "");

//        domain_url = domain_url.replace("*.", "");
        //Если содержит "/", значит это url
        //Иначе это домен
        if (domain_url.contains("/")) {
            domain_url = domain_url.substring(0, domain_url.indexOf("/"));
        }

        String this_first_domain_level = null;
        //Откусываем кусок домена 1-го уровня
        for (String fdl : first_domain_levels) {
//            Запоминаем домен первого уровня
            if (domain_url.contains(fdl)) {
                this_first_domain_level = fdl;
            }
            domain_url = domain_url.replace(fdl, "");

        }
//        Отрезаем все до точки, пока не останется домен второго уровня
//        Можно, конечно, через lastIndexOf, но... костылиииии и велосипееееды
        while (domain_url.contains(".")) {
            domain_url = domain_url.substring(domain_url.indexOf(".") + 1);
//            System.out.println(domain_url);
        }

//        //Ключевая ошибка: 
//        //я начал делить по точке,но это неправильно, правильно отсекать домен верхнего уровня
//        String[] splitted_domain_url = domain_url.split(".");
//
//        System.out.println(domain_url.indexOf(".")+" "+domain_url.contains(".")+" "+domain_url + " Длинна домена "+(splitted_domain_url.length - 1));
//        String first_domain_level = splitted_domain_url[splitted_domain_url.length - 1];
//        String second_domain_level = splitted_domain_url[splitted_domain_url.length - 2];
        //Тут мы все соединяем и добавляем в blacklist
        //Просто domain.com
        String domain = domain_url + this_first_domain_level;
//        System.out.println(domain);
        addToBL(domain);

        //Домен третьего уровня
        for (String tDL : third_domain_level) {
            tDL = tDL + "." + domain;
            addToBL(tDL);
        }

    }

    //Непосредственная добавка домена в сам BlackList
    private static void addToBL(String domain) {
        log.add(log, "Попытка добавить домен: " + domain + " ...");
        String dont_added_domain = blockedSites.put(domain, domain);
        if (dont_added_domain == null) {
            log.add(log, domain + " Домен успешно добавлен");
        }
    }

}
