package org.alfresco.bm.user;

import java.util.List;

/**
 * Abstract implementation providing some common method implementations
 * 
 * @author steveglover
 * @since 1.3
 */
public abstract class AbstractUserDataService implements UserDataService
{
    protected abstract List<UserData> getUsers(boolean created, int startIndex, int count);

    /**
     * Get a list of usernames that are NOT created in alfresco with paging
     * 
     * @param startIndex index to start getting users from  
     * @param count number of users to fetch
     * @return      List of user data, which may be empty or less than the required count
     */
    public List<UserData> getUsersPendingCreation(int startIndex, int count)
    {
        return getUsers(false, startIndex, count);
    }

    /**
     * Get a list of usernames that are created in alfresco with paging
     * 
     * @param startIndex    index to start getting users from  
     * @param count         number of users to fetch
     * @return              List of user data, which may be empty or less than the required count
     */
    public List<UserData> getCreatedUsers(int startIndex, int count)
    {
        return getUsers(true, startIndex, count);
    }
}
