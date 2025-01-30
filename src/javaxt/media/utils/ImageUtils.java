package javaxt.media.utils;

import java.util.*;
import javaxt.json.JSONObject;


public class ImageUtils {


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
   */
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
}