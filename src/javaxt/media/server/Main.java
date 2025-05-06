package javaxt.media.server;
import javaxt.media.utils.*;


import java.awt.*;
import java.util.*;
import java.security.KeyStore;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;


import javaxt.io.Jar;
import javaxt.json.JSONObject;
import javaxt.utils.ThreadPool;
import static javaxt.utils.Console.*;

import javaxt.express.*;
import javaxt.express.utils.*;
import javaxt.express.utils.StatusLogger;
import static javaxt.express.utils.StringUtils.*;
import static javaxt.express.ConfigFile.updateDir;
import javaxt.express.notification.NotificationService;



//******************************************************************************
//**  Main
//******************************************************************************
/**
 *   Command line interface used to start the web server or to run specialized
 *   functions (e.g. indexing photos, maintenance scripts, tests, etc).
 *
 ******************************************************************************/

public class Main {


  //**************************************************************************
  //** Main
  //**************************************************************************
  /** Entry point for the application
   */
    public static void main(String[] arguments) throws Exception {
        HashMap<String, String> args = parseArgs(arguments);


      //Get jar file and schema
        Jar jar = new Jar(Main.class);
        javaxt.io.File jarFile = new javaxt.io.File(jar.getFile());


      //Get config file
        javaxt.io.File configFile;
        if (args.containsKey("-config")){
            configFile = ConfigFile.getFile(args.get("-config"), jarFile);
            if (!configFile.exists()) {
                System.out.println("Could not find config file. " +
                "Use the \"-config\" parameter to specify a path to a config");
                return;
            }
        }
        else{
            javaxt.io.Directory dir = jarFile.getDirectory();
            configFile = new javaxt.io.File(dir, "config.json");
            if (!configFile.exists() && dir.getName().equals("dist")) {
                configFile = new javaxt.io.File(dir.getParentDirectory(), "config.json");
            }
        }



      //Initialize config
        Config.load(configFile);




      //Process command line args
        if (args.containsKey("-add")){
            add(args);
        }
        else if (args.containsKey("-update")){
            update(args);
        }
        else if (args.containsKey("-delete")){
            delete(args);
        }
        else if (args.containsKey("-query")){
            query(args);
        }
        else if (args.containsKey("-test")){
            test(args);
        }
        else{
            try{
                startServer(args, jar);
            }
            catch(Exception e){
                var msg = e.getMessage();
                if (msg==null) e.printStackTrace();
                else System.out.println(msg);
            }
        }

    }


