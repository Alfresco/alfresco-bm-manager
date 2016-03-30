package org.alfresco.bm.result.data;

import org.alfresco.bm.result.defs.ResultOperation;
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
        return new Document("resultOperation", this.resultOperation)
                .append("description", this.description);
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
}
