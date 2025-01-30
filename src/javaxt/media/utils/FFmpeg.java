package javaxt.media.utils;

import java.util.*;
import javaxt.json.JSONObject;
import static javaxt.media.utils.FileUtils.escape;

public class FFmpeg {

    private String ffmpeg;
    private String ffprobe;
    private String[] fileFilters = new String[]{
        "mov", "mts", "m4v", "mp4", "webm", "ogg"
    };


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** @param ffmpeg String representing the full path to the "ffmpeg"
   *  executable
   */
    public FFmpeg(String ffmpeg){

      //Check ffmpeg
        try{
            new javaxt.io.Shell(ffmpeg + " -version").run();
        }
        catch(Exception e){
            throw new RuntimeException("Invalid path to the \"ffmpeg\" executable");
        }
        this.ffmpeg = ffmpeg;



      //Check ffprobe
        int idx = ffmpeg.toLowerCase().lastIndexOf("ffmpeg");
        if (idx==-1) return;
        String path = "";
        if (idx>0) path = ffmpeg.substring(0, idx);
        String fileExt = ffmpeg.substring(idx+6);
        String ffprobe = null;
        try{
            try{
                ffprobe = path + "ffprobe" + fileExt;
                new javaxt.io.Shell(ffprobe + " -version").run();

            }
            catch(Exception e){
                try{
                    ffprobe = path + "ffmpeg.ffprobe" + fileExt;
                    new javaxt.io.Shell(ffprobe + " -version").run();
                }
                catch(Exception ex){
                    throw ex;
                }
            }
        }
        catch(Exception e){
            throw new RuntimeException("Invalid path to the \"ffprobe\" executable");
        }
        this.ffprobe = ffprobe;

    }


  //**************************************************************************
  //** createJPEG
  //**************************************************************************
  /** Used to create a jpeg image for a given file
   */
    public javaxt.io.File createJPEG(javaxt.io.File file){
        String fileName = file.getName(false);
        javaxt.io.File output = new javaxt.io.File(file.getDirectory(), fileName + ".JPG");
        createJPEG(file, output);
        return output;
    }

    public void createJPEG(javaxt.io.File input, javaxt.io.File output){
        if (!input.exists() || !isMovie(input) || output.exists()) return;


      //Set temp file name/path
        var tempFile = FileUtils.getTempFile(output);
        if (tempFile.exists()){
            var result = tempFile.delete();
            if (result==false) throw new RuntimeException("Failed to delete temp file: " + tempFile);
        }


      //Run FFmpeg
        String cmd = ffmpeg + " -ss 00:00:04 -i " + escape(input) + " -frames:v 1 " + escape(tempFile);
        new javaxt.io.Shell(cmd).run();


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
  //** createMP4
  //**************************************************************************
  /** Used to create a mp4 file for a given file
   */
    public void createMP4(javaxt.io.File input, javaxt.io.File output){
        if (!input.exists() || !isMovie(input) || output.exists()) return;


      //Set temp file name/path
        var tempFile = FileUtils.getTempFile(output);
        if (tempFile.exists()){
            var result = tempFile.delete();
            if (result==false) throw new RuntimeException("Failed to delete temp file: " + tempFile);
        }


        String cmd = ffmpeg + " -i " + escape(input);
        String ext = input.getExtension().toLowerCase();
        if (ext.equals("mov")){
            cmd += " -c copy -c:a aac -movflags +faststart ";
        }
        else{
            cmd += " -c:v libx264 -c:a aac -vf format=yuv420p -movflags +faststart ";
        }
        cmd += escape(tempFile);
        //javaxt.utils.Console.console.log(cmd);

        new javaxt.io.Shell(cmd).run();



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
  //** getMetadata
  //**************************************************************************
  /** Returns metadata for a given file
   */
    public JSONObject getMetadata(javaxt.io.File file){
        if (!isMovie(file)) return null;


        String cmd = ffprobe + " -i " + escape(file) + " -hide_banner -show_streams";
        javaxt.io.Shell exe = new javaxt.io.Shell(cmd);
        exe.run();


        var streams = new ArrayList<TreeMap<String, javaxt.utils.Value>>();
        TreeMap<String, javaxt.utils.Value> currMap = null;
        Iterator<String> it = exe.getOutput().iterator();
        while (it.hasNext()){
            String row = it.next();
            if (row==null) continue;
            row = row.trim();
            if (row.equals("[STREAM]")){
                currMap = new TreeMap<>();
                streams.add(currMap);
            }
            else{
                var idx = row.indexOf("=");
                if (idx>-1){
                    var key = row.substring(0, idx).trim();
                    var val = row.substring(idx+1).trim();
                    if (!val.isEmpty()){
                        currMap.put(key, new javaxt.utils.Value(val));
                    }
                }
            }
        }

        JSONObject metadata = new JSONObject();
        for (var map : streams){

            var key = "codec_type";
            if (map.containsKey(key)){
                var type = map.get(key).toString();
            }

            key = "TAG:creation_time";
            if (map.containsKey(key)){
                var date = map.get(key).toDate();
                if (date!=null) metadata.set("date", date);
            }

            key = "width";
            if (map.containsKey(key)){
                var width = map.get(key).toInteger();
                if (width!=null) metadata.set("width", width);
            }

            key = "height";
            if (map.containsKey(key)){
                var height = map.get(key).toInteger();
                if (height!=null) metadata.set("height", height);
            }

            key = "duration";
            if (map.containsKey(key)){
                var duration = map.get(key).toDouble();
                if (duration!=null) metadata.set("duration", duration);
            }

            key = "avg_frame_rate";
            if (map.containsKey(key)){
                var frameRate = map.get(key).toString();
                var idx = frameRate.indexOf("/");
                if (idx>-1){
                    try{
                        var a = Double.parseDouble(frameRate.substring(0, idx).trim());
                        var b = Double.parseDouble(frameRate.substring(idx+1).trim());
                        if (b>0) metadata.set("frameRate", a/b);
                    }
                    catch(Exception e){}
                }
            }

        }
        return metadata;

    }


  //**************************************************************************
  //** getDuration
  //**************************************************************************
  /** Returns the duration of a movie, in seconds
   */
    public Double getDuration(javaxt.io.File file){
        if (!isMovie(file)) return null;


        String cmd = ffprobe + " -i " + escape(file) + " -show_entries format=duration -v quiet -of csv=\"p=0\"";
        javaxt.io.Shell exe = new javaxt.io.Shell(cmd);
        exe.run();


        try{
            return Double.parseDouble(exe.getOutput().getFirst().toString());
        }
        catch(Exception e){
            javaxt.utils.Console.console.log("Failed to extract duration for " + file);
            return null;
        }

    }


  //**************************************************************************
  //** isMovie
  //**************************************************************************
  /** Returns true if the given file's extension matches a known movie format
   */
    public boolean isMovie(javaxt.io.File file){
        if (file==null || !file.exists()) return false;

        String fileExt = file.getExtension().toLowerCase();
        for (String ext : fileFilters){
            if (fileExt.equals(ext)) return true;
        }
        return false;
    }


  //**************************************************************************
  //** getSupportedFileExtensions
  //**************************************************************************
  /** Returns a list of known file extensions
   */
    public String[] getSupportedFileExtensions(){
        return fileFilters;
    }


}