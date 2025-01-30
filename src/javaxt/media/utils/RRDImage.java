package javaxt.media.utils;

import java.io.*;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.channels.FileChannel;
import java.util.*;
import javaxt.io.Image;

//******************************************************************************
//**  RRDImage Class
//******************************************************************************
/**
 *   Used to generate RRDImage files and retrieve individual entries.
 *   RRDImage files contain reduced resolution copies of a source image. Users
 *   can retrieve individual image files via a unique identifier.
 *
 *   The RRDImage file structure itself is really quite simple. It consists of
 *   a header and the actual image files (stored as jpegs). The header contains
 *   an index of all the images in the file. Each entry in the index contains
 *   the following information:
 *
 *   (1) ID: a unique identifier for the image. This is typically supplied by
 *       the user.
 *   (2) WIDTH: the actual width of the image
 *   (3) HEIGHT: the height of the image
 *   (4) SIZE: the total size of the image (in bytes)
 *   (5) OFFSET: the offset (in bytes) from the beginning of the file.
 *
 *
 ******************************************************************************/

public class RRDImage {

    private java.io.File file;
    private FileChannel output;
    private byte[] header = new byte[1024];
    private long totalSize;
    private List index = new LinkedList();


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public RRDImage(java.io.File file) {
        this.file = file;
    }

    public RRDImage(javaxt.io.File file) {
        this.file = file.toFile();
    }

    public RRDImage(javaxt.media.models.File file){
        this(new javaxt.io.File(file.getPath().getDir() + file.getName() + "." + file.getExtension()));
    }

    public boolean exists(){
        return file.exists();
    }

    public boolean delete(){
        return file.delete();
    }

    public java.io.File getFile(){
        return file;
    }

  //**************************************************************************
  //** addImage
  //**************************************************************************
  /**  Used to add an image to the thumbnail file.
   */
    public void addImage(String id, Image image) throws IOException {

        if (!file.exists()) file.getParentFile().mkdirs();

      //Create new OutputStream (as needed)
        if (output==null){
            output = new FileOutputStream(file).getChannel();
            output.write(ByteBuffer.wrap(header));
            totalSize = header.length;
        }

      //Convert image to byte array
        byte[] array = image.getByteArray("jpg");

      //Insert image into the rrd file
        output.write(ByteBuffer.wrap(array));

      //Update index
        index.add(
            new Entry(id, image.getWidth(), image.getHeight(), array.length, totalSize)
        );

      //Update the total file size
        totalSize+=array.length;

    }



  //**************************************************************************
  //** Close
  //**************************************************************************
  /** Used to finalize any write operations. Call this function after you have
   *  finished adding images to the file.
   */
    public void close() throws IOException {
        if (output!=null){

          //Update Header
            output.position(0);

            for (int i=0; i<index.size(); i++){
                 Entry entry = (Entry) index.get(i);
                 output.write( ByteBuffer.wrap( getBytes(entry.toString()) ) );
            }
            output.close();
            output = null;

            //System.out.println("Total Size = " + totalSize/1024 + " kb");
        }
    }


  //**************************************************************************
  //** getImage
  //**************************************************************************
  /** Used to retrieve an image and export it to a valid output stream (e.g.
   *  FileOutputStream, ServletOutputStream, ByteArrayOutputStream, etc.).
   *  Note that the output stream will remain open after this method is called.
   */
    public void getImage(String id, OutputStream output) throws IOException {

        if (file.exists()==false) return;


        try(FileInputStream input = new FileInputStream(file)){
            input.read(header);
            List index = getIndex(header);
            //System.out.println("Total Entries = " + index.size());
            for (int i=0; i<index.size(); i++){

                 Entry entry = (Entry) index.get(i);
                 if (entry.equals(id)){
                     //System.out.println(entry.toString().trim());
                     input.skip(entry.getOffset()-header.length);
                     byte[] b = new byte[1024];
                     int x=0;
                     long t=0;
                     while((x=input.read(b,0,b.length))>-1) {
                            output.write(b,0,x);
                            t+=b.length;
                            if (t>entry.getSize()){
                                b = new byte[(int)(t-entry.getSize())];
                            }
                            else if (t==entry.getSize()){
                                break; //exit while loop
                            }
                     }
                     break; //exit for loop
                 }

            }

        }
    }


  //**************************************************************************
  //** getImage
  //**************************************************************************
  /**  Used to retrieve one of the images found in the thumbnail file.   */

