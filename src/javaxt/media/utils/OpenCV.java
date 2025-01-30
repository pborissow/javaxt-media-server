package javaxt.media.utils;

//Java imports
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.awt.Rectangle;
import java.util.regex.Pattern;
import java.awt.image.*;

//JavaXT imports
import javaxt.io.Jar;
import javaxt.json.JSONArray;
import javaxt.json.JSONObject;

//OpenCV imports
import org.opencv.dnn.*;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.FaceDetectorYN;
import org.opencv.objdetect.FaceRecognizerSF;



//******************************************************************************
//**  OpenCV
//******************************************************************************
/**
 *  Used to extract faces from images
 *
 ******************************************************************************/

public class OpenCV {

    private static Boolean dllLoaded;


  //**************************************************************************
  //** getFaceDetector
  //**************************************************************************
  /** Returns an instance of the FaceDetectorYN model used to detect faces
   */
    public static Object getFaceDetector(javaxt.io.File modelFile) throws Exception {

      //Load library
        if (!loadLib()) throw new Exception();


      //Load the YuNet model (validation purposes only)
        Net net = Dnn.readNetFromONNX(modelFile.toString()); //"yunet.onnx"
        //console.log("Loaded model " + net.getLayerNames().size() + " with layers");


      //Set up model
        return FaceDetectorYN.create(
            modelFile.toString(), //model name
            "", //
            new Size(640, 640), //dimensions of the input image
            0.6f, //confThreshold
            0.3f, //nmsThreshold
            5000, //topK
            0, //backendId
            0 //targetId
        );

    }


  //**************************************************************************
  //** getFaceRecognizer
  //**************************************************************************
  /** Returns an instance of the FaceRecognizerSF model used to match faces
   */
    public static Object getFaceRecognizer(javaxt.io.File modelFile) throws Exception {

      //Load library
        if (!loadLib()) throw new Exception();


      //Load the YuNet model (validation purposes only)
        Net net = Dnn.readNetFromONNX(modelFile.toString()); //"yunet.onnx"
        //console.log("Loaded model " + net.getLayerNames().size() + " with layers");

        return FaceRecognizerSF.create(
            modelFile.toString(), //model name
            ""
        );
    }


  //**************************************************************************
  //** detectFaces
  //**************************************************************************
  /** Used to detect faces in an image
   *  @param imageFile A path to an image file (e.g. jpg). The image should
   *  be rotated correctly prior to calling this method.
   *  @param model Instance of an OpenCV model (e.g. FaceDetectorYN)
   */
    public static ArrayList<Face> detectFaces(
        javaxt.io.File imageFile, Object model) throws Exception {
        return detectFaces(getMat(imageFile), model);
    }


  //**************************************************************************
  //** detectFaces
  //**************************************************************************
  /** Used to detect faces in an image
   *  @param image A valid image. The image should be rotated correctly prior
   *  to calling this method.
   *  @param model Instance of an OpenCV model (e.g. FaceDetectorYN)
   */
    public static ArrayList<Face> detectFaces(
        javaxt.io.Image image, Object model) throws Exception {
        return detectFaces(getMat(image), model);
    }


    public static ArrayList<Face> detectFaces(
        Mat image, Object model) throws Exception {

        if (model==null) throw new Exception();
        FaceDetectorYN fd = (FaceDetectorYN) model;


      //Load image
        int orgWidth = image.cols();
        int orgHeight = image.rows();


      //Resize image as needed
        int maxWidth = 600;
        int inputWidth = orgWidth;
        int inputHeight = orgHeight;
        if (orgWidth>maxWidth){

          //Calculate the new height while maintaining aspect ratio
            int newHeight = (int) (image.height() * (maxWidth / (double) image.width()));

          //Resize the image
            Mat resizedImage = new Mat();
            Imgproc.resize(image, resizedImage, new Size(maxWidth, newHeight));

            image = resizedImage;
            inputWidth = image.cols();
            inputHeight = image.rows();
        }




      //Run image detection
        Mat detections = new Mat();
        fd.setInputSize(new Size(inputWidth, inputHeight));
        fd.detect(image, detections);



      //Extract faces
        var faces = new ArrayList<Face>();
        for (int i=0; i<detections.rows(); i++) {
            Mat detection = detections.row(i);


          //Get confidence score
            Mat confidence = detection.colRange(2, 3);
            double confidenceValue = confidence.get(0, 0)[0];



          //Get rectangle
            int x = (int) detection.get(0, 0)[0];
            int y = (int) detection.get(0, 1)[0];
            int w = (int) detection.get(0, 2)[0];
            int h = (int) detection.get(0, 3)[0];


            if (orgWidth!=inputWidth || orgHeight!=inputHeight){
                x = (x*orgWidth)/inputWidth;
                y = (y*orgHeight)/inputHeight;
                w = (w*orgWidth)/inputWidth;
                h = (h*orgHeight)/inputHeight;
            }
            Rectangle rect = new Rectangle(x,y,w,h);


            faces.add(new Face(rect, confidenceValue, inputWidth, inputHeight, detection));
        }

        return faces;
    }


