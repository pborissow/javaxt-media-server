package javaxt.media.server;

//Java includes
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


//JavaXT includes
import javaxt.sql.*;
import javaxt.json.*;

import javaxt.express.*;
import javaxt.http.servlet.*;
import javaxt.express.services.FileService;
import javaxt.express.services.QueryService;
import javaxt.http.websocket.WebSocketListener;
import javaxt.express.notification.NotificationService;

import javaxt.media.models.Feature;
import javaxt.media.models.Setting;
import javaxt.media.utils.FFmpeg;
import javaxt.media.utils.ImageMagick;


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
    private ConcurrentHashMap<String, WebService> webservices;
    private ConcurrentHashMap<Long, WebSocketListener> listeners;
    private AtomicLong webSocketID;
    private FileService fileService;
    private long dbDate;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public WebServices(javaxt.sql.Database database) throws Exception {
        this.database = database;
        this.dbDate = database.getRecord(
        "select value from setting where key='db_date'").get(0).toLong();


      //Init apps and models
        Config.initApps();
        Config.initModels();


      //Register models
        javaxt.io.Jar jar = new javaxt.io.Jar(this.getClass());
        for (Class c : jar.getClasses()){
            if (javaxt.sql.Model.class.isAssignableFrom(c)) addModel(c);
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


      //Instaniate the FileService
        fileService = new FileService();


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
                javaxt.express.services.QueryService.QueryJob queryJob =
                (javaxt.express.services.QueryService.QueryJob) data.toObject();
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
        javaxt.media.utils.RRDImage thumbnail;
        try{
            Object[] arr = getImageFiles(mediaID);
            file = (javaxt.io.File) arr[0];
            thumbnail = (javaxt.media.utils.RRDImage) arr[1];
        }
        catch(Exception e){
            return new ServiceResponse(e);
        }
        if (file==null || !file.exists()) return new ServiceResponse(404);


      //Set version number using the file date
        javaxt.utils.Date lastModified = new javaxt.utils.Date(file.getDate());
        long currVersion = lastModified.toLong();



      //Get requested version number from the url
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
  //** getVideo
  //**************************************************************************
    public ServiceResponse getVideo(ServiceRequest request) throws Exception {

      //Get mediaID
        Long mediaID = getMediaID(request);
        if (mediaID==null) return new ServiceResponse(400, "id is required");


        //TODO: Check permissions


      //Get file
        javaxt.io.File file = null;
        try (javaxt.sql.Connection conn = database.getConnection()){

            var record = conn.getRecord(
            "select file.id from media_item_file " +
            "join file on media_item_file.file_id=file.id " +
            "where media_item_file.media_item_id=" + mediaID +
            " and lower(extension)='mp4'");

            if (record!=null){
                var fileID = record.get(0).toLong();
                javaxt.media.models.File f = new javaxt.media.models.File(fileID);
                var dir = new javaxt.io.Directory(f.getPath().getDir());
                file = new javaxt.io.File(dir, f.getName() + "." + f.getExtension());
            }

        }
        catch(Exception e){

        }
        if (file==null || !file.exists()) return new ServiceResponse(404);



      //Set version number using the file date
        javaxt.utils.Date lastModified = new javaxt.utils.Date(file.getDate());
        long currVersion = lastModified.toLong();



      //Get requested version number from the url
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


      //If we're still here, return the file
        return new ServiceResponse(file);
    }


  //**************************************************************************
  //** getFace
  //**************************************************************************
    public ServiceResponse getFace(ServiceRequest request) throws Exception {

      //Get featureID
        Long featureID = getFeatureID(request);
        if (featureID==null) return new ServiceResponse(400, "id is required");


      //Get requested version number from the url
        javaxt.utils.URL url = request.getURL();
        long requestedVersion = 0;
        try{ requestedVersion = Long.parseLong(url.getParameter("v")); }
        catch(Exception e){}
        long requestedDB = 0;
        try{ requestedDB = Long.parseLong(url.getParameter("db")); }
        catch(Exception e){}


      //Send redirect as needed
        if (requestedDB!=dbDate){
            url.setParameter("db", dbDate+"");
            String location = url.toString();
            return new ServiceResponse(301, location);
        }


        byte[] thumbnail = null;
        try(Connection conn = database.getConnection()){
            javaxt.sql.Record record = conn.getRecord(
                "select thumbnail from feature where id=" + featureID
            );
            if (record != null){
                thumbnail = (byte[]) record.get("thumbnail").toObject();
            }
        }

      //Return image
        if (thumbnail==null){
            return new ServiceResponse(404);
        }
        else{
            ServiceResponse response = new ServiceResponse(thumbnail);
            response.setContentType("image/jpeg");
            return response;
        }
    }


  //**************************************************************************
  //** getMatchingFaces
  //**************************************************************************
  /** Returns media items that match a given featureID (face)
   */
    public ServiceResponse getMatchingFaces(ServiceRequest request) throws Exception {

      //Get featureID
        Long featureID = getFeatureID(request);
        if (featureID==null) return new ServiceResponse(400, "id is required");


      //Find transitive matches
        int maxDepth = 10;
        Set<Long> matches = new HashSet<>();
        Set<Long> currentLevel = new HashSet<>();
        currentLevel.add(featureID);

        for (int depth = 0; depth < maxDepth && !currentLevel.isEmpty(); depth++) {
            Set<Long> nextLevel = new HashSet<>();

          //Get direct matches for all features in current level
            try (Connection conn = database.getConnection()) {
                for (Long featureId : currentLevel) {

                  //Get matches where this feature is the source
                    for (javaxt.sql.Record record : conn.getRecords(
                        "SELECT matching_feature_id FROM feature_match " +
                        "WHERE feature_id = " + featureId + " AND ignore_match = false")) {
                        Long matchId = record.get("matching_feature_id").toLong();
                        if (!matches.contains(matchId) && !matchId.equals(featureID)) {
                            nextLevel.add(matchId);
                            matches.add(matchId);
                        }
                    }

                  //Get matches where this feature is the target
                    for (javaxt.sql.Record record : conn.getRecords(
                        "SELECT feature_id FROM feature_match " +
                        "WHERE matching_feature_id = " + featureId + " AND ignore_match = false")) {
                        Long matchId = record.get("feature_id").toLong();
                        if (!matches.contains(matchId) && !matchId.equals(featureID)) {
                            nextLevel.add(matchId);
                            matches.add(matchId);
                        }
                    }
                }
            }

            currentLevel = nextLevel;
        }


      //Return media items
        var mediaItems = new JSONArray();
        if (!matches.isEmpty()){
            String sql =
            "SELECT DISTINCT mi.id, mi.name, mi.type, mi.hash FROM feature f " +
            "JOIN media_item mi ON mi.id = f.item_id WHERE f.id IN (" +
            matches.stream().map(Object::toString).collect(Collectors.joining(",")) +
            ") ORDER BY mi.id" + request.getOffsetLimitStatement(database.getDriver());
            try (Connection conn = database.getConnection()){
                for (javaxt.sql.Record record : conn.getRecords(sql)){
                    mediaItems.add(record.toJson());
                }
            }
        }
        return new ServiceResponse(mediaItems);
    }


  //**************************************************************************
  //** getFeatureID
  //**************************************************************************
    public Long getFeatureID(ServiceRequest request) throws Exception {
        Long featureID = request.getParameter("featureID").toLong();
        if (featureID==null) featureID = request.getID();
        return featureID;
    }


  //**************************************************************************
  //** getThumbnails
  //**************************************************************************
  /** Returns a list of all the thumbnail sizes available for a given mediaID
   */
    public ServiceResponse getThumbnails(ServiceRequest request) throws Exception {

      //Get mediaID
        Long mediaID = getMediaID(request);
        if (mediaID==null) return new ServiceResponse(400, "id is required");


        //TODO: Check permissions


      //Get file
        javaxt.io.File file;
        javaxt.media.utils.RRDImage thumbnail;
        try{
            Object[] arr = getImageFiles(mediaID);
            file = (javaxt.io.File) arr[0];
            thumbnail = (javaxt.media.utils.RRDImage) arr[1];
        }
        catch(Exception e){
            return new ServiceResponse(e);
        }
        if (file==null || !file.exists()) return new ServiceResponse(404);



      //Set image version number using the file date
        javaxt.utils.Date lastModified = new javaxt.utils.Date(file.getDate());
        long currVersion = lastModified.toLong();



      //Get requested version number from the url
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

        JSONArray arr = new JSONArray();
        if (thumbnail!=null){
            for (javaxt.media.utils.RRDImage.Entry entry : thumbnail.getIndex()){
                arr.add(entry.getWidth()+"x"+entry.getHeight());
            }
        }
        return new ServiceResponse(arr);
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
  //** getMediaItem
  //**************************************************************************
  /** Intercept and prune MediaItem requests
   */
    public ServiceResponse getMediaItem(ServiceRequest request) throws Exception {
        Long mediaID = getMediaID(request);
        if (mediaID==null) return new ServiceResponse(400, "id is required");

        var json = new javaxt.media.models.MediaItem(mediaID).toJson();
        json.remove("files");
        return new ServiceResponse(json);
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
                sql = "select media_item.id, name, type, hash from media_item " +
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
                String folderQuery = "select id, name, 'folder' as type, '-' as hash from folder " +
                "where parent_id="+ parentID + " order by name";

                String mediaQuery = "select media_item.id, name, type, hash from media_item " +
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
                items.add(record.toJson());
            }
            return new ServiceResponse(items);
        }
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
  /** Custom implementation of a Setting model get request. Returns the
   *  "value" for a requested "key" in plain text.
   */
    public ServiceResponse getSetting(ServiceRequest request) throws Exception {

        var key = request.getParameter("key").toString();
        if (key==null) key = request.getPath(1).toString();
        if (key!=null) key = key.trim().toLowerCase();
        if (key==null || key.isEmpty()) return new ServiceResponse(400, "Key is required");

        var setting = Setting.get("key=",key);
        var value = setting==null ? null : setting.getValue();
        if (value==null) return new ServiceResponse(404);
        else return new ServiceResponse(value);
    }


  //**************************************************************************
  //** saveSetting
  //**************************************************************************
  /** Custom implementation of a Setting model save request. Looks for a
   *  "value" parameter in the payload of the request, if the parameter is not
   *  found using conventional means. Also validates certian key/value pairs
   *  before creating/updating a setting.
   */
    public ServiceResponse saveSetting(ServiceRequest request) throws Exception {

      //Validate user
        Integer accessLevel;
        var user = (javaxt.media.models.User) request.getUser();
        try(Connection conn = database.getConnection()){
            accessLevel = SecurityFilter.getAccessLevel(user, "SysAdmin", conn);
        }
        if (accessLevel<5) return new ServiceResponse(403, "Forbidden");


      //Parse payload
        request.parseJson();


      //Get key
        var key = request.getParameter("key").toString();
        if (key==null) key = request.getPath(1).toString();
        if (key!=null) key = key.trim().toLowerCase();
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


      //Validate setting
        if (!value.isEmpty()){
            if (key.equals("imagemagick")){
                try {new ImageMagick(value);}
                catch(Exception e){return new ServiceResponse(400, "Invalid path");}
            }
            else if (key.equals("ffmpeg")){
                try {new FFmpeg(value);}
                catch(Exception e){return new ServiceResponse(400, "Invalid path");}
            }
            else if (key.equals("face_detection") || key.equals("facial_recognition")){
                try {
                    var file = new javaxt.io.File(value);
                    if (!file.exists() || !file.getExtension().equalsIgnoreCase("onnx")){
                        throw new Exception();
                    }
                }
                catch(Exception e){return new ServiceResponse(400, "Invalid path");}
            }
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
  //** dir
  //**************************************************************************
  /** Custom method used to browse files on the server, upload new files,
   *  and download.
   */
    public ServiceResponse dir(ServiceRequest request) throws Exception {

      //Validate user
        Integer accessLevel;
        var user = (javaxt.media.models.User) request.getUser();
        try(Connection conn = database.getConnection()){
            accessLevel = SecurityFilter.getAccessLevel(user, "SysAdmin", conn);
        }
        if (accessLevel<5) return new ServiceResponse(403, "Forbidden");


      //Generate response
        String op = request.getPath(0).toString();
        if (op==null) op = "";
        else op = op.toLowerCase().trim();

        if (op.equals("upload")){
            return fileService.upload(request, (javaxt.utils.Record record)->{

                String path = record.get("path").toString();
                javaxt.io.File file = (javaxt.io.File) record.get("file").toObject();
                path += file.getName();

                //NotificationService.notify(record.get("op").toString(), "File", new javaxt.utils.Value(path));
            });
        }
        else if (op.equals("download")){
            ServiceResponse response = fileService.getFile(request);
            String fileName = response.get("name").toString();
            response.setContentDisposition(fileName);
            return response;
        }
        else{
            return fileService.getList(request);
        }
    }


  //**************************************************************************
  //** getImageFiles
  //**************************************************************************
    private Object[] getImageFiles(long mediaID) throws Exception {

        javaxt.io.File file = null;
        javaxt.media.utils.RRDImage thumbnail = null;
        try (javaxt.sql.Connection conn = database.getConnection()){
            javaxt.media.models.File f = null;

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
                if (record!=null){
                    var fileID = record.get(0).toLong();
                    f = new javaxt.media.models.File(fileID);
                }
            }
            else{
                var fileID = record.get(0).toLong();
                f = new javaxt.media.models.File(fileID);
                thumbnail = new javaxt.media.utils.RRDImage(f);
            }

            if (f!=null){
                var dir = new javaxt.io.Directory(f.getPath().getDir());
                file = new javaxt.io.File(dir, f.getName() + "." + f.getExtension());
            }
        }

        return new Object[]{file, thumbnail};
    }


  //**************************************************************************
  //** getRecordset
  //**************************************************************************
  /** Used to update queries and modify payloads for model-related services.
   */
    protected Recordset getRecordset(ServiceRequest request, String op,
        Class c, String sql, Connection conn) throws Exception {
        return SecurityFilter.getRecordset(request, op, c, sql, conn);
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

          //Get service request
            ServiceRequest req = new ServiceRequest(request);


          //Parse payload as needed
            boolean parseJson = true;
            String contentType = request.getContentType();
            if (contentType!=null){
                if (contentType.startsWith("multipart/form-data")){ //e.g. media upload
                    parseJson = false;
                }
            }
            if (parseJson) req.parseJson();


          //Get service response
            ServiceResponse rsp;
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
            for (Long key : listeners.keySet()){
                listeners.get(key).send(msg);
            }
        }
    }

}