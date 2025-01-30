package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;
import javaxt.utils.Date;

//******************************************************************************
//**  MediaLog Class
//******************************************************************************
/**
 *   Used to represent a MediaLog
 *
 ******************************************************************************/

public class MediaLog extends javaxt.sql.Model {

    private MediaItem item;
    private User user;
    private Date date;
    private String action;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public MediaLog(){
        super("media_log", java.util.Map.ofEntries(
            
            java.util.Map.entry("item", "item_id"),
            java.util.Map.entry("user", "user_id"),
            java.util.Map.entry("date", "date"),
            java.util.Map.entry("action", "action")

        ));
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public MediaLog(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  MediaLog.
   */
    public MediaLog(JSONObject json){
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
            Long userID = getValue(rs, "user_id").toLong();
            this.date = getValue(rs, "date").toDate();
            this.action = getValue(rs, "action").toString();



          //Set item
            if (itemID!=null) item = new MediaItem(itemID);


          //Set user
            if (userID!=null) user = new User(userID);

        }
        catch(Exception e){
            if (e instanceof SQLException) throw (SQLException) e;
            else throw new SQLException(e.getMessage());
        }
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to update attributes with attributes from another MediaLog.
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
        if (json.has("user")){
            user = new User(json.get("user").toJSONObject());
        }
        else if (json.has("userID")){
            try{
                user = new User(json.get("userID").toLong());
            }
            catch(Exception e){}
        }
        this.date = json.get("date").toDate();
        this.action = json.get("action").toString();
    }


    public MediaItem getItem(){
        return item;
    }

    public void setItem(MediaItem item){
        this.item = item;
    }

    public User getUser(){
        return user;
    }

    public void setUser(User user){
        this.user = user;
    }

    public Date getDate(){
        return date;
    }

    public void setDate(Date date){
        this.date = date;
    }

    public String getAction(){
        return action;
    }

    public void setAction(String action){
        this.action = action;
    }
    
    


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Used to find a MediaLog using a given set of constraints. Example:
   *  MediaLog obj = MediaLog.get("item_id=", item_id);
   */
    public static MediaLog get(Object...args) throws SQLException {
        Object obj = _get(MediaLog.class, args);
        return obj==null ? null : (MediaLog) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find MediaLogs using a given set of constraints.
   */
    public static MediaLog[] find(Object...args) throws SQLException {
        Object[] obj = _find(MediaLog.class, args);
        MediaLog[] arr = new MediaLog[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (MediaLog) obj[i];
        }
        return arr;
    }
}