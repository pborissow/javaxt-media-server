package javaxt.media.utils;

import java.awt.*;
import java.util.*;
import java.sql.PreparedStatement;
import java.util.stream.Collectors;
import java.awt.image.BufferedImage;
import javaxt.express.notification.NotificationService;


import javaxt.sql.*;
import javaxt.json.*;
import javaxt.utils.ThreadPool;
import javaxt.media.utils.OpenCV.Face;
import static javaxt.utils.Console.console;
import static javaxt.media.utils.ImageUtils.*;

import org.opencv.objdetect.FaceDetectorYN;


//******************************************************************************
//**  FileIndex Class
//******************************************************************************
/**
 *   Used to index images and videos on a local drive
 *
 ******************************************************************************/

public class FileIndex {


    /** Must be in descending order! */
    private String[] imageSizes = new String[]{
        "2560x1440","1920x1080", "1536x864","1366x768","1280x720", //wide desktop screens
        "1024x768","800x600", //old screens and tablets
        "300x300", "150x100" //thumbnails
    };

    private String thumbnailFileExtension = "rrd"; //"xtn"



    private javaxt.media.models.Folder rootFolder;
    private javaxt.media.models.Host host;
    private ImageMagick magick;
    private FFmpeg ffmpeg;
    private Database database;
    private javaxt.io.File faceDetecionModel;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Used to instantiate this class
   */
    public FileIndex(ImageMagick magick, FFmpeg ffmpeg,
        javaxt.io.File faceDetecionModel, Database database) throws Exception {

        this.magick = magick;
        this.ffmpeg = ffmpeg;
        this.database = database;
        this.faceDetecionModel = faceDetecionModel;



      //Get or create host
        String hostName = "localhost";
        host = javaxt.media.models.Host.get("name=", hostName);
        if (host==null){
            host = new javaxt.media.models.Host();
            host.setName(hostName);
            host.save();
        }



      //Get or create "root" folder
        String folderName = "root";
        //Can't use Folder.get() because root parent folder id = 0
        //rootFolder = javaxt.mml.Folder.get("name=", folderName);
        rootFolder = new javaxt.media.models.Folder();
        rootFolder.setName(folderName);
        javaxt.media.models.Folder f = new javaxt.media.models.Folder();
        f.setID(0L);
        rootFolder.setParent(f);
        try (Connection conn = database.getConnection()){
            javaxt.sql.Record r = conn.getRecord(
            "select id from folder where name='" + folderName + "'");
            Long id = r==null ? null : r.get(0).toLong();
            if (id==null) rootFolder.save();
            else rootFolder.setID(id);
        }
    }


  //**************************************************************************
  //** addDirectory
  //**************************************************************************
  /** Used to index all the files in a given directory
   */
    public void addDirectory(javaxt.io.Directory directory,
        String[] fileFilter, int numThreads, Long taskID) throws Exception {

      //Get name of this class (for notification purposes)
        String source = getClass().getSimpleName();


      //
        deleteOrphans(numThreads);



      //Start thread pool used to create MediaItems
        int poolSize = 1000;
        ThreadPool pool = new ThreadPool(numThreads, poolSize){

            public void process(Object obj){
                try{
                    var arr = (Object[]) obj;
                    var fileIDs = (Long[]) arr[0];
                    var folderID = (Long) arr[1];
                    var folderIndex = (Long) arr[2];



                  //Get or create an instance of the FaceDetectorYN class. The
                  //class is not thread-safe so each thread will have it's own
                  //dedicated instance.
                    FaceDetectorYN faceDetector = (FaceDetectorYN) get("faceDetector", ()->{
                        return OpenCV.getFaceDetector(faceDetecionModel);
                    });



                  //Create MediaItem
                    createMediaItem(fileIDs, folderID, folderIndex, fileFilter, faceDetector);


                }
                catch(Throwable e){
                    e.printStackTrace();
                }



                if (taskID!=null){
                    JSONObject msg = new JSONObject();
                    msg.set("taskID", taskID);
                    NotificationService.notify("update", source, new javaxt.utils.Value(msg));
                }
            }
        }.start();



      //Generate list of files to process
        HashSet<Long> pathIDs = addPaths(directory, fileFilter);
        ArrayList<Long[]> fileIDs = addFiles(pathIDs, fileFilter);



      //Update status logger with a total count
        if (taskID!=null){
            JSONObject msg = new JSONObject();
            msg.set("taskID", taskID);
            msg.set("totalRecords", fileIDs.size());
            NotificationService.notify("update", source, new javaxt.utils.Value(msg));
        }



      //Add items to the pool
        HashMap<javaxt.io.Directory, javaxt.media.models.Folder> folders = new HashMap<>();
        folders.put(directory, getOrCreateFolder(directory, rootFolder));
        javaxt.io.Directory prevDir = null;
        long idx = 0;
        try (Connection conn = database.getConnection()) {
            for (Long[] ids : fileIDs){


              //Get directory associated with this file group
                javaxt.io.Directory dir = null;
                javaxt.sql.Record r = conn.getRecord(
                "select path_id from file where id="+ids[0]);
                if (r!=null) dir = getDirectory(r.get(0).toLong(), conn);


              //Get or create folder for the directory
                javaxt.media.models.Folder folder = folders.get(dir);
                if (folder==null){

                  //Generate list of dirs all the way up to the parent directory
                    ArrayList<javaxt.io.Directory> dirs = new ArrayList<>();
                    while (true){
                        dirs.add(dir);
                        dir = dir.getParentDirectory();
                        if (dir.equals(directory)){
                            break;
                        }
                    }


                  //Create folder for each dir
                    javaxt.media.models.Folder parentFolder = folders.get(directory);
                    for (int i=dirs.size()-1; i>=0; i--){
                        dir = dirs.get(i);
                        folder = getOrCreateFolder(dir, parentFolder);
                        folders.put(dir, folder);
                        parentFolder = folder;
                    }
                }



              //Update sequence ID
                if (dir.equals(prevDir)) idx++;
                else idx = 0;
                prevDir = dir;



              //Update thread pool
                pool.add(new Object[]{ids, folder.getID(), idx});
            }
        }



        pool.done();
        pool.join();
    }


