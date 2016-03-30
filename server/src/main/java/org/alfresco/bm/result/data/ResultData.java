package org.alfresco.bm.result.data;

import java.io.Serializable;

import org.alfresco.bm.result.defs.ResultOperation;
import org.bson.Document;

/**
 * Benchmark result data to serialize/de-serialize to/from MongoDB
 * 
 * @author Frank Becker
 * @since 2.1.2
 */
public interface ResultData extends Serializable
{
    /** gets the BSON description field */
    Document getDescription();
    
    /** sets the BSON description field */
    void setDescription(Document description);
    
    /** conversion to BSON document */
    Document toDocumentBSON();
    
    /** gets the operation result */
    ResultOperation getResultOperation();
    
    /** sets the operation result */
    void setResultOperation(ResultOperation operation);
}
