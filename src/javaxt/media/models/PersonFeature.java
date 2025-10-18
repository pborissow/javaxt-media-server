package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;


//******************************************************************************
//**  PersonFeature Class
//******************************************************************************
/**
 *   Used to represent a PersonFeature
 *
 ******************************************************************************/

public class PersonFeature extends javaxt.sql.Model {

    private Feature feature;
    private Person person;
    private JSONObject info;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public PersonFeature(){
        super("person_feature", java.util.Map.ofEntries(
            
            java.util.Map.entry("feature", "feature_id"),
            java.util.Map.entry("person", "person_id"),
            java.util.Map.entry("info", "info")

        ));
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public PersonFeature(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  PersonFeature.
   */
    public PersonFeature(JSONObject json){
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
            Long featureID = getValue(rs, "feature_id").toLong();
            Long personID = getValue(rs, "person_id").toLong();
            this.info = new JSONObject(getValue(rs, "info").toString());



          //Set feature
            if (featureID!=null) feature = new Feature(featureID);


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
  /** Used to update attributes with attributes from another PersonFeature.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        if (json.has("feature")){
            feature = new Feature(json.get("feature").toJSONObject());
        }
        else if (json.has("featureID")){
            try{
                feature = new Feature(json.get("featureID").toLong());
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
    }


    public Feature getFeature(){
        return feature;
    }

    public void setFeature(Feature feature){
        this.feature = feature;
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
    
    


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Used to find a PersonFeature using a given set of constraints. Example:
   *  PersonFeature obj = PersonFeature.get("feature_id=", feature_id);
   */
    public static PersonFeature get(Object...args) throws SQLException {
        Object obj = _get(PersonFeature.class, args);
        return obj==null ? null : (PersonFeature) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find PersonFeatures using a given set of constraints.
   */
    public static PersonFeature[] find(Object...args) throws SQLException {
        Object[] obj = _find(PersonFeature.class, args);
        PersonFeature[] arr = new PersonFeature[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (PersonFeature) obj[i];
        }
        return arr;
    }
}