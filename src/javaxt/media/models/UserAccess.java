package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;


//******************************************************************************
//**  UserAccess Class
//******************************************************************************
/**
 *   Used to represent a UserAccess
 *
 ******************************************************************************/

public class UserAccess extends javaxt.sql.Model {

    private User user;
    private Component component;
    private Integer level;
    private JSONObject info;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public UserAccess(){
        super("user_access", java.util.Map.ofEntries(
            
            java.util.Map.entry("user", "user_id"),
            java.util.Map.entry("component", "component_id"),
            java.util.Map.entry("level", "level"),
            java.util.Map.entry("info", "info")

        ));
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public UserAccess(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  UserAccess.
   */
    public UserAccess(JSONObject json){
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
            Long componentID = getValue(rs, "component_id").toLong();
            this.level = getValue(rs, "level").toInteger();
            this.info = new JSONObject(getValue(rs, "info").toString());



          //Set user
            if (userID!=null) user = new User(userID);


          //Set component
            if (componentID!=null) component = new Component(componentID);

        }
        catch(Exception e){
            if (e instanceof SQLException) throw (SQLException) e;
            else throw new SQLException(e.getMessage());
        }
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to update attributes with attributes from another UserAccess.
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
        if (json.has("component")){
            component = new Component(json.get("component").toJSONObject());
        }
        else if (json.has("componentID")){
            try{
                component = new Component(json.get("componentID").toLong());
            }
            catch(Exception e){}
        }
        this.level = json.get("level").toInteger();
        this.info = json.get("info").toJSONObject();
    }


    public User getUser(){
        return user;
    }

    public void setUser(User user){
        this.user = user;
    }

    public Component getComponent(){
        return component;
    }

    public void setComponent(Component component){
        this.component = component;
    }

    public Integer getLevel(){
        return level;
    }

    public void setLevel(Integer level){
        this.level = level;
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
  /** Used to find a UserAccess using a given set of constraints. Example:
   *  UserAccess obj = UserAccess.get("user_id=", user_id);
   */
    public static UserAccess get(Object...args) throws SQLException {
        Object obj = _get(UserAccess.class, args);
        return obj==null ? null : (UserAccess) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find UserAccesss using a given set of constraints.
   */
    public static UserAccess[] find(Object...args) throws SQLException {
        Object[] obj = _find(UserAccess.class, args);
        UserAccess[] arr = new UserAccess[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (UserAccess) obj[i];
        }
        return arr;
    }
}