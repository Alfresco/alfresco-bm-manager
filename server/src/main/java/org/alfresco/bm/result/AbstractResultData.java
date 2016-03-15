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
    
    /** constructor */
    public AbstractResultData(String description)
    {
        this.description = description;
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
}
