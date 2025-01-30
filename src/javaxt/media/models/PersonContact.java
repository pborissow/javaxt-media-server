package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;


//******************************************************************************
//**  PersonContact Class
//******************************************************************************
/**
 *   Used to represent a PersonContact
 *
 ******************************************************************************/

public class PersonContact extends javaxt.sql.Model {

    private Person person;
    private String contact;
    private String type;
    private JSONObject info;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public PersonContact(){
        super("person_contact", java.util.Map.ofEntries(
            
            java.util.Map.entry("person", "person_id"),
            java.util.Map.entry("contact", "contact"),
            java.util.Map.entry("type", "type"),
            java.util.Map.entry("info", "info")

        ));
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public PersonContact(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  PersonContact.
   */
    public PersonContact(JSONObject json){
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
            this.contact = getValue(rs, "contact").toString();
            this.type = getValue(rs, "type").toString();
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
  /** Used to update attributes with attributes from another PersonContact.
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
        this.contact = json.get("contact").toString();
        this.type = json.get("type").toString();
        this.info = json.get("info").toJSONObject();
    }


    public Person getPerson(){
        return person;
    }

    public void setPerson(Person person){
        this.person = person;
    }

    public String getContact(){
        return contact;
    }

    public void setContact(String contact){
        this.contact = contact;
    }

    public String getType(){
        return type;
    }

    public void setType(String type){
        this.type = type;
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
  /** Used to find a PersonContact using a given set of constraints. Example:
   *  PersonContact obj = PersonContact.get("person_id=", person_id);
   */
    public static PersonContact get(Object...args) throws SQLException {
        Object obj = _get(PersonContact.class, args);
        return obj==null ? null : (PersonContact) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find PersonContacts using a given set of constraints.
   */
    public static PersonContact[] find(Object...args) throws SQLException {
        Object[] obj = _find(PersonContact.class, args);
        PersonContact[] arr = new PersonContact[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (PersonContact) obj[i];
        }
        return arr;
    }
}