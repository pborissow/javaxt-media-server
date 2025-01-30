package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;


//******************************************************************************
//**  FolderEntry Class
//******************************************************************************
/**
 *   Used to represent a FolderEntry
 *
 ******************************************************************************/

public class FolderEntry extends javaxt.sql.Model {

    private Folder folder;
    private MediaItem item;
    private Long index;
    private JSONObject info;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public FolderEntry(){
        super("folder_entry", java.util.Map.ofEntries(
            
            java.util.Map.entry("folder", "folder_id"),
            java.util.Map.entry("item", "item_id"),
            java.util.Map.entry("index", "index"),
            java.util.Map.entry("info", "info")

        ));
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public FolderEntry(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  FolderEntry.
   */
    public FolderEntry(JSONObject json){
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
            Long folderID = getValue(rs, "folder_id").toLong();
            Long itemID = getValue(rs, "item_id").toLong();
            this.index = getValue(rs, "index").toLong();
            this.info = new JSONObject(getValue(rs, "info").toString());



          //Set folder
            if (folderID!=null) folder = new Folder(folderID);


          //Set item
            if (itemID!=null) item = new MediaItem(itemID);

        }
        catch(Exception e){
            if (e instanceof SQLException) throw (SQLException) e;
            else throw new SQLException(e.getMessage());
        }
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to update attributes with attributes from another FolderEntry.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        if (json.has("folder")){
            folder = new Folder(json.get("folder").toJSONObject());
        }
        else if (json.has("folderID")){
            try{
                folder = new Folder(json.get("folderID").toLong());
            }
            catch(Exception e){}
        }
        if (json.has("item")){
            item = new MediaItem(json.get("item").toJSONObject());
        }
        else if (json.has("itemID")){
            try{
                item = new MediaItem(json.get("itemID").toLong());
            }
            catch(Exception e){}
        }
        this.index = json.get("index").toLong();
        this.info = json.get("info").toJSONObject();
    }


    public Folder getFolder(){
        return folder;
    }

    public void setFolder(Folder folder){
        this.folder = folder;
    }

    public MediaItem getItem(){
        return item;
    }

    public void setItem(MediaItem item){
        this.item = item;
    }

    public Long getIndex(){
        return index;
    }

    public void setIndex(Long index){
        this.index = index;
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
  /** Used to find a FolderEntry using a given set of constraints. Example:
   *  FolderEntry obj = FolderEntry.get("folder_id=", folder_id);
   */
    public static FolderEntry get(Object...args) throws SQLException {
        Object obj = _get(FolderEntry.class, args);
        return obj==null ? null : (FolderEntry) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find FolderEntrys using a given set of constraints.
   */
    public static FolderEntry[] find(Object...args) throws SQLException {
        Object[] obj = _find(FolderEntry.class, args);
        FolderEntry[] arr = new FolderEntry[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (FolderEntry) obj[i];
        }
        return arr;
    }
}