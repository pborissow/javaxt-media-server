package javaxt.media.server;

import java.util.*;

import javaxt.sql.*;
import javaxt.json.*;
import javaxt.encryption.BCrypt;
import javaxt.express.ServiceRequest;
import static javaxt.utils.Console.console;

import javaxt.media.models.*;
import javaxt.media.utils.FFmpeg;
import javaxt.media.utils.ImageMagick;


public class SecurityFilter {


  //**************************************************************************
  //** getRecordset
  //**************************************************************************
  /** Used to update queries and modify payloads for WebService requests
   *  @param op Options include "list, "get", "save", and "delete"
   */
    protected static Recordset getRecordset(ServiceRequest request, String op,
        Class c, String sql, Connection conn) throws Exception {

        User user = (User) request.getUser();
        String className = c.getSimpleName();
        if (className.equals("User")){

            if (op.equals("save") || op.equals("delete")){


              //Check permissions
                Integer accessLevel = getAccessLevel(user, "UserAdmin", conn);
                if (accessLevel<5){
                    Long userID = request.getParameter("id").toLong();
                    if (userID==null || user.getID()!=userID.longValue()){
                        throw new SecurityException();
                    }
                }


              //Make sure we don't delete the only admin
                if (op.equals("delete")){
                    Long userID = request.getParameter("id").toLong();
                    long numAdmins = getAdminCount(userID, "UserAdmin", conn);
                    if (numAdmins==0) throw new IllegalArgumentException(
                    "cannot delete only admin");
                }

            }
            else if (op.equals("list")){


              //Parse requested fields and check if requests fields include
              //accessLevel or lastAccess
                ArrayList<ServiceRequest.Field> fields = new ArrayList<>();
                boolean getUserAccessLevel = false;
                boolean getUserActivity = false;
                for (ServiceRequest.Field field : request.getFields()){
                    String fieldName = field.getColumn();
                    if (fieldName.equalsIgnoreCase("accessLevel") ||
                        fieldName.equalsIgnoreCase("access_level")){
                        getUserAccessLevel = true;
                    }
                    else if (fieldName.equalsIgnoreCase("lastAccess") ||
                        fieldName.equalsIgnoreCase("last_access")){
                        getUserActivity = true;
                    }
                    else{
                        fields.add(field);
                    }
                }


/*
              //Join "user" to "user_access" as needed
                if (getUserAccessLevel){

                    javaxt.sql.Parser parser = new javaxt.sql.Parser(sql);
                    String tableName = parser.getFromString();

                    String select = "";
                    for (ServiceRequest.Field field : fields){
                        if (select.length()>0) select += ", ";
                        select += tableName + "." + field.getColumn();
                    }
                    select += ", user_access.level as access_level";


                    sql = "select " + select + " from " + tableName +
                    " join user_access on " + tableName + ".id=user_access.user_id";




                  //Add "where" statement. Since we are now joining two tables,
                  //we need to generate a new where statement
                    String where = request.getWhereStatement(User.class, UserAccess.class);
                    if (where!=null) sql += where;


                  //Add order by
                    String orderBy = parser.getOrderByString();
                    if (orderBy!=null) sql += " order by " + orderBy;


                  //Add limit and offset
                    sql += request.getOffsetLimitStatement(conn.getDatabase().getDriver());
                }
*/


              //Join current query to the "user_activity" table as needed
                if (getUserActivity){

                    javaxt.sql.Parser parser = new javaxt.sql.Parser(sql);
                    String orderBy = parser.getOrderByString();

                    sql = "select * from (\n" +
                        sql + "\n" +
                    ") a\n" +
                    "left join (\n" +
                        "select user_id, max(hour || LPAD(minute::text, 2, '0')) " +
                        "as last_access from user_activity group by user_id" +
                    ") b on id = user_id";

                    if (orderBy!=null && !orderBy.isBlank()){
                        sql += " order by " + orderBy;
                    }
                    //console.log(sql);
                }
            }

        }
        else if (className.equals("UserPreference")){

            javaxt.sql.Parser parser = new javaxt.sql.Parser(sql);
            if (op.equals("get") || op.equals("list")){
                parser.setWhere("user_id=" + user.getID());
                sql = parser.toString();
            }
            else{
                sql = "select id from USER_PREFERENCE where " +
                "key='" + request.getParameter("key") + "' and " +
                "user_id=" + user.getID();


                if (op.equals("save")){
                    javaxt.sql.Record r = conn.getRecord(sql);
                    JSONObject payload = new JSONObject();
                    if (r!=null) payload.set("id", r.get("id"));
                    payload.set("key", request.getParameter("key"));
                    payload.set("value", request.getParameter("value"));
                    payload.set("userID", user.getID());
                    request.setPayload(payload.toString().getBytes("UTF-8"));
                }
            }

        }
        else if (className.equals("UserAuthentication")){

            if (op.equals("save") || op.equals("delete")){

              //Parse request
                Long userID = request.getParameter("userID").toLong();
                if (userID==null) throw new IllegalArgumentException("userID is required");

                String service = request.getParameter("service").toString();
                if (service==null) throw new IllegalArgumentException("service is required");
                service = service.toLowerCase();


              //Check permissions
                Integer accessLevel = getAccessLevel(user, "UserAdmin", conn);
                if (accessLevel<5){
                    if (user.getID()!=userID.longValue()) throw new SecurityException();
                }


              //Get tableName
                javaxt.sql.Parser parser = new javaxt.sql.Parser(sql);
                String tableName = parser.getFromString();


              //Update password as needed
                if (op.equals("save") && service.equals("database")){

                    JSONObject json = request.getJson();
                    String password = json.get("value").toString();
                    if (password==null){
                        javaxt.sql.Record record = conn.getRecord(
                        "select value from " + tableName +
                        " where user_id=" + userID +
                        " and service='" + service + "'");

                        if (record!=null){
                            password = record.get(0).toString();
                        }
                    }
                    else{
                        if (!BCrypt.hasSalt(password)){
                            password = BCrypt.hashpw(password);
                        }
                    }

                    json.set("value", password);
                }


              //Update query
                sql = "select id from " + tableName +
                " where user_id=" + userID +
                " and service='" + service + "'";


              //Update id
                setID(request, op, sql, conn);

            }

        }
        else if (className.equals("UserAccess")){

            if (op.equals("save") || op.equals("delete")){

              //Get userID
                Long userID = request.getParameter("userID").toLong();
                if (userID==null) throw new IllegalArgumentException("userID is required");


              //Get table name
                javaxt.sql.Parser parser = new javaxt.sql.Parser(sql);
                String userAccessTable = parser.getFromString();


              //Find component ID for the admin tab
                javaxt.sql.Record record;
                record = conn.getRecord("select id from component where key='UserAdmin'");
                Long userAdmin = record==null ? -1 : record.get(0).toLong();


              //Check permissions
                record = conn.getRecord("select level from " + userAccessTable +
                " where component_id=" + userAdmin + " and user_id=" + user.getID());
                Integer accessLevel = record==null ? 0 : record.get(0).toInteger();
                if (accessLevel<5) throw new SecurityException();


              //Get component ID
                Long componentID = request.getParameter("componentID").toLong();
                if (componentID==null){
                    Long id = request.getID();
                    if (id==null) throw new IllegalArgumentException("id is required");

                    record = conn.getRecord(
                    "select component_id from " + userAccessTable + " where id=" + id);
                    if (record==null) throw new IllegalArgumentException("id is required");
                    componentID = record.get(0).toLong();
                }


              //Make sure we don't delete the only user admin
                if (componentID.equals(userAdmin)){
                    Integer currLevel = accessLevel;
                    if (currLevel==5){
                        boolean count = false;
                        if (op.equals("delete")){
                            count = true;
                        }
                        else{
                            Integer newLevel = request.getJson().get("level").toInteger();
                            if (newLevel==null || newLevel<currLevel){
                                count = true;
                            }
                        }

                        if (count){
                            long numAdmins = getAdminCount(userID, userAdmin, conn);
                            if (numAdmins==0) throw new IllegalArgumentException(
                            "At least one user admin is required");
                        }
                    }
                }


              //Update query
                sql = "select id from " + userAccessTable + " where user_id=" + userID;
                sql += " and component_id=" + componentID;
                Long id = request.getID();
                if (id!=null) sql += " and id=" + id;


              //Update id
                setID(request, op, sql, conn);

            }

        }
        else if (className.equals("Component")){

            if (op.equals("save") || op.equals("delete")){
                Integer accessLevel = getAccessLevel(user, "SysAdmin", conn);
                if (accessLevel<5) throw new SecurityException();
            }

        }
        else if (className.equals("Datatype")){

            if (op.equals("save") || op.equals("delete")){
                Integer accessLevel = getAccessLevel(user, "SysAdmin", conn);
                if (accessLevel<5) throw new SecurityException();
            }

        }
        else if (className.equals("Dataset")){


          //Check permissions
            String filter = "select DATASET_ID from DATASET_PERMISSION where " +
            "USER_ID=" + user.getID();

            if (!op.equals("list")){

                Long id = request.getID();
                filter += " and DATASET_ID=" + id;

                if (op.equals("save") || op.equals("delete")){
                    if (id==null) filter = null;
                    else filter += " and READONLY=false";
                }
            }

            if (filter!=null){
                javaxt.sql.Parser parser = new javaxt.sql.Parser(sql);
                String where = parser.getWhereString();
                if (where==null) where = "";
                else where += " and";
                where += " id in (" + filter + ")";
                parser.setWhere(where);
                sql = parser.toString();
            }
        }
        else if (className.equals("DatasetPermission")){

            if (op.equals("save") || op.equals("delete")){

              //Parse request
                Long userID = request.getParameter("userID").toLong();
                if (userID==null) throw new IllegalArgumentException("userID is required");

                Long datasetID = request.getParameter("datasetID").toLong();
                if (datasetID==null) throw new IllegalArgumentException("datasetID is required");


              //Check permissions
                Integer accessLevel = getAccessLevel(user, "SysAdmin", conn);
                if (accessLevel<5){

                    javaxt.sql.Record record = conn.getRecord(
                    "select owner_id from dataset where id=" + datasetID);
                    Long ownerID = record==null ? null : record.get(0).toLong();

                    if (ownerID!=null){
                        if (ownerID!=user.getID().longValue()) throw new SecurityException();
                    }
                }


              //Update query
                javaxt.sql.Parser parser = new javaxt.sql.Parser(sql);
                String tableName = parser.getFromString();
                sql = "select id from " + tableName +
                " where dataset_id=" + datasetID + " and user_id=" + userID;


              //Update id
                setID(request, op, sql, conn);

            }
        }
        else if (className.equals("Setting")){

            if (op.equals("delete")){
                throw new IllegalArgumentException("Settings cannot be deleted");
            }
            else{
                //No need to do anything. Get and save is handled in WebServices...
            }
        }


      //Execute query and return recordset
        Recordset rs = new Recordset();
        if (op.equals("list")) rs.setFetchSize(1000);
        try{
            rs.open(sql, conn);
            return rs;
        }
        catch(Exception e){
            console.log(sql);
            throw e;
        }
    }


  //**************************************************************************
  //** setID
  //**************************************************************************
    private static void setID(ServiceRequest request, String op, String sql, Connection conn) throws Exception {
        javaxt.sql.Record record = conn.getRecord(sql);
        Long id = record==null ? null : record.get(0).toLong();
        request.setParameter("id", id==null ? null : id.toString());
        if (op.equals("save")){
            request.getJson().set("id", id);
        }
    }


  //**************************************************************************
  //** getAccessLevel
  //**************************************************************************
    public static int getAccessLevel(User user, String component, Connection conn) throws Exception {
        javaxt.sql.Record record = conn.getRecord(
            "select level from user_access " +
            "join component on user_access.component_id=component.id " +
            "where user_id=" + user.getID() + " and key='" + component + "'"
        );
        if (record!=null) return record.get(0).toInteger();
        else return 0;
    }


  //**************************************************************************
  //** getAdminCount
  //**************************************************************************
  /** Returns number of admins, excluding given userID
   */
    private static long getAdminCount(long userID, String component, Connection conn) throws Exception {
        javaxt.sql.Record record = conn.getRecord(
        "select count(user_access.id) from user_access " +
        "join component on user_access.component_id=component.id " +
        "where key='" + component + "' and level=5 and user_id<>" + userID);
        return record==null ? 0 : record.get(0).toLong();
    }


  //**************************************************************************
  //** getAdminCount
  //**************************************************************************
  /** Returns number of admins, excluding given userID
   */
    private static long getAdminCount(long userID, long componentID, Connection conn) throws Exception {
        javaxt.sql.Record record = conn.getRecord(
        "select count(id) from user_access " +
        "where component_id=" + componentID + " and level=5 and user_id<>" + userID);
        return record==null ? 0 : record.get(0).toLong();
    }
}