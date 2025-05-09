package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;


//******************************************************************************
//**  Setting Class
//******************************************************************************
/**
 *   Used to represent a Setting
 *
 ******************************************************************************/

public class Setting extends javaxt.sql.Model {

    private String key;
    private String value;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Setting(){
        super("setting", java.util.Map.ofEntries(
            
            java.util.Map.entry("key", "key"),
            java.util.Map.entry("value", "value")

        ));
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public Setting(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  Setting.
   */
    public Setting(JSONObject json){
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
            this.value = getValue(rs, "value").toString();


        }
        catch(Exception e){
            if (e instanceof SQLException) throw (SQLException) e;
            else throw new SQLException(e.getMessage());
        }
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to update attributes with attributes from another Setting.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        this.key = json.get("key").toString();
        this.value = json.get("value").toString();
    }


    public String getKey(){
        return key;
    }

    public void setKey(String key){
        this.key = key;
    }

    public String getValue(){
        return value;
    }

    public void setValue(String value){
        this.value = value;
    }
    
    


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Used to find a Setting using a given set of constraints. Example:
   *  Setting obj = Setting.get("key=", key);
   */
    public static Setting get(Object...args) throws SQLException {
        Object obj = _get(Setting.class, args);
        return obj==null ? null : (Setting) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find Settings using a given set of constraints.
   */
    public static Setting[] find(Object...args) throws SQLException {
        Object[] obj = _find(Setting.class, args);
        Setting[] arr = new Setting[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (Setting) obj[i];
        }
        return arr;
    }
}