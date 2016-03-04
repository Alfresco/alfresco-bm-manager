package org.alfresco.bm.site;

import java.util.Random;

/**
 * Site membership role based on site type.
 * 
 * @author steveglover
 */
public enum SiteRole
{
    SiteManager(),
    SiteCollaborator(),
    SiteContributor(),
    SiteConsumer();
    
    private static Random random = new Random();
    public static SiteRole getRandomRole()
    {
        return values()[random.nextInt(4)];
    }
}
