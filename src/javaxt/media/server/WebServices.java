package javaxt.media.server;

//Java includes
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


//JavaXT includes
import javaxt.json.*;
import javaxt.io.Image;
import javaxt.sql.Model;

import javaxt.express.*;
import javaxt.http.servlet.*;
import javaxt.express.services.QueryService;
import javaxt.http.websocket.WebSocketListener;
import javaxt.express.notification.NotificationService;
import javaxt.media.models.Feature;
import javaxt.media.models.Setting;


//******************************************************************************
//**  WebServices
//******************************************************************************
/**
 *   Used to query, add, edit, and delete records from the photo archive via
 *   HTTP. This service requires direct access to a database.
 *
 ******************************************************************************/

public class WebServices extends WebService {

    private javaxt.sql.Database database;
    private long dbDate;
    private ConcurrentHashMap<String, WebService> webservices;
    private ConcurrentHashMap<Long, WebSocketListener> listeners;
    private AtomicLong webSocketID;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public WebServices(javaxt.sql.Database database) throws Exception {
        this.database = database;
        this.dbDate = database.getRecord(
        "select value from setting where key='db_date'").get(0).toLong();


      //Register models
        javaxt.io.Jar jar = new javaxt.io.Jar(this.getClass());
        for (Class c : jar.getClasses()){
            if (javaxt.sql.Model.class.isAssignableFrom(c)){
                Model.init(c, database.getConnectionPool());
                addModel(c);
            }
        }


      //Instantiate web services
        webservices = new ConcurrentHashMap<>();
        try{
            webservices.put("sql", new QueryService(database,
                new javaxt.io.Directory(Config.get("temp").toString()), null)
            );
        }
        catch(Exception e){
            var msg = e.getMessage();
            if (msg==null) e.printStackTrace();
            else console.log(msg);
        }


      //Websocket stuff
        listeners = new ConcurrentHashMap<>();
        webSocketID = new AtomicLong(0);


      //Route notifications to websocket listeners
        WebServices me = this;
        NotificationService.addListener((
            String event, String model, javaxt.utils.Value data, long timestamp)->{

          //Simulate notify(String action, Model model, User user)
          //notify(action+","+model.getClass().getSimpleName()+","+model.getID()+","+userID);
            long modelID = 0;
            long userID = -1;
            if (model.equals("SQL")){
                javaxt.express.services.QueryService.QueryJob queryJob = (javaxt.express.services.QueryService.QueryJob) data.toObject();
                userID = queryJob.getUserID();
                me.notify(event+","+model+","+queryJob.getID()+","+userID);
            }
            else if (model.equals("File")){
                me.notify(event+","+model+","+data+","+userID);
            }
            else if (model.equals("WebFile")){
                me.notify(event+","+model+","+modelID+","+userID);
            }
            else if (model.equals("WebRequest")){
                javaxt.utils.Record webRequest = (javaxt.utils.Record) data.toObject();
                userID = webRequest.get("userID").toLong();
                //updateActiveUsers(webRequest, timestamp);
                me.notify(event+","+model+","+modelID+","+userID);
            }

        });

    }


