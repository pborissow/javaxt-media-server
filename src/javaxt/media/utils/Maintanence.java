package javaxt.media.utils;
import java.util.*;
import static javaxt.utils.Console.console;

public class Maintanence {

    public static void deleteThumbnails(javaxt.io.Directory directory){

        var dirs = new ArrayList<javaxt.io.Directory>();

        List files = directory.getChildren(true, null, false);
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
                if (obj instanceof javaxt.io.Directory){
                    javaxt.io.Directory dir = (javaxt.io.Directory) obj;
                    if (dir.getName().equals(".thumbnails")){
                        dirs.add(dir);
                    }

                }
            }
        }


        for (var dir : dirs){
            if (dir.exists()) dir.delete();
            if (dir.exists()) console.log("Failed to delete " + dir);
        }

    }

}