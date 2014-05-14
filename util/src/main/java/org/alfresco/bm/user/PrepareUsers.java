/*
 * Copyright (C) 2005-2014 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.bm.user;

import org.alfresco.bm.event.AbstractEventProcessor;
import org.alfresco.bm.event.Event;
import org.alfresco.bm.event.EventResult;

/**
 * Prepare a specific number of users for interaction with Alfresco.
 * This does not actually create users in Alfresco but merely creates
 * a population of users (email addresses, names, etc) that can be used
 * by subsequent operations.</br>
 * <p/>
 * Numerical-based values can be used in:
 * <ul>
 *   <li>{@link #setEmailDomainPattern(String)}</li>
 *   <li>{@link #setFirstNamePattern(String)}</li>
 *   <li>{@link #setLastNamePattern(String)}</li>
 * </ul>
 * Value substitution can be used in:
 * <ul>
 *   <li>{@link #setDomainPattern(String)}</li>
 *   <li>{@link #setEmailAddressPattern(String)}</li>
 *   <li>{@link #setUsernamePattern(String)}</li>
 *   <li>{@link #setPasswordPattern(String)}</li>
 * </ul>
 * Values that can be substituted are:
 * <ul>
 *   <li>{@link #PATTERN_EMAIL_DOMAIN}</li>
 *   <li>{@link #PATTERN_FIRST_NAME}</li>
 *   <li>{@link #PATTERN_LAST_NAME}</li>
 *   <li>{@link #PATTERN_EMAIL_ADDRESS}</li>
 * </ul>
 * User data will be similar to this:<pre>
 *      {
 *        "_id" : ObjectId("504f19ce4ece382c484d3dc1"),
 *        "_class" : "org.alfresco.bm.user.UserData",
 *        "randomizer" : 774971,
 *        "username" : "0000001.test@00000.test",
 *        "password" : "0000001.test@00000.test",
 *        "created" : false,
 *        "firstName" : "0000001",
 *        "lastName" : "Test",
 *        "email" : "0000001.test@00000.test",
 *        "domain" : "00000.test"
 *      }
 * </pre> where the "randomizer" field is used to return users in a random order when
 * querying for lists of users.<br/>
 * Username, first name, last name and email patterns can be set.
 * 
 * <h1>Input</h1>
 * 
 * None.
 * 
 * <h1>Data</h1>
 * 
 * No data requirements.
 * 
 * <h1>Actions</h1>
 * 
 * Users data objects are created.  This data is local only.
 * 
 * <h1>Output</h1>
 * {@link #EVENT_NAME_USERS_PREPARED}: Marker indicating completion
 *
 * @author Derek Hulley
 * @since 1.1
 */
public class PrepareUsers extends AbstractEventProcessor
{
    private static final String PATTERN_EMAIL_DOMAIN = "[emailDomain]";
    private static final String PATTERN_FIRST_NAME = "[firstName]";
    private static final String PATTERN_LAST_NAME = "[lastName]";
    private static final String PATTERN_EMAIL_ADDRESS = "[emailAddress]";
    
    public static final long DEFAULT_USERS_PER_DOMAIN = 100L;
    public static final String EVENT_NAME_USERS_PREPARED = "usersPrepared";
    public static final String DEFAULT_DOMAIN_PATTERN = UserDataService.DEFAULT_DOMAIN;
    public static final String DEFAULT_EMAIL_DOMAIN_PATTERN = "%05d.example.com";
    public static final String DEFAULT_FIRST_NAME_PATTERN = "%07d";
    public static final String DEFAULT_LAST_NAME_PATTERN = "Test";
    public static final String DEFAULT_EMAIL_ADDRESS_PATTERN = PATTERN_FIRST_NAME + "." + PATTERN_LAST_NAME + "@" + PATTERN_EMAIL_DOMAIN;
    public static final String DEFAULT_USERNAME_PATTERN = PATTERN_EMAIL_ADDRESS;
    public static final String DEFAULT_PASSWORD_PATTERN = PATTERN_EMAIL_ADDRESS;
    
