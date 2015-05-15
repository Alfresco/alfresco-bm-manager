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
package org.alfresco.bm.event;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

/**
 * @author Steve Glover
 * @since 1.3
 */
public class RandomWeightedSelector<T>
{
    private final NavigableMap<Double, T> map = new TreeMap<Double, T>();

    private final Random random;
    private double total = 0;

    public RandomWeightedSelector()
    {
        this.random = new Random(System.currentTimeMillis());
    }

    /**
     * Add an object to the list.
     * 
     * @param weight        any relative weight that will give a larger or smaller chance of selection
     * @param result        the result to return, if chosen
     */
    public void add(double weight, T result)
    {
        if (weight <= 0) return;
        total += weight;
        map.put(total, result);
    }

    /**
     * Chooses randomly from the list of objects based on the weightings provided.
     * 
     * @return              a randomly chosen instance of <tt>null</tt> if none are available
     */
    public T next()
    {
        double value = random.nextDouble() * total;
        Map.Entry<Double, T> entry = map.ceilingEntry(value);
        return (entry != null ? entry.getValue() : null);
    }

    public int size()
    {
        return map != null ? map.size() : 0;
    }
}
