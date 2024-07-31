package main;

import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * A class with static member variables and methods that can be used to count multiple stuff.
 * safe.
 *
 *
 */
public class MultiCounter {

    /**
     * Variable holding our counters. We support up to 10 counters
     */
    private static final AtomicIntegerArray counters = new AtomicIntegerArray(10);

    /**
     * Resets the internal counter of counterIndex to zero
     *
     * @return true if operation was successful otherwise false
     */
    public static boolean resetCounter(int counterIndex) {
        if (counterIndex >= 1 && counterIndex-1 < counters.length()) {
            counters.set(counterIndex-1, 0);
            return true;
        }
        return false;
    }

    /**
     * Returns the current count for given counterIndex
     *
     * @return the current count for given counterIndex or -1 if specified index is out of bounds
     */
    public static int getCount(int counterIndex) {
        if (counterIndex >= 1 && counterIndex-1 < counters.length())
            return  counters.get(counterIndex-1);
        return -1;
    }

    /**
     * Increases the current count of counterIndex by 1. Returns always true so that it can be used
     * in boolean statements
     *
     * @return true if operation was successful otherwise false
     */
    public static boolean increaseCounter(int counterIndex) {
        if (counterIndex >= 1 && counterIndex-1 < counters.length()) {
            counters.incrementAndGet(counterIndex-1);
            return true;
        }
        return false;
    }

    /**
     * Increases the current count of counter given by counterIndex by step. Returns always true so that it can be
     * used in boolean statements. Step could be negative. It is up to the specific
     * usage scenario whether this is desirable or not.
     *
     * @param step The amount to increase the counter
     * @return true if operation was successful otherwise false
     */
    public static boolean increaseCounter(int counterIndex, int step) {
        if (counterIndex >= 1 && counterIndex-1 < counters.length()) {
            counters.set(counterIndex - 1, counters.get(counterIndex - 1) + step);
            return true;
        }
        return false;
    }
}