    private UserDataService userDataService;
    private long numberOfUsers;
    private long usersPerDomain;
    private String domainPattern;
    private String eventNameUsersPrepared;
    private String emailDomainPattern;
    private String firstNamePattern;
    private String lastNamePattern;
    private String emailAddressPattern;
    private String usernamePattern;
    private String passwordPattern;
    private boolean assumeCreated;
    
    /**
     * @param userDataService       service for {@link UserData} operations
     * @param numberOfUsers         number of users to create in total
     */
    public PrepareUsers(UserDataService userDataService, long numberOfUsers)
    {
        this.userDataService = userDataService;
        this.numberOfUsers = numberOfUsers;
        this.usersPerDomain = DEFAULT_USERS_PER_DOMAIN;
        this.domainPattern = DEFAULT_DOMAIN_PATTERN;
        this.eventNameUsersPrepared = EVENT_NAME_USERS_PREPARED;
        this.emailDomainPattern = DEFAULT_EMAIL_DOMAIN_PATTERN;
        this.firstNamePattern = DEFAULT_FIRST_NAME_PATTERN;
        this.lastNamePattern = DEFAULT_LAST_NAME_PATTERN;
        this.emailAddressPattern = DEFAULT_EMAIL_ADDRESS_PATTERN;
        this.usernamePattern = DEFAULT_USERNAME_PATTERN;
        this.passwordPattern = DEFAULT_PASSWORD_PATTERN;
        this.assumeCreated = false;
    }

    /**
     * Override the {@link #EVENT_NAME_USERS_PREPARED default} event name when users have been prepared.
     */
    public void setEventNameUsersCreated(String eventNameUsersPrepared)
    {
        this.eventNameUsersPrepared = eventNameUsersPrepared;
    }
    
    /**
     * Override the {@link #DEFAULT_USERS_PER_DOMAIN default} number of users per email domain
     */
    public void setUsersPerDomain(long usersPerDomain)
    {
        this.usersPerDomain = usersPerDomain;
    }

    /**
     * Override the {@link #DEFAULT_DOMAIN_PATTERN default} pattern for user domain names.
     * A numerical domain name counter is <b>not</b> available.  Consider using the
     * {@link #PATTERN_EMAIL_DOMAIN} e.g. <b>[emailDomain]</b>
     * 
     * @param   domainPattern           a pattern e.g. <b>[emailDomain]</b>
     */
    public void setDomainPattern(String domainPattern)
    {
        this.domainPattern = domainPattern;
    }

    /**
     * Override the {@link #DEFAULT_EMAIL_DOMAIN_PATTERN default} pattern for email domains.
     * A numerical domain name counter is available.
     * 
     * @param emailDomainPattern        a pattern that may contain something like <code>%05d</code>
     */
    public void setEmailDomainPattern(String emailDomainPattern)
    {
        this.emailDomainPattern = emailDomainPattern;
    }

    /**
     * Override the {@link #DEFAULT_FIRST_NAME_PATTERN default} pattern for user first names.
     * A numberical user counter is available.
     * 
     * @param firstNamePattern          a pattern that may contain something like <code>%05d</code>
     */
    public void setFirstNamePattern(String firstNamePattern)
    {
        this.firstNamePattern = firstNamePattern;
    }

    /**
     * Override the {@link #DEFAULT_LAST_NAME_PATTERN default} pattern for user last names.
     * A numberical user counter is available.
     * 
     * @param lastNamePattern           a pattern that may contain something like <code>%05d</code>
     */
    public void setLastNamePattern(String lastNamePattern)
    {
        this.lastNamePattern = lastNamePattern;
    }

    /**
     * Override the {@link #DEFAULT_EMAIL_ADDRESS_PATTERN default} pattern for the email address.
     * Values <b>${firstName}</b>, <b>${lastName}</b> and <b>${emailDomain}</b> will be substituted
     * with values generated for the fist name, last name and email domain respectively.
     * 
     * @param emailAddressPattern       a pattern containing the placeholders to change.
     */
    public void setEmailAddressPattern(String emailAddressPattern)
    {
        this.emailAddressPattern = emailAddressPattern;
    }

    /**
     * Override the {@link #DEFAULT_USERNAME_PATTERN default} pattern for the username.
     * Values <b>${firstName}</b>, <b>${lastName}</b>, <b>${emailDomain}</b> and <b>${emailAddress}
     * will be substituted with values generated for the fist name, last name, email domain
     * and email address respectively.
     * 
     * @param usernamePattern           a pattern containing the placeholders to change.
     */
    public void setUsernamePattern(String usernamePattern)
    {
        this.usernamePattern = usernamePattern;
    }

