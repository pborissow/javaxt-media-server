package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;


//******************************************************************************
//**  Path Class
//******************************************************************************
/**
 *   Used to represent a Path
 *
 ******************************************************************************/

public class Path extends javaxt.sql.Model {

    private String dir;
    private Host host;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Path(){
        super("path", java.util.Map.ofEntries(
            
            java.util.Map.entry("dir", "dir"),
            java.util.Map.entry("host", "host_id")

        ));
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public Path(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  Path.
   */
    public Path(JSONObject json){
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
            this.dir = getValue(rs, "dir").toString();
            Long hostID = getValue(rs, "host_id").toLong();



          //Set host
            if (hostID!=null) host = new Host(hostID);

        }
        catch(Exception e){
            if (e instanceof SQLException) throw (SQLException) e;
            else throw new SQLException(e.getMessage());
        }
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to update attributes with attributes from another Path.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        this.dir = json.get("dir").toString();
        if (json.has("host")){
            host = new Host(json.get("host").toJSONObject());
        }
        else if (json.has("hostID")){
            try{
                host = new Host(json.get("hostID").toLong());
            }
            catch(Exception e){}
        }
    }


    public String getDir(){
        return dir;
    }

    public void setDir(String dir){
        this.dir = dir;
    }

    public Host getHost(){
        return host;
    }

    public void setHost(Host host){
        this.host = host;
    }
    
    


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Used to find a Path using a given set of constraints. Example:
   *  Path obj = Path.get("dir=", dir);
   */
    public static Path get(Object...args) throws SQLException {
        Object obj = _get(Path.class, args);
        return obj==null ? null : (Path) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find Paths using a given set of constraints.
   */
    public static Path[] find(Object...args) throws SQLException {
        Object[] obj = _find(Path.class, args);
        Path[] arr = new Path[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (Path) obj[i];
        }
        return arr;
    }
}