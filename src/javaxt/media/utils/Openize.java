package javaxt.media.utils;

import java.awt.image.BufferedImage;

import openize.io.IOMode;
import openize.io.IOFileStream;
import openize.heic.decoder.HeicImage;
import openize.heic.decoder.PixelFormat;


//******************************************************************************
//**  Openize
//******************************************************************************
/**
 *   Used to extract metadata and generate images from using the Openize.Heic
 *   library. This class is used as a fallback when ImageMagick is not
 *   available.
 *
 ******************************************************************************/

public class Openize {


  //**************************************************************************
  //** createJPEG
  //**************************************************************************
  /** Used to create a jpeg image for a given file
   */
    public static javaxt.io.File createJPEG(javaxt.io.File file){

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
    public static void convert(javaxt.io.File input, javaxt.io.File output) {
        if (!input.exists() || output.exists()) return;


      //Set temp file name/path
        var tempFile = FileUtils.getTempFile(output);
        if (tempFile.exists()){
            var result = tempFile.delete();
            if (result==false) throw new RuntimeException("Failed to delete temp file: " + tempFile);
        }


      //Create temp file
        try (IOFileStream fs = new IOFileStream(input.toFile(), IOMode.READ)){
            HeicImage heic = HeicImage.load(fs);

            int[] pixels = heic.getInt32Array(PixelFormat.Argb32);
            int width = (int) heic.getWidth();
            int height = (int) heic.getHeight();


            BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            bi.setRGB(0, 0, width, height, pixels, 0, width);

            javaxt.io.Image image = new javaxt.io.Image(bi);
            image.saveAs(tempFile.toFile());
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

}