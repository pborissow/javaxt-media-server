package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;


//******************************************************************************
//**  MediaMatch Class
//******************************************************************************
/**
 *   Used to represent a MediaMatch
 *
 ******************************************************************************/

public class MediaMatch extends javaxt.sql.Model {

    private MediaItem mediaItem;
    private MediaItem matchingItem;
    private JSONObject matchInfo;
    private Boolean ignoreMatch;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public MediaMatch(){
        super("media_match", java.util.Map.ofEntries(
            
            java.util.Map.entry("mediaItem", "media_item_id"),
            java.util.Map.entry("matchingItem", "matching_item_id"),
            java.util.Map.entry("matchInfo", "match_info"),
            java.util.Map.entry("ignoreMatch", "ignore_match")

        ));
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public MediaMatch(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  MediaMatch.
   */
    public MediaMatch(JSONObject json){
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
            Long mediaItemID = getValue(rs, "media_item_id").toLong();
            Long matchingItemID = getValue(rs, "matching_item_id").toLong();
            this.matchInfo = new JSONObject(getValue(rs, "match_info").toString());
            this.ignoreMatch = getValue(rs, "ignore_match").toBoolean();



          //Set mediaItem
            if (mediaItemID!=null) mediaItem = new MediaItem(mediaItemID);


          //Set matchingItem
            if (matchingItemID!=null) matchingItem = new MediaItem(matchingItemID);

        }
        catch(Exception e){
            if (e instanceof SQLException) throw (SQLException) e;
            else throw new SQLException(e.getMessage());
        }
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to update attributes with attributes from another MediaMatch.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        if (json.has("mediaItem")){
            mediaItem = new MediaItem(json.get("mediaItem").toJSONObject());
        }
        else if (json.has("mediaItemID")){
            try{
                mediaItem = new MediaItem(json.get("mediaItemID").toLong());
            }
            catch(Exception e){}
        }
        if (json.has("matchingItem")){
            matchingItem = new MediaItem(json.get("matchingItem").toJSONObject());
        }
        else if (json.has("matchingItemID")){
            try{
                matchingItem = new MediaItem(json.get("matchingItemID").toLong());
            }
            catch(Exception e){}
        }
        this.matchInfo = json.get("matchInfo").toJSONObject();
        this.ignoreMatch = json.get("ignoreMatch").toBoolean();
    }


    public MediaItem getMediaItem(){
        return mediaItem;
    }

    public void setMediaItem(MediaItem mediaItem){
        this.mediaItem = mediaItem;
    }

    public MediaItem getMatchingItem(){
        return matchingItem;
    }

    public void setMatchingItem(MediaItem matchingItem){
        this.matchingItem = matchingItem;
    }

    public JSONObject getMatchInfo(){
        return matchInfo;
    }

    public void setMatchInfo(JSONObject matchInfo){
        this.matchInfo = matchInfo;
    }

    public Boolean getIgnoreMatch(){
        return ignoreMatch;
    }

    public void setIgnoreMatch(Boolean ignoreMatch){
        this.ignoreMatch = ignoreMatch;
    }
    
    


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Used to find a MediaMatch using a given set of constraints. Example:
   *  MediaMatch obj = MediaMatch.get("media_item_id=", media_item_id);
   */
    public static MediaMatch get(Object...args) throws SQLException {
        Object obj = _get(MediaMatch.class, args);
        return obj==null ? null : (MediaMatch) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find MediaMatchs using a given set of constraints.
   */
    public static MediaMatch[] find(Object...args) throws SQLException {
        Object[] obj = _find(MediaMatch.class, args);
        MediaMatch[] arr = new MediaMatch[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (MediaMatch) obj[i];
        }
        return arr;
    }
}