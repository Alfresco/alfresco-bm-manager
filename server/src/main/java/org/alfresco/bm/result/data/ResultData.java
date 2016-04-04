package org.alfresco.bm.result.data;

import java.io.Serializable;

import org.alfresco.bm.exception.BenchmarkResultException;
import org.alfresco.bm.result.defs.ResultDBDataFields;
import org.alfresco.bm.result.defs.ResultOperation;
import org.bson.Document;

/**
 * Benchmark result data to serialize/de-serialize to/from MongoDB
 * 
 * @author Frank Becker
 * @since 2.1.2
 */
public interface ResultData extends Serializable, ResultDBDataFields
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
    
    /** returns the data type */ 
    String getDataType();
    
    /** appends the params that makes the query unique */
    void appendQuery(Document queryDoc);
    
    /**
     * Creates a new instance from a BSON document
     * 
     * @param doc (BSON Document, mandatory)
     * 
     * @return ResultData
     */
    public static ResultData create(Document doc) throws BenchmarkResultException
    {
        String dataType = doc.getString(FIELD_DATA_TYPE);
        switch (dataType)
        {
            case ObjectsPerSecondResultData.DATA_TYPE:
                return new ObjectsPerSecondResultData(doc);

            case ObjectsResultData.DATA_TYPE:
                return new ObjectsResultData(doc);
                
            case RuntimeResultData.DATA_TYPE:
                return new RuntimeResultData(doc);
                
            default:
                throw new BenchmarkResultException("Unknown data type: '" + dataType + "'");
        }
    }
}
