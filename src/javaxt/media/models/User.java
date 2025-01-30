package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;


//******************************************************************************
//**  User Class
//******************************************************************************
/**
 *   Used to represent a User
 *
 ******************************************************************************/

public class User extends javaxt.sql.Model 
    implements java.security.Principal, javaxt.express.User {

    private Person person;
    private Boolean active;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public User(){
        super("user", java.util.Map.ofEntries(
            
            java.util.Map.entry("person", "person_id"),
            java.util.Map.entry("active", "active")

        ));
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public User(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  User.
   */
    public User(JSONObject json){
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
            Long personID = getValue(rs, "person_id").toLong();
            this.active = getValue(rs, "active").toBoolean();



          //Set person
            if (personID!=null) person = new Person(personID);

        }
        catch(Exception e){
            if (e instanceof SQLException) throw (SQLException) e;
            else throw new SQLException(e.getMessage());
        }
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to update attributes with attributes from another User.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        if (json.has("person")){
            person = new Person(json.get("person").toJSONObject());
        }
        else if (json.has("personID")){
            try{
                person = new Person(json.get("personID").toLong());
            }
            catch(Exception e){}
        }
        this.active = json.get("active").toBoolean();
    }


    public String getName(){
        return null;
    }

    public Person getPerson(){
        return person;
    }

    public void setPerson(Person person){
        this.person = person;
    }

    public Boolean getActive(){
        return active;
    }

    public void setActive(Boolean active){
        this.active = active;
    }
    
    


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Used to find a User using a given set of constraints. Example:
   *  User obj = User.get("person_id=", person_id);
   */
    public static User get(Object...args) throws SQLException {
        Object obj = _get(User.class, args);
        return obj==null ? null : (User) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find Users using a given set of constraints.
   */
    public static User[] find(Object...args) throws SQLException {
        Object[] obj = _find(User.class, args);
        User[] arr = new User[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (User) obj[i];
        }
        return arr;
    }
}