  //**************************************************************************
  //** Face Class
  //**************************************************************************
    public static class Face {
        private Rectangle rect;
        private Double confidence;
        private Integer inputWidth;
        private Integer inputHeight;
        private Mat detection;

        public Face(Rectangle rect, Double confidence,
            Integer inputWidth, Integer inputHeight, Mat detection){
            this.rect = rect;
            this.confidence = confidence;
            this.inputWidth = inputWidth;
            this.inputHeight = inputHeight;
            this.detection = detection;
        }

        public Face(JSONObject face){

            JSONObject r = face.get("coordinates").get("rect").toJSONObject();
            rect = new Rectangle(
                r.get("x").toInteger(), r.get("y").toInteger(),
                r.get("w").toInteger(), r.get("h").toInteger()
            );

            confidence = face.get("confidence").toDouble();

            JSONObject d = face.get("detection").toJSONObject();
            JSONObject input = d.get("input").toJSONObject();
            inputWidth = input.get("width").toInteger();
            inputHeight = input.get("height").toInteger();

            JSONObject output = d.get("output").toJSONObject();
            detection = json2mat(output.get("mat").toJSONObject());

        }

        public double getConfidence(){
            return confidence;
        }

        public Rectangle getRectangle(){
            return rect;
        }

        public void setRectangle(Rectangle rect){
            this.rect = rect;
        }

        public Mat getDetection(){
            return detection;
        }

        public Rectangle getRectangle(Mat detection){
            int x = (int) detection.get(0, 0)[0];
            int y = (int) detection.get(0, 1)[0];
            int w = (int) detection.get(0, 2)[0];
            int h = (int) detection.get(0, 3)[0];
            return new Rectangle(x,y,w,h);
        }

        public Integer getInputWidth(){
            return inputWidth;
        }

        public Integer getInputHeight(){
            return inputHeight;
        }

        public JSONObject toJson(){
            JSONObject face = new JSONObject();

            JSONObject coordinates = new JSONObject();
            JSONObject r = new JSONObject();
            r.set("x", rect.x);
            r.set("y", rect.y);
            r.set("w", rect.width);
            r.set("h", rect.height);
            coordinates.set("rect", r);
            face.set("coordinates", coordinates);

            face.set("confidence", confidence);

            JSONObject d = new JSONObject();
            face.set("detection", d);

            JSONObject input = new JSONObject();
            d.set("input", input);
            input.set("width", inputWidth);
            input.set("height", inputHeight);

            JSONObject output = new JSONObject();
            d.set("output", output);
            output.set("mat", mat2json(detection));

            return face;
        }
    }


  //**************************************************************************
  //** getMat
  //**************************************************************************
  /** Returns a Mat for an image file
   */
    public static Mat getMat(javaxt.io.File imageFile) {
        return Imgcodecs.imread(imageFile.toString());
    }


