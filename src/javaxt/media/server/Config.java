package javaxt.media.server;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import javaxt.json.*;
import javaxt.sql.Model;
import javaxt.sql.Database;
import javaxt.express.utils.DbUtils;
import static javaxt.utils.Console.console;
import static javaxt.express.ConfigFile.updateDir;
import static javaxt.express.ConfigFile.updateFile;

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


      //Add "models" to the config
        JSONObject models = new JSONObject();
        modelConfig.set("opencv", "models/opencv");
        updateDir("opencv", modelConfig, configFile, false);
        for (javaxt.io.File file : new javaxt.io.Directory(modelConfig.get("opencv").toString()).getFiles("*.onnx")){
            models.set(file.getName(false), file.toString());
        }
        config.set("models", models);


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


              //Get database schema
                String schema = null;
                JSONValue v = config.get("schemaFile");
                if (!v.isNull()){
                    javaxt.io.File schemaFile = (javaxt.io.File) v.toObject();
                    if (schemaFile.exists()) schema = schemaFile.getText();
                }
                if (schema==null) throw new Exception("Schema not found");


              //Initialize schema (create tables, indexes, etc)
                if (schema!=null) DbUtils.initSchema(database, schema, null);


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
  //** initModels
  //**************************************************************************
    public static void initModels() throws Exception {
        Database database = getDatabase();
        javaxt.io.Jar jar = new javaxt.io.Jar(Config.class);
        Model.init(jar, database.getConnectionPool());
    }


  //**************************************************************************
  //** getOnnxFile
  //**************************************************************************
    public static javaxt.io.File getOnnxFile(String name){
        String path = get("models").get(name).toString();
        if (path==null) return null;
        return new javaxt.io.File(path);
    }
}