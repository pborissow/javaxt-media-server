package javaxt.media.utils;


public class FileUtils {


  //**************************************************************************
  //** escape
  //**************************************************************************
  /** Used to update white spaces in a file path. Returns a path suitable for
   *  executing command line apps.
   */
    public static String escape(javaxt.io.File file){
        String input = file.toString();
        if (input.contains(" ")){
            boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
            if (isWindows){
                input = "\"" + input + "\"";
            }
            else{
                input = input.replace(" ", "\\ ");
            }
        }
        return input;
    }


  //**************************************************************************
  //** getTempFile
  //**************************************************************************
  /** Returns a temporary file (file name + path). The temp file name consists
   *  of a quasi-unique file name. The file path and extension are the same
   *  as the given file.
   */
    public static javaxt.io.File getTempFile(javaxt.io.File file){
        String fileName = "temp_" + javaxt.express.utils.DateUtils.getCurrentTime();
        return new javaxt.io.File(file.getDirectory(), fileName + "." + file.getExtension());
    }
}