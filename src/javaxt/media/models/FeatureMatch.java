package javaxt.media.models;
import javaxt.json.*;
import java.sql.SQLException;


//******************************************************************************
//**  FeatureMatch Class
//******************************************************************************
/**
 *   Used to represent a FeatureMatch
 *
 ******************************************************************************/

public class FeatureMatch extends javaxt.sql.Model {

    private Feature feature;
    private Feature matchingFeature;
    private JSONObject matchInfo;
    private Boolean ignoreMatch;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public FeatureMatch(){
        super("feature_match", java.util.Map.ofEntries(
            
            java.util.Map.entry("feature", "feature_id"),
            java.util.Map.entry("matchingFeature", "matching_feature_id"),
            java.util.Map.entry("matchInfo", "match_info"),
            java.util.Map.entry("ignoreMatch", "ignore_match")

        ));
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public FeatureMatch(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  FeatureMatch.
   */
    public FeatureMatch(JSONObject json){
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
            Long featureID = getValue(rs, "feature_id").toLong();
            Long matchingFeatureID = getValue(rs, "matching_feature_id").toLong();
            this.matchInfo = new JSONObject(getValue(rs, "match_info").toString());
            this.ignoreMatch = getValue(rs, "ignore_match").toBoolean();



          //Set feature
            if (featureID!=null) feature = new Feature(featureID);


          //Set matchingFeature
            if (matchingFeatureID!=null) matchingFeature = new Feature(matchingFeatureID);

        }
        catch(Exception e){
            if (e instanceof SQLException) throw (SQLException) e;
            else throw new SQLException(e.getMessage());
        }
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to update attributes with attributes from another FeatureMatch.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        if (json.has("feature")){
            feature = new Feature(json.get("feature").toJSONObject());
        }
        else if (json.has("featureID")){
            try{
                feature = new Feature(json.get("featureID").toLong());
            }
            catch(Exception e){}
        }
        if (json.has("matchingFeature")){
            matchingFeature = new Feature(json.get("matchingFeature").toJSONObject());
        }
        else if (json.has("matchingFeatureID")){
            try{
                matchingFeature = new Feature(json.get("matchingFeatureID").toLong());
            }
            catch(Exception e){}
        }
        this.matchInfo = json.get("matchInfo").toJSONObject();
        this.ignoreMatch = json.get("ignoreMatch").toBoolean();
    }


    public Feature getFeature(){
        return feature;
    }

    public void setFeature(Feature feature){
        this.feature = feature;
    }

    public Feature getMatchingFeature(){
        return matchingFeature;
    }

    public void setMatchingFeature(Feature matchingFeature){
        this.matchingFeature = matchingFeature;
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
  /** Used to find a FeatureMatch using a given set of constraints. Example:
   *  FeatureMatch obj = FeatureMatch.get("feature_id=", feature_id);
   */
    public static FeatureMatch get(Object...args) throws SQLException {
        Object obj = _get(FeatureMatch.class, args);
        return obj==null ? null : (FeatureMatch) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find FeatureMatchs using a given set of constraints.
   */
    public static FeatureMatch[] find(Object...args) throws SQLException {
        Object[] obj = _find(FeatureMatch.class, args);
        FeatureMatch[] arr = new FeatureMatch[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (FeatureMatch) obj[i];
        }
        return arr;
    }
}