    /**
     * Override the {@link #DEFAULT_PASSWORD_PATTERN default} pattern for the password.
     * Values <b>${firstName}</b>, <b>${lastName}</b>, <b>${emailDomain}</b> and <b>${emailAddress}
     * will be substituted with values generated for the fist name, last name, email domain
     * and email address respectively.
     * 
     * @param passwordPattern           a pattern containing the placeholders to change.
     */
    public void setPasswordPattern(String passwordPattern)
    {
        this.passwordPattern = passwordPattern;
    }

    /**
     * Assume that all users already exist on the server i.e. mark them as created.
     * 
     * @param assumeCreated         <tt>true</tt> to mark users as created immediately
     */
    public void setAssumeCreated(boolean assumeCreated)
    {
        this.assumeCreated = assumeCreated;
    }

    public EventResult processEvent(Event event) throws Exception
    {
        // How many users must we create?
        long userCount = userDataService.countUsers(true);
        long usersToCreate = numberOfUsers - userCount;

        // How many domains must we create?
        long domainsToCreate = (long) (Math.ceil((double)numberOfUsers/(double)usersPerDomain));
        
        long count = 0L;
        // Loop over domains and users and make sure they exist
        for (long i = 0L; i < domainsToCreate; i++)
        {
            // Numerical pattern for email domain
            String emailDomain = emailDomainPattern;
            if (emailDomain.contains("%"))
            {
                emailDomain = String.format(emailDomainPattern, i);
            }
            // Substitutions for email
            String domain = domainPattern;
            domain = domain.replace(PATTERN_EMAIL_DOMAIN, emailDomain);
            // Build users for the domain
            for (int j = 0; count < usersToCreate && j < usersPerDomain; j++)
            {
                // Numerical pattern for first name
                String firstName = firstNamePattern;
                if (firstName.contains("%"))
                {
                    firstName = String.format(firstNamePattern, count);
                }
                // Numerical pattern for last name
                String lastName = lastNamePattern;
                if (lastName.contains("%"))
                {
                    lastName = String.format(lastNamePattern, count);
                }
                // Substitutions for email
                String email = emailAddressPattern;
                email = email.replace(PATTERN_FIRST_NAME, firstName);
                email = email.replace(PATTERN_LAST_NAME, lastName);
                email = email.replace(PATTERN_EMAIL_DOMAIN, emailDomain);
                // Substitutions for username
                String username = usernamePattern;
                username = username.replace(PATTERN_EMAIL_ADDRESS, email);
                username = username.replace(PATTERN_FIRST_NAME, firstName);
                username = username.replace(PATTERN_LAST_NAME, lastName);
                username = username.replace(PATTERN_EMAIL_DOMAIN, emailDomain);
                // Substitutions for password
                String password = passwordPattern;
                password = password.replace(PATTERN_EMAIL_ADDRESS, email);
                password = password.replace(PATTERN_FIRST_NAME, firstName);
                password = password.replace(PATTERN_LAST_NAME, lastName);
                password = password.replace(PATTERN_EMAIL_DOMAIN, emailDomain);
                // Does the user exist (by email)
                UserData user = userDataService.findUserByUsername(username);
                if (user != null)
                {
                    // User already exists
                    continue;
                }
                // Create data
                user = new UserData();
                user.setCreated(false);
                user.setDomain(domain);
                user.setEmail(email);
                user.setFirstName(firstName);
                user.setLastName(lastName);
                user.setNodeId(null);
                user.setPassword(password);
                user.setTicket(null);
                user.setUsername(username);
                // Check if the user must be assumed to exist
                if (assumeCreated)
                {
                    user.setCreated(true);
                }
                // Persist
                userDataService.createNewUser(user);
                count++;
            }
        }
        // Raise an event saying we're done
        String msg = "Created " + count + " users.";
        Event doneEvent = new Event(eventNameUsersPrepared, System.currentTimeMillis(), msg);
        // Done
        EventResult result = new EventResult("", doneEvent);
        return result;
    }
}