  //**************************************************************************
  //** startServer
  //**************************************************************************
    private static void startServer(HashMap<String, String> args, Jar jar)
        throws Exception {



      //Get web config
        JSONObject webConfig = Config.get("webapp").toJSONObject();
        if (webConfig==null){
            webConfig = new JSONObject();
            Config.set("webapp", webConfig);
        }


      //Set path to the web directory
        javaxt.io.Directory web;
        String webDir = getValue(args, "--web", "-w").toString();
        if (webDir!=null){
            webConfig.set("webDir", webDir);
            updateDir("webDir", webConfig, new javaxt.io.File(jar.getFile()), false);
        }
        if (webConfig.has("webDir")){
            webDir = webConfig.get("webDir").toString();
            web = new javaxt.io.Directory(webDir);
            if (!web.exists() || webDir.length()==0){
                throw new IllegalArgumentException("Invalid \"webDir\" defined in config file");
            }
        }
        else{
            javaxt.io.Directory jarDir = new javaxt.io.Directory(jar.getFile().getParent());
            web = new javaxt.io.Directory(jarDir + "web");
            if (!web.exists()) web = new javaxt.io.Directory(jarDir.getParentDirectory() + "web");
            if (!web.exists()) throw new IllegalArgumentException("Failed to find web directory");
        }
        webConfig.set("webDir", web.toString());


      //Get path to the temp directory
        javaxt.io.Directory temp = null;
        String tempDir = Config.get("temp").toString();
        if (tempDir!=null){
            temp = new javaxt.io.Directory(tempDir);
            if (!temp.exists()) temp.create();
            if (!temp.exists()) throw new IllegalArgumentException(
                "Invalid \"temp\" directory defined in config file");
        }
        else{
            javaxt.io.Directory jarDir = new javaxt.io.Directory(jar.getFile().getParent());
            String dirName = jarDir.getName();
            if (dirName.equals("target") || dirName.equals("dist")) jarDir = jarDir.getParentDirectory();
            temp = new javaxt.io.Directory(jarDir + "temp");
            if (!temp.exists()) temp.create();
            if (!temp.exists()) throw new IllegalArgumentException("Failed to find temp directory");
        }
        Config.set("temp", temp.toString());


      //Set port to run on
        Integer port = getValue(args, "--port", "-p").toInteger();
        if (port==null) port = webConfig.get("port").toInteger();
        if (port==null || port<0) port = 8080;
        webConfig.set("port", port);


      //Set number of threads
        Integer maxThreads = getValue(args, "--threads", "-t").toInteger();
        if (maxThreads==null) maxThreads = webConfig.get("maxThreads").toInteger();
        if (maxThreads==null || maxThreads<0) maxThreads = 250;
        webConfig.set("maxThreads", maxThreads);


      //Get keystore (optional)
        KeyStore keystore = null;
        char[] keypass = null;
        if (webConfig.has("keystore") && webConfig.has("keypass")){
            try{
                keypass = webConfig.get("keypass").toString().toCharArray();
                keystore = KeyStore.getInstance("JKS");
                keystore.load(new java.io.FileInputStream(webConfig.get("keystore").toString()), keypass);
            }
            catch(Exception e){
                keystore = null;
                keypass = null;
            }
        }



      //Instantiate servlet
        javaxt.http.servlet.HttpServlet servlet = new WebApp();



      //Add keystore and truststore as needed
        if (keystore!=null){
            servlet.setKeyStore(keystore, new String(keypass));
            servlet.setTrustStore(keystore);
        }



      //Start web logger
        if (webConfig.has("logDir")){
            try{
                javaxt.io.Directory logDir = new javaxt.io.Directory(webConfig.get("logDir").toString());
                if (!logDir.exists()) logDir.create();
                if (logDir.exists()){
                    Logger logger = new Logger(logDir.toFile());
                    new Thread(logger).start();
                }
                else{
                    throw new Exception();
                }
            }
            catch(Exception e){
                System.out.println("WARNING: Invalid \"logDir\" defined in config file. Logging disabled.");
            }
        }




      //Start web server
        ArrayList<InetSocketAddress> addresses = new ArrayList<>();
        if (keystore==null){
            addresses.add(new InetSocketAddress("0.0.0.0", port));
        }
        else{
            addresses.add(new InetSocketAddress("0.0.0.0", 80));
            addresses.add(new InetSocketAddress("0.0.0.0", 443));
        }
        new javaxt.http.Server(addresses, maxThreads, servlet).start();
    }


  //**************************************************************************
  //** add
  //**************************************************************************
    private static void add(HashMap<String, String> args) throws Exception {

        String add = args.get("-add");
        if (add.equals("directory")){
            addDirectory(args);
        }
        else{
            System.out.println("Invalid -add");
        }
    }


  //**************************************************************************
  //** update
  //**************************************************************************
    private static void update(HashMap<String, String> args) throws Exception {

        String update = args.get("-update");
        if (update.equals("directory")){

        }
        else{
            System.out.println("Invalid -add");
        }
    }


  //**************************************************************************
  //** delete
  //**************************************************************************
    private static void delete(HashMap<String, String> args) throws Exception {

        String delete = args.get("-delete");
        if (delete.equals("thumbnails")){
            javaxt.io.Directory dir = getDirectory(args);
            if (dir==null) return;
            Maintanence.deleteThumbnails(dir);
        }
        else if (delete.equals("orphans")){

            Integer numThreads = getValue(args, "--threads", "-t").toInteger();
            if (numThreads==null || numThreads<0) numThreads = 4;

            Config.initModels();

            FileIndex fileIndex = new FileIndex(null, null, null, Config.getDatabase());
            fileIndex.deleteOrphans(numThreads);
        }
        else{
            System.out.println("Invalid -delete");
        }
    }


