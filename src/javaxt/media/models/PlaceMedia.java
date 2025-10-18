package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;


//******************************************************************************
//**  PlaceMedia Class
//******************************************************************************
/**
 *   Used to represent a PlaceMedia
 *
 ******************************************************************************/

public class PlaceMedia extends javaxt.sql.Model {

    private MediaItem media;
    private Place place;
    private JSONObject info;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public PlaceMedia(){
        super("place_media", java.util.Map.ofEntries(
            
            java.util.Map.entry("media", "media_id"),
            java.util.Map.entry("place", "place_id"),
            java.util.Map.entry("info", "info")

        ));
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public PlaceMedia(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  PlaceMedia.
   */
    public PlaceMedia(JSONObject json){
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
            Long mediaID = getValue(rs, "media_id").toLong();
            Long placeID = getValue(rs, "place_id").toLong();
            this.info = new JSONObject(getValue(rs, "info").toString());



          //Set media
            if (mediaID!=null) media = new MediaItem(mediaID);


          //Set place
            if (placeID!=null) place = new Place(placeID);

        }
        catch(Exception e){
            if (e instanceof SQLException) throw (SQLException) e;
            else throw new SQLException(e.getMessage());
        }
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to update attributes with attributes from another PlaceMedia.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        if (json.has("media")){
            media = new MediaItem(json.get("media").toJSONObject());
        }
        else if (json.has("mediaID")){
            try{
                media = new MediaItem(json.get("mediaID").toLong());
            }
            catch(Exception e){}
        }
        if (json.has("place")){
            place = new Place(json.get("place").toJSONObject());
        }
        else if (json.has("placeID")){
            try{
                place = new Place(json.get("placeID").toLong());
            }
            catch(Exception e){}
        }
        this.info = json.get("info").toJSONObject();
    }


    public MediaItem getMedia(){
        return media;
    }

    public void setMedia(MediaItem media){
        this.media = media;
    }

    public Place getPlace(){
        return place;
    }

    public void setPlace(Place place){
        this.place = place;
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
  /** Used to find a PlaceMedia using a given set of constraints. Example:
   *  PlaceMedia obj = PlaceMedia.get("media_id=", media_id);
   */
    public static PlaceMedia get(Object...args) throws SQLException {
        Object obj = _get(PlaceMedia.class, args);
        return obj==null ? null : (PlaceMedia) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find PlaceMedias using a given set of constraints.
   */
    public static PlaceMedia[] find(Object...args) throws SQLException {
        Object[] obj = _find(PlaceMedia.class, args);
        PlaceMedia[] arr = new PlaceMedia[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (PlaceMedia) obj[i];
        }
        return arr;
    }
}