package com.ifttt.hqlighttts.util.datastructure.sortedintmap;

import java.util.Arrays;

/**
 *
 */
public class SortedIntMap implements SortedIntMapGettable {

    private int[] mKeys;
    private int[] mValues;
    private int mSize;

    /**
     *
     */
    public SortedIntMap() {
        this(8);
    }

    /**
     *
     *
     * @param initialCapacity
     */
    public SortedIntMap(int initialCapacity) {
        mKeys = new int[initialCapacity];
        mValues = new int[initialCapacity];
        mSize = 0;
    }

    /**
     *
     *
     * @param key
     * @param value
     */
    public void put(int key, int value) {
        int index = getIndexForKey(key);
        if (index >= 0) {
            mValues[index] = value;
        } else {
            index = ~index;
            mKeys = insert(mKeys, mSize, index, key);
            mValues = insert(mValues, mSize, index, value);
            mSize++;
        }
    }

    @Override
    public int keyAt(int index) {
        return mKeys[index];
    }

    @Override
    public int valueAt(int index) {
        return mValues[index];
    }

    @Override
    public int get(int key) {
        return doGet(key, 0);
    }

    @Override
    public int get(int key, int valueIfKeyNotFound) {
        return doGet(key, valueIfKeyNotFound);
    }

    @Override
    public int size() {
        return mSize;
    }

    private int doGet(int key, int valueIfKeyNotFound) {
        int index = getIndexForKey(key);
        if (index < 0) {
            return valueIfKeyNotFound;
        }
        return mValues[index];
    }

    private int getIndexForKey(int key) {
        return Arrays.binarySearch(mKeys, 0, mSize, key);
    }

    private static int[] insert(int[] array, int currentSize, int index, int element) {
        if (currentSize > array.length) {
            throw new AssertionError("currentSize > array.length");
        }

        if (currentSize + 1 <= array.length) {
            System.arraycopy(array, index, array, index + 1, currentSize - index);
            array[index] = element;
            return array;
        }

        int[] newArray = new int[growSize(currentSize)];
        System.arraycopy(array, 0, newArray, 0, index);
        newArray[index] = element;
        System.arraycopy(array, index, newArray, index + 1, array.length - index);
        return newArray;
    }

    private static int growSize(int currentSize) {
        return currentSize <= 4 ? 8 : currentSize * 2;
    }
}
