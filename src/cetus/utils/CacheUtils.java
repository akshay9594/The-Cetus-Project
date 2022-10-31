package cetus.utils;

import java.util.List;

import cetus.hir.ArrayAccess;

public class CacheUtils {

    /**
     * Calculate the block size for a group of array accesses based on the cache size passed as a parameter.
     * arSize: required bits for all the array accesses
     * cacheSize: total bits in cache
     * blockSize = arSize/cacheSize
     * @param bitsCacheSize total amount of bits in cache
     * @param arrayAccesses array accesses to calculate the block size
     * @return the block size in bits required for all the array accesses.
     */

    public static final int getRawBlockSize(int bitsCacheSize, List<ArrayAccess> arrayAccesses) {
        int arrayBits = 0;
        for (ArrayAccess arrayAccess : arrayAccesses) {
            arrayBits += ArrayUtils.getTypeSize(arrayAccess);
        }
        return bitsCacheSize/arrayBits;
    }
}
