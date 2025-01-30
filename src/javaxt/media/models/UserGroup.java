package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;
import java.util.ArrayList;

//******************************************************************************
//**  UserGroup Class
//******************************************************************************
/**
 *   Used to represent a UserGroup
 *
 ******************************************************************************/

public class UserGroup extends javaxt.sql.Model {

    private String name;
    private String description;
    private ArrayList<User> users;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public UserGroup(){
        super("user_group", java.util.Map.ofEntries(
            
            java.util.Map.entry("name", "name"),
            java.util.Map.entry("description", "description"),
            java.util.Map.entry("users", "users")

        ));
        users = new ArrayList<User>();
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public UserGroup(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  UserGroup.
   */
    public UserGroup(JSONObject json){
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


            try (javaxt.sql.Connection conn = getConnection(this.getClass())) {


              //Set users
                for (javaxt.sql.Record record : conn.getRecords(
                    "select user_id from user_group_user where user_group_id="+id)){
                    users.add(new User(record.get(0).toLong()));
                }
            }


        }
        catch(Exception e){
            if (e instanceof SQLException) throw (SQLException) e;
            else throw new SQLException(e.getMessage());
        }
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to update attributes with attributes from another UserGroup.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        this.name = json.get("name").toString();
        this.description = json.get("description").toString();

      //Set users
        if (json.has("users")){
            for (JSONValue _users : json.get("users").toJSONArray()){
                users.add(new User(_users.toJSONObject()));
            }
        }
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

    public User[] getUsers(){
        return users.toArray(new User[users.size()]);
    }

    public void setUsers(User[] arr){
        users = new ArrayList<User>();
        for (int i=0; i<arr.length; i++){
            users.add(arr[i]);
        }
    }

    public void addUser(User user){
        this.users.add(user);
    }
    
  //**************************************************************************
  //** save
  //**************************************************************************
  /** Used to save a UserGroup in the database.
   */
    public void save() throws SQLException {

      //Update record in the user_group table
        super.save();


      //Save models
        try (javaxt.sql.Connection conn = getConnection(this.getClass())) {
            String target;
            
          //Save users
            ArrayList<Long> userIDs = new ArrayList<>();
            for (User obj : this.users){
                obj.save();
                userIDs.add(obj.getID());
            }


          //Link users to this UserGroup
            target = "user_group_user where user_group_id=" + this.id;
            conn.execute("delete from " + target);
            try (javaxt.sql.Recordset rs = conn.getRecordset("select * from " + target, false)){
                for (long userID : userIDs){
                    rs.addNew();
                    rs.setValue("user_group_id", this.id);
                    rs.setValue("user_id", userID);
                    rs.update();
                }
            }
        }
    }

    


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Used to find a UserGroup using a given set of constraints. Example:
   *  UserGroup obj = UserGroup.get("name=", name);
   */
    public static UserGroup get(Object...args) throws SQLException {
        Object obj = _get(UserGroup.class, args);
        return obj==null ? null : (UserGroup) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find UserGroups using a given set of constraints.
   */
    public static UserGroup[] find(Object...args) throws SQLException {
        Object[] obj = _find(UserGroup.class, args);
        UserGroup[] arr = new UserGroup[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (UserGroup) obj[i];
        }
        return arr;
    }
}