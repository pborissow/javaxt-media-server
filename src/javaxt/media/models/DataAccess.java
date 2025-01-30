package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;


//******************************************************************************
//**  DataAccess Class
//******************************************************************************
/**
 *   Used to represent a DataAccess
 *
 ******************************************************************************/

public class DataAccess extends javaxt.sql.Model {

    private Data dataset;
    private UserGroup group;
    private Boolean readOnly;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public DataAccess(){
        super("data_access", java.util.Map.ofEntries(
            
            java.util.Map.entry("dataset", "dataset_id"),
            java.util.Map.entry("group", "group_id"),
            java.util.Map.entry("readOnly", "read_only")

        ));
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public DataAccess(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  DataAccess.
   */
    public DataAccess(JSONObject json){
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
            Long datasetID = getValue(rs, "dataset_id").toLong();
            Long groupID = getValue(rs, "group_id").toLong();
            this.readOnly = getValue(rs, "read_only").toBoolean();



          //Set dataset
            if (datasetID!=null) dataset = new Data(datasetID);


          //Set group
            if (groupID!=null) group = new UserGroup(groupID);

        }
        catch(Exception e){
            if (e instanceof SQLException) throw (SQLException) e;
            else throw new SQLException(e.getMessage());
        }
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to update attributes with attributes from another DataAccess.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        if (json.has("dataset")){
            dataset = new Data(json.get("dataset").toJSONObject());
        }
        else if (json.has("datasetID")){
            try{
                dataset = new Data(json.get("datasetID").toLong());
            }
            catch(Exception e){}
        }
        if (json.has("group")){
            group = new UserGroup(json.get("group").toJSONObject());
        }
        else if (json.has("groupID")){
            try{
                group = new UserGroup(json.get("groupID").toLong());
            }
            catch(Exception e){}
        }
        this.readOnly = json.get("readOnly").toBoolean();
    }


    public Data getDataset(){
        return dataset;
    }

    public void setDataset(Data dataset){
        this.dataset = dataset;
    }

    public UserGroup getGroup(){
        return group;
    }

    public void setGroup(UserGroup group){
        this.group = group;
    }

    public Boolean getReadOnly(){
        return readOnly;
    }

    public void setReadOnly(Boolean readOnly){
        this.readOnly = readOnly;
    }
    
    


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Used to find a DataAccess using a given set of constraints. Example:
   *  DataAccess obj = DataAccess.get("dataset_id=", dataset_id);
   */
    public static DataAccess get(Object...args) throws SQLException {
        Object obj = _get(DataAccess.class, args);
        return obj==null ? null : (DataAccess) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find DataAccesss using a given set of constraints.
   */
    public static DataAccess[] find(Object...args) throws SQLException {
        Object[] obj = _find(DataAccess.class, args);
        DataAccess[] arr = new DataAccess[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (DataAccess) obj[i];
        }
        return arr;
    }
}