  //**************************************************************************
  //** addDirectory
  //**************************************************************************
  /** Used to index all the images and videos in a directory
   */
    private static void addDirectory(HashMap<String, String> args) throws Exception {

      //Get directory from args
        javaxt.io.Directory dir = getDirectory(args);
        if (dir==null) return;


      //Get thread count
        Integer numThreads = getValue(args, "--threads", "-t").toInteger();
        if (numThreads==null || numThreads<0) numThreads = 4;


      //Instantiate command line apps
        ImageMagick magick = new ImageMagick(Config.get("apps").get("ImageMagick").toString());
        FFmpeg ffmpeg = new FFmpeg(Config.get("apps").get("FFmpeg").toString());


      //Get face detection model
        javaxt.io.File faceDetecionModel = Config.getOnnxFile("face_detection_yunet_2023mar");


      //Generate list of supported file types
        ArrayList<String> fileFilters = new ArrayList<>();
        Collections.addAll(fileFilters, new String[]{
            "*.jpg", "*.jpeg", "*.jpe", //jpeg varients
            "*.png", "*.heic", "*.webp" //other image formats
        });
        for (String ext : ffmpeg.getSupportedFileExtensions()){
            fileFilters.add("*."+ext);
        }
        String[] filter = fileFilters.toArray(new String[fileFilters.size()]);



      //Init models
        Config.initModels();




      //Start NotificationService and watch for updates from the FileIndex
        var recordCounter = new AtomicLong(0);
        var taskID = DateUtils.getCurrentTime();
        var v = new javaxt.utils.Record();
        System.out.print("Scanning directory...");
        NotificationService.start();
        NotificationService.addListener((
            String event, String source, javaxt.utils.Value data, long timestamp)->{

            if (source.equals(FileIndex.class.getSimpleName())){
                JSONObject msg = (JSONObject) data.toObject();
                if (msg.get("taskID").equals(taskID)){


                    Long totalRecords = msg.get("totalRecords").toLong();
                    if (totalRecords==null){ //update count
                        long numRecords = recordCounter.incrementAndGet();
                        if (v.has("statusLogger")){
                            var statusLogger = (StatusLogger) v.get("statusLogger").toObject();
                            if (numRecords==statusLogger.getTotalRecords()){
                                statusLogger.shutdown();
                                NotificationService.stop();
                            }
                        }
                    }
                    else{ //set total

                        System.out.println("Done!");


                        if (recordCounter.get()==totalRecords){
                            NotificationService.stop();
                        }
                        else{
                            v.set("statusLogger",
                                new StatusLogger(recordCounter, new AtomicLong(totalRecords))
                            );
                        }
                    }

                }
            }
        });




      //Instantiate FileIndex and add files
        FileIndex fileIndex = new FileIndex(magick, ffmpeg, faceDetecionModel, Config.getDatabase());
        fileIndex.addDirectory(dir, filter, numThreads, taskID);
    }



  //**************************************************************************
  //** convert
  //**************************************************************************
  /** Used to create JPEGs alongside HEIC files
   */
    private static void convert(HashMap<String, String> args) throws Exception {

        ImageMagick magick = new ImageMagick(Config.get("apps").get("ImageMagick").toString());

        java.io.File f = new java.io.File(args.get("-convert"));
        if (f.isFile()){
            magick.createJPEG(new javaxt.io.File(f));
        }
        else{

            Integer numThreads = getValue(args, "--threads", "-t").toInteger();
            if (numThreads==null || numThreads<0) numThreads = 4;

            ThreadPool pool = new ThreadPool(numThreads, 200){
                public void process(Object obj){
                    javaxt.io.File file = (javaxt.io.File) obj;
                    magick.createJPEG(file);
                }
            }.start();


            javaxt.io.Directory dir = new javaxt.io.Directory(f);
            console.log(dir);
            java.util.List files = dir.getChildren(true, null, false);
            while (true){

                Object obj;
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

                if (obj==null){
                    pool.done();
                    break;
                }
                else{
                    if (obj instanceof javaxt.io.File){
                        pool.add(obj);
                    }
                }
            }

            pool.join();
        }
    }


