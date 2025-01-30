package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;


//******************************************************************************
//**  Component Class
//******************************************************************************
/**
 *   Used to represent a Component
 *
 ******************************************************************************/

public class Component extends javaxt.sql.Model {

    private String key;
    private String label;
    private String description;
    private JSONObject info;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Component(){
        super("component", java.util.Map.ofEntries(
            
            java.util.Map.entry("key", "key"),
            java.util.Map.entry("label", "label"),
            java.util.Map.entry("description", "description"),
            java.util.Map.entry("info", "info")

        ));
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public Component(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  Component.
   */
    public Component(JSONObject json){
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
            this.key = getValue(rs, "key").toString();
            this.label = getValue(rs, "label").toString();
            this.description = getValue(rs, "description").toString();
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
  /** Used to update attributes with attributes from another Component.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        this.key = json.get("key").toString();
        this.label = json.get("label").toString();
        this.description = json.get("description").toString();
        this.info = json.get("info").toJSONObject();
    }


    public String getKey(){
        return key;
    }

    public void setKey(String key){
        this.key = key;
    }

    public String getLabel(){
        return label;
    }

    public void setLabel(String label){
        this.label = label;
    }

    public String getDescription(){
        return description;
    }

    public void setDescription(String description){
        this.description = description;
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
  /** Used to find a Component using a given set of constraints. Example:
   *  Component obj = Component.get("key=", key);
   */
    public static Component get(Object...args) throws SQLException {
        Object obj = _get(Component.class, args);
        return obj==null ? null : (Component) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find Components using a given set of constraints.
   */
    public static Component[] find(Object...args) throws SQLException {
        Object[] obj = _find(Component.class, args);
        Component[] arr = new Component[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (Component) obj[i];
        }
        return arr;
    }
}