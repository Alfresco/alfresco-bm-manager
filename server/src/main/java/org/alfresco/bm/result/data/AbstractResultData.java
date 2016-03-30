package org.alfresco.bm.result.data;

import org.alfresco.bm.exception.BenchmarkResultException;
import org.alfresco.bm.result.defs.ResultOperation;
import org.alfresco.bm.util.ArgumentCheck;
import org.bson.Document;

/**
 * Abstract result data implementation
 * 
 * @author Frank Becker
 * @since 2.1.2
 */
public abstract class AbstractResultData implements ResultData
{
    /** Serialization ID */
    private static final long serialVersionUID = -2897188916167052760L;

    /** stores the BSON description field */
    private Document description;
    /** stores the result operation */
    private ResultOperation resultOperation;

    /** constructor */
    public AbstractResultData(Document description, ResultOperation resultOperation)
    {
        setDescription(description);
        setResultOperation(resultOperation);
    }

    /** Constructor */
    public AbstractResultData(Document doc) throws BenchmarkResultException
    {
        try
        {
            setDescription((Document) doc.get(FIELD_DESCRIPTION));
            setResultOperation(ResultOperation.valueOf(doc.getString(FIELD_RESULT_OP)));
        }
        catch (Exception ex)
        {
            throw new BenchmarkResultException("Unable to parse ResultData from Document '" + doc.toJson() + "'.", ex);
        }
    }

    @Override
    public Document getDescription()
    {
        return description;
    }

    @Override
    public void setDescription(Document description)
    {
        this.description = description;
    }

    @Override
    public abstract Document toDocumentBSON();

    /**
     * returns the BSON document representation of this data result base class
     * 
     * @return the BSON document representation of this data result base class
     */
    protected Document getDocumentBSON()
    {
        return new Document(FIELD_RESULT_OP, this.resultOperation.toString())
                .append(FIELD_DESCRIPTION, this.description)
                .append(FIELD_DATA_TYPE, getDataType());
    }

    @Override
    public String toString()
    {
        return toDocumentBSON().toJson();
    }

    @Override
    public ResultOperation getResultOperation()
    {
        return this.resultOperation;
    }

    @Override
    public void setResultOperation(ResultOperation operation)
    {
        this.resultOperation = operation;
    }

    @Override
    public void appendQuery(Document queryDoc)
    {
        ArgumentCheck.checkMandatoryObject(queryDoc, "queryDoc");
        queryDoc.append(FIELD_RESULT_OP, this.resultOperation)
                .append(FIELD_DESCRIPTION, this.description)
                .append(FIELD_DATA_TYPE, getDataType());
        appendQueryParams(queryDoc);
    }

    /** appends the class-specific query params */
    protected abstract void appendQueryParams(Document quDocument);
}