  //**************************************************************************
  //** query
  //**************************************************************************
    private static void query(HashMap<String, String> args) throws Exception {

        String query = args.get("-query");
        console.log(query);

        try (javaxt.sql.Connection conn = Config.getDatabase().getConnection()){
            for (javaxt.sql.Record record : conn.getRecords(query)){
                console.log(record.toJson());
            }
        }
    }


  //**************************************************************************
  //** test
  //**************************************************************************
    private static void test(HashMap<String, String> args) throws Exception {

        String test = args.get("-test");
        if (test.equals("ImageMagick")){
            new ImageMagick(Config.get("apps").get("ImageMagick").toString());
            System.out.println("OK");
        }
        else if (test.equals("FFmpeg")){
            new FFmpeg(Config.get("apps").get("FFmpeg").toString());
            System.out.println("OK");
        }
        else if (test.equals("metadata")){

            javaxt.io.File file = new javaxt.io.File(args.get("-file"));



          //Instantiate command line apps
            ImageMagick magick = new ImageMagick(Config.get("apps").get("ImageMagick").toString());
            FFmpeg ffmpeg = new FFmpeg(Config.get("apps").get("FFmpeg").toString());

            if (ffmpeg.isMovie(file)){


                System.out.println(ffmpeg.getMetadata(file).toString(3));

            }
            else{


              //Print metadata from JavaXT
                javaxt.io.Image img = file.getImage();
                System.out.println(ImageUtils.getMetadata(img).toString(3));
                System.out.println("----------------------------");

              //Print metadata from ImageMagick
                System.out.println(ImageUtils.getMetadata(file, magick).toString(3));


              //Print and match all EXIF metadata from JavaXT and ImageMagick
                if (1<0){
                    HashMap<String, String> vals = new HashMap<>();

                    console.log("ImageMagick metadata:");
                    TreeMap<String, String> exif = magick.getExif(file);
                    for (String key : exif.keySet()){
                        String val = exif.get(key);
                        vals.put(val, key);
                        console.log(key, val);
                    }

                    console.log("-----------------------");

                    console.log("JavaXT metadata:");
                    HashMap<Integer, Object> tags = img.getExifTags();
                    for (Integer i : tags.keySet()){
                        Object val = tags.get(i);
                        console.log(vals.get(val.toString()), String.format("0x%04X", i), val);
                    }

                    console.log("-----------------------");

                    console.log("GPS-specific data:");
                    tags = img.getGpsTags();
                    for (Integer i : tags.keySet()){
                        Object val = tags.get(i);
                        console.log(vals.get(val.toString()), String.format("0x%04X", i), val);
                    }
                }
            }

        }
        else if (test.equals("faces")){

          //Get ImageMagick
            ImageMagick magick = new ImageMagick(Config.get("apps").get("ImageMagick").toString());


          //Get image from file
            javaxt.io.File file = new javaxt.io.File(args.get("-file"));
            javaxt.io.Image img = getImage(file, magick);



          //Detect faces
            javaxt.io.File model = Config.getOnnxFile("face_detection_yunet_2023mar");
            var faces = OpenCV.detectFaces(img, OpenCV.getFaceDetector(model));


          //Render rectangles
            Graphics2D g2d = img.getBufferedImage().createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(new Color(255, 0, 0));
            g2d.setStroke(new BasicStroke(3));
            for (OpenCV.Face face : faces){
                Rectangle r = face.getRectangle();
                g2d.drawRect(r.x, r.y, r.width, r.height);
            }
            g2d.dispose();


            img.saveAs(file.getDirectory() + file.getName(false) + "_FACES_LG_v4.jpg");
        }
        else if (test.equals("faceComparison")){

            ImageMagick magick = new ImageMagick(Config.get("apps").get("ImageMagick").toString());
            Object faceDetecionModel = OpenCV.getFaceDetector(Config.getOnnxFile("face_detection_yunet_2023mar"));
            Object faceRecognitionModel = OpenCV.getFaceRecognizer(Config.getOnnxFile("face_recognition_sface_2021dec"));


          //Get faces from file
            javaxt.io.File file = new javaxt.io.File(args.get("-file"));
            javaxt.io.Image img = getImage(file, magick);
            org.opencv.core.Mat mat = OpenCV.getMat(img);
            var faces = OpenCV.detectFaces(mat, faceDetecionModel);
            console.log("found " + faces.size() + " faces in " + file.getName());


          //Get faces from file2
            javaxt.io.File file2 = new javaxt.io.File(args.get("-file2"));
            javaxt.io.Image img2 = getImage(file2, magick);
            org.opencv.core.Mat mat2 = OpenCV.getMat(img2);
            var faces2 = OpenCV.detectFaces(mat2, faceDetecionModel);
            console.log("found " + faces2.size() + " faces in " + file2.getName());


            if (faces.isEmpty() || faces2.isEmpty()) return;


          //Update mats
            OpenCV.Face f = faces.getFirst();
            img.resize(f.getInputWidth(), f.getInputHeight(), false);
            mat = OpenCV.getMat(img);
            f = faces2.getFirst();
            img2.resize(f.getInputWidth(), f.getInputHeight(), false);
            mat2 = OpenCV.getMat(img2);



          //Compare faces
            int x = 0;
            for (OpenCV.Face face1 : faces){
                x++;
                //if (face1.getConfidence()<50.0) continue;



                int y = 0;
                for (OpenCV.Face face2 : faces2){
                    y++;
                    //if (face2.getConfidence()<50.0) continue;


                    double similarity =  OpenCV.compareFaces(
                        face1.getDetection(), mat,
                        face2.getDetection(), mat2,
                        faceRecognitionModel
                    );


                    //console.log(similarity);
                    if (similarity>=0.363){
                        console.log("face " + x + " is similar to face " + y);
                    }
                }
            }
        }
        else if (test.equals("fileDates")){
            int x = 0;
            javaxt.io.Directory dir = getDirectory(args);
            if (dir==null) return;
            var files = dir.getChildren(true, null, false);
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
                    break;
                }
                else{
                    if (obj instanceof javaxt.io.File){
                        x++;
                        var file = (javaxt.io.File) obj;
                        new javaxt.utils.Date(file.getDate());
                        if (file.getDate()==null) console.log(file);
                    }
                }
            }
            console.log("Processed " + x + " files");
        }
        else{
            System.out.println("Invalid -test");
        }
    }


  //**************************************************************************
  //** getImage
  //**************************************************************************
    private static javaxt.io.Image getImage(javaxt.io.File file, ImageMagick magick){
        javaxt.io.Image image = file.getImage();
        if (image.getBufferedImage()==null){ //unsupported image format (e.g. HEIF)
            javaxt.io.File f = magick.createJPEG(file);
            JSONObject metadata = ImageUtils.getMetadata(f, magick);
            image = f.getImage();
            Integer orientation = metadata.get("orientation").toInteger();
            console.log(orientation);
            if (orientation!=null) ImageUtils.rotate(image, orientation);
            return image;
        }
        else{
            //console.log(image.getExifTags().get(0x0112)); //orientation
            image.rotate();
            return image;
        }
    }


  //**************************************************************************
  //** getDirectory
  //**************************************************************************
    private static javaxt.io.Directory getDirectory(HashMap<String, String> args) throws Exception {

        String path = args.get("-path");

      //Clean up the path if windows
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        if (isWindows && path.contains("\\\\")){
            boolean addSlash = path.startsWith("\\\\");
            path = path.replace("\\\\", "/");
            if (addSlash) path = "/" + path;
        }


      //Get directory from path
        try{
            javaxt.io.Directory dir = new javaxt.io.Directory(path);
            if (!dir.exists()) throw new Exception();
            return dir;

        }
        catch(Exception e){
            System.out.println("Invalid or missing directory specified in \"-index\". " + path);
            return null;
        }
    }
}