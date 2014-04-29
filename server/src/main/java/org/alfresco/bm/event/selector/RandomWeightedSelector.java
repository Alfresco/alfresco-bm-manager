package org.alfresco.bm.event.selector;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

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
     * @param weight
     * @param result
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
     * @param input         ignored
     * @param response      ignored
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
