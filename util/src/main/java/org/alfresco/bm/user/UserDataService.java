package org.alfresco.bm.user;

import java.util.Iterator;
import java.util.List;

import org.alfresco.bm.data.DataCreationState;

import com.mongodb.DuplicateKeyException;

/**
 * Service providing access to {@link UserData} storage. All {@link UserData} returned from and persisted
 * with this service will be testrun-specific. The testrun-identifier is set in the constructor.
 *
 * @author Frederik Heremans
 * @author Derek Hulley
 * @author steveglover
 * @since 1.1
 */
public interface UserDataService
{
    /**
     * The {@link UserData#getDomain() domain} given to users who belong to the default domain
     */
    public static final String DEFAULT_DOMAIN = "default";
    
    public interface UserCallback
    {
        boolean callback(UserData user);
    };
    
    /**
     * @throws DuplicateKeyException    if the username is already used
     */
    public void createNewUser(UserData data);

    /**
     * Update a user's password
     */
    public void setUserPassword(String username, String password);

    /**
     * Change the 'created' state of the user i.e. whether the user exists on the server or not
     */
    public void setUserCreationState(String username, DataCreationState creationState);
    
    /**
     * @param domain                the domain to search or <tt>null</tt> for all domains
     * @param creationState         optional creation state to filter the count or <tt>null</tt> for all 
     */
    public long countUsers(String domain, DataCreationState creationState);

    /**
     * Delete users by create state
     * 
     * @param creationState         the user creation state to target or <tt>null<tt> to delete all users
     */
    public long deleteUsers(DataCreationState creationState);
    
    /**
     * Find a user by username
     * 
     * @return                          the {@link UserData} found otherwise <tt>null</tt>.
     */
    public UserData findUserByUsername(String username);
    
    /**
     * Find a user by email address
     * 
     * @return                          the {@link UserData} found otherwise <tt>null</tt>.
     */
    public UserData findUserByEmail(String email);
    
    /**
     * Get the users based on user creation state
     * 
     * @param creationState the current creation state
     * @param startIndex    index to start getting users from  
     * @param count         number of users to fetch
     * @return              List of user data, which may be empty or less than the required count
     */
    public List<UserData> getUsersByCreationState(DataCreationState creationState, int startIndex, int count);
    
    /**
     * Select a random, pre-created user.
     * <p/>
     * Note that this is useful only for large numbers of users.
     * 
     * @return      a random user or <tt>null</tt> if none are available
     */
    public UserData getRandomUser();

    /*
     * USER DOMAIN SERVICES
     */

    /**
     * Access created users by their user domain using paging
     * 
     * @param domain                    the user domain
     * @param startIndex                the start index for paging
     * @param count                     the number of users to retrieve
     * 
     * @return a list of users in the user domain
     * 
     * @see #DEFAULT_DOMAIN
     */
    public List<UserData> getUsersInDomain(String domain, int startIndex, int count);

    /**
     * An iterator over domains in the users collection.
     * 
     * @return an iterator over domains
     * 
     * @see #DEFAULT_DOMAIN
     */
    public Iterator<String> getDomainsIterator();
    
    /**
     * Select a random, pre-created user.
     * <p/>
     * Note that this is useful only for large numbers of users.
     * 
     * @param       domain the user domain
     * @return      a random user or <tt>null</tt> if none are available
     * 
     * @see #DEFAULT_DOMAIN
     */
    public UserData getRandomUserFromDomain(String domain);

    /**
     * Select a random, pre-created user that is a member of one of the given domains.
     * <p/>
     * Note that this is useful only for large numbers of users.
     * 
     * @param       domain the user domain
     * @return      a random user or <tt>null</tt> if none are available
     * 
     * @see #DEFAULT_DOMAIN
     */
    UserData getRandomUserFromDomains(List<String> domains);
}
