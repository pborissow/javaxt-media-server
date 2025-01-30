package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;
import javaxt.utils.Date;

//******************************************************************************
//**  UserRating Class
//******************************************************************************
/**
 *   Used to represent a UserRating
 *
 ******************************************************************************/

public class UserRating extends javaxt.sql.Model {

    private User user;
    private MediaItem item;
    private Date date;
    private Integer rating;
    private String comment;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public UserRating(){
        super("user_rating", java.util.Map.ofEntries(
            
            java.util.Map.entry("user", "user_id"),
            java.util.Map.entry("item", "item_id"),
            java.util.Map.entry("date", "date"),
            java.util.Map.entry("rating", "rating"),
            java.util.Map.entry("comment", "comment")

        ));
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public UserRating(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  UserRating.
   */
    public UserRating(JSONObject json){
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
            Long userID = getValue(rs, "user_id").toLong();
            Long itemID = getValue(rs, "item_id").toLong();
            this.date = getValue(rs, "date").toDate();
            this.rating = getValue(rs, "rating").toInteger();
            this.comment = getValue(rs, "comment").toString();



          //Set user
            if (userID!=null) user = new User(userID);


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
  /** Used to update attributes with attributes from another UserRating.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        if (json.has("user")){
            user = new User(json.get("user").toJSONObject());
        }
        else if (json.has("userID")){
            try{
                user = new User(json.get("userID").toLong());
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
        this.date = json.get("date").toDate();
        this.rating = json.get("rating").toInteger();
        this.comment = json.get("comment").toString();
    }


    public User getUser(){
        return user;
    }

    public void setUser(User user){
        this.user = user;
    }

    public MediaItem getItem(){
        return item;
    }

    public void setItem(MediaItem item){
        this.item = item;
    }

    public Date getDate(){
        return date;
    }

    public void setDate(Date date){
        this.date = date;
    }

    public Integer getRating(){
        return rating;
    }

    public void setRating(Integer rating){
        this.rating = rating;
    }

    public String getComment(){
        return comment;
    }

    public void setComment(String comment){
        this.comment = comment;
    }
    
    


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Used to find a UserRating using a given set of constraints. Example:
   *  UserRating obj = UserRating.get("user_id=", user_id);
   */
    public static UserRating get(Object...args) throws SQLException {
        Object obj = _get(UserRating.class, args);
        return obj==null ? null : (UserRating) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find UserRatings using a given set of constraints.
   */
    public static UserRating[] find(Object...args) throws SQLException {
        Object[] obj = _find(UserRating.class, args);
        UserRating[] arr = new UserRating[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (UserRating) obj[i];
        }
        return arr;
    }
}