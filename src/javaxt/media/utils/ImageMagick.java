package javaxt.media.utils;

import java.util.*;
import static javaxt.media.utils.FileUtils.escape;


public class ImageMagick {

    private String magick;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** @param magick String representing the full path to the "magick"
   *  executable
   */
    public ImageMagick(String magick){
        try{
            new javaxt.io.Shell(magick + " -version").run();
        }
        catch(Exception e){
            throw new RuntimeException("Invalid path to the \"magick\" executable");
        }
        this.magick = magick;
    }


  //**************************************************************************
  //** createJPEG
  //**************************************************************************
  /** Used to create a jpeg image for a given file
   */
    public javaxt.io.File createJPEG(javaxt.io.File file){

        String fileExt = file.getExtension().toLowerCase();
        if (fileExt.equals("jpg") || fileExt.equals("jpeg")) return file;

        String fileName = file.getName(false);
        javaxt.io.File output = new javaxt.io.File(file.getDirectory(), fileName + ".jpg");
        convert(file, output);
        return output;
    }


  //**************************************************************************
  //** convert
  //**************************************************************************
  /** Used to convert an input image to another format
   */
    public void convert(javaxt.io.File input, javaxt.io.File output){
        if (!input.exists() || output.exists()) return;


      //Set temp file name/path
        var tempFile = FileUtils.getTempFile(output);
        if (tempFile.exists()){
            var result = tempFile.delete();
            if (result==false) throw new RuntimeException("Failed to delete temp file: " + tempFile);
        }


      //Run ImageMagick
        String cmd = magick + " " + escape(input) + " -quality 100% -orient undefined " + escape(tempFile);
        new javaxt.io.Shell(cmd).run();


      //Sometimes ImageMagick creates multiple images and appends IDs to the
      //end of the file names. We need to find the biggest file and delete the rest.
        if (!tempFile.exists()){

          //Find files
            var files = new TreeMap<Long, javaxt.io.File>();
            String filter = tempFile.getName(false) + "-*." + tempFile.getExtension();
            for (javaxt.io.File file : tempFile.getDirectory().getFiles(filter)){
                files.put(file.getSize(), file);
            }

          //Get biggest file and delete the rest
            if (!files.isEmpty()){
                String tempFileName = tempFile.getName();
                tempFile = files.remove(files.lastKey());
                tempFile.rename(tempFileName);

                for (long key : files.keySet()){
                    files.get(key).delete();
                }
            }
        }


      //Rename temp file
        if (tempFile.exists()){
            tempFile.rename(output.getName());
            if (!output.exists()){
                tempFile.delete();
                throw new RuntimeException("Failed to rename temp file: " + tempFile);
            }
        }
    }


  //**************************************************************************
  //** getSize
  //**************************************************************************
  /** Returns width and height of an image. Uses the "-ping" option to
   *  efficiently get the dimensions without reading the entire file.
   */
    public int[] getSize(javaxt.io.File file){

      //Run command
        String cmd = magick + " identify -ping -format %wx%h " + escape(file);
        javaxt.io.Shell exe = new javaxt.io.Shell(cmd);
        exe.run();

      //Parse output
        Iterator<String> it = exe.getOutput().iterator();
        while (it.hasNext()){
            String row = it.next();
            if (row==null) continue;
            row = row.trim();
            if (row.contains("x")){
                String[] arr = row.split("x");
                return new int[]{Integer.parseInt(arr[0]), Integer.parseInt(arr[1])};
            }
        }
        return null;
    }


  //**************************************************************************
  //** getExif
  //**************************************************************************
  /** Returns EXIF metadata from a given file
   */
    public TreeMap<String, String> getExif(javaxt.io.File file){


      //Run command
        String cmd = magick + " identify -format '%[EXIF:*]' " + escape(file);
        javaxt.io.Shell exe = new javaxt.io.Shell(cmd);
        exe.run();


      //Parse output
        TreeMap<String, String> exif = new TreeMap<>();
        Iterator<String> it = exe.getOutput().iterator();
        while (it.hasNext()){
            String row = it.next();
            if (row==null) continue;
            if (row.startsWith("exif:")){
                row = row.substring(5);
                int idx = row.indexOf("=");
                if (idx>-1){
                    String key = row.substring(0, idx);
                    String val = row.substring(idx+1);
                    exif.put(key, val);
                }
            }
        }

        return exif;
    }


}