  //**************************************************************************
  //** getImage
  //**************************************************************************
    public ServiceResponse getImage(ServiceRequest request) throws Exception {

      //Get mediaID
        Long mediaID = getMediaID(request);
        if (mediaID==null) return new ServiceResponse(400, "id is required");


      //Get requested width and height
        Integer width = request.getParameter("width").toInteger();
        Integer height = request.getParameter("height").toInteger();


        //TODO: Check permissions


      //Get file
        javaxt.io.File file;
        javaxt.media.utils.RRDImage thumbnail = null;
        try (javaxt.sql.Connection conn = database.getConnection()){
            javaxt.media.models.File f;

            var record = conn.getRecord(
            "select file.id from media_item_file " +
            "join file on media_item_file.file_id=file.id " +
            "where media_item_file.media_item_id=" + mediaID +
            " and lower(extension)='rrd'");

            if (record==null){ //small images don't have an rrd
                record = conn.getRecord(
                "select file.id from media_item_file " +
                "join file on media_item_file.file_id=file.id " +
                "where media_item_file.media_item_id=" + mediaID +
                " and is_primary=true");
                var fileID = record.get(0).toLong();
                f = new javaxt.media.models.File(fileID);
            }
            else{
                var fileID = record.get(0).toLong();
                f = new javaxt.media.models.File(fileID);
                thumbnail = new javaxt.media.utils.RRDImage(f);
            }

            var dir = new javaxt.io.Directory(f.getPath().getDir());
            file = new javaxt.io.File(dir, f.getName() + "." + f.getExtension());
            if (!file.exists()) throw new Exception();

        }
        catch(Exception e){
            return new ServiceResponse(404);
        }


      //Set image version number using the file date
        javaxt.utils.Date lastModified = new javaxt.utils.Date(file.getDate());
        long currVersion = lastModified.toLong();



      //Get requested thumbnail version number from the url
        javaxt.utils.URL url = request.getURL();
        long requestedVersion = 0;
        try{ requestedVersion = Long.parseLong(url.getParameter("v")); }
        catch(Exception e){}
        long requestedDB = 0;
        try{ requestedDB = Long.parseLong(url.getParameter("db")); }
        catch(Exception e){}


      //Send redirect to a newer version as needed
        if (requestedVersion!=currVersion || requestedDB!=dbDate){
            url.setParameter("v", currVersion+"");
            url.setParameter("db", dbDate+"");
            String location = url.toString();
            return new ServiceResponse(301, location);
        }



      //Get image
        byte[] b = null;
        String contentType = null;
        if (thumbnail==null){
            var ext = file.getExtension().toLowerCase();
            if (ext.equals("jpg") || ext.equals("png")){
                b = file.getBytes().toByteArray();
                contentType = file.getContentType();
            }
            else{
                b = file.getImage().getByteArray();
                contentType = "image/jpeg";
            }
        }
        else{


          //Get thumbnail entry that best fits the requested dimensions
            javaxt.media.utils.RRDImage.Entry[] entries = thumbnail.getIndex();
            String thumbnailID = null;
            if (width==null && height==null){
                thumbnailID = entries[0].getID();
            }
            else{
                for (javaxt.media.utils.RRDImage.Entry entry : entries){

                    if (width==null){
                        if (entry.getHeight()<=height){
                            thumbnailID = entry.getID();
                            break;
                        }
                    }
                    else{
                        if (height==null){
                            if (entry.getWidth()<=width){
                                thumbnailID = entry.getID();
                                break;
                            }
                        }
                        else{

                            if (entry.getHeight()<=height){
                                if (entry.getWidth()<=width){
                                    thumbnailID = entry.getID();
                                    break;
                                }
                            }
                        }
                    }
                }
            }


          //Get bytes
            if (thumbnailID!=null){
                try (ByteArrayOutputStream output = new ByteArrayOutputStream()){
                    thumbnail.getImage(thumbnailID, output);
                    b = output.toByteArray();
                    contentType = "image/jpeg";
                }
            }

        }



      //Return response
        if (b==null){
            return new ServiceResponse(400, "Invalid request");
        }
        else{
            ServiceResponse response = new ServiceResponse(b);
            //response.setCacheControl("public, max-age=31536000, immutable");
            response.setContentType(contentType);
            response.setDate(lastModified);
            return response;
        }

    }


  //**************************************************************************
  //** getFace
  //**************************************************************************
    public ServiceResponse getFace(ServiceRequest request) throws Exception {

      //Get mediaID
        Long mediaID = getMediaID(request);
        if (mediaID==null) return new ServiceResponse(400, "id is required");

      //Get faceID
        Integer idx = request.getParameter("idx").toInteger();
        if (idx==null) return new ServiceResponse(400, "idx is required");

      //Return image
        Feature[] features = Feature.find("item_id=", mediaID, "label=", "FACE");
        if (idx<features.length){
            ServiceResponse response = new ServiceResponse(features[idx].getThumbnail());
            response.setContentType("image/jpeg");
            return response;
        }

        return new ServiceResponse(404);
    }


