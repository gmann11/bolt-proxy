package org.neo4j.field.boltproxy;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

public class Utils {

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    public static byte[] hexToByteArray(String s) throws IllegalArgumentException {
        int len = s.length();
        if (len % 2 == 1) {
            throw new IllegalArgumentException("Hex string must have even number of characters");
        }
        byte[] data = new byte[len / 2]; // Allocate 1 byte per 2 hex characters
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
    
    public static List<byte[]> tokens(byte[] array, byte[] delimiter) {
        List<byte[]> byteArrays = new LinkedList<>();
        if (delimiter.length == 0) {
            return byteArrays;
        }
        int begin = 0;

        outer:
        for (int i = 0; i < array.length - delimiter.length + 1; i++) {
            for (int j = 0; j < delimiter.length; j++) {
                if (array[i + j] != delimiter[j]) {
                    continue outer;
                }
            }
            byteArrays.add(Arrays.copyOfRange(array, begin, i));
            begin = i + delimiter.length;
        }
        byteArrays.add(Arrays.copyOfRange(array, begin, array.length));
        return byteArrays;
    }
    
    public static List<byte[]> messages(byte[] array) {
        List<byte[]> messageArrays = new LinkedList<>();
        int i=0;
        
        while (i < array.length) {
            short len=(short)(((array[i] & 0xFF) << 8) | (array[i+1] & 0xFF));
            i += 2;
            messageArrays.add(Arrays.copyOfRange(array, i, i+len));
            i+=len;
            i += 2; //marker
        }
        return messageArrays;
    }
    
    public static byte[] prune(byte[] bytes) {
        if (bytes.length == 0) return bytes;
        var i = bytes.length - 1;
        while (bytes[i] == 0) {
            i--;
        }
        byte[] copy = Arrays.copyOfRange(bytes,0, i + 1);
        return copy;
    }
    
    public static byte[] len(short x) {
        return new byte[]{(byte)((x>>8)&0xFF),(byte)(x&0xFF)};
    }
    
    // combine with len and eom marker
    public static byte[] combine(List<byte[]> msgs) {
        byte[] ret = null;
        for (byte[] entry : msgs ) {
            short len = (short)entry.length;
            byte[] x = ArrayUtils.addAll(ArrayUtils.addAll(Utils.len(len),entry), new byte[]{0,0});
            ret = ArrayUtils.addAll(ret, x);
        }
        return ret;
    }
}
