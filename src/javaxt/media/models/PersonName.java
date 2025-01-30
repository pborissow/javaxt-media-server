package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;


//******************************************************************************
//**  PersonName Class
//******************************************************************************
/**
 *   Used to represent a PersonName
 *
 ******************************************************************************/

public class PersonName extends javaxt.sql.Model {

    private Person person;
    private String name;
    private Boolean preferred;
    private JSONObject info;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public PersonName(){
        super("person_name", java.util.Map.ofEntries(
            
            java.util.Map.entry("person", "person_id"),
            java.util.Map.entry("name", "name"),
            java.util.Map.entry("preferred", "preferred"),
            java.util.Map.entry("info", "info")

        ));
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public PersonName(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  PersonName.
   */
    public PersonName(JSONObject json){
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
            this.name = getValue(rs, "name").toString();
            this.preferred = getValue(rs, "preferred").toBoolean();
            this.info = new JSONObject(getValue(rs, "info").toString());



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
  /** Used to update attributes with attributes from another PersonName.
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
        this.name = json.get("name").toString();
        this.preferred = json.get("preferred").toBoolean();
        this.info = json.get("info").toJSONObject();
    }


    public Person getPerson(){
        return person;
    }

    public void setPerson(Person person){
        this.person = person;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public Boolean getPreferred(){
        return preferred;
    }

    public void setPreferred(Boolean preferred){
        this.preferred = preferred;
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
  /** Used to find a PersonName using a given set of constraints. Example:
   *  PersonName obj = PersonName.get("person_id=", person_id);
   */
    public static PersonName get(Object...args) throws SQLException {
        Object obj = _get(PersonName.class, args);
        return obj==null ? null : (PersonName) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find PersonNames using a given set of constraints.
   */
    public static PersonName[] find(Object...args) throws SQLException {
        Object[] obj = _find(PersonName.class, args);
        PersonName[] arr = new PersonName[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (PersonName) obj[i];
        }
        return arr;
    }
}