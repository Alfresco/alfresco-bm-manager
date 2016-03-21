package org.alfresco.bm.result;

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
    
    /** stores the JSON description field */
    private String description;
    /** stores the result operation */
    private ResultOperation resultOperation;
    
    /** constructor */
    public AbstractResultData(String description, ResultOperation resultOperation)
    {
        setDescription(description);
        setResultOperation(resultOperation);
    }
    
    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public void setDescription(String description)
    {
        this.description = description;
    }

    @Override
    public abstract String toJSON();
    
    @Override
    public String toString()
    {
        return toJSON();
    }
    
    @Override
	public ResultOperation getResultOperation() {
		return this.resultOperation;
	}

	@Override
	public void setResultOperation(ResultOperation operation) {
		this.resultOperation = operation;
	}
}
