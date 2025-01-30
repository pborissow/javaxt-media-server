package javaxt.media.utils;


public class FileUtils {

  //**************************************************************************
  //** escape
  //**************************************************************************
  /** Used to replace white spaces in a file path
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

    public static javaxt.io.File getTempFile(javaxt.io.File file){
        String fileName = "temp_" + javaxt.express.utils.DateUtils.getCurrentTime();
        return new javaxt.io.File(file.getDirectory(), fileName + "." + file.getExtension());
    }
}
