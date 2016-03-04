package org.alfresco.bm.site;

import java.util.Random;

/**
 * Site visibility.
 * 
 * @author steveglover
 *
 */
public enum SiteVisibility
{
    PUBLIC, MODERATED, PRIVATE;

    private static Random random = new Random();

    public static String getRandomVisibility()
    {
        return values()[random.nextInt(3)].toString();
    }
}
