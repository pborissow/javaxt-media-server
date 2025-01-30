package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;


//******************************************************************************
//**  Keyword Class
//******************************************************************************
/**
 *   Used to represent a Keyword
 *
 ******************************************************************************/

public class Keyword extends javaxt.sql.Model {

    private String word;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Keyword(){
        super("keyword", java.util.Map.ofEntries(
            
            java.util.Map.entry("word", "word")

        ));
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public Keyword(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  Keyword.
   */
    public Keyword(JSONObject json){
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
            this.word = getValue(rs, "word").toString();


        }
        catch(Exception e){
            if (e instanceof SQLException) throw (SQLException) e;
            else throw new SQLException(e.getMessage());
        }
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to update attributes with attributes from another Keyword.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        this.word = json.get("word").toString();
    }


    public String getWord(){
        return word;
    }

    public void setWord(String word){
        this.word = word;
    }
    
    


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Used to find a Keyword using a given set of constraints. Example:
   *  Keyword obj = Keyword.get("word=", word);
   */
    public static Keyword get(Object...args) throws SQLException {
        Object obj = _get(Keyword.class, args);
        return obj==null ? null : (Keyword) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find Keywords using a given set of constraints.
   */
    public static Keyword[] find(Object...args) throws SQLException {
        Object[] obj = _find(Keyword.class, args);
        Keyword[] arr = new Keyword[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (Keyword) obj[i];
        }
        return arr;
    }
}