  //**************************************************************************
  //** deleteOrphans
  //**************************************************************************
  /** Checks whether files in the database still exist on disk. If not, the
   *  files are deleted from the database. If the corresponding media item is
   *  left without any files, the media item and thumbnail are deleted as
   *  well. Similarly, any paths that are left without files are removed from
   *  the database.
   */
    public void deleteOrphans(int numThreads) throws Exception {


      //Start thread pool used to create media items
        int poolSize = 1000;
        ThreadPool pool = new ThreadPool(numThreads, poolSize){
            public void process(Object obj){
                Long fileID = (Long) obj;

                try (Connection conn = database.getConnection()){

                    javaxt.sql.Record record = conn.getRecord(
                        "select name, extension, dir, path_id from file " +
                        "join path on file.path_id=path.id " +
                        "where file.id = " + fileID
                    );

                    if (record!=null){
                        String fileName = record.get("name").toString();
                        String fileExt = record.get("extension").toString();
                        String dir = record.get("dir").toString();
                        Long pathID = record.get("path_id").toLong();

                        javaxt.io.File file = new javaxt.io.File(
                            new javaxt.io.Directory(dir), fileName + "." + fileExt
                        );


                        if (!file.exists()){


                          //Generate a list of media items that use this file
                            HashSet<Long> mediaIDs = new HashSet<>();
                            for (javaxt.sql.Record r : conn.getRecords(
                                "select distinct(MEDIA_ITEM_ID) from MEDIA_ITEM_FILE " +
                                "where FILE_ID=" + fileID)){
                                mediaIDs.add(r.get(0).toLong());
                            }


                          //Delete entry from the database
                            conn.execute("delete from file where id=" + fileID);



                          //Delete media items with no files
                            for (Long mediaID : mediaIDs){
                                record = conn.getRecord(
                                    "select count(*) from MEDIA_ITEM_FILE " +
                                    "where MEDIA_ITEM_ID = " + mediaID
                                );
                                if (record==null || record.get(0).toLong().longValue()==0){

                                  //Delete record from the database
                                    conn.execute("delete from MEDIA_ITEM where ID=" + mediaID);

                                  //Delete thumbnail
                                    String path = getThumbnailDirectory(file) + file.getName(false) + "." + thumbnailFileExtension;
                                    new javaxt.io.File(path).delete();
                                }
                            }



                          //Delete paths without any files
                            record = conn.getRecord(
                                "select count(*) from FILE " +
                                "where PATH_ID = " + pathID
                            );
                            if (record==null || record.get(0).toLong().longValue()==0){
                                conn.execute("delete from PATH where ID=" + pathID);
                            }

                        }
                    }


                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
        }.start();



        try (Connection conn = database.getConnection()){
            for (javaxt.sql.Record record : conn.getRecords("select id from file")){
                pool.add(record.get(0).toLong());
            }
        }


        pool.done();
        pool.join();

    }

    private javaxt.io.Directory getDirectory(Long pathID, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.getConnection().prepareStatement(
        "select dir from path where id=?")){
            return getDirectory(pathID, stmt);
        }
    }

    private javaxt.io.Directory getDirectory(Long pathID, PreparedStatement stmt) throws Exception {
        javaxt.io.Directory dir = null;
        stmt.setLong(1, pathID);
        java.sql.ResultSet r2 = stmt.executeQuery();
        try (Recordset rs = new Recordset()){
            rs.open(r2);
            if (rs.hasNext()){
                dir = new javaxt.io.Directory(rs.getValue(0).toString());
            }
        }
        return dir;
    }


  //**************************************************************************
  //** addFiles
  //**************************************************************************
  /** Creates entries in the FILE table for directories that contain media
   *  files. Returns an ordered set of file IDs, grouped by file name and path.
   */
    private ArrayList<Long[]> addFiles(HashSet<Long> pathIDs, String[] fileFilter) throws Exception {

        TreeMap<String, Long[]> groups = new TreeMap<>();


      //Create a series of prepared statements and then process the pathIDs
        try (Connection c2 = database.getConnection()){
            try (PreparedStatement s2 = c2.getConnection().prepareStatement(
            "select id from file where path_id=? and LOWER(name)=LOWER(?) and LOWER(extension)=LOWER(?)")){

                try (Connection conn = database.getConnection()) {
                    try (PreparedStatement stmt = conn.getConnection().prepareStatement(
                    "select dir from path where id=?")){




                      //Process pathIDs
                        for (Long pathID : pathIDs){


                          //Get directory
                            javaxt.io.Directory dir = getDirectory(pathID, stmt);
                            if (dir==null) continue;



                          //Get files, grouped by file name
                            TreeMap<String, ArrayList<javaxt.io.File>> files = new TreeMap<>();
                            for (javaxt.io.File file : dir.getFiles()){
                                String fileName = file.getName(false).toLowerCase();
                                ArrayList<javaxt.io.File> arr = files.get(fileName);
                                if (arr==null){
                                    arr = new ArrayList<>();
                                    files.put(fileName, arr);
                                }
                                arr.add(file);
                            }



                          //Loop through the groups
                            for (String fileName : files.keySet()){


                              //Check if the current group of files contains any media files
                                boolean hasMediaFile = false;
                                for (javaxt.io.File file : files.get(fileName)){
                                    String ext = "*." + file.getExtension();
                                    for (String filter : fileFilter){
                                        if (ext.equalsIgnoreCase(filter)){
                                            hasMediaFile = true;
                                            break;
                                        }
                                    }
                                    if (hasMediaFile) break;
                                }
                                if (!hasMediaFile) continue;




                              //Create/update records in the FILE table
                                HashMap<Long, javaxt.io.File> filesWithIDs =
                                addFiles(files.get(fileName), pathID, s2);





                              //Send fileIDs to the pool to process
                                Long[] fileIDs = new Long[filesWithIDs.size()];
                                int i = 0;
                                for (long fileID : filesWithIDs.keySet()){
                                    fileIDs[i] = fileID;
                                    i++;
                                }

                                groups.put(dir+fileName, fileIDs);
                            }

                        }

                    }
                }

            }
        }


        ArrayList<Long[]> arr = new ArrayList<>();
        for (String key : groups.keySet()){
            arr.add(groups.get(key));
        }
        return arr;

    }


    private HashMap<Long, javaxt.io.File> addFiles(
        ArrayList<javaxt.io.File> files, long pathID, PreparedStatement stmt)
        throws Exception {

        HashMap<Long, javaxt.io.File> filesWithIDs = new HashMap<>();

        try (Connection conn = database.getConnection()) {

            for (javaxt.io.File file : files){

                String fileName = file.getName(false);
                String fileExt = file.getExtension();
                Long fileID = null;


                stmt.setLong(1, pathID);
                stmt.setString(2, fileName);
                stmt.setString(3, fileExt);
                java.sql.ResultSet r2 = stmt.executeQuery();
                try (Recordset rs = new Recordset()){
                    rs.open(r2);
                    if (rs.hasNext()) fileID = rs.getValue(0).toLong();
                }

                try (Recordset rs = new Recordset()){

                    if (fileID==null){
                        rs.open("select * from file where id=-1", conn, false);
                        rs.addNew();
                        rs.setValue("name", fileName);
                        rs.setValue("extension", fileExt);
                        rs.setValue("path_id", pathID);
                        rs.setValue("type", file.getContentType());
                    }
                    else{
                        rs.open("select * from file where id="+fileID, conn, false);
                    }

                    rs.setValue("date", file.getDate());
                    rs.setValue("size", file.getSize());
                    rs.setValue("hash", file.getMD5()); //vs SHA1?
                    rs.update();

                    if (fileID==null) fileID = rs.getGeneratedKey().toLong();
                }

                filesWithIDs.put(fileID, file);
            }
        }

        return filesWithIDs;
    }



  //**************************************************************************
  //** addPaths
  //**************************************************************************
  /** Creates entries in the PATH table for directories that contain media
   *  files. Returns an unordered, unique set of path IDs.
   */
    private HashSet<Long> addPaths(javaxt.io.Directory directory, String[] fileFilter) throws Exception {
        try (Connection conn = database.getConnection()) {
            try (PreparedStatement stmt = conn.getConnection().prepareStatement(
            "select id from path where host_id=? and dir=?")){
                return addPaths(directory, fileFilter, stmt);
            }
        }
    }

    private HashSet<Long> addPaths(javaxt.io.Directory directory, String[] fileFilter, PreparedStatement stmt)
        throws Exception {

        var paths = new HashMap<String, Long>(100000);


        java.util.List files = directory.getChildren(true, fileFilter, false);
        Object obj;
        while (true){
            synchronized (files) {
                while (files.isEmpty()) {
                    try {
                        files.wait();
                    }
                    catch (InterruptedException e) {
                        break;
                    }
                }
                obj = files.remove(0);
                files.notifyAll();
            }


            if (obj==null){ //file search is complete
                return new HashSet<>(paths.values());
            }
            else{
                if (obj instanceof javaxt.io.File){
                    javaxt.io.File file = (javaxt.io.File) obj;
                    String dir = file.getDirectory().toString().replace("\\", "/");

                    if (paths.containsKey(dir)) continue;
                    if (dir.contains("/.") || dir.contains("/_")) continue;


                  //Create path as needed
                    try{

                        Long pathID = null;
                        stmt.setLong(1, host.getID());
                        stmt.setString(2, dir);
                        java.sql.ResultSet r2 = stmt.executeQuery();
                        try (Recordset rs = new Recordset()){
                            rs.open(r2);
                            if (rs.hasNext()) pathID = rs.getValue(0).toLong();
                        }

                        javaxt.media.models.Path path;
                        if (pathID==null){
                            path = new javaxt.media.models.Path();
                            path.setHost(host);
                            path.setDir(dir);
                            path.save();
                        }
                        else{
                            path = new javaxt.media.models.Path(pathID);
                        }


                        paths.put(dir, path.getID());

                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }

                }
            }
        }
    }



  //**************************************************************************
  //** createMediaItem
  //**************************************************************************
  /** Used to create or update a MediaItem from a group of files. Assumes the
   *  files all have the same name and are in the same directory. If a
   *  MediaItem exists for a given group of files, this method will simply
   *  update the file list associated with the MediaItem. Otherwise, this
   *  method will create a new MediaItem (insert records into the database,
   *  extract features, and generate a thumbnail).
   *  @param faceDetector An instance of a FaceDetectorYN used to extract
   *  faces from images.
   */
    private void createMediaItem(Long[] fileIDs, Long folderID, Long folderIndex,
        String[] fileFilter, FaceDetectorYN faceDetector) throws Exception {


      //Generate a comma delimited list of file IDs
        String ids = Arrays.stream(fileIDs)
        .map(String::valueOf) //.mapToObj(String::valueOf) // convert each long to a string
        .collect(Collectors.joining(", ")); // join them with ", "


        try (Connection conn = database.getConnection()){


          //Get ordered list of files and path ID
            var filesWithIDs = new LinkedHashMap<Long, javaxt.io.File>();
            Long pathID = null;
            for (javaxt.sql.Record record : conn.getRecords(
                "select file.id, path_id, name, extension, dir from file " +
                "join path on file.path_id=path.id " +
                "where file.id IN (" + ids + ") order by date, size desc")){


                long fileID = record.get("id").toLong();
                String fileName = record.get("name").toString();
                String fileExt = record.get("extension").toString();
                String dir = record.get("dir").toString();
                pathID = record.get("path_id").toLong();

                javaxt.io.File file = new javaxt.io.File(
                    new javaxt.io.Directory(dir), fileName + "." + fileExt
                );

                filesWithIDs.put(fileID, file);
            }


          //Identify primary file in the group (e.g. HEIF)
            Long primaryID = getPrimaryMediaFile(filesWithIDs, fileFilter);
            javaxt.io.File primaryFile = filesWithIDs.get(primaryID);



          //Find any MediaItems that use these files
            HashSet<Long> mediaIDs = new HashSet<>();
            for (javaxt.sql.Record record : conn.getRecords(
                "select distinct(MEDIA_ITEM_ID) from MEDIA_ITEM_FILE " +
                "where FILE_ID in (" + ids + ")")){
                mediaIDs.add(record.get(0).toLong());
            }


          //If there are records, remove any MediaItems that contain mixed
          //files (different file names or paths)
            if (!mediaIDs.isEmpty()){
                for (Long id : mediaIDs.toArray(new Long[mediaIDs.size()])){

                    HashSet<String> uniquePaths = new HashSet<>();
                    javaxt.media.models.MediaItem item = new javaxt.media.models.MediaItem(id);
                    for (javaxt.media.models.File f : item.getFiles()){
                        String path = f.getPath().getDir();
                        if (!path.contains(".thumbnails")){
                            uniquePaths.add(path + f.getName());
                        }
                    }

                    if (uniquePaths.size()>1){ //mixed collection of files
                        mediaIDs.remove(id);
                    }
                }
            }


          //Create/update record in the MEDIA_ITEM table
            Long mediaID;
            if (mediaIDs.isEmpty()){ //if no records, create a new MediaItem


              //Get metadata and create thumbnail
                JSONObject metadata;
                Long pHash;
                javaxt.io.File tn = null;


                Object[] img = getOrCreateImage(primaryFile);
                javaxt.io.Image image = (javaxt.io.Image) img[0];
                javaxt.io.File file = (javaxt.io.File) img[1];


                if (!primaryFile.equals(file)){ //unsupported image format (e.g. HEIF, video, etc)
                    var jpeg = file;

                  //Extract metadata
                    if (ffmpeg.isMovie(primaryFile)){
                        metadata = ffmpeg.getMetadata(primaryFile);
                    }
                    else{
                        metadata = getMetadata(primaryFile, magick);
                    }


                  //If metadata is empty, check whether there's a sidecar JPEG
                  //file (e.g. older ipads)
                    if (metadata.isEmpty()){
                        javaxt.io.File f = new javaxt.io.File(
                            primaryFile.getDirectory(),
                            primaryFile.getName(false) + ".JPG"
                        );
                        if (f.exists()) metadata = getMetadata(f, magick);
                    }



                  //If the primaryFile is a movie, create an MP4 for streaming
                    if (ffmpeg.isMovie(primaryFile)){
                        if (!primaryFile.getExtension().equalsIgnoreCase("MP4")){
                            javaxt.io.File mp4 = new javaxt.io.File(
                                jpeg.getDirectory(), primaryFile.getName(false) + ".mp4"
                            );
                            ffmpeg.createMP4(primaryFile, mp4);
                            if (mp4.exists()){

                              //Update list of file IDs
                                javaxt.media.models.File f = getOrCreateFile(mp4, conn);
                                filesWithIDs.put(f.getID(), mp4);

                              //If a jpeg of the movie doesn't exist,
                              //try creating one again using the mp4
                                if (!jpeg.exists()){
                                    ffmpeg.createJPEG(mp4, jpeg);
                                    image = jpeg.getImage();
                                }
                            }
                        }
                    }


                  //Create thumbail of the JPEG
                    if (jpeg.exists()){



                        if (image.getBufferedImage()==null){
                            console.log("Failed to open JPEG. " + jpeg);
                            image = null;
                            pHash = null;
                        }
                        else{

                          //Rotate the image
                            /** Nevermind! ImageMagick seems to be auto-rotating the JPEGs */
                            //Integer orientation = metadata.get("orientation").toInteger();
                            //if (orientation!=null) rotate(image, orientation);


                          //Get pHash
                            pHash = image.getPHash();


                          //Create thumbnail
                            try{
                                tn = getThumbnailFile(primaryFile);
                                createThumbnail(image, tn);
                            }
                            catch(Exception e){ //let's not make this a deal breaker...
                                e.printStackTrace();
                                tn = null;
                            }


                          //Save the JPEG file in the database
                            javaxt.media.models.File f = getOrCreateFile(jpeg,
                            new javaxt.media.models.Path(pathID));



                          //Update files HashMap
                            filesWithIDs.put(f.getID(), jpeg);

                        }
                    }
                    else{
                        console.log("Failed to create JPEG for " + primaryFile);
                        pHash = null;
                    }

                }
                else{ //supported image

                  //Get metadata
                    metadata = getMetadata(image);


                  //Rotate the image
                    Integer orientation = metadata.get("orientation").toInteger();
                    if (orientation!=null) rotate(image, orientation);



                  //Get pHash
                    pHash = image.getPHash();

                    try{
                        tn = getThumbnailFile(primaryFile);
                        createThumbnail(image, tn);
                    }
                    catch(Exception e){ //let's not make this a deal breaker...
                        e.printStackTrace();
                        tn = null;
                    }
                }



              //Add thumbnail to the database
                javaxt.media.models.File thumbnail;
                if (tn!=null){
                    thumbnail = getOrCreateFile(tn, conn);
                    filesWithIDs.put(thumbnail.getID(), tn);
                }



              //Update the MEDIA_ITEM table
                try (Recordset rs = conn.getRecordset(
                    "select * from MEDIA_ITEM where id=-1", false)){

                    rs.addNew();
                    rs.setValue("name", primaryFile.getName(false));
                    rs.setValue("date", cint(primaryFile.getDate()));

                    if (pHash!=null){
                        rs.setValue("hash", Long.toBinaryString(pHash));
                    }


                    if (metadata!=null && !metadata.isEmpty()){

                      //Extract date from metadata
                        try{
                            Integer date = cint(metadata.get("date").toDate());
                            if (date!=null) rs.setValue("date", date);
                        }
                        catch(Exception e){}


                      //Get location from metadata
                        try{
                            rs.setValue("location",
                                new org.locationtech.jts.io.WKTReader().read(
                                    metadata.get("location").toString()
                                )
                            );
                        }
                        catch(Exception e){}


                      //Save metadata
                        rs.setValue("info", new javaxt.sql.Function(
                            "?::jsonb", metadata.toString()
                        ));

                    }

                    rs.update();
                    mediaID = rs.getGeneratedKey().toLong();
                }


              //Update the MEDIA_ITEM_FILE table
                for (Long fileID : filesWithIDs.keySet()){
                    conn.execute(
                        "insert into MEDIA_ITEM_FILE(MEDIA_ITEM_ID, FILE_ID, IS_PRIMARY) " +
                        "values(" + mediaID + "," + fileID + "," + fileID.equals(primaryID) + ")"
                    );
                }





              //Extract features
                try{

                    ArrayList<Face> faces = getFaces(
                        new RRDImage(tn),
                        image.getWidth(),
                        image.getHeight(),
                        faceDetector
                    );


                    if (!faces.isEmpty()){
                        //console.log(primaryFile.getName(), faces.size());


                        JSONObject info = new JSONObject();
                        info.set("nmsThreshold", faceDetector.getNMSThreshold());
                        info.set("scoreThreshold", faceDetector.getScoreThreshold());
                        info.set("topK", faceDetector.getTopK());
                        info.set("inputSize", faceDetector.getInputSize().width + "x" + faceDetector.getInputSize().height);


                        try (Recordset rs = conn.getRecordset(
                            "select * from FEATURE where id=-1", false)){
                            for (Face face : faces){

                                JSONObject _face = face.toJson();
                                info.set("confidence", face.getConfidence());
                                info.set("mat", _face.get("mat"));


                              //Create thumbnail
                                Rectangle r = face.getRectangle();
                                javaxt.io.Image i = image.copyRect(r.x, r.y, r.width, r.height);
                                int w = i.getWidth();
                                int h = i.getHeight();
                                if (w>=h){
                                    if (w>300) i.setWidth(300);
                                }
                                else{
                                    if (h>300) i.setHeight(300);
                                }
                                byte[] b = i.getByteArray();


                              //Save record
                                rs.addNew();
                                rs.setValue("ITEM_ID", mediaID);
                                rs.setValue("LABEL", "FACE");
                                rs.setValue("THUMBNAIL", b);
                                rs.setValue("COORDINATES", new javaxt.sql.Function(
                                    "?::jsonb", _face.get("coordinates").toString()
                                ));
                                rs.setValue("INFO", new javaxt.sql.Function(
                                    "?::jsonb", info.toString()
                                ));
                                rs.update();

                            }
                        }
                    }

                }
                catch(Exception e){
                    //TODO: log this error
                }

            }
            else{ //otherwise, update the MediaItem


                if (mediaIDs.size()==1){
                    mediaID = mediaIDs.iterator().next();



                  //Create thumbnail as needed
                    javaxt.io.File tn = getThumbnailFile(primaryFile);
                    if (!tn.exists()){
                        try{
                            Object[] img = getOrCreateImage(primaryFile);
                            var image = (javaxt.io.Image) img[0];
                            createThumbnail(image, tn);
                        }
                        catch(Exception e){ //let's not make this a deal breaker...
                            e.printStackTrace();
                        }
                    }


                  //Add thumbnail and any other related files in the thumbnails
                  //directory to list of fileIDs
                    for (var file : tn.getDirectory().getFiles(tn.getName(false) + ".*")){
                        var f = getOrCreateFile(file, conn);
                        filesWithIDs.put(f.getID(), tn);
                    }



                  //Update MEDIA_ITEM_FILE table
                    var additions = filesWithIDs.keySet();
                    var deletions = new ArrayList<Long>();
                    for (javaxt.sql.Record record : conn.getRecords(
                        "select FILE_ID from MEDIA_ITEM_FILE " +
                        "where MEDIA_ITEM_ID=" + mediaID)){

                        Long fileID = record.get(0).toLong();
                        if (filesWithIDs.containsKey(fileID)){
                            additions.remove(fileID);
                        }
                        else{
                            deletions.add(fileID);
                        }
                    }


                    for (Long addition : additions){
                        conn.execute(
                            "insert into MEDIA_ITEM_FILE(MEDIA_ITEM_ID, FILE_ID) " +
                            "values(" + mediaID + "," + addition + ")"
                        );
                    }

                    for (Long deletion : deletions){
                        conn.execute(
                            "delete from MEDIA_ITEM_FILE where " +
                            "MEDIA_ITEM_ID=" + mediaID + " and FILE_ID=" + deletion
                        );
                    }

                }
                else{
                    throw new Exception("Multiple MediaItems found");
                }
            }



          //Add MediaItem to a Folder
            if (folderID!=null){
                try (Recordset rs = conn.getRecordset(
                    "select * from folder_entry where folder_id=" + folderID +
                    " and item_id=" + mediaID, false)){
                    if (rs.EOF){
                        rs.addNew();
                        rs.setValue("folder_id", folderID);
                        rs.setValue("item_id", mediaID);
                    }
                    rs.setValue("index", folderIndex);
                    rs.update();
                }
            }
        }
    }


  //**************************************************************************
  //** getPrimaryMediaFile
  //**************************************************************************
  /** Used to identify the primary file in a file group. For example, some
   *  iPhone models create multiple files for a single photo (e.g.
   *  "IMG_0001.AAE", "IMG_0001.HEIC", "IMG_0001.MOV"). In some cases there
   *  are JPEG thumbnails sitting next to a source file. This method returns
   *  a best guess as to which file contains the actual photo.
   */
    private Long getPrimaryMediaFile(LinkedHashMap<Long, javaxt.io.File> filesWithIDs,
        String[] fileFilter) throws Exception{

        if (filesWithIDs.size()==1) return filesWithIDs.keySet().iterator().next();


      //Generate a list of supported media files
        ArrayList<Long> mediaFiles = new ArrayList<>();
        for (Long fileID : filesWithIDs.keySet()){
            javaxt.io.File file = filesWithIDs.get(fileID);
            String ext = file.getExtension().toLowerCase();


          //Immediately return heic
            if (ext.equals("heif") || ext.equals("heic")) return fileID;


          //Update mediaFiles
            for (String filter : fileFilter){
                if (filter.startsWith("*.")) filter = filter.substring(2);
                if (ext.equals(filter)){
                    mediaFiles.add(fileID);
                }
            }
        }


      //Return first non-jpeg media file
        for (Long fileID : mediaFiles){
            javaxt.io.File file = filesWithIDs.get(fileID);
            String ext = file.getExtension().toLowerCase();
            if (!ext.equals("jpg")){


              //Special case for short clips (e.g. MOV sidecar file for iPhones)
                if (ffmpeg.isMovie(file)){
                    Double duration = ffmpeg.getDuration(file);
                    if (duration!=null && duration<4.0) continue;
                }

                return fileID;
            }
        }




      //Return a jpeg if there is one
        for (Long fileID : mediaFiles){
            javaxt.io.File file = filesWithIDs.get(fileID);
            String ext = file.getExtension().toLowerCase();
            if (ext.equals("jpg")) return fileID;
        }


      //If we're still here, simply return the first file from the list
        return filesWithIDs.keySet().iterator().next();
    }


  //**************************************************************************
  //** getThumbnailDirectory
  //**************************************************************************
  /** This method is used to determine where to save a thumbnail. It is called
   *  at runtime. Override as needed.
   */
    public javaxt.io.Directory getThumbnailDirectory(javaxt.io.File file){
        return new javaxt.io.Directory(file.getDirectory() + ".thumbnails");
    }


  //**************************************************************************
  //** getThumbnailFile
  //**************************************************************************
    private javaxt.io.File getThumbnailFile(javaxt.io.File primaryFile){
        String path = getThumbnailDirectory(primaryFile) + primaryFile.getName(false);
        return new javaxt.io.File(path + "." + thumbnailFileExtension);
    }


  //**************************************************************************
  //** createThumbnail
  //**************************************************************************
  /** Used to create a thumbnail file (RRDImage) in a thumbnail directory
   */
    private void createThumbnail(javaxt.io.Image img, javaxt.io.File thumbnailFile) throws Exception {
        if (thumbnailFile.exists()) return;


      //Check if we have a valid image
        BufferedImage bufferedImage = img.getBufferedImage();
        if (bufferedImage==null) throw new Exception("Invalid image");



      //Delete old temp file
        javaxt.io.File temp = new javaxt.io.File(thumbnailFile.getDirectory(), thumbnailFile.getName(false) + ".tmp");
        if (temp.exists()) temp.delete();
        if (temp.exists()) throw new Exception("Could not delete temp file: " + temp);


      //Copy image so we don't alter the original
        img = img.copy();


      //Create rrd
        RRDImage rrd = new RRDImage(temp.toFile());
        try{

            for (String id : imageSizes){

              //Set local variables
                String[] arr = id.toLowerCase().split("x");
                int maxWidth = Integer.parseInt(arr[0]);
                int maxHeight = Integer.parseInt(arr[1]);


                if (img.getWidth()>maxWidth ||
                    img.getHeight()>maxHeight){

                  //Update height
                    if (img.getHeight()>maxHeight){
                        img.setHeight(maxHeight);
                    }


                  //Update width
                    if (img.getWidth()>maxWidth){
                        img.setWidth(maxWidth);
                    }


                  //Sharpen image and set output quality
                    if (img.getWidth()<=300 && img.getHeight()<=300){
                        img.sharpen();
                        img.setOutputQuality(1.00f);
                    }
                    else{
                        img.setOutputQuality(0.9f);
                    }


                  //Add image to the thumbail
                    rrd.addImage(id,img);
                }

            }

          //Close the thumbnail file
            rrd.close();


          //Rename the temp file
            temp.rename(thumbnailFile.getName());

        }
        catch(Exception e){ //Something happened. Let's clean-up

            rrd.close();

            if (temp.exists()) temp.delete();
            if (thumbnailFile.exists()) thumbnailFile.delete();

            if (temp.exists()) throw new Exception("Could not delete temp file: " + temp);
            if (thumbnailFile.exists()) throw new Exception("Could not delete thumbnail file: " + thumbnailFile);

            throw e;
        }
    }


  //**************************************************************************
  //** getFaces
  //**************************************************************************
  /** Used to detect faces in an image. Note that we are using a thumbnail
   *  file instead of the original image because (a) thumbnails are rotated
   *  correctly and (b) we may need a lower resolution copy of the original
   *  image for the face detector.
   */
    private ArrayList<Face> getFaces(RRDImage rrd, int orgWidth,
        int orgHeight, FaceDetectorYN faceDetector) throws Exception {
        var faces = new ArrayList<Face>();


      //Get image from the thumbnail
        javaxt.io.Image image = null;
        TreeMap<Integer, String> index = new TreeMap<>();
        for (RRDImage.Entry e : rrd.getIndex()){
            index.put(e.getWidth(), e.getID());
        }
        Iterator<Integer> it = index.descendingKeySet().iterator();
        while (it.hasNext()){
            Integer width = it.next();
            String id = index.get(width);
            if (width<=800){
                image = rrd.getImage(id);
                break;
            }
        }
        if (image==null) return faces;



      //Get image size
        int inputWidth = image.getWidth();
        int inputHeight = image.getHeight();



      //Run the face detector
        for (int numTries=0; numTries<5; numTries++){
            try{
                var f = new ArrayList<Face>();
                for (Face face : OpenCV.detectFaces(image, faceDetector)){


                  //Get rectangle
                    Rectangle r = face.getRectangle();
                    int x = r.x;
                    int y = r.y;
                    int w = r.width;
                    int h = r.height;


                  //Resize rectangle to fit original image
                    if (orgWidth!=inputWidth || orgHeight!=inputHeight){
                        x = (x*orgWidth)/inputWidth;
                        y = (y*orgHeight)/inputHeight;
                        w = (w*orgWidth)/inputWidth;
                        h = (h*orgHeight)/inputHeight;

                        face.setRectangle(new Rectangle(x,y,w,h));
                    }

                    f.add(face);
                }


                if (f.size()>0 && f.size()<20){
                    //console.log("Found " + f.size() + " faces after " + (numTries+1) + " tries");
                    faces = f;
                    break;
                }
            }
            catch(Exception e){
            }
        }


        return faces;
    }


  //**************************************************************************
  //** getOrCreateFile
  //**************************************************************************
  /** Used to save a file in the database
   */
    private javaxt.media.models.File getOrCreateFile(javaxt.io.File file, Connection conn)
        throws Exception {


      //Get or create path
        javaxt.media.models.Path path = null;
        String dir = file.getDirectory().toString().replace("\\", "/");
        try (PreparedStatement stmt = conn.getConnection().prepareStatement(
        "select id from path where host_id=? and dir=?")){


            Long pathID = null;
            for (var i=0; i<4; i++){
                try{
                    stmt.setLong(1, host.getID());
                    stmt.setString(2, dir);
                    java.sql.ResultSet r2 = stmt.executeQuery();
                    try (Recordset rs = new Recordset()){
                        rs.open(r2);
                        if (rs.hasNext()) pathID = rs.getValue(0).toLong();
                    }

                    if (pathID==null){
                        path = new javaxt.media.models.Path();
                        path.setHost(host);
                        path.setDir(dir);
                        path.save();
                    }
                    else{
                        path = new javaxt.media.models.Path(pathID);
                    }
                    break;
                }
                catch(Exception e){
                    Thread.sleep(300);
                }
            }
        }

        if (path==null) throw new Exception();
        return getOrCreateFile(file, path);
    }


    private javaxt.media.models.File getOrCreateFile(
        javaxt.io.File file, javaxt.media.models.Path path)
        throws Exception {



      //Get or create file
        String fileName = file.getName(false);
        String fileExt = file.getExtension();
        javaxt.media.models.File f = javaxt.media.models.File.get(
            "path_id=", path.getID(),
            "name=", fileName,
            "lower(extension)=", fileExt.toLowerCase()
        );
        if (f==null){
            f = new javaxt.media.models.File();
            f.setName(fileName);
            f.setExtension(fileExt);
            f.setPath(path);
            f.setSize(file.getSize());
            f.setHash(file.getMD5());
            f.setType(file.getContentType());
            try{
            f.setDate(new javaxt.utils.Date(file.getDate()));
            }
            catch(Exception e){
                console.log(file.getDate(), file);
            }
            f.save();
        }

        return f;
    }


  //**************************************************************************
  //** getOrCreateFolder
  //**************************************************************************
  /** Used to save a folder in the database
   */
    private javaxt.media.models.Folder getOrCreateFolder(javaxt.io.Directory directory,
        javaxt.media.models.Folder parentFolder) throws Exception {

        //Can't use Folder.get() because root parent folder id = 0
        //javaxt.mml.Folder folder = javaxt.mml.Folder.get(
        //"name=", directory.getName(), "parent_id=", rootFolder.getID());


        String folderName = directory.getName();
        javaxt.media.models.Folder folder = new javaxt.media.models.Folder();
        folder.setName(folderName);
        folder.setParent(parentFolder);

        try (Connection conn = database.getConnection()){
            javaxt.sql.Record r = conn.getRecord(
            "select id from folder where name='" + folderName.replace("'", "''") + "' " +
            "and parent_id=" + parentFolder.getID());
            Long id = r==null ? null : r.get(0).toLong();
            if (id==null) folder.save();
            else folder.setID(id);
        }

        return folder;
    }



    private Object[] getOrCreateImage(javaxt.io.File primaryFile){

        javaxt.io.Image image = primaryFile.getImage();
        if (image.getBufferedImage()==null){ //unsupported image format (e.g. HEIF, video, etc)


          //Set path to JPEG for the primaryFile
            javaxt.io.File jpeg = new javaxt.io.File(
                getThumbnailDirectory(primaryFile),
                primaryFile.getName(false) + ".jpg"
            );


          //Create JPEG
            if (ffmpeg.isMovie(primaryFile)){
                ffmpeg.createJPEG(primaryFile, jpeg);
            }
            else{
                magick.convert(primaryFile, jpeg);
            }


            return new Object[]{jpeg.getImage(), jpeg};

        }
        else{
            return new Object[]{image, primaryFile};
        }
    }



    private Integer cint(java.util.Date date){
        try{
            return cint(new javaxt.utils.Date(date));
        }
        catch(Exception e){
            return null;
        }
    }

    private Integer cint(javaxt.utils.Date date){
        try{
            return Integer.parseInt(date.toString("yyyyMMdd"));
        }
        catch(Exception e){
            return null;
        }
    }
}