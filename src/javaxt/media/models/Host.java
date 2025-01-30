package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;


//******************************************************************************
//**  Host Class
//******************************************************************************
/**
 *   Used to represent a Host
 *
 ******************************************************************************/

public class Host extends javaxt.sql.Model {

    private String name;
    private String description;
    private JSONObject metadata;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Host(){
        super("host", java.util.Map.ofEntries(
            
            java.util.Map.entry("name", "name"),
            java.util.Map.entry("description", "description"),
            java.util.Map.entry("metadata", "metadata")

        ));
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public Host(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  Host.
   */
    public Host(JSONObject json){
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
            this.metadata = new JSONObject(getValue(rs, "metadata").toString());


        }
        catch(Exception e){
            if (e instanceof SQLException) throw (SQLException) e;
            else throw new SQLException(e.getMessage());
        }
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to update attributes with attributes from another Host.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        this.name = json.get("name").toString();
        this.description = json.get("description").toString();
        this.metadata = json.get("metadata").toJSONObject();
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

    public JSONObject getMetadata(){
        return metadata;
    }

    public void setMetadata(JSONObject metadata){
        this.metadata = metadata;
    }
    
    


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Used to find a Host using a given set of constraints. Example:
   *  Host obj = Host.get("name=", name);
   */
    public static Host get(Object...args) throws SQLException {
        Object obj = _get(Host.class, args);
        return obj==null ? null : (Host) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find Hosts using a given set of constraints.
   */
    public static Host[] find(Object...args) throws SQLException {
        Object[] obj = _find(Host.class, args);
        Host[] arr = new Host[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (Host) obj[i];
        }
        return arr;
    }
}