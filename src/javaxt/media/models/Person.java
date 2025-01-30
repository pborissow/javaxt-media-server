package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;


//******************************************************************************
//**  Person Class
//******************************************************************************
/**
 *   Used to represent a Person
 *
 ******************************************************************************/

public class Person extends javaxt.sql.Model {

    private String gender;
    private Integer birthday;
    private JSONObject info;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Person(){
        super("person", java.util.Map.ofEntries(
            
            java.util.Map.entry("gender", "gender"),
            java.util.Map.entry("birthday", "birthday"),
            java.util.Map.entry("info", "info")

        ));
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public Person(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  Person.
   */
    public Person(JSONObject json){
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
            this.gender = getValue(rs, "gender").toString();
            this.birthday = getValue(rs, "birthday").toInteger();
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
  /** Used to update attributes with attributes from another Person.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        this.gender = json.get("gender").toString();
        this.birthday = json.get("birthday").toInteger();
        this.info = json.get("info").toJSONObject();
    }


    public String getGender(){
        return gender;
    }

    public void setGender(String gender){
        this.gender = gender;
    }

    public Integer getBirthday(){
        return birthday;
    }

    public void setBirthday(Integer birthday){
        this.birthday = birthday;
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
  /** Used to find a Person using a given set of constraints. Example:
   *  Person obj = Person.get("gender=", gender);
   */
    public static Person get(Object...args) throws SQLException {
        Object obj = _get(Person.class, args);
        return obj==null ? null : (Person) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find Persons using a given set of constraints.
   */
    public static Person[] find(Object...args) throws SQLException {
        Object[] obj = _find(Person.class, args);
        Person[] arr = new Person[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (Person) obj[i];
        }
        return arr;
    }
}