  //**************************************************************************
  //** getMediaID
  //**************************************************************************
    public Long getMediaID(ServiceRequest request) throws Exception {

        Long mediaID = request.getID();
        if (mediaID==null) mediaID = request.getParameter("mediaID").toLong();
        if (mediaID==null) mediaID = request.getParameter("itemID").toLong();
        if (mediaID==null){ //Find mediaID using the URL path

            String path = javaxt.utils.URL.decode(request.getPath());
            path = path.substring("/image".length());
            if (path.endsWith("/")) path = path.substring(path.length()-1);
            var idx = path.lastIndexOf("/");
            if (idx==-1) throw new Exception("Invalid path");

            String name = path.substring(idx+1);
            path = path.substring(0, idx+1);

            HashSet<Long> mediaIDs = new HashSet<>();
            try (javaxt.sql.Connection conn = database.getConnection()){
                for (javaxt.sql.Record record : conn.getRecords(
                    "select media_item.id from media_item join media_item_file " +
                    "on media_item.id=media_item_file.media_item_id " +
                    "where lower(name)=lower('" + name.replace("'", "''")  + "') " +
                    "and media_item_file.file_id in (" +

                    "select file.id from path join file on path.id=file.path_id " +
                    "where lower(dir) like lower('%" + path.replace("'", "''") + "') " +

                    ")")){

                    mediaIDs.add(record.get(0).toLong());
                }
            }

            if (mediaIDs.size()!=1) throw new Exception("Invalid path");
            mediaID = mediaIDs.iterator().next();
        }
        return mediaID;
    }



  //**************************************************************************
  //** getIndex
  //**************************************************************************
    public ServiceResponse getIndex(ServiceRequest request) throws Exception {


      //Remove "thumbnails" from the path
        String path = javaxt.utils.URL.decode(request.getPath());
        path = path.substring("/index".length());
        if (path.endsWith("/")) path = path.substring(path.length()-1);
        //console.log(path);


      //Split path into separate directories
        if (path.startsWith("/")) path = path.substring(1);
        String[] dirs = path.split("/");


      //Parse params
        Boolean recursive = request.getParameter("recursive").toBoolean();
        if (recursive==null) recursive = false;
        String offsetAndLimit = request.getOffsetLimitStatement(database.getDriver());


      //Generate response
        try (javaxt.sql.Connection conn = database.getConnection()){


          //Find parent folder
            Long parentID = 1L;
            for (String folderName : dirs){
                //console.log(folderName);
                javaxt.sql.Record record = conn.getRecord(
                "select * from folder where name='" + folderName.replace("'", "''") +
                "' and parent_id="+ parentID);
                if (record==null) break;
                parentID = record.get("id").toLong();
            }


          //Compile query
            String sql;
            if (recursive){

                ArrayList<Long> folderIDs = new ArrayList<>();
                getSubFolders(parentID, folderIDs, conn);
                sql = "select media_item.id, name, hash from media_item " +
                "join folder_entry on media_item.id=folder_entry.item_id " +
                "where folder_id in(" + folderIDs
                    .stream().map(Object::toString).collect(Collectors.joining(",")) +
                ")";
                if (!offsetAndLimit.isBlank()){
                    sql += offsetAndLimit;
                }

            }
            else{


              //Find folders and images
                String folderQuery = "select id, name, '-' as hash from folder " +
                "where parent_id="+ parentID + " order by name";

                String mediaQuery = "select media_item.id, name, hash from media_item " +
                "join folder_entry on media_item.id=folder_entry.item_id " +
                "where folder_id=" + parentID + " " +
                "order by index";

                sql =
                "select * from (" + folderQuery + ") folderQuery \n" +
                "UNION ALL \n" +
                "select * from (" + mediaQuery + ") mediaQuery \n";
                if (!offsetAndLimit.isBlank()){
                    sql = "select * from (\n" + sql + "\n) x \n " + offsetAndLimit;
                }

            }



          //Execute query and generate response
            JSONArray items = new JSONArray();
            for (javaxt.sql.Record record : conn.getRecords(sql)){
                JSONObject item = record.toJson();
                items.add(item);
            }
            return new ServiceResponse(items);
        }
    }

    public ServiceResponse saveIndex(ServiceRequest request) throws Exception {
        request.parseJson();
        return getIndex(request);
    }


