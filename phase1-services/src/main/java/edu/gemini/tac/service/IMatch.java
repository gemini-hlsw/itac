package edu.gemini.tac.service;

/**
 * Matcher class
 *
 * Author: lobrien
 * Date: 1/5/11
 */
public interface IMatch<T> {
    /**
     * Implementations should return true or false on arg, e.g., class IsEven implements Matcher { bool match(Object o) { return ((Integer) o)%2 == 0; }}
     *
     * @param target
     * @return
     */
    boolean match(T arg);
}
