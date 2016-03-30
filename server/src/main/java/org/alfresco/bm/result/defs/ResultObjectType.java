package org.alfresco.bm.result.defs;

/**
 * Object types in the result data service to collect data for.
 * 
 * @author Frank Becker
 * @since 2.1.2
 */
public enum ResultObjectType
{
    Repository, 
    
    /** please don't use - extension only */
    UnspecifiedNode, 
   
    User,
    
    Site, 
       
    Folder, 
    
    Document,
    
    Aspect,
    
    Property,
    
    Relationship
}
