package org.alfresco.bm.event.mongo;

import org.alfresco.bm.util.ArgumentCheck;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;

public final class Mongo3Helper
{
    /**
     * Checks if the collection exists
     * 
     * @param db
     *        (MongoDatabase, mandatory)
     * @param collectionName
     *        (String, mandatory) name of the collection
     *        
     * @return (boolean)
     */
    public static boolean collectionExists(MongoDatabase db, String collectionName)
    {
        ArgumentCheck.checkMandatoryObject(db, "db");
        ArgumentCheck.checkMandatoryString(collectionName, "collectionName");

        MongoIterable<String> collectionNames = db.listCollectionNames();
        for (final String name : collectionNames)
        {
            if (name.equalsIgnoreCase(collectionName))
            {
                return true;
            }
        }

        return false;
    }
    
    /**
     * Converts object ID to hex-string 
     * 
     * @param id (ObjectId, mandatory)
     * 
     * @return String representation of object ID
     */
    public static String objectIdToString(ObjectId id)
    {
        ArgumentCheck.checkMandatoryObject(id, "id");
        return id.toHexString();
    }
}
