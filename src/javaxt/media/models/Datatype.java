package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;


//******************************************************************************
//**  Datatype Class
//******************************************************************************
/**
 *   Used to represent a Datatype
 *
 ******************************************************************************/

public class Datatype extends javaxt.sql.Model {

    private String label;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Datatype(){
        super("datatype", java.util.Map.ofEntries(
            
            java.util.Map.entry("label", "label")

        ));
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public Datatype(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  Datatype.
   */
    public Datatype(JSONObject json){
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
            this.label = getValue(rs, "label").toString();


        }
        catch(Exception e){
            if (e instanceof SQLException) throw (SQLException) e;
            else throw new SQLException(e.getMessage());
        }
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to update attributes with attributes from another Datatype.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        this.label = json.get("label").toString();
    }


    public String getLabel(){
        return label;
    }

    public void setLabel(String label){
        this.label = label;
    }
    
    


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Used to find a Datatype using a given set of constraints. Example:
   *  Datatype obj = Datatype.get("label=", label);
   */
    public static Datatype get(Object...args) throws SQLException {
        Object obj = _get(Datatype.class, args);
        return obj==null ? null : (Datatype) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find Datatypes using a given set of constraints.
   */
    public static Datatype[] find(Object...args) throws SQLException {
        Object[] obj = _find(Datatype.class, args);
        Datatype[] arr = new Datatype[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (Datatype) obj[i];
        }
        return arr;
    }
}