    public Image getImage(String id) throws IOException {
        if (file.exists()==false) return null;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()){
            getImage(id,output);
            ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
            return new Image(input);
        }
    }


  //**************************************************************************
  //** getIndex
  //**************************************************************************
  /** Used retrieve a list of images found in the thumbnail file.
   */
    private List getIndex(byte[] header){
        List entries = new LinkedList();
        String[] row = (new StringBuffer()).append( getChars(header) ).toString().trim().split("\n");
        for (int i=0; i<row.length; i++){
             entries.add(new Entry(row[i]));
        }
        return entries;
    }


  //**************************************************************************
  //** getIndex (Public Member - do not call internally)
  //**************************************************************************
  /** Used retrieve a list of images found in the thumbnail file.
   */
    public Entry[] getIndex() throws IOException {

      //Parse header and extract index
        if (index.size()==0){
            try(FileInputStream input = new FileInputStream(file)){
                input.read(header);
                index = getIndex(header);
            }
        }

      //Convert index into an array
        Entry[] entries = new Entry[index.size()];
        for (int i=0; i<entries.length; i++){
            entries[i] = (Entry) index.get(i);
        }

      //Return array
        return entries;
    }


  //**************************************************************************
  //** rotate
  //**************************************************************************
  /**  Used to rotate the thumbnail (clockwise). Rotation angle is specified
   *   in degrees relative to the top of the image.
   */
    public void rotate(double d) throws IOException {

      //Create a new rrd image file
        RRDImage output = new RRDImage(new java.io.File(file.toString() + ".tmp"));
        if (output.exists()) output.delete();

      //Add images to the new rrd file
        Entry[] index = getIndex();
        for (int i=0; i<index.length; i++){
             String id = index[i].getID();
             Image img = getImage(id);
             img.rotate(d);
             output.addImage(id, img);
        }
        output.close();

      //Delete the original rrd file
        file.delete();

      //Rename the new rrd file
        output.getFile().renameTo(file);
    }


  //**************************************************************************
  //** rotateClockwise
  //**************************************************************************
  /**  Used to rotate the thumbnail 90 degrees. */

    public void rotateClockwise() throws IOException {
        rotate(90);
    }


  //**************************************************************************
  //** rotateCounterClockwise
  //**************************************************************************
  /**  Used to rotate the thumbnail -90 degrees. */

    public void rotateCounterClockwise() throws IOException {
        rotate(-90);
    }


    public String toString(){
        return file.toString();
    }


  //**************************************************************************
  //** Entry
  //**************************************************************************
  /**  Used to represent a single record in the thumbnail header. */

    public class Entry{

        private String id;
        private int width;
        private int height;
        private long size;
        private long offset;

        public Entry(String entry){
            String[] col = entry.trim().split(",");
            this.id = col[0];
            this.width = getInt(col[1]);
            this.height = getInt(col[2]);
            this.size = getLong(col[3]);
            this.offset = getLong(col[4]);
        }

        public Entry(String id, int width, int height, long size, long offset){
            this.id = id;
            this.width = width;
            this.height = height;
            this.size = size;
            this.offset = offset;
            //System.out.println(width + "x" + height + " (" + size + ")");
        }

        public String getID(){ return id; }
        public int getWidth(){ return width; }
        public int getHeight(){ return height; }
        public long getSize(){ return size; }
        public long getOffset(){ return offset; }

        public String toString(){
            return id + "," + width + "," + height + "," + size + "," + offset + "\n";
        }

        public boolean equals(Object obj){
            if (obj==null) return false;
            else return obj.toString().equals(id);
        }

    } //end entry


    private byte[] getBytes(String string) {
        return getBytes(string.toCharArray());
    }

    private byte[] getBytes(char[] chars) {
        Charset cs = Charset.forName("UTF-8");
        CharBuffer cb = CharBuffer.allocate (chars.length);
        cb.put(chars);
        cb.flip();
        ByteBuffer bb = cs.encode(cb);
        return bb.array();
    }

    private char[] getChars(byte[] bytes) {
        Charset cs = Charset.forName("UTF-8");
        ByteBuffer bb = ByteBuffer.allocate(bytes.length);
        bb.put(bytes);
        bb.flip();
        CharBuffer cb = cs.decode(bb);
        return cb.array();
    }

    private int getInt(String str){
        return Integer.valueOf(str).intValue();
    }

    private long getLong(String str){
        return Long.valueOf(str);
    }

}