    private void getSubFolders(long parentID, ArrayList<Long> folderIDs, javaxt.sql.Connection conn) throws Exception {
        folderIDs.add(parentID);

        String folderQuery = "select id from folder " +
        "where parent_id="+ parentID + " order by name";

        ArrayList<Long> ids = new ArrayList<>();
        for (javaxt.sql.Record record : conn.getRecords(folderQuery)){
            long folderID = record.get(0).toLong();
            ids.add(folderID);
        }

        for (long id : ids){
            getSubFolders(id, folderIDs, conn);
        }
    }


  //**************************************************************************
  //** getSetting
  //**************************************************************************
    public ServiceResponse getSetting(ServiceRequest request) throws Exception {
        var key = request.getParameter("key").toString();
        var setting = Setting.get("key=",key.toLowerCase());
        var value = setting==null ? null : setting.getValue();
        if (value==null) return new ServiceResponse(404);
        else return new ServiceResponse(value);
    }


  //**************************************************************************
  //** saveSetting
  //**************************************************************************
    public ServiceResponse saveSetting(ServiceRequest request) throws Exception {

      //Validate user
        var user = (User) request.getUser();
        if (!isAdmin(user)) return new ServiceResponse(403, "Forbidden");

        
      //Parse payload
        request.parseJson();


      //Get key
        var key = request.getParameter("key").toString();
        if (key!=null) key = key.toLowerCase();
        if (key==null || key.isEmpty()) return new ServiceResponse(400, "Key is required");


      //Get value
        var value = request.getParameter("value").toString();
        if (value==null){
            try{
                value = new String(request.getPayload()).trim();
            }
            catch(Exception e){
                value = "";
            }
        }
        else{
            value = value.trim();
        }


      //Create or update setting
        var setting = Setting.get("key=",key);
        if (setting==null){
            setting = new Setting();
            setting.setKey(key);
        }
        setting.setValue(value);
        setting.save();
        notify("update", setting, user);
        return new ServiceResponse(setting.toJson());
    }


  //**************************************************************************
  //** deleteSetting
  //**************************************************************************
  /** Overrides the default deleteSetting method to prevent settings from
   *  being deleted.
   */
    public ServiceResponse deleteSetting(ServiceRequest request) throws Exception {
        return new ServiceResponse(403, "Forbidden");
    }


    private boolean isAdmin(User user){
        return true;
    }


  //**************************************************************************
  //** processRequest
  //**************************************************************************
  /** Used to process an HTTP request and generate an HTTP response.
   *  @param service The first "directory" found in the path.
   *  @param path The URL path, excluding the servlet context.
   */
    protected void processRequest(String service, String path,
        HttpServletRequest request, HttpServletResponse response)
        throws Exception {


        if (request.isWebSocket()){
            new WebSocketListener(request, response){
                private Long id;
                public void onConnect(){
                    id = webSocketID.incrementAndGet();
                    synchronized(listeners){
                        listeners.put(id, this);
                    }
                }
                public void onDisconnect(int statusCode, String reason){
                    synchronized(listeners){
                        listeners.remove(id);
                    }
                }
            };
        }
        else{

          //Get service response
            ServiceResponse rsp;
            ServiceRequest req = new ServiceRequest(request);
            if (webservices.containsKey(service)){
                if (path.startsWith("/")) path = path.substring(1);
                path = path.substring(service.length());
                req.setPath(path);
                rsp = webservices.get(service).getServiceResponse(req, database);
            }
            else{
                rsp = getServiceResponse(req, database);
            }


          //Send the response to the client
            rsp.send(response);
        }

    }



    public void onCreate(Object obj, ServiceRequest request){
        notify("create", (Model) obj, (User) request.getUser());
    };

    public void onUpdate(Object obj, ServiceRequest request){
        notify("update", (Model) obj, (User) request.getUser());
    };

    public void onDelete(Object obj, ServiceRequest request){
        notify("delete", (Model) obj, (User) request.getUser());
    };


    private void notify(String action, Model model, User user){
        long userID = user==null ? -1 : user.getID();
        notify(
            action+","+
            model.getClass().getSimpleName()+","+
            model.getID()+","+
            userID
        );
    }


    private void notify(String msg){
        synchronized(listeners){
            Iterator<Long> it = listeners.keySet().iterator();
            while(it.hasNext()){
                WebSocketListener ws = listeners.get(it.next());
                ws.send(msg);
            }
        }
    }

}