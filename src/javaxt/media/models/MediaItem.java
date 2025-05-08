package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;
import java.util.ArrayList;
import javaxt.utils.Date;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;

//******************************************************************************
//**  MediaItem Class
//******************************************************************************
/**
 *   Used to represent a MediaItem
 *
 ******************************************************************************/

public class MediaItem extends javaxt.sql.Model {

    private String name;
    private String description;
    private String type;
    private Date startDate;
    private Date endDate;
    private String hash;
    private Geometry location;
    private JSONObject info;
    private ArrayList<File> files;
    private ArrayList<Keyword> keywords;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public MediaItem(){
        super("media_item", java.util.Map.ofEntries(
            
            java.util.Map.entry("name", "name"),
            java.util.Map.entry("description", "description"),
            java.util.Map.entry("type", "type"),
            java.util.Map.entry("startDate", "start_date"),
            java.util.Map.entry("endDate", "end_date"),
            java.util.Map.entry("hash", "hash"),
            java.util.Map.entry("location", "location"),
            java.util.Map.entry("info", "info"),
            java.util.Map.entry("files", "files"),
            java.util.Map.entry("keywords", "keywords")

        ));
        files = new ArrayList<File>();
        keywords = new ArrayList<Keyword>();
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public MediaItem(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  MediaItem.
   */
    public MediaItem(JSONObject json){
        this();
        update(json);
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to update attributes using a record in the database.
   */
    protected void update(Object rs) throws SQLException {

        try{
            this.id = getValue(rs, "id").toLong();
            this.name = getValue(rs, "name").toString();
            this.description = getValue(rs, "description").toString();
            this.type = getValue(rs, "type").toString();
            this.startDate = getValue(rs, "start_date").toDate();
            this.endDate = getValue(rs, "end_date").toDate();
            this.hash = getValue(rs, "hash").toString();
            try{this.location = new WKTReader().read(getValue(rs, "location").toString());}catch(Exception e){}
            this.info = new JSONObject(getValue(rs, "info").toString());


            try (javaxt.sql.Connection conn = getConnection(this.getClass())) {


              //Set files
                for (javaxt.sql.Record record : conn.getRecords(
                    "select file_id from media_item_file where media_item_id="+id)){
                    files.add(new File(record.get(0).toLong()));
                }


              //Set keywords
                for (javaxt.sql.Record record : conn.getRecords(
                    "select keyword_id from media_item_keyword where media_item_id="+id)){
                    keywords.add(new Keyword(record.get(0).toLong()));
                }
            }


        }
        catch(Exception e){
            if (e instanceof SQLException) throw (SQLException) e;
            else throw new SQLException(e.getMessage());
        }
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to update attributes with attributes from another MediaItem.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        this.name = json.get("name").toString();
        this.description = json.get("description").toString();
        this.type = json.get("type").toString();
        this.startDate = json.get("startDate").toDate();
        this.endDate = json.get("endDate").toDate();
        this.hash = json.get("hash").toString();
        try {
            this.location = new WKTReader().read(json.get("location").toString());
        }
        catch(Exception e) {}
        this.info = json.get("info").toJSONObject();

      //Set files
        if (json.has("files")){
            for (JSONValue _files : json.get("files").toJSONArray()){
                files.add(new File(_files.toJSONObject()));
            }
        }


      //Set keywords
        if (json.has("keywords")){
            for (JSONValue _keywords : json.get("keywords").toJSONArray()){
                keywords.add(new Keyword(_keywords.toJSONObject()));
            }
        }
    }


    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getDescription(){
        return description;
    }

    public void setDescription(String description){
        this.description = description;
    }

    public String getType(){
        return type;
    }

    public void setType(String type){
        this.type = type;
    }

    public Date getStartDate(){
        return startDate;
    }

    public void setStartDate(Date startDate){
        this.startDate = startDate;
    }

    public Date getEndDate(){
        return endDate;
    }

    public void setEndDate(Date endDate){
        this.endDate = endDate;
    }

    public String getHash(){
        return hash;
    }

    public void setHash(String hash){
        this.hash = hash;
    }

    public Geometry getLocation(){
        return location;
    }

    public void setLocation(Geometry location){
        this.location = location;
    }

    public JSONObject getInfo(){
        return info;
    }

    public void setInfo(JSONObject info){
        this.info = info;
    }

    public File[] getFiles(){
        return files.toArray(new File[files.size()]);
    }

    public void setFiles(File[] arr){
        files = new ArrayList<File>();
        for (int i=0; i<arr.length; i++){
            files.add(arr[i]);
        }
    }

    public void addFile(File file){
        this.files.add(file);
    }

    public Keyword[] getKeywords(){
        return keywords.toArray(new Keyword[keywords.size()]);
    }

    public void setKeywords(Keyword[] arr){
        keywords = new ArrayList<Keyword>();
        for (int i=0; i<arr.length; i++){
            keywords.add(arr[i]);
        }
    }

    public void addKeyword(Keyword keyword){
        this.keywords.add(keyword);
    }
    
  //**************************************************************************
  //** save
  //**************************************************************************
  /** Used to save a MediaItem in the database.
   */
    public void save() throws SQLException {

      //Update record in the media_item table
        super.save();


      //Save models
        try (javaxt.sql.Connection conn = getConnection(this.getClass())) {
            String target;
            
          //Save files
            ArrayList<Long> fileIDs = new ArrayList<>();
            for (File obj : this.files){
                obj.save();
                fileIDs.add(obj.getID());
            }


          //Link files to this MediaItem
            target = "media_item_file where media_item_id=" + this.id;
            conn.execute("delete from " + target);
            try (javaxt.sql.Recordset rs = conn.getRecordset("select * from " + target, false)){
                for (long fileID : fileIDs){
                    rs.addNew();
                    rs.setValue("media_item_id", this.id);
                    rs.setValue("file_id", fileID);
                    rs.update();
                }
            }

          //Save keywords
            ArrayList<Long> keywordIDs = new ArrayList<>();
            for (Keyword obj : this.keywords){
                obj.save();
                keywordIDs.add(obj.getID());
            }


          //Link keywords to this MediaItem
            target = "media_item_keyword where media_item_id=" + this.id;
            conn.execute("delete from " + target);
            try (javaxt.sql.Recordset rs = conn.getRecordset("select * from " + target, false)){
                for (long keywordID : keywordIDs){
                    rs.addNew();
                    rs.setValue("media_item_id", this.id);
                    rs.setValue("keyword_id", keywordID);
                    rs.update();
                }
            }
        }
    }

    


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Used to find a MediaItem using a given set of constraints. Example:
   *  MediaItem obj = MediaItem.get("name=", name);
   */
    public static MediaItem get(Object...args) throws SQLException {
        Object obj = _get(MediaItem.class, args);
        return obj==null ? null : (MediaItem) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find MediaItems using a given set of constraints.
   */
    public static MediaItem[] find(Object...args) throws SQLException {
        Object[] obj = _find(MediaItem.class, args);
        MediaItem[] arr = new MediaItem[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (MediaItem) obj[i];
        }
        return arr;
    }
}