package javaxt.media.server;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javaxt.json.*;
import javaxt.sql.Model;
import javaxt.sql.Database;
import javaxt.express.utils.DbUtils;
import static javaxt.express.ConfigFile.*;
import static javaxt.utils.Console.console;

import javaxt.media.utils.FFmpeg;
import javaxt.media.utils.ImageMagick;


//******************************************************************************
//**  Config Class
//******************************************************************************
/**
 *   Provides thread-safe, static methods used to get and set application
 *   variables.
 *
 ******************************************************************************/

public class Config {

    private static javaxt.express.Config config = new javaxt.express.Config();
    private static AtomicBoolean dbInitialized = new AtomicBoolean(false);
    private static AtomicBoolean modelsInitialized = new AtomicBoolean(false);
    private Config(){}


  //**************************************************************************
  //** load
  //**************************************************************************
  /** Used to load a config file (JSON) and update config settings
   */
    public static void load(javaxt.io.File configFile) throws Exception {
      //Parse config file
        JSONObject json;
        if (configFile.exists()) {
            json = new JSONObject(configFile.getText());
        }
        else{
            json = new JSONObject();
        }

        load(json, configFile);
    }


  //**************************************************************************
  //** load
  //**************************************************************************
  /** Used to load a config file (JSON) and update config settings
   *  @param json Config information for the app
   *  @param configFile File used to resolve relative paths found in the json
   */
    public static void load(JSONObject json, javaxt.io.File configFile) throws Exception {


      //Update relative paths in the web config
        JSONObject webConfig = json.get("webapp").toJSONObject();
        if (webConfig!=null){
            updateDir("webDir", webConfig, configFile, false);
            updateDir("logDir", webConfig, configFile, true);
            updateFile("keystore", webConfig, configFile);
        }


      //Get database config
        JSONObject dbConfig = json.get("database").toJSONObject();
        if (dbConfig==null || dbConfig.isEmpty()){
            dbConfig = new JSONObject();
            dbConfig.set("driver", "H2");
            dbConfig.set("maxConnections", "25");
            dbConfig.set("path", "data/database");
            json.set("database", dbConfig);
        }


      //Process path variable in the database config
        if (dbConfig.has("path")){
            updateFile("path", dbConfig, configFile);
            String path = dbConfig.get("path").toString().replace("\\", "/");
            dbConfig.set("host", path);
            dbConfig.remove("path");
        }


      //Load config
        config.init(json);



      //Add "schemaFile" to the config
        JSONObject modelConfig = new JSONObject();
        modelConfig.set("schema", "models/schema.sql");
        updateFile("schema", modelConfig, configFile);
        config.set("schemaFile", new javaxt.io.File(modelConfig.get("schema").toString()));


      //Run validations
        Database database = config.getDatabase();
        if (database==null) throw new Exception("Invalid database");
    }


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Returns the value for a given key.
   */
    public static JSONValue get(String key){
        return config.get(key);
    }


  //**************************************************************************
  //** set
  //**************************************************************************
    public static void set(String key, Object value){
        config.set(key, value);
    }


  //**************************************************************************
  //** getDatabase
  //**************************************************************************
    public static Database getDatabase() throws Exception {

      //Get database
        Database database = config.getDatabase();
        if (database==null) throw new Exception("Invalid database");


      //Initialize the database as needed
        synchronized(dbInitialized){
            boolean initialized = dbInitialized.get();
            if (!initialized){


              //Update database properties as needed
                if (database.getDriver().equals("H2")){

                  //Set H2 to PostgreSQL mode
                    Properties properties = database.getProperties();
                    if (properties==null){
                        properties = new java.util.Properties();
                        database.setProperties(properties);
                    }
                    properties.setProperty("MODE", "PostgreSQL");
                    properties.setProperty("DATABASE_TO_LOWER", "TRUE");
                    properties.setProperty("DEFAULT_NULL_ORDERING", "HIGH");


                  //Update list of reserved keywords
                    Properties props = database.getProperties();
                    props.setProperty("NON_KEYWORDS", "KEY,VALUE,HOUR,MINUTE");
                }


              //Update connection pool size as needed
                javaxt.io.Jar jar = new javaxt.io.Jar(Config.class);
                long numModels = Arrays.stream(jar.getClasses())
                    .filter(c -> javaxt.sql.Model.class.isAssignableFrom(c))
                    .count();
                int minConnections = (int) Math.ceil(numModels*1.5);
                if (database.getConnectionPoolSize()<minConnections){
                    database.setConnectionPoolSize(minConnections);
                    //System.out.println("Increased connection pool to " + minConnections);
                }


              //Get database schema
                String schema = null;
                JSONValue v = config.get("schemaFile");
                if (!v.isNull()){
                    javaxt.io.File schemaFile = (javaxt.io.File) v.toObject();
                    if (schemaFile.exists()) schema = schemaFile.getText();
                }
                if (schema==null) throw new Exception("Schema not found");


              //Initialize schema (create tables, indexes, etc)
                initSchema(schema, database);


              //Enable database caching
                database.enableMetadataCache(true);


              //Inititalize connection pool
                database.initConnectionPool();


              //Update status
                dbInitialized.set(true);
            }
        }

        return database;
    }
    

  //**************************************************************************
  //** initSchema
  //**************************************************************************
    private static void initSchema(String schema, javaxt.sql.Database database) throws Exception {

      //Initialize schema (create tables, indexes, etc)
        boolean schemaInitialized = DbUtils.initSchema(database, schema, null);


      //Insert data
        if (schemaInitialized){
            try(javaxt.sql.Connection conn = database.getConnection()){

              //Insert date into the settings table
                conn.execute(
                    "insert into setting(key,value) " +
                    "values('db_date','"+ new javaxt.utils.Date().toLong()+"')"
                );


              //Disable authentication
                conn.execute(
                    "insert into setting(key,value) " +
                    "values('auth_status','disabled')"
                );


              //Create default user
                String userTable = "\"user\"";
                conn.execute("insert into person(id) values(-1)");
                conn.execute(
                    "insert into " + userTable + "(id,person_id,status) " +
                    "values(-1,-1,1)"
                );


              //Create components
                for (String component : new String[]{"SysAdmin","UserAdmin","Media","Contact"}){
                    conn.execute("insert into component(key) values('" + component + "')");
                }


              //Grant default user admin access to all components
                var componentIDs = new ArrayList<Long>();
                for (var r : conn.getRecords("select id from component")){
                    componentIDs.add(r.get(0).toLong());
                }
                for (var componentID : componentIDs){
                    conn.execute(
                        "insert into user_access(user_id,component_id,level) " +
                        "values(-1," + componentID + ",5)"
                    );
                }
            }
        }
    }


  //**************************************************************************
  //** initModels
  //**************************************************************************
    public static void initModels() throws Exception {
        synchronized(modelsInitialized){
            boolean initialized = modelsInitialized.get();
            if (!initialized){
                Database database = getDatabase();
                javaxt.io.Jar jar = new javaxt.io.Jar(Config.class);
                Model.init(jar, database.getConnectionPool());
                modelsInitialized.set(true);
            }
        }
    }


  //**************************************************************************
  //** getSetting
  //**************************************************************************
  /** Returns a setting from the database
   */
    public static String getSetting(String key) throws Exception {
        initModels();
        return javaxt.media.models.Setting.get("key=",key.toLowerCase()).getValue();
    }


  //**************************************************************************
  //** getImageMagick
  //**************************************************************************
    public static ImageMagick getImageMagick(){
        try {return new ImageMagick(getSetting("ImageMagick"));}
        catch(Exception e){return null;}
    }


  //**************************************************************************
  //** getFFmpeg
  //**************************************************************************
    public static FFmpeg getFFmpeg(){
        try {return new FFmpeg(getSetting("FFmpeg"));}
        catch(Exception e){return null;}
    }


  //**************************************************************************
  //** getOnnxFile
  //**************************************************************************
    public static javaxt.io.File getOnnxFile(String name){
        try {
            if (name.equals("face_detection") || name.equals("facial_recognition")){
                var file = new javaxt.io.File(getSetting(name));
                if (file.exists()) return file;
            }
        }
        catch(Exception e){}
        return null;
    }
}