package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;

//******************************************************************************
//**  Place Class
//******************************************************************************
/**
 *   Used to represent a Place
 *
 ******************************************************************************/

public class Place extends javaxt.sql.Model {

    private Geometry location;
    private JSONObject info;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Place(){
        super("place", java.util.Map.ofEntries(
            
            java.util.Map.entry("location", "location"),
            java.util.Map.entry("info", "info")

        ));
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public Place(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  Place.
   */
    public Place(JSONObject json){
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
            try{this.location = new WKTReader().read(getValue(rs, "location").toString());}catch(Exception e){}
            this.info = new JSONObject(getValue(rs, "info").toString());


        }
        catch(Exception e){
            if (e instanceof SQLException) throw (SQLException) e;
            else throw new SQLException(e.getMessage());
        }
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to update attributes with attributes from another Place.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        try {
            this.location = new WKTReader().read(json.get("location").toString());
        }
        catch(Exception e) {}
        this.info = json.get("info").toJSONObject();
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
    
    


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Used to find a Place using a given set of constraints. Example:
   *  Place obj = Place.get("location=", location);
   */
    public static Place get(Object...args) throws SQLException {
        Object obj = _get(Place.class, args);
        return obj==null ? null : (Place) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find Places using a given set of constraints.
   */
    public static Place[] find(Object...args) throws SQLException {
        Object[] obj = _find(Place.class, args);
        Place[] arr = new Place[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (Place) obj[i];
        }
        return arr;
    }
}