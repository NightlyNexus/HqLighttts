package com.ifttt.hqlighttts.util.datastructure.sortedintmap;

/**
 *
 */
public interface SortedIntMapGettable {

    /**
     *
     *
     * @param index
     * @return
     */
    int keyAt(int index);

    /**
     *
     *
     * @param index
     * @return
     */
    int valueAt(int index);

    /**
     *
     *
     * @param key
     * @return
     */
    int get(int key);

    /**
     *
     *
     * @param key
     * @param valueIfKeyNotFound
     * @return
     */
    int get(int key, int valueIfKeyNotFound);

    /**
     *
     *
     * @return
     */
    int size();
}
