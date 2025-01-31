# JavaXT Media Server
The media server is used to catalog and index photos and videos in a database. 
As items are indexed, thumbnails are generated, objects are detected (e.g. faces), 
metadata is extracted, and facial recognition is performed. 
Once indexed, photos and videos can be viewed in a web app.

## Dependencies
-	Java
-	[OpenCV](../../wiki/OpenCV)
-	[ImageMagick](../../wiki/ImageMagick)
-	[FFmpeg](../../wiki/FFmpeg)
-	H2 or PostgreSQL database

## Command-line Interface
You can start the server like this:
```
java -jar media-server.jar -port 8080
```
You can index a folder like this:
```
java -jar media-server.jar -add directory -path /path/to/files
```

## License
All JavaXT software is free and open source released under a permissive MIT license. 
The software comes with no guarantees or warranties and you may use the software in any open source or commercial project.
