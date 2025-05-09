var package = "javaxt.media.models";
var models = {


  //**************************************************************************
  //** File
  //**************************************************************************
  /** Used to represent an individual file found in a directory
   */
    File: {
        fields: [
            {name: 'name',         type: 'string',  required: true}, //without file extension!
            {name: 'extension',    type: 'string',  required: true}, //file extension
            {name: 'path',         type: 'Path',    required: true},
            {name: 'type',         type: 'string',  required: true},
            {name: 'date',         type: 'date',    required: true},
            {name: 'size',         type: 'long',    required: true},
            {name: 'hash',         type: 'string',  required: true},
            {name: 'metadata',     type: 'json'}
        ],
        indexes: [
            {name: 'idx_unique_file', type: 'unique', field: ['path','name','extension']}
        ]
    },



  //**************************************************************************
  //** Path
  //**************************************************************************
  /** Used to represent an individual directory/path containing files.
   */
    Path: {
        fields: [
            {name: 'dir',   type: 'string', required: true, unique: true},
            {name: 'host',  type: 'Host',   required: true}
        ],
        indexes: [
            {name: 'idx_unique_path', type: 'unique', field: ['dir','host']}
        ]
    },


  //**************************************************************************
  //** Host
  //**************************************************************************
    Host: {
        fields: [
            {name: 'name',         type: 'string',  required: true, unique: true},
            {name: 'description',  type: 'string'},
            {name: 'metadata',     type: 'json'}
        ]
    },


//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@




  //**************************************************************************
  //** MediaItem
  //**************************************************************************
  /** Used to represent an image (e.g. photo, graphic, screenshot, etc),
   *  audio clip, or video. The media item may consist of one or more files.
   */
    MediaItem: {
        fields: [
            {name: 'name',          type: 'string'},
            {name: 'description',   type: 'string'},
            {name: 'type',          type: 'string'},
            {name: 'startDate',     type: 'date'},
            {name: 'endDate',       type: 'date'},
            {name: 'hash',          type: 'string'}, //phash
            {name: 'location',      type: 'geo'},
            {name: 'info',          type: 'json'}
        ],
        hasMany: [
            {model: 'File',         name: 'files',      unique: true},
            {model: 'Keyword',      name: 'keywords',   unique: true}
        ],
        indexes: ['startDate', 'endDate' ,'hash']
    },



//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@



  //**************************************************************************
  //** Folder
  //**************************************************************************
  /** A collection of items (photos, videos, documents, etc)
   */
    Folder: {
        fields: [
            {name: 'name',         type: 'string',  required: true},
            {name: 'description',  type: 'string'},
            {name: 'parent',       type: 'Folder'},
            {name: 'info',         type: 'json'}
        ],
        indexes: [
            {name: 'idx_folder', type: 'unique', field: ['name','parent']}
        ]
    },


  //**************************************************************************
  //** FolderEntry
  //**************************************************************************
  /** Used to represent an individual item in a Folder.
   */
    FolderEntry: {
        fields: [
            {name: 'folder',   type: 'Folder',      required: true},
            {name: 'item',     type: 'MediaItem',   required: true},
            {name: 'index',    type: 'long'},
            {name: 'info',     type: 'json'}
        ],
        indexes: [
            {name: 'idx_folder_entry', type: 'unique', field: ['folder','item']}
        ]
    },



//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@



  //**************************************************************************
  //** Person
  //**************************************************************************
  /** Used to represent an individual.
   */
    Person: {
        fields: [
            {name: 'gender',     type: 'string'}, //M,F
            {name: 'birthday',   type: 'int'}, //yyyymmdd
            {name: 'info',       type: 'json'}
        ],
        constraints: [
            {name: 'gender',        length: 1}
        ]
    },



  //**************************************************************************
  //** PersonName
  //**************************************************************************
  /** Used to represent a name of a person
   */
    PersonName: {
        fields: [
            {name: 'person',    type: 'Person',     required: true},
            {name: 'name',      type: 'string',     required: true},
            {name: 'preferred', type: 'boolean'},
            {name: 'info',      type: 'json'}
        ],
        indexes: [
            {name: 'idx_person_name', type: 'unique', field: ['name','person']}
        ]
    },


  //**************************************************************************
  //** PersonAddress
  //**************************************************************************
  /** Used to represent a address of a person
   */
    PersonAddress: {
        fields: [
            {name: 'address',   type: 'Address',    required: true},
            {name: 'person',    type: 'Person',     required: true},
            {name: 'type',      type: 'string',     required: true}, //string literals like "work", "home", etc
            {name: 'preferred', type: 'boolean'},
            {name: 'info',      type: 'json'}
        ],
        indexes: [
            {name: 'idx_person_address', type: 'unique', field: ['name','address']}
        ]
    },



  //**************************************************************************
  //** PersonContact
  //**************************************************************************
  /** Used to represent a contact info for a person
   */
    PersonContact: {
        fields: [
            {name: 'person',    type: 'Person',     required: true},
            {name: 'contact',   type: 'string',     required: true}, //email address, phone number, etc
            {name: 'type',      type: 'string'}, //string literals like "phone", "email", etc
            {name: 'info',      type: 'json'}
        ],
        indexes: [
            {name: 'idx_person_contact', type: 'unique', field: ['contact','person']}
        ]
    },



//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@


  //**************************************************************************
  //** Place
  //**************************************************************************
  /** Used to represent a location on the earth. Can be used to represent a
   *  descrete location such as a GPS point, a route, an area (e.g. park,
   *  country, state, etc).
   */
    Place: {
        fields: [
            {name: 'location',  type: 'geo'},
            {name: 'info',      type: 'json'}
        ]
    },


  //**************************************************************************
  //** PlaceName
  //**************************************************************************
  /** Used to represent a name of a place
   */
    PlaceName: {
        fields: [
            {name: 'place',     type: 'Place',      required: true},
            {name: 'name',      type: 'string',     required: true},
            {name: 'preferred', type: 'boolean'},
            {name: 'info',      type: 'json'}
        ],
        indexes: [
            {name: 'idx_place_name', type: 'unique', field: ['name','place']}
        ]
    },


  //**************************************************************************
  //** Address
  //**************************************************************************
  /** Used to represent a mailing address
   */
    Address: {
        fields: [
            {name: 'street',        type: 'string'},
            {name: 'city',          type: 'string'},
            {name: 'state',         type: 'string'},
            {name: 'postalCode',    type: 'string'},
            {name: 'place',         type: 'Place'}
        ]
    },





//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@


    Keyword: {
        fields: [
            {name: 'word',  type: 'string', unique: true, required: true}
        ]
    },



  //**************************************************************************
  //** Feature
  //**************************************************************************
  /** Used to represent a feature found in a media item (e.g. face in a photo)
   */
    Feature: {
        fields: [
            {name: 'item',          type: 'MediaItem',  required: true},
            {name: 'coordinates',   type: 'json',       required: true},
            {name: 'thumbnail',     type: 'binary'},
            {name: 'label',         type: 'string'}, //face, landmark, etc
            {name: 'info',          type: 'json'}
        ]
    },



  //**************************************************************************
  //** MediaPerson
  //**************************************************************************
  /** Used to associate a person to a media item.
   */
    MediaPerson: {
        fields: [
            {name: 'item',          type: 'MediaItem',  required: true},
            {name: 'person',        type: 'Person',     required: true},
            {name: 'info',          type: 'json'}
        ],
        hasMany: [
            {model: 'Feature',      name: 'features'}
        ]
    },



  //**************************************************************************
  //** MediaPlace
  //**************************************************************************
  /** Used to associate a place to a media item.
   */
    MediaPlace: {
        fields: [
            {name: 'item',          type: 'MediaItem',  required: true},
            {name: 'place',         type: 'Place',      required: true},
            {name: 'info',          type: 'json'}
        ],
        hasMany: [
            {model: 'Feature',      name: 'features'}
        ]
    },



//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@


  //**************************************************************************
  //** Component
  //**************************************************************************
  /** Used to represent a feature in the app. Access to individual components
   *  are managed via UserAccess
   */
    Component: {
        fields: [
            {name: 'key',           type: 'string'},
            {name: 'label',         type: 'string'},
            {name: 'description',   type: 'string'},
            {name: 'info',          type: 'json'}
        ],
        constraints: [
            {field: 'key',  required: true,  length: 50, unique: true}
        ]
    },



//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@



  //**************************************************************************
  //** User
  //**************************************************************************
    User: {
        implements: ['java.security.Principal', 'javaxt.express.User'],
        fields: [
            {name: 'person',    type: 'Person' ,   required: true},
            {name: 'active',    type: 'boolean',   required: true}
        ],
        defaults: [
            {name: 'active',    value: true}
        ],
        indexes: [
            {name: 'idx_user', type: 'unique', field: ['person']}
        ]
    },


  //**************************************************************************
  //** UserAccess
  //**************************************************************************
  /** Used to represent permissions associated with a user. This is an
   *  application table.
   */
    UserAccess: {
        fields: [
            {name: 'user',      type: 'User',       required: true},
            {name: 'component', type: 'Component',  required: true},
            {name: 'level',     type: 'int',        required: true},
            {name: 'info',      type: 'json'}
        ],
        defaults: [
            {name: 'level',     value: 3}
        ],
        indexes: [
            {name: 'idx_user_access', type: 'unique', fields: ['user','component']}
        ]
    },


  //**************************************************************************
  //** UserAuthentication
  //**************************************************************************
  /** Used to encapsulate authentication information associated with an
   *  individual user.
   */
    UserAuthentication: {
        fields: [
            {name: 'user',        type: 'User',     required: true},
            {name: 'service',     type: 'string',   required: true},
            {name: 'key',         type: 'string',   required: true},
            {name: 'value',       type: 'string'},
            {name: 'info',        type: 'json'}
        ],
        indexes: [
            {name: 'idx_user_auth', type: 'unique', field: ['service','key']} //no user!
        ]
    },


  //**************************************************************************
  //** UserPreference
  //**************************************************************************
    UserPreference: {
        fields: [
            {name: 'user',     type: 'User',    required: true},
            {name: 'key',      type: 'string',  required: true},
            {name: 'value',    type: 'json',    required: true}
        ],
        indexes: [
            {name: 'idx_user_preference', type: 'unique', field: ['user','key','value']}
        ]
    },


  //**************************************************************************
  //** UserActivity
  //**************************************************************************
  /** Used to count user activity within a given time period (requests per
   *  day/hour/minute)
   */
    UserActivity: {
        fields: [
            {name: 'user',      type: 'User',   required: true,  onDelete: 'no action'},
            {name: 'hour',      type: 'int',    required: true}, //yyyymmddhh in utc
            {name: 'minute',    type: 'int',    required: true},
            {name: 'count',     type: 'int',    required: true} //e.g. number of requests
        ],
        indexes: [
            {name: 'idx_user_activity', type: 'unique', field: ['user','hour','minute']},
            {field: 'hour'}
        ]
    },


  //**************************************************************************
  //** UserRating
  //**************************************************************************
  /** Used to record user ratings
   */
    UserRating: {
        fields: [
            {name: 'user',      type: 'User',       required: true},
            {name: 'item',      type: 'MediaItem',  required: true},
            {name: 'date',      type: 'date',       required: true},
            {name: 'rating',    type: 'int',        required: true},
            {name: 'comment',   type: 'string'}
        ],
        indexes: [
            {name: 'idx_user_rating', type: 'unique', field: ['user','item']}
        ]
    },


  //**************************************************************************
  //** UserGroup
  //**************************************************************************
  /** Used to represent a group of users
   */
    UserGroup: {
        fields: [
            {name: 'name',          type: 'string', unique: true, required: true},
            {name: 'description',   type: 'string'}
        ],
        hasMany: [
            {model: 'User',         name: 'users', unique: true}
        ]
    },



//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@


  //**************************************************************************
  //** MediaAccess
  //**************************************************************************
  /** Used to restrict access to a media item
   */
    MediaAccess: {
        fields: [
            {name: 'item',      type: 'MediaItem',  required: true},
            {name: 'group',     type: 'UserGroup',  required: true},
            {name: 'readOnly',  type: 'boolean',    required: true}
        ],
        indexes: [
            {name: 'idx_media_access', type: 'unique', field: ['item','group']}
        ]
    },



  //**************************************************************************
  //** MediaLog
  //**************************************************************************
  /** Used to record whenever a MediaItem was created, updated, or accessed
   *  by a user.
   */
    MediaLog: {
        fields: [
            {name: 'item',      type: 'MediaItem',  required: true},
            {name: 'user',      type: 'User'},      //optional (e.g. anonymous users)
            {name: 'date',      type: 'date',       required: true},
            {name: 'action',    type: 'string',     required: true} //"create", "view", "download" etc
        ],
        indexes: ['date','action']
    },



//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@


  //**************************************************************************
  //** Datatype
  //**************************************************************************
    Datatype: {
        fields: [
            {name: 'label', type: 'string', required: true, unique: true}
        ]
    },


  //**************************************************************************
  //** Data
  //**************************************************************************
  /** General purpose table used to store application data (e.g. settings,
   *  watch folders, etc).
   */
    Data: {
        fields: [
            {name: 'name',          type: 'string',     required: true},
            {name: 'description',   type: 'string'},
            {name: 'type',          type: 'Datatype',   required: true},
            {name: 'data',          type: 'json'},
            {name: 'date',          type: 'date'},
            {name: 'thumbnail',     type: 'binary'}
        ]
    },


  //**************************************************************************
  //** DataAccess
  //**************************************************************************
  /** Used to manage permssions for a dataset.
   */
    DataAccess: {
        fields: [
            {name: 'dataset',   type: 'Data',       required: true},
            {name: 'group',     type: 'UserGroup',  required: true},
            {name: 'readOnly',  type: 'boolean',    required: true, default: true}
        ],
        indexes: [
            {name: 'idx_data_access', type: 'unique', field: ['dataset','group']}
        ]
    },


  //**************************************************************************
  //** Setting
  //**************************************************************************
  /** Used to manage application config/settings.
   */
    Setting: {
        fields: [
            {name: 'key',      type: 'string',  required: true},
            {name: 'value',    type: 'string',  required: true}
        ],
        indexes: [
            {name: 'idx_setting', type: 'unique', field: ['key']}
        ]
    }

};