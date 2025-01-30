package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;


//******************************************************************************
//**  MediaAccess Class
//******************************************************************************
/**
 *   Used to represent a MediaAccess
 *
 ******************************************************************************/

public class MediaAccess extends javaxt.sql.Model {

    private MediaItem item;
    private UserGroup group;
    private Boolean readOnly;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public MediaAccess(){
        super("media_access", java.util.Map.ofEntries(
            
            java.util.Map.entry("item", "item_id"),
            java.util.Map.entry("group", "group_id"),
            java.util.Map.entry("readOnly", "read_only")

        ));
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public MediaAccess(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  MediaAccess.
   */
    public MediaAccess(JSONObject json){
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
            Long groupID = getValue(rs, "group_id").toLong();
            this.readOnly = getValue(rs, "read_only").toBoolean();



          //Set item
            if (itemID!=null) item = new MediaItem(itemID);


          //Set group
            if (groupID!=null) group = new UserGroup(groupID);

        }
        catch(Exception e){
            if (e instanceof SQLException) throw (SQLException) e;
            else throw new SQLException(e.getMessage());
        }
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to update attributes with attributes from another MediaAccess.
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
        if (json.has("group")){
            group = new UserGroup(json.get("group").toJSONObject());
        }
        else if (json.has("groupID")){
            try{
                group = new UserGroup(json.get("groupID").toLong());
            }
            catch(Exception e){}
        }
        this.readOnly = json.get("readOnly").toBoolean();
    }


    public MediaItem getItem(){
        return item;
    }

    public void setItem(MediaItem item){
        this.item = item;
    }

    public UserGroup getGroup(){
        return group;
    }

    public void setGroup(UserGroup group){
        this.group = group;
    }

    public Boolean getReadOnly(){
        return readOnly;
    }

    public void setReadOnly(Boolean readOnly){
        this.readOnly = readOnly;
    }
    
    


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Used to find a MediaAccess using a given set of constraints. Example:
   *  MediaAccess obj = MediaAccess.get("item_id=", item_id);
   */
    public static MediaAccess get(Object...args) throws SQLException {
        Object obj = _get(MediaAccess.class, args);
        return obj==null ? null : (MediaAccess) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find MediaAccesss using a given set of constraints.
   */
    public static MediaAccess[] find(Object...args) throws SQLException {
        Object[] obj = _find(MediaAccess.class, args);
        MediaAccess[] arr = new MediaAccess[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (MediaAccess) obj[i];
        }
        return arr;
    }
}