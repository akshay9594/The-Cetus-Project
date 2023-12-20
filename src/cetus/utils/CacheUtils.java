package cetus.utils;

import java.util.List;

import cetus.hir.ArrayAccess;
import cetus.hir.Expression;
import cetus.hir.IntegerLiteral;
import cetus.hir.Symbolic;

public class CacheUtils {

    /**
     * Calculate the block size for a group of array accesses based on the cache
     * size passed as a parameter.
     * arSize: required bits for all the array accesses
     * cacheSize: total bits in cache
     * blockSize = arSize/cacheSize
     * 
     * @param bitsCacheSize total amount of bits in cache
     * @param arrayAccesses array accesses to calculate the block size
     * @return the block size in bits required for all the array accesses.
     */

    public static final Expression getRawBlockSize(Expression cacheSizeInBytes, List<ArrayAccess> arrayAccesses) {
        int typeSizeInBits = 0;
        for (ArrayAccess arrayAccess : arrayAccesses) {
            typeSizeInBits = Math.max(typeSizeInBits, ArrayUtils.getTypeSize(arrayAccess));
        }
        IntegerLiteral arrayBytesLiteral = new IntegerLiteral(1);
        if (typeSizeInBits > 8) {
            arrayBytesLiteral = new IntegerLiteral((int) Math.ceil((double) typeSizeInBits / 8));
        }
        return Symbolic.divide(cacheSizeInBytes, arrayBytesLiteral);

    }
}