  //**************************************************************************
  //** getMat
  //**************************************************************************
  /** Returns a Mat for an image
   */
    public static Mat getMat(javaxt.io.Image image) {

      //Get buffered image
        BufferedImage bi = image.getBufferedImage();
        if (bi.getType() != BufferedImage.TYPE_INT_RGB) {
            BufferedImage img = new BufferedImage(bi.getWidth(), bi.getHeight(),
            BufferedImage.TYPE_3BYTE_BGR);
            img.getGraphics().drawImage(bi, 0, 0, null);
            bi = img;
        }


      //Get bytes
        byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();


      //Create mat
        Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, data);
        return mat;
    }



  //**************************************************************************
  //** compareFaces
  //**************************************************************************
  /** Returns the cosine distance between two faces. Faces have same identity
   *  if the cosine distance is greater than or equal to 0.363
   */
    public static Double compareFaces(
        Mat face1, Mat image1,
        Mat face2, Mat image2,
        Object model) throws Exception {

        if (model==null) throw new Exception();
        FaceRecognizerSF faceRecognizerSF = (FaceRecognizerSF) model;


        Mat alignedF1 = new Mat();
        Mat alignedF2 = new Mat();
        faceRecognizerSF.alignCrop(image1, face1, alignedF1);
        faceRecognizerSF.alignCrop(image2, face2, alignedF2);
        Mat feature1 = new Mat();
        Mat feature2 = new Mat();
        faceRecognizerSF.feature(alignedF1, feature1);
        feature1 = feature1.clone();
        faceRecognizerSF.feature(alignedF2, feature2);
        feature2 = feature2.clone();
        return faceRecognizerSF.match(feature1, feature2, FaceRecognizerSF.FR_COSINE);
    }

    public static Double compareFaces(
        Face face1, javaxt.io.Image image1,
        Face face2, javaxt.io.Image image2,
        Object model) throws Exception {

        return compareFaces(
            face1.getDetection(), getMat(image1),
            face2.getDetection(), getMat(image2),
            model
        );
    }



  //**************************************************************************
  //** mat2Json
  //**************************************************************************
  /** Used to serialize a Mat to a JSONObject
   */
    public static JSONObject mat2json(Mat mat){
        int type = mat.type();


        JSONObject json = new JSONObject();
        json.set("type", type);
        json.set("rows", mat.rows());
        json.set("cols", mat.cols());


        if (type==0 || type==1 || type==8 || type==9 || type==16 || type==17 || type==24 || type==25){
            byte[] bytes = new byte[(int)(mat.total()*mat.elemSize())];
            mat.get(0,0,bytes);
            json.set("bytes", bytes);
        }
        else if (type==2 || type==3 || type==10 || type==11 || type==18 ||  type==19 || type==26 || type==27){
            short[] shorts = new short[(int)(mat.total()*mat.elemSize())];
            mat.get(0,0,shorts);
            json.set("shorts", shorts);
        }
        else if (type==4 || type==12|| type==20 || type==28){
            int[] ints = new int[(int)(mat.total()*mat.elemSize())];
            mat.get(0,0,ints);
            json.set("ints", ints);
        }
        else if (type==5 || type==13 || type==21 || type==29) {
            float[] floats = new float[(int)(mat.total()*mat.elemSize())];
            mat.get(0,0,floats);
            json.set("floats", floats);
        }
        else if (type==6 || type==14 || type==22 || type==30){
            double[] doubles = new double[(int)(mat.total()*mat.elemSize())];
            mat.get(0,0,doubles);
            json.set("doubles", doubles);
        }


        return json;
    }


  //**************************************************************************
  //** mat2Json
  //**************************************************************************
  /** Used to deserialize a JSONObject to a Mat
   */
    public static Mat json2mat(JSONObject json){

        int type = json.get("type").toInteger();
        int rows = json.get("rows").toInteger();
        int cols = json.get("cols").toInteger();

        Mat mat = new Mat(rows, cols, type);

        if (type==0 || type==1 || type==8 || type==9 || type==16 || type==17 || type==24 || type==25){
            byte[] bytes = (byte[]) json.get("bytes").toObject();
            mat.put(0, 0, bytes);
        }
        else if (type==2 || type==3 || type==10 || type==11 || type==18 ||  type==19 || type==26 || type==27){
            JSONArray arr = json.get("shorts").toJSONArray();
            short[] shorts = new short[arr.length()];
            for (int i=0; i<shorts.length; i++){
                shorts[i] = arr.get(i).toShort();
            }
            mat.put(0,0,shorts);
        }
        else if (type==4 || type==12 || type==20 || type==28){
            JSONArray arr = json.get("ints").toJSONArray();
            int[] ints = new int[arr.length()];
            for (int i=0; i<ints.length; i++){
                ints[i] = arr.get(i).toInteger();
            }
            mat.put(0,0,ints);
        }
        else if (type==5 || type==13 || type==21 || type==29) {
            JSONArray arr = json.get("floats").toJSONArray();
            float[] floats = new float[arr.length()];
            for (int i=0; i<floats.length; i++){
                floats[i] = arr.get(i).toFloat();
            }
            mat.put(0,0,floats);
        }
        else if (type==6 || type==14 || type==22 || type==30){
            JSONArray arr = json.get("doubles").toJSONArray();
            double[] doubles = new double[arr.length()];
            for (int i=0; i<doubles.length; i++){
                doubles[i] = arr.get(i).toDouble();
            }
            mat.put(0,0,doubles);
        }

        return mat;
    }


  //**************************************************************************
  //** loadLib
  //**************************************************************************
  /** Used to extract and load a library (.dll, .so, etc) from the opencv
   *  jar file.
   */
    private static synchronized boolean loadLib() {

        if (dllLoaded!=null) return dllLoaded;
        try{

          //Set relative path to the library
            OS os = OS.getCurrent();
            Arch arch = Arch.getCurrent();
            Jar jar = new Jar(nu.pattern.OpenCV.class);
            String path = "nu/pattern/opencv/" +
            (os.toString().toLowerCase()) + "/";
            String a = arch.toString();
            if (a.startsWith("X")) a = a.toLowerCase();
            path += a + "/";


          //Get library file name
            String name;
            if (os==OS.WINDOWS){
                name = "opencv_java";
            }
            else{
                name = "libopencv_java";
            }


          //Get library file extension
            String ext = null;
            if (os==OS.LINUX){
                ext = "so";
            }
            else if (os==OS.OSX){
                ext = "dylib";
            }
            else if (os==OS.WINDOWS){
                ext = "dll";
            }


          //Find library entry in the zip/jar file
            ZipEntry entry = null;
            String fileName = null;
            try (ZipInputStream in = new ZipInputStream(new FileInputStream(jar.getFile()))){

                ZipEntry zipEntry;
                while((zipEntry = in.getNextEntry())!=null){
                    String relPath = zipEntry.getName();
                    if (relPath.startsWith(path + name)){
                        if (relPath.endsWith(ext)){
                            entry = zipEntry;
                            path = relPath;
                            fileName = relPath.substring(relPath.lastIndexOf("/")+1);
                            break;
                        }
                    }
                }
            }
            if (entry==null) throw new Exception("Failed to find library");


          //Extract library as needed
            java.io.File lib = new java.io.File(jar.getFile().getParentFile(), fileName);
            long checksum = entry.getCrc();
            if (lib.exists()){

              //Check whether the library equals the jar entry. Extract as needed.
                byte[] b = new byte[(int)lib.length()];

                try (java.io.DataInputStream is = new java.io.DataInputStream(new FileInputStream(lib))) {

                    is.readFully(b, 0, b.length);

                    java.util.zip.CRC32 crc = new java.util.zip.CRC32();
                    crc.update(b);
                    if (checksum!=crc.getValue()){
                        lib.delete();
                        extractEntry(path, jar, lib);
                    }
                }

            }
            else{

              //File does not exist so extract the library
                extractEntry(path, jar, lib);
            }


          //Load the library
            if (lib.exists()){
                System.load(lib.toString());
                //console.log("Loaded!", Core.getVersionMajor(), Core.getVersionMinor());
                dllLoaded = true;
            }
            else{
                throw new Exception("Failed to load library");
            }

        }
        catch(Exception e){
            dllLoaded = false;
        }

        return dllLoaded;
    }


  //**************************************************************************
  //** extractEntry
  //**************************************************************************
  /** Used to extract an entry for a jar file
   */
    private static void extractEntry(String path, javaxt.io.Jar jar,
        java.io.File destination) throws Exception {

        destination.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(destination)){
            try (ZipInputStream in = new ZipInputStream(new FileInputStream(jar.getFile()))){
                ZipEntry zipEntry;
                while((zipEntry = in.getNextEntry())!=null){
                    if (zipEntry.getName().equals(path)){

                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                        break;
                    }
                }
            }
        }
    }


  //**************************************************************************
  //** OS
  //**************************************************************************
    static enum OS {
      OSX("^[Mm]ac OS X$"),
      LINUX("^[Ll]inux$"),
      WINDOWS("^[Ww]indows.*");

      private final Set<Pattern> patterns;

      private OS(final String... patterns) {
        this.patterns = new HashSet<>();

        for (final String pattern : patterns) {
          this.patterns.add(Pattern.compile(pattern));
        }
      }

      private boolean is(final String id) {
        for (final Pattern pattern : patterns) {
          if (pattern.matcher(id).matches()) {
            return true;
          }
        }
        return false;
      }

      public static OS getCurrent() {
        final String osName = System.getProperty("os.name");

        for (final OS os : OS.values()) {
          if (os.is(osName)) {
            //logger.log(Level.FINEST, "Current environment matches operating system descriptor \"{0}\".", os);
            return os;
          }
        }

        throw new UnsupportedOperationException(String.format("Operating system \"%s\" is not supported.", osName));
      }
    }


  //**************************************************************************
  //** Arch
  //**************************************************************************
    static enum Arch {
      X86_32("i386", "i686", "x86"),
      X86_64("amd64", "x86_64"),
      ARMv7("arm"),
      ARMv8("aarch64", "arm64");

      private final Set<String> patterns;

      private Arch(final String... patterns) {
        this.patterns = new HashSet<String>(Arrays.asList(patterns));
      }

      private boolean is(final String id) {
        return patterns.contains(id);
      }

      public static Arch getCurrent() {
        final String osArch = System.getProperty("os.arch");

        for (final Arch arch : Arch.values()) {
          if (arch.is(osArch)) {
            //logger.log(Level.FINEST, "Current environment matches architecture descriptor \"{0}\".", arch);
            return arch;
          }
        }

        throw new UnsupportedOperationException(String.format("Architecture \"%s\" is not supported.", osArch));
      }
    }

}