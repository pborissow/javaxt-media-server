package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;
import javaxt.utils.Date;

//******************************************************************************
//**  Data Class
//******************************************************************************
/**
 *   Used to represent a Data
 *
 ******************************************************************************/

public class Data extends javaxt.sql.Model {

    private String name;
    private String description;
    private Datatype type;
    private JSONObject data;
    private Date date;
    private byte[] thumbnail;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Data(){
        super("data", java.util.Map.ofEntries(
            
            java.util.Map.entry("name", "name"),
            java.util.Map.entry("description", "description"),
            java.util.Map.entry("type", "type_id"),
            java.util.Map.entry("data", "data"),
            java.util.Map.entry("date", "date"),
            java.util.Map.entry("thumbnail", "thumbnail")

        ));
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public Data(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  Data.
   */
    public Data(JSONObject json){
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
            Long typeID = getValue(rs, "type_id").toLong();
            this.data = new JSONObject(getValue(rs, "data").toString());
            this.date = getValue(rs, "date").toDate();
            this.thumbnail = getValue(rs, "thumbnail").toByteArray();



          //Set type
            if (typeID!=null) type = new Datatype(typeID);

        }
        catch(Exception e){
            if (e instanceof SQLException) throw (SQLException) e;
            else throw new SQLException(e.getMessage());
        }
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to update attributes with attributes from another Data.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        this.name = json.get("name").toString();
        this.description = json.get("description").toString();
        if (json.has("type")){
            type = new Datatype(json.get("type").toJSONObject());
        }
        else if (json.has("typeID")){
            try{
                type = new Datatype(json.get("typeID").toLong());
            }
            catch(Exception e){}
        }
        this.data = json.get("data").toJSONObject();
        this.date = json.get("date").toDate();
        this.thumbnail = json.get("thumbnail").toByteArray();
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

    public Datatype getType(){
        return type;
    }

    public void setType(Datatype type){
        this.type = type;
    }

    public JSONObject getData(){
        return data;
    }

    public void setData(JSONObject data){
        this.data = data;
    }

    public Date getDate(){
        return date;
    }

    public void setDate(Date date){
        this.date = date;
    }

    public byte[] getThumbnail(){
        return thumbnail;
    }

    public void setThumbnail(byte[] thumbnail){
        this.thumbnail = thumbnail;
    }
    
    


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Used to find a Data using a given set of constraints. Example:
   *  Data obj = Data.get("name=", name);
   */
    public static Data get(Object...args) throws SQLException {
        Object obj = _get(Data.class, args);
        return obj==null ? null : (Data) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find Datas using a given set of constraints.
   */
    public static Data[] find(Object...args) throws SQLException {
        Object[] obj = _find(Data.class, args);
        Data[] arr = new Data[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (Data) obj[i];
        }
        return arr;
    }
}