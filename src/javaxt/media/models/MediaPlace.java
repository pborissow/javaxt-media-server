package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;
import java.util.ArrayList;

//******************************************************************************
//**  MediaPlace Class
//******************************************************************************
/**
 *   Used to represent a MediaPlace
 *
 ******************************************************************************/

public class MediaPlace extends javaxt.sql.Model {

    private MediaItem item;
    private Place place;
    private JSONObject info;
    private ArrayList<Feature> features;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public MediaPlace(){
        super("media_place", java.util.Map.ofEntries(
            
            java.util.Map.entry("item", "item_id"),
            java.util.Map.entry("place", "place_id"),
            java.util.Map.entry("info", "info"),
            java.util.Map.entry("features", "features")

        ));
        features = new ArrayList<Feature>();
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public MediaPlace(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  MediaPlace.
   */
    public MediaPlace(JSONObject json){
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
            Long placeID = getValue(rs, "place_id").toLong();
            this.info = new JSONObject(getValue(rs, "info").toString());


            try (javaxt.sql.Connection conn = getConnection(this.getClass())) {


              //Set features
                for (javaxt.sql.Record record : conn.getRecords(
                    "select feature_id from media_place_feature where media_place_id="+id)){
                    features.add(new Feature(record.get(0).toLong()));
                }
            }



          //Set item
            if (itemID!=null) item = new MediaItem(itemID);


          //Set place
            if (placeID!=null) place = new Place(placeID);

        }
        catch(Exception e){
            if (e instanceof SQLException) throw (SQLException) e;
            else throw new SQLException(e.getMessage());
        }
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to update attributes with attributes from another MediaPlace.
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
        if (json.has("place")){
            place = new Place(json.get("place").toJSONObject());
        }
        else if (json.has("placeID")){
            try{
                place = new Place(json.get("placeID").toLong());
            }
            catch(Exception e){}
        }
        this.info = json.get("info").toJSONObject();

      //Set features
        if (json.has("features")){
            for (JSONValue _features : json.get("features").toJSONArray()){
                features.add(new Feature(_features.toJSONObject()));
            }
        }
    }


    public MediaItem getItem(){
        return item;
    }

    public void setItem(MediaItem item){
        this.item = item;
    }

    public Place getPlace(){
        return place;
    }

    public void setPlace(Place place){
        this.place = place;
    }

    public JSONObject getInfo(){
        return info;
    }

    public void setInfo(JSONObject info){
        this.info = info;
    }

    public Feature[] getFeatures(){
        return features.toArray(new Feature[features.size()]);
    }

    public void setFeatures(Feature[] arr){
        features = new ArrayList<Feature>();
        for (int i=0; i<arr.length; i++){
            features.add(arr[i]);
        }
    }

    public void addFeature(Feature feature){
        this.features.add(feature);
    }
    
  //**************************************************************************
  //** save
  //**************************************************************************
  /** Used to save a MediaPlace in the database.
   */
    public void save() throws SQLException {

      //Update record in the media_place table
        super.save();


      //Save models
        try (javaxt.sql.Connection conn = getConnection(this.getClass())) {
            String target;
            
          //Save features
            ArrayList<Long> featureIDs = new ArrayList<>();
            for (Feature obj : this.features){
                obj.save();
                featureIDs.add(obj.getID());
            }


          //Link features to this MediaPlace
            target = "media_place_feature where media_place_id=" + this.id;
            conn.execute("delete from " + target);
            try (javaxt.sql.Recordset rs = conn.getRecordset("select * from " + target, false)){
                for (long featureID : featureIDs){
                    rs.addNew();
                    rs.setValue("media_place_id", this.id);
                    rs.setValue("feature_id", featureID);
                    rs.update();
                }
            }
        }
    }

    


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Used to find a MediaPlace using a given set of constraints. Example:
   *  MediaPlace obj = MediaPlace.get("item_id=", item_id);
   */
    public static MediaPlace get(Object...args) throws SQLException {
        Object obj = _get(MediaPlace.class, args);
        return obj==null ? null : (MediaPlace) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find MediaPlaces using a given set of constraints.
   */
    public static MediaPlace[] find(Object...args) throws SQLException {
        Object[] obj = _find(MediaPlace.class, args);
        MediaPlace[] arr = new MediaPlace[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (MediaPlace) obj[i];
        }
        return arr;
    }
}