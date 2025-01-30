package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;


//******************************************************************************
//**  UserAuthentication Class
//******************************************************************************
/**
 *   Used to represent a UserAuthentication
 *
 ******************************************************************************/

public class UserAuthentication extends javaxt.sql.Model {

    private User user;
    private String service;
    private String key;
    private String value;
    private JSONObject info;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public UserAuthentication(){
        super("user_authentication", java.util.Map.ofEntries(
            
            java.util.Map.entry("user", "user_id"),
            java.util.Map.entry("service", "service"),
            java.util.Map.entry("key", "key"),
            java.util.Map.entry("value", "value"),
            java.util.Map.entry("info", "info")

        ));
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public UserAuthentication(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  UserAuthentication.
   */
    public UserAuthentication(JSONObject json){
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
            Long userID = getValue(rs, "user_id").toLong();
            this.service = getValue(rs, "service").toString();
            this.key = getValue(rs, "key").toString();
            this.value = getValue(rs, "value").toString();
            this.info = new JSONObject(getValue(rs, "info").toString());



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
  /** Used to update attributes with attributes from another UserAuthentication.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        if (json.has("user")){
            user = new User(json.get("user").toJSONObject());
        }
        else if (json.has("userID")){
            try{
                user = new User(json.get("userID").toLong());
            }
            catch(Exception e){}
        }
        this.service = json.get("service").toString();
        this.key = json.get("key").toString();
        this.value = json.get("value").toString();
        this.info = json.get("info").toJSONObject();
    }


    public User getUser(){
        return user;
    }

    public void setUser(User user){
        this.user = user;
    }

    public String getService(){
        return service;
    }

    public void setService(String service){
        this.service = service;
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

    public JSONObject getInfo(){
        return info;
    }

    public void setInfo(JSONObject info){
        this.info = info;
    }
    
    


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Used to find a UserAuthentication using a given set of constraints. Example:
   *  UserAuthentication obj = UserAuthentication.get("user_id=", user_id);
   */
    public static UserAuthentication get(Object...args) throws SQLException {
        Object obj = _get(UserAuthentication.class, args);
        return obj==null ? null : (UserAuthentication) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find UserAuthentications using a given set of constraints.
   */
    public static UserAuthentication[] find(Object...args) throws SQLException {
        Object[] obj = _find(UserAuthentication.class, args);
        UserAuthentication[] arr = new UserAuthentication[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (UserAuthentication) obj[i];
        }
        return arr;
    }
}