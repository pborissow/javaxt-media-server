package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;


//******************************************************************************
//**  Feature Class
//******************************************************************************
/**
 *   Used to represent a Feature
 *
 ******************************************************************************/

public class Feature extends javaxt.sql.Model {

    private MediaItem item;
    private JSONObject coordinates;
    private byte[] thumbnail;
    private String label;
    private JSONObject info;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Feature(){
        super("feature", java.util.Map.ofEntries(
            
            java.util.Map.entry("item", "item_id"),
            java.util.Map.entry("coordinates", "coordinates"),
            java.util.Map.entry("thumbnail", "thumbnail"),
            java.util.Map.entry("label", "label"),
            java.util.Map.entry("info", "info")

        ));
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public Feature(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  Feature.
   */
    public Feature(JSONObject json){
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
            Long itemID = getValue(rs, "item_id").toLong();
            this.coordinates = new JSONObject(getValue(rs, "coordinates").toString());
            this.thumbnail = getValue(rs, "thumbnail").toByteArray();
            this.label = getValue(rs, "label").toString();
            this.info = new JSONObject(getValue(rs, "info").toString());



          //Set item
            if (itemID!=null) item = new MediaItem(itemID);

        }
        catch(Exception e){
            if (e instanceof SQLException) throw (SQLException) e;
            else throw new SQLException(e.getMessage());
        }
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to update attributes with attributes from another Feature.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        if (json.has("item")){
            item = new MediaItem(json.get("item").toJSONObject());
        }
        else if (json.has("itemID")){
            try{
                item = new MediaItem(json.get("itemID").toLong());
            }
            catch(Exception e){}
        }
        this.coordinates = json.get("coordinates").toJSONObject();
        this.thumbnail = json.get("thumbnail").toByteArray();
        this.label = json.get("label").toString();
        this.info = json.get("info").toJSONObject();
    }


    public MediaItem getItem(){
        return item;
    }

    public void setItem(MediaItem item){
        this.item = item;
    }

    public JSONObject getCoordinates(){
        return coordinates;
    }

    public void setCoordinates(JSONObject coordinates){
        this.coordinates = coordinates;
    }

    public byte[] getThumbnail(){
        return thumbnail;
    }

    public void setThumbnail(byte[] thumbnail){
        this.thumbnail = thumbnail;
    }

    public String getLabel(){
        return label;
    }

    public void setLabel(String label){
        this.label = label;
    }

    public JSONObject getInfo(){
        return info;
    }

    public void setInfo(JSONObject info){
        this.info = info;
    }
    
    


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Used to find a Feature using a given set of constraints. Example:
   *  Feature obj = Feature.get("item_id=", item_id);
   */
    public static Feature get(Object...args) throws SQLException {
        Object obj = _get(Feature.class, args);
        return obj==null ? null : (Feature) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find Features using a given set of constraints.
   */
    public static Feature[] find(Object...args) throws SQLException {
        Object[] obj = _find(Feature.class, args);
        Feature[] arr = new Feature[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (Feature) obj[i];
        }
        return arr;
    }
}