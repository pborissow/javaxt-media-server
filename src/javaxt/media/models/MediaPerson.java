package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;
import java.util.ArrayList;

//******************************************************************************
//**  MediaPerson Class
//******************************************************************************
/**
 *   Used to represent a MediaPerson
 *
 ******************************************************************************/

public class MediaPerson extends javaxt.sql.Model {

    private MediaItem item;
    private Person person;
    private JSONObject info;
    private ArrayList<Feature> features;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public MediaPerson(){
        super("media_person", java.util.Map.ofEntries(
            
            java.util.Map.entry("item", "item_id"),
            java.util.Map.entry("person", "person_id"),
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
    public MediaPerson(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  MediaPerson.
   */
    public MediaPerson(JSONObject json){
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
            Long personID = getValue(rs, "person_id").toLong();
            this.info = new JSONObject(getValue(rs, "info").toString());


            try (javaxt.sql.Connection conn = getConnection(this.getClass())) {


              //Set features
                for (javaxt.sql.Record record : conn.getRecords(
                    "select feature_id from media_person_feature where media_person_id="+id)){
                    features.add(new Feature(record.get(0).toLong()));
                }
            }



          //Set item
            if (itemID!=null) item = new MediaItem(itemID);


          //Set person
            if (personID!=null) person = new Person(personID);

        }
        catch(Exception e){
            if (e instanceof SQLException) throw (SQLException) e;
            else throw new SQLException(e.getMessage());
        }
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to update attributes with attributes from another MediaPerson.
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
        if (json.has("person")){
            person = new Person(json.get("person").toJSONObject());
        }
        else if (json.has("personID")){
            try{
                person = new Person(json.get("personID").toLong());
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

    public Person getPerson(){
        return person;
    }

    public void setPerson(Person person){
        this.person = person;
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
  /** Used to save a MediaPerson in the database.
   */
    public void save() throws SQLException {

      //Update record in the media_person table
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


          //Link features to this MediaPerson
            target = "media_person_feature where media_person_id=" + this.id;
            conn.execute("delete from " + target);
            try (javaxt.sql.Recordset rs = conn.getRecordset("select * from " + target, false)){
                for (long featureID : featureIDs){
                    rs.addNew();
                    rs.setValue("media_person_id", this.id);
                    rs.setValue("feature_id", featureID);
                    rs.update();
                }
            }
        }
    }

    


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Used to find a MediaPerson using a given set of constraints. Example:
   *  MediaPerson obj = MediaPerson.get("item_id=", item_id);
   */
    public static MediaPerson get(Object...args) throws SQLException {
        Object obj = _get(MediaPerson.class, args);
        return obj==null ? null : (MediaPerson) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find MediaPersons using a given set of constraints.
   */
    public static MediaPerson[] find(Object...args) throws SQLException {
        Object[] obj = _find(MediaPerson.class, args);
        MediaPerson[] arr = new MediaPerson[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (MediaPerson) obj[i];
        }
        return arr;
    }
}