package javaxt.media.utils;

import java.util.*;
import javaxt.json.JSONObject;

//******************************************************************************
//**  ImageUtils
//******************************************************************************
/**
 *   Used to extract metadata and generate images from visual media (photos
 *   and videos). Leverages FFmpeg and ImageMagick when available.
 *
 ******************************************************************************/

public class ImageUtils {

    private ImageMagick magick;
    private FFmpeg ffmpeg;
    private javaxt.io.File faceDetecionModel;
    private ImageUtils me = this;
    private String[] fileExtensions = new String[]{
        "jpg", "jpeg", "jpe", "jfif", //jpeg varients
        "png", "webp" //other image formats
    };


  //**************************************************************************
  //** Image Class
  //**************************************************************************
    public class Image extends javaxt.io.Image {
        private javaxt.io.File source;
        private boolean derived;
        private JSONObject metadata;
        private Image(javaxt.io.Image image, javaxt.io.File source, boolean derived){
            super(image.getBufferedImage());
            this.source = source;
            this.derived = derived;
        }
        public javaxt.io.File getSource(){
            return source;
        }
        public JSONObject getMetadata(){
            if (metadata==null){
                if (derived) metadata = me.getMetadata(source);
                else metadata = me.getMetadata((javaxt.io.Image) this);
            }
            return metadata;
        }
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public ImageUtils(Object... args){
        for (Object arg : args){
            if (arg instanceof ImageMagick) addImageMagick((ImageMagick) arg);
            if (arg instanceof FFmpeg) addFFmpeg((FFmpeg) arg);
        }
    }


  //**************************************************************************
  //** addImageMagick
  //**************************************************************************
    public void addImageMagick(ImageMagick magick){
        this.magick = magick;

        if (magick!=null){
            var newFilter = new LinkedHashSet<>();
            Collections.addAll(newFilter, fileExtensions);
            newFilter.add("heic");
            fileExtensions = newFilter.toArray(new String[newFilter.size()]);
        }
    }


  //**************************************************************************
  //** addFFmpeg
  //**************************************************************************
    public void addFFmpeg(FFmpeg ffmpeg){
        this.ffmpeg = ffmpeg;

        if (ffmpeg!=null){
            var newFilter = new LinkedHashSet<>();
            Collections.addAll(newFilter, fileExtensions);
            Collections.addAll(newFilter, ffmpeg.getSupportedFileExtensions());
            fileExtensions = newFilter.toArray(new String[newFilter.size()]);
        }
    }


  //**************************************************************************
  //** addFaceDetecionModel
  //**************************************************************************
    public void addFaceDetecionModel(javaxt.io.File faceDetecionModel){
        this.faceDetecionModel = faceDetecionModel;
    }


  //**************************************************************************
  //** getFaceDetecionModel
  //**************************************************************************
    public javaxt.io.File getFaceDetecionModel(){
        return faceDetecionModel;
    }


  //**************************************************************************
  //** getSupportedFileExtensions
  //**************************************************************************
  /** Returns a list of known file extensions
   */
    public String[] getSupportedFileExtensions(){
        return fileExtensions;
    }


  //**************************************************************************
  //** isMovie
  //**************************************************************************
  /** Returns true if the file is a supported video/movie.
   */
    public boolean isMovie(javaxt.io.File file){
        if (ffmpeg!=null) return ffmpeg.isMovie(file);
        return false;
    }


  //**************************************************************************
  //** getDuration
  //**************************************************************************
  /** Returns the length of a movie, in seconds. Returns null if the file is
   *  invalid or if the length cannot be determined.
   */
    public Double getDuration(javaxt.io.File file){
        if (isMovie(file)) return ffmpeg.getDuration(file);
        return null;
    }


  //**************************************************************************
  //** createMP4
  //**************************************************************************
    public void createMP4(javaxt.io.File input, javaxt.io.File output){
        if (ffmpeg!=null) ffmpeg.createMP4(input, output);
    }


  //**************************************************************************
  //** getOrCreateImage
  //**************************************************************************
  /** Returns an image for a given file. Returns null if the file is invalid
   *  or if an image cannot be extracted.
   *  @param primaryFile Media file (image or video)
   */
    public Image getImage(javaxt.io.File primaryFile){
        if (primaryFile==null || !primaryFile.exists()) return null;

        javaxt.io.Image image = primaryFile.getImage();
        if (image.getBufferedImage()==null){ //unsupported image format (e.g. HEIF, video, etc)


          //Set path to JPEG for the primaryFile
            javaxt.io.File jpeg = new javaxt.io.File(
                primaryFile.getDirectory(),
                primaryFile.getName(false) + ".jpg"
            );


          //Create javaxt.io.Image from JPEG
            if (jpeg.exists()){
                image = jpeg.getImage();
            }
            else{

              //Create temp JPEG
                if (isMovie(primaryFile)){
                    ffmpeg.createJPEG(primaryFile, jpeg);
                }
                else{
                    if (magick!=null) magick.convert(primaryFile, jpeg);
                }

                image = jpeg.getImage();
                jpeg.delete();
            }


            if (image==null || image.getBufferedImage()==null) return null;
            else return new Image(image, jpeg, true);
        }
        else{

            /*
            Image img = new Image(image, primaryFile, false);
            Integer orientation = img.getMetadata().get("orientation").toInteger();
            if (orientation!=null) rotate(image, orientation);
            return img;
            */

            image.rotate();
            return new Image(image, primaryFile, false);
        }
    }


  //**************************************************************************
  //** getMetadata
  //**************************************************************************
  /** Used to extract metadata from a file using ImageMagick or FFmpeg
   */
    private JSONObject getMetadata(javaxt.io.File file){
        JSONObject metadata = null;

        if (isMovie(file)){
            metadata = ffmpeg.getMetadata(file);
        }
        else{

            if (magick!=null){
                metadata = getMetadata(file, magick);


              //If metadata is empty, check whether there's a sidecar JPEG
              //file (e.g. older ipads)
                if (metadata.isEmpty()){
                    javaxt.io.File f = new javaxt.io.File(
                        file.getDirectory(),
                        file.getName(false) + ".JPG"
                    );
                    if (f.exists()) metadata = getMetadata(f, magick);
                }

            }

        }

        if (metadata==null) metadata = new JSONObject();

        return metadata;
    }


  //**************************************************************************
  //** getMetadata
  //**************************************************************************
  /** Used to extract metadata from a javaxt.io.Image file
   */
    public static JSONObject getMetadata(javaxt.io.Image image){
        JSONObject metadata = new JSONObject();

        try{
            metadata.set("width", image.getWidth());
            metadata.set("height", image.getHeight());
        }
        catch(Exception e){}


      //Parse iptc
        try{
            java.util.HashMap<Integer, Object> iptc = image.getIptcTags();
            metadata.set("date", new javaxt.utils.Date(iptc.get(0x0237).toString()));
        }
        catch(Exception e){}


      //Parse exif
        try{
            java.util.HashMap<Integer, Object> exif = image.getExifTags();


          //Parse date
            javaxt.utils.Date date = null;
            try{
                Object a = exif.get(0x9003);
                Object b = exif.get(0x9011);
                if (a!=null){
                    if (b==null){
                        date = new javaxt.utils.Date(a.toString());
                    }
                    else{
                        date = new javaxt.utils.Date(a + " " + b);
                    }
                }
                else{
                    date = new javaxt.utils.Date(exif.get(0x0132).toString());
                }
            }
            catch(Exception e){}

            metadata.set("date", date);
            metadata.set("orientation", exif.get(0x0112));
            metadata.set("cameraMake", exif.get(0x010f));
            metadata.set("cameraModel", exif.get(0x0110));
            metadata.set("focalLength", getFraction(exif.get(0x920A).toString()));
            metadata.set("fStop", getFraction(exif.get(0x829D).toString()));
            metadata.set("shutterSpeed", getFraction(exif.get(0x9201).toString()));


            double[] coordinate = image.getGPSCoordinate(); //returns double[]{lon, lat};
            if (coordinate!=null) metadata.set("location",
            "POINT(" + coordinate[0] + " " + coordinate[1] + ")");

            metadata.set("datum", image.getGPSDatum());

            HashMap<Integer, Object> gpsTags = image.getGpsTags();
            metadata.set("azimuth", getFraction(gpsTags.get(0x0011).toString()));
        }
        catch(Exception e){}

        return metadata;
    }


  //**************************************************************************
  //** getMetadata
  //**************************************************************************
  /** Used to extract metadata from a file using ImageMagick
   */
    public static JSONObject getMetadata(javaxt.io.File file, ImageMagick magick){
        JSONObject metadata = new JSONObject();


      //Add dimensions
        try{
            int[] size = magick.getSize(file);
            metadata.set("width", size[0]);
            metadata.set("height", size[1]);
        }
        catch(Exception e){}


      //Add exif metadata
        try{
            Map<String, String> exif = magick.getExif(file);


          //Parse date
            try{
                Object a = exif.get("DateTimeOriginal");
                Object b = exif.get("OffsetTimeOriginal");
                if (a!=null){
                    if (b==null){
                        metadata.set("date", new javaxt.utils.Date(a.toString()));
                    }
                    else{
                        metadata.set("date", new javaxt.utils.Date(a + " " + b));
                    }
                }
            }
            catch(Exception e){}


            try{
                metadata.set("orientation", Integer.parseInt(exif.get("Orientation"))); //YCbCrPositioning
            }
            catch(Exception e){
            }

            metadata.set("cameraMake", exif.get("Make"));
            metadata.set("cameraModel", exif.get("Model"));
            metadata.set("focalLength", getFraction(exif.get("FocalLength")));
            metadata.set("fStop", getFraction(exif.get("FNumber")));
            metadata.set("shutterSpeed", getFraction(exif.get("ShutterSpeedValue")));


          //Parse coordinates
            try{
                Double lat = getCoordinate(exif.get("GPSLatitude"));
                Double lon = getCoordinate(exif.get("GPSLongitude"));
                String latRef = exif.get("GPSLatitudeRef"); //N
                String lonRef = exif.get("GPSLongitudeRef"); //W

                if (!latRef.equalsIgnoreCase("N")) lat = -lat;
                if (!lonRef.equalsIgnoreCase("E")) lon = -lon;

                metadata.set("location", "POINT(" + lon + " " + lat + ")");
                metadata.set("datum", exif.get("GPSMapDatum"));
                metadata.set("azimuth", getFraction(exif.get("GPSImgDirection")));
            }
            catch(Exception e){
                //e.printStackTrace();
            }

        }
        catch(Exception e){}

        return metadata;
    }


  //**************************************************************************
  //** getFraction
  //**************************************************************************
    private static Double getFraction(String str){
        try{
            String[] arr = str.trim().split("/");
            return Double.parseDouble(arr[0])/Double.parseDouble(arr[1]);
        }
        catch(Exception e){
            return null;
        }
    }


  //**************************************************************************
  //** getCoordinate
  //**************************************************************************
  /** Used to rotate an image based on the EXIF Orientation tag. Code copied
   *  from javaxt.io.Image class.
   */
    private static double getCoordinate(String RationalArray) {

        //num + "/" + den
        String[] arr = RationalArray.split(",");
        String[] deg = arr[0].trim().split("/");
        String[] min = arr[1].trim().split("/");
        String[] sec = arr[2].trim().split("/");

        double degNumerator = Double.parseDouble(deg[0]);
        double degDenominator = 1D; try{degDenominator = Double.parseDouble(deg[1]);} catch(Exception e){}
        double minNumerator = Double.parseDouble(min[0]);
        double minDenominator = 1D; try{minDenominator = Double.parseDouble(min[1]);} catch(Exception e){}
        double secNumerator = Double.parseDouble(sec[0]);
        double secDenominator = 1D; try{secDenominator = Double.parseDouble(sec[1]);} catch(Exception e){}

        double m = 0;
        if (degDenominator != 0 || degNumerator != 0){
            m = (degNumerator / degDenominator);
        }

        if (minDenominator != 0 || minNumerator != 0){
            m += (minNumerator / minDenominator) / 60D;
        }

        if (secDenominator != 0 || secNumerator != 0){
            m += (secNumerator / secDenominator / 3600D);
        }

        return m;
    }


  //**************************************************************************
  //** rotate
  //**************************************************************************
  /** Used to rotate an image based on the EXIF Orientation tag. Code copied
   *  from javaxt.io.Image class.
    public static void rotate(javaxt.io.Image img, Integer orientation){
        try {

            switch (orientation) {
                case 1: return; //"Top, left side (Horizontal / normal)"
                case 2: img.flip(); break; //"Top, right side (Mirror horizontal)";
                case 3: img.rotate(180); break; //"Bottom, right side (Rotate 180)";
                case 4: {img.flip(); img.rotate(180);} break; //"Bottom, left side (Mirror vertical)";
                case 5: {img.flip(); img.rotate(270);} break; //"Left side, top (Mirror horizontal and rotate 270 CW)";
                case 6: img.rotate(90); break; //"Right side, top (Rotate 90 CW)";
                case 7: {img.flip(); img.rotate(90);} break; //"Right side, bottom (Mirror horizontal and rotate 90 CW)";
                case 8: img.rotate(270); break; //"Left side, bottom (Rotate 270 CW)";
            }
        }
        catch(Exception e){
            //Failed to parse exif orientation.
        }
    }
    */
}