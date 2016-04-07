package org.alfresco.bm.exception;

/**
 * ObjectNotFoundException
 * 
 * @author Frank Becker
 * @since 2.1.2
 */
public class ObjectNotFoundException extends Exception
{
    /**
     * Constructor
     * 
     * @param objectName
     *        (String, mandatory) Name of object that wasn't found although
     *        expected to be there.
     */
    public ObjectNotFoundException(String objectName)
    {
        super("Object '" + objectName + "' not found!");
    }

    /**
     * Constructor with a cause
     * 
     * @param objectName
     *        (String, mandatory) Name of object that wasn't found although
     *        expected to be there.
     * @param cause
     *        (Throwable) cause
     */
    public ObjectNotFoundException(String objectName, Throwable cause)
    {
        super("'" + objectName + "' not found!", cause);
    }

    /**
     * Checks and throws if object is null.
     * 
     * @param obj
     *        (Object) object expected not to be null
     * @param objName
     *        (String, mandatory), name of object
     * 
     * @throws ObjectNotFoundException
     */
    public static void checkObject(Object obj, String objName) throws ObjectNotFoundException
    {
        if (null == obj)
        {
            throw new ObjectNotFoundException(objName);
        }
    }
}
