package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;


//******************************************************************************
//**  PlaceName Class
//******************************************************************************
/**
 *   Used to represent a PlaceName
 *
 ******************************************************************************/

public class PlaceName extends javaxt.sql.Model {

    private Place place;
    private String name;
    private Boolean preferred;
    private JSONObject info;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public PlaceName(){
        super("place_name", java.util.Map.ofEntries(
            
            java.util.Map.entry("place", "place_id"),
            java.util.Map.entry("name", "name"),
            java.util.Map.entry("preferred", "preferred"),
            java.util.Map.entry("info", "info")

        ));
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public PlaceName(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  PlaceName.
   */
    public PlaceName(JSONObject json){
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
            Long placeID = getValue(rs, "place_id").toLong();
            this.name = getValue(rs, "name").toString();
            this.preferred = getValue(rs, "preferred").toBoolean();
            this.info = new JSONObject(getValue(rs, "info").toString());



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
  /** Used to update attributes with attributes from another PlaceName.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        if (json.has("place")){
            place = new Place(json.get("place").toJSONObject());
        }
        else if (json.has("placeID")){
            try{
                place = new Place(json.get("placeID").toLong());
            }
            catch(Exception e){}
        }
        this.name = json.get("name").toString();
        this.preferred = json.get("preferred").toBoolean();
        this.info = json.get("info").toJSONObject();
    }


    public Place getPlace(){
        return place;
    }

    public void setPlace(Place place){
        this.place = place;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public Boolean getPreferred(){
        return preferred;
    }

    public void setPreferred(Boolean preferred){
        this.preferred = preferred;
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
  /** Used to find a PlaceName using a given set of constraints. Example:
   *  PlaceName obj = PlaceName.get("place_id=", place_id);
   */
    public static PlaceName get(Object...args) throws SQLException {
        Object obj = _get(PlaceName.class, args);
        return obj==null ? null : (PlaceName) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find PlaceNames using a given set of constraints.
   */
    public static PlaceName[] find(Object...args) throws SQLException {
        Object[] obj = _find(PlaceName.class, args);
        PlaceName[] arr = new PlaceName[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (PlaceName) obj[i];
        }
        return arr;
    }
}