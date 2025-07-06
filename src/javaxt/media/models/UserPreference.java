package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;


//******************************************************************************
//**  UserPreference Class
//******************************************************************************
/**
 *   Used to represent a UserPreference
 *
 ******************************************************************************/

public class UserPreference extends javaxt.sql.Model {

    private User user;
    private String key;
    private String value;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public UserPreference(){
        super("user_preference", java.util.Map.ofEntries(
            
            java.util.Map.entry("user", "user_id"),
            java.util.Map.entry("key", "key"),
            java.util.Map.entry("value", "value")

        ));
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public UserPreference(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  UserPreference.
   */
    public UserPreference(JSONObject json){
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
            this.key = getValue(rs, "key").toString();
            this.value = getValue(rs, "value").toString();



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
  /** Used to update attributes with attributes from another UserPreference.
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
        this.key = json.get("key").toString();
        this.value = json.get("value").toString();
    }


    public User getUser(){
        return user;
    }

    public void setUser(User user){
        this.user = user;
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
  /** Used to find a UserPreference using a given set of constraints. Example:
   *  UserPreference obj = UserPreference.get("user_id=", user_id);
   */
    public static UserPreference get(Object...args) throws SQLException {
        Object obj = _get(UserPreference.class, args);
        return obj==null ? null : (UserPreference) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find UserPreferences using a given set of constraints.
   */
    public static UserPreference[] find(Object...args) throws SQLException {
        Object[] obj = _find(UserPreference.class, args);
        UserPreference[] arr = new UserPreference[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (UserPreference) obj[i];
        }
        return arr;
    }
}