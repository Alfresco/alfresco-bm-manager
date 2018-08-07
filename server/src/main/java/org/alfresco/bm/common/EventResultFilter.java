package org.alfresco.bm.common;

/**
 * Filter values for the event results in the {@see ResultsRestAPI}
 * 
 * @author Frank Becker
 * @since 2.0.10
 */
public enum EventResultFilter
{
    /** returns all event results independent from the succeed status */
    All,
    
    /** returns only successful event results */
    Success,
    
    /** returns only failed event results */
    Failed
}
