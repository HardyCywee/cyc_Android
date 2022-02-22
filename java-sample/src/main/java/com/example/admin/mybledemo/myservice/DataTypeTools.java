package com.example.admin.mybledemo.myservice;

/**
 * Created by dh on 2022/2/18.
 */

public class DataTypeTools {

    public static String byte2Str(byte [] aa){
        return new String(aa);
    }

    public static byte[] str2Byte(String aa){
        return aa.getBytes();
    }

    public static byte[] bytesSplit(byte[] src,int startPosition){
        byte[] dest = new byte[src.length-startPosition];
        System.arraycopy(src, startPosition, dest, 0, src.length-startPosition);
        return dest;
    }

    // 合并
    public static byte[] byteMerger(byte[] byte_1, byte[] byte_2){
        byte[] byte_3 = new byte[byte_1.length+byte_2.length];
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
        return byte_3;
    }

}
