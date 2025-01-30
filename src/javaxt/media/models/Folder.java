package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;


//******************************************************************************
//**  Folder Class
//******************************************************************************
/**
 *   Used to represent a Folder
 *
 ******************************************************************************/

public class Folder extends javaxt.sql.Model {

    private String name;
    private String description;
    private Folder parent;
    private JSONObject info;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Folder(){
        super("folder", java.util.Map.ofEntries(
            
            java.util.Map.entry("name", "name"),
            java.util.Map.entry("description", "description"),
            java.util.Map.entry("parent", "parent_id"),
            java.util.Map.entry("info", "info")

        ));
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public Folder(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  Folder.
   */
    public Folder(JSONObject json){
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
            Long parentID = getValue(rs, "parent_id").toLong();
            this.info = new JSONObject(getValue(rs, "info").toString());



          //Set parent
            if (parentID!=null) parent = new Folder(parentID);

        }
        catch(Exception e){
            if (e instanceof SQLException) throw (SQLException) e;
            else throw new SQLException(e.getMessage());
        }
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to update attributes with attributes from another Folder.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        this.name = json.get("name").toString();
        this.description = json.get("description").toString();
        if (json.has("parent")){
            parent = new Folder(json.get("parent").toJSONObject());
        }
        else if (json.has("parentID")){
            try{
                parent = new Folder(json.get("parentID").toLong());
            }
            catch(Exception e){}
        }
        this.info = json.get("info").toJSONObject();
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

    public Folder getParent(){
        return parent;
    }

    public void setParent(Folder parent){
        this.parent = parent;
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
  /** Used to find a Folder using a given set of constraints. Example:
   *  Folder obj = Folder.get("name=", name);
   */
    public static Folder get(Object...args) throws SQLException {
        Object obj = _get(Folder.class, args);
        return obj==null ? null : (Folder) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find Folders using a given set of constraints.
   */
    public static Folder[] find(Object...args) throws SQLException {
        Object[] obj = _find(Folder.class, args);
        Folder[] arr = new Folder[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (Folder) obj[i];
        }
        return arr;
    }
}