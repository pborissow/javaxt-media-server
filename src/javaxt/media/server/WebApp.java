package javaxt.media.server;

import java.util.*;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


import javaxt.sql.*;
import javaxt.json.*;
import javaxt.express.*;
import javaxt.io.Directory;
import javaxt.http.servlet.*;
import javaxt.utils.ThreadPool;
import javaxt.express.utils.DateUtils;
import static javaxt.utils.Console.console;
import static javaxt.utils.Timer.setInterval;
import javaxt.http.websocket.WebSocketListener;
import javaxt.express.notification.NotificationService;


import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;


//******************************************************************************
//**  WebApp
//******************************************************************************
/**
 *   Used to serve up static files (html, javascript, images, etc) and respond
 *   to API requests.
 *
 ******************************************************************************/

public class WebApp extends HttpServlet {


    private WebServices ws;
    private javaxt.io.Directory web;
    private FileManager fileManager;
    private EventProcessor eventProcessor;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public WebApp() throws Exception {
        try{

          //Start the NotificationService
            NotificationService.start();



          //Instantiate WebServices
            String remoteHost = Config.get("remoteHost").toString();
            if (remoteHost==null) ws = new WebServices(Config.getDatabase());



          //Instantiate the FileManager
            JSONObject webConfig = Config.get("webapp").toJSONObject();
            if (webConfig!=null){
                String webDir = webConfig.get("webDir").toString();
                javaxt.io.Directory dir = new javaxt.io.Directory(webDir);
                if (dir.exists()){
                    web = dir;

                  //Instantiate EventProcessor
                    eventProcessor = new EventProcessor();


                  //Instantiate FileManager
                    fileManager = new FileManager(web);


                  //Watch for changes to the web directory
                    fileManager.getFileUpdates((Directory.Event event) -> {
                        eventProcessor.processEvent(event);
                    });

                }
            }
        }
        catch(Exception e){
            NotificationService.stop();
            throw e;
        }
    }


//  //**************************************************************************
//  //** init
//  //**************************************************************************
//    public void init(Object servletConfig) throws ServletException {
//        this.getServletContext().setContextPath(web.toString());
//    }


  //**************************************************************************
  //** processRequest
  //**************************************************************************
  /** Used to process http get and post requests.
   */
    public void processRequest(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {


      //Check if the server support HTTPS
        if (this.supportsHttps()){

          //Set "Content-Security-Policy"
            response.setHeader("Content-Security-Policy", "upgrade-insecure-requests");


          //Redirect http request to https as needed
            javaxt.utils.URL url = new javaxt.utils.URL(request.getURL());
            if (!url.getProtocol().equalsIgnoreCase("https")){
                url.setProtocol("https");
                //url.setPort(sslPort); //TODO: Get SSL port
                response.sendRedirect(url.toString(), true);
                return;
            }
        }



      //Add CORS support
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Headers","*");
        response.addHeader("Access-Control-Allow-Methods", "*");



      //Get path from url, excluding servlet path and leading "/" character
        String path = request.getPathInfo();
        if (path!=null) path = path.substring(1);



      //Get first "directory" in the path
        String service = path==null ? "" : path.toLowerCase();
        if (service.contains("/")) service = service.substring(0, service.indexOf("/"));



      //Log the request
        eventProcessor.processEvent(request);



        //Pass the request to the authenticator. If it can handle the request, return early.
        //javaxt.express.Authenticator authenticator = (javaxt.express.Authenticator) getAuthenticator(request);
        //if (authenticator!=null && authenticator.handleRequest(service, response)) return;




      //Send static file if we can
        if (!request.isWebSocket() && fileManager!=null){
            java.io.File file = fileManager.getFile(path);
            if (file!=null){
                fileManager.sendFile(file, request, response);
                return;
            }
        }



      //If we're still here, send the request to the webservices endpoint
        String remoteHost = Config.get("remoteHost").toString();
        try{
            if (remoteHost==null){
                ws.processRequest(service, path, request, response);
            }
            else{
                forwardRequest(service, path, remoteHost, request, response);
            }
        }
        catch(Exception e){
            throw new ServletException(500, e.getMessage(), e);
        }

    }


  //**************************************************************************
  //** forwardRequest
  //**************************************************************************
    private void forwardRequest(String service, String path, String remoteHost,
        HttpServletRequest request, HttpServletResponse response)
        throws Exception {


      //Check if this is a WebSocket request
        if (request.isWebSocket()){
            final String wss = "wss://" + remoteHost + "/" + path;
            final HashMap<String, String> headers = new HashMap<>();
            headers.put("Authorization", request.getHeader("Authorization"));



          //Create new WebSocket listener and create a WebSocket connection
          //to the remote host. We will forward messages from the client to
          //the remote server and vice versa.
            new WebSocketListener(request, response){
                private WebSocketClient wc;

                public void onConnect(){

                    final WebSocketListener me = this;
                    try{
                        wc = new WebSocketClient(new java.net.URI(wss), new Draft_6455(), headers, 0){

                            public void onMessage( String message ) {
                                me.send(message);
                            }
                            public void onOpen( ServerHandshake handshake ) {
                            }
                            public void onClose( int code, String reason, boolean remote ) {
                                me.close();
                            }
                            public void onError( Exception ex ) {
                                //ex.printStackTrace();
                            }
                        };

                        wc.connect();
                    }
                    catch(Exception e){
                        //e.printStackTrace();
                    }
                }
                public void onDisconnect(int statusCode, String reason){
                    if (wc!=null) wc.close();
                }
                public void onText(String str){
                    if (wc!=null) wc.send(str);
                }
            };
        }

        else{

          //Execute web service request and return response
            String method = path.substring(service.length());
            if (method.length()>0) if (method.startsWith("/")) method = method.substring(1);
            if (method.endsWith("/")) method = method.substring(0, method.length()-1);

            if (method.length()>0){
                try{
                    javaxt.http.Response ws = getServiceResponse(request);
                    if (ws.getStatus()==-1) throw new Exception("Communication Failure.");
                    response.setStatus(ws.getStatus());
                    Map<String, List<String>> headers = ws.getHeaders();
                    for (String key : headers.keySet()){
                        for (String value : headers.get(key)){
                            if (value!=null) response.setHeader(key, value);
                        }
                    }
                    response.write(ws.getBytes().toByteArray(), false); //<--Don't compress the data! It may already be compressed...
                }
                catch(Exception e){
                    response.setStatus(500);
                    String s = e.getClass().getName();
                    s = s.substring(s.lastIndexOf(".")+1);
                    String message = e.getLocalizedMessage();
                    String error = (message != null) ? (s + ": " + message) : s;
                    response.write(error);
                    e.printStackTrace();
                }
            }
            else{
                response.setStatus(403);
            }
        }
    }


  //**************************************************************************
  //** getServiceResponse
  //**************************************************************************
  /** Redirects web service requests to a remote endpoint and returns the HTTP
   *  response.
   */
    private javaxt.http.Response getServiceResponse(HttpServletRequest servletRequest) throws IOException {


      //Parse servlet request
        javaxt.utils.URL url = new javaxt.utils.URL(servletRequest.getURL());
        byte[] payload = servletRequest.getBody();
        String headers = servletRequest.toString();


      //Update url
        String remoteHost = Config.get("remoteHost").toString();
        String host = remoteHost + (remoteHost.contains(":")? "" : ":80");
        url.setHost(host);
        url.setProtocol("https");


      //Create http request
        javaxt.http.Request request = new javaxt.http.Request(url.toString());
        request.setNumRedirects(0); //<-- Do this to avoid hitting the server twice for POST requests!


      //Check whether to validate SSL Certificates
        String str = url.getHost();
        if (str.startsWith("[")){
            request.validateSSLCertificates(false);
        }
        else{
            try{
                int idx = str.indexOf(":");
                if (idx>0) str = str.substring(0, idx);
                java.net.InetAddress.getByName(str);
                request.validateSSLCertificates(false);
            }
            catch(Exception e){
            }
        }



      //Update request headers
        for (String entry : headers.split("\r\n")){
            if (entry!=null){
                if (entry.contains(":")){
                    String key = entry.substring(0, entry.indexOf(":"));
                    String val = entry.substring(entry.indexOf(":")+1).trim();
                    request.setHeader(key, val);
                }
            }
        }
        request.setHeader("Host", host);


      //Send payload
        if (payload!=null && payload.length>0) request.write(payload);


      //Return response
        return request.getResponse();
    }


  //**************************************************************************
  //** EventProcessor
  //**************************************************************************
  /** Used to process changes to the web directory and log web requests
   */
    private class EventProcessor{

        private final ConcurrentHashMap<String, Long>
        fileUpdates = new ConcurrentHashMap<>();
        private int len = web.getPath().length();


        private ThreadPool pool;
        private final ConcurrentHashMap<Long, HashMap<Long, AtomicInteger>>
        userActivity = new ConcurrentHashMap<>();


        public EventProcessor(){


          //Create timer task to periodically update the NotificationService
          //when files in the web directory have been created, edited, moved,
          //or deleted. Instead of broadcasting every single file change, this
          //timer task will wait for the directory to "settle down" and simply
          //broadcast the fact that something has changed.
            setInterval(()->{
                synchronized(fileUpdates){
                    if (!fileUpdates.isEmpty()){

                        long currTime = System.currentTimeMillis();
                        long lastUpdate = Integer.MAX_VALUE;


                        for (String key : fileUpdates.keySet()){
                            long t = fileUpdates.get(key);
                            if (t>lastUpdate) lastUpdate = t;
                        }


                        if (currTime-lastUpdate>2000){
                            NotificationService.notify("update", "WebFile", null);
                            fileUpdates.clear();
                            fileUpdates.notify();
                        }
                    }
                }
            }, 250);




          //Create timer task to periodically save user activity
            setInterval(()->{

              //Get current time as a "yyyyMMddHHmm" long
                long currTime = DateUtils.getMilliseconds(DateUtils.getCurrentTime());
                javaxt.utils.Date date = new javaxt.utils.Date(currTime);
                currTime = Long.parseLong(date.toString("yyyyMMddHHmm", DateUtils.getUTC()));


              //Insert records
                synchronized(userActivity){

                    HashSet<Long> oldKeys = new HashSet<>();
                    Connection conn = null;
                    Recordset rs = null;


                    for (long key : userActivity.keySet()){
                        if (key<currTime){

                            String time = key + "";
                            int l = time.length();
                            int hour = Integer.parseInt(time.substring(0, l-2));
                            int minute = Integer.parseInt(time.substring(l-2));
                            HashMap<Long, AtomicInteger> ua = userActivity.get(key);

                            HashSet<Long> inserts = new HashSet<>();
                            for (long userID : ua.keySet()){
                                int hits = ua.get(userID).get();

                                try{
                                    if (rs==null){
                                        conn = Config.getDatabase().getConnection();
                                        rs = conn.getRecordset("select * from user_activity where id=-1", false);
                                    }
                                    rs.addNew();
                                    rs.setValue("hour", hour);
                                    rs.setValue("minute", minute);
                                    rs.setValue("count", hits);
                                    rs.setValue("user_id", userID);
                                    rs.update();
                                    inserts.add(userID);
                                }
                                catch(Exception e){
                                }
                            }

                            for (long userID : inserts) ua.remove(userID);
                            if (ua.isEmpty()) oldKeys.add(key);
                        }
                    }

                  //Clean up
                    if (rs!=null) rs.close();
                    if (conn!=null) conn.close();

                    for (long key : oldKeys) userActivity.remove(key);

                    userActivity.notifyAll();
                }


            }, 2*60*1000); //2 minutes



          //Start thread pool used to update user activity
            pool = new ThreadPool(4){
                public void process(Object obj){
                    Object[] arr = (Object[]) obj;
                    Long userID = (Long) arr[0];
                    Long timestamp = DateUtils.getMilliseconds((Long) arr[1]);
                    javaxt.utils.Date date = new javaxt.utils.Date(timestamp);
                    String time = date.toString("yyyyMMddHHmm", DateUtils.getUTC());
                    long key = Long.parseLong(time);

                    synchronized(userActivity){
                        HashMap<Long, AtomicInteger> ua = userActivity.get(key);
                        if (ua==null){
                            ua = new HashMap<>();
                            userActivity.put(key, ua);
                        }
                        AtomicInteger hits = ua.get(userID);
                        if (hits==null){
                            hits = new AtomicInteger(0);
                            ua.put(userID, hits);
                        }
                        hits.incrementAndGet();
                        userActivity.notifyAll();
                    }
                }
            }.start();
        }


        public void processEvent(Directory.Event event){
            java.io.File f = new java.io.File(event.getFile());
            String path = f.toString().substring(len).replace("\\", "/");
            synchronized(fileUpdates){
                fileUpdates.put(path, event.getDate().getTime());
                fileUpdates.notify();
            }
        }


        public void processEvent(HttpServletRequest request){
            //if (logger!=null) logger.log(request);
            if (!request.isWebSocket()){
                User user = (User) request.getUserPrincipal();
                if (user!=null){
                    long userID = user.getID();
                    long timestamp = DateUtils.getCurrentTime();
                    String event = request.getMethod() + " " + request.getPathInfo();
                    String model = "WebRequest";

                    javaxt.utils.Record info = new javaxt.utils.Record();
                    info.set("method", request.getMethod());
                    info.set("path", request.getPathInfo());
                    info.set("userID", userID);

                    javaxt.utils.Value data = new javaxt.utils.Value(info);
                    NotificationService.notify(event, model, data);
                    pool.add(new Object[]{userID, timestamp});
                }
            }
        }
    }

}