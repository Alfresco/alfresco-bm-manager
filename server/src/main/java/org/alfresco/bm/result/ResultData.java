package org.alfresco.bm.result;

import java.io.Serializable;

/**
 * Benchmark result data to serialize/de-serialize to/from MongoDB
 * 
 * @author Frank Becker
 * @since 2.1.2
 */
public interface ResultData extends Serializable
{
    /** gets the JSON description field */
    String getDescription();
    
    /** sets the JSON description field */
    void setDescription(String description);
    
    /** conversion to JSON string */
    String toJSON();
    
    /** gets the operation result */
    ResultOperation getResultOperation();
    
    /** sets the operation result */
    void setResultOperation(ResultOperation operation);
}
