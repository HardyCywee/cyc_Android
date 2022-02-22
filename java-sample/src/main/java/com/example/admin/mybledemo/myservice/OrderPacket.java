package com.example.admin.mybledemo.myservice;

/**
 * Created by dh on 2022/2/18.
 */

public class OrderPacket {

    // 频率设置  25 50 100HZ
    public static byte[] setHz25 = new byte[]{(byte) 0xee, 0x00, 0x02, 0x01, 0x01};
    public static byte[] setHz50 = new byte[]{(byte) 0xee, 0x00, 0x02, 0x01, 0x02};
    public static byte[] setHz100 = new byte[]{(byte) 0xee, 0x00, 0x02, 0x01, 0x03};


    //查询设备信息  返回结果：电量 电池状态  频率   固件版本   算法版本   设备型号
    public static byte[] searchDeviceInfo = new byte[]{(byte) 0xee, 0x01, 0x02, 0x01, 0x00};


}
