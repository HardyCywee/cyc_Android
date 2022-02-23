package com.example.admin.mybledemo.ui;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.admin.mybledemo.R;
import com.example.admin.mybledemo.Utils;
import com.example.admin.mybledemo.adapter.DeviceInfoAdapter;
import com.example.admin.mybledemo.myservice.AdvertiserService;
import com.example.admin.mybledemo.myservice.DataTypeTools;
import com.example.admin.mybledemo.myservice.OrderPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleLog;
import cn.com.heaton.blelibrary.ble.callback.BleConnectCallback;
import cn.com.heaton.blelibrary.ble.callback.BleNotifyCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteCallback;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.utils.ByteUtils;

public class DeviceInfoActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "DeviceInfoActivity";
    public static final String EXTRA_TAG = "device";
    private BleDevice bleDevice;
    private Ble<BleDevice> ble;
    private ActionBar actionBar;
    private RecyclerView recyclerView;
    private DeviceInfoAdapter adapter;
    private List<BluetoothGattService> gattServices;

    public static UUID UUID_SERVER = UUID.fromString("fceefcee-43e6-47b7-9cb0-5fc21d4ae340");
    public static UUID UUID_SERVER_WRITE = UUID.fromString("fceefce2-43e6-47b7-9cb0-5fc21d4ae340");
    private static UUID UUID_SERVER_NOTIFY = UUID.fromString("fceefce1-43e6-47b7-9cb0-5fc21d4ae340");


    private RadioGroup hzSelect_rg;
    private RadioButton hzSelect_rb1,hzSelect_rb2,hzSelect_rb3;

    private TextView batteryNum_tv,batteryState_tv;

    private TextView deviceName_tv, firmwareVersion_tv, calculationVersion_tv;

    private Button showService_tv,showLog_tv,clearLog_tv;

    private TextView text_log;

    private ScrollView log_sv;

    private TextView deviceAddress_tv;




    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deviceinfo);
        initView();
        initData();
    }

    private void initData() {
        ble = Ble.getInstance();
        bleDevice = getIntent().getParcelableExtra(EXTRA_TAG);
        if (bleDevice == null) return;
        ble.connect(bleDevice, connectCallback);

        deviceAddress_tv.setText("mac 地址："+bleDevice.getBleAddress());
    }

    private void initView() {
        actionBar = getSupportActionBar();
        actionBar.setTitle("详情信息");
        actionBar.setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.recyclerView);
        gattServices = new ArrayList<>();
        adapter = new DeviceInfoAdapter(this, gattServices);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.getItemAnimator().setChangeDuration(300);
        recyclerView.getItemAnimator().setMoveDuration(300);
        recyclerView.setAdapter(adapter);

        hzSelect_rg = findViewById(R.id.hzSelect_rg);
        hzSelect_rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                Log.i("ttt", i + "----i---");  // i 值为 1 2 3
                byte[] hzData = new byte[4];

                switch (i) {
                    case R.id.hzSelect_rb1:
                        setHz=25;
                        hzData=OrderPacket.setHz25;

                        break;
                    case R.id.hzSelect_rb2:
                        setHz=50;
                        hzData=OrderPacket.setHz50;

                        break;
                    case R.id.hzSelect_rb3:
                        setHz=100;
                        hzData=OrderPacket.setHz100;

                        break;
                }
                //if (canSetHz){
                    if (currentHz!=setHz){   //需要设置
                        searchDeviceInfo(hzData);
                    }
                //}


            }
        });

        batteryNum_tv=findViewById(R.id.batteryNum_tv);
        batteryState_tv=findViewById(R.id.batteryState_tv);

        deviceName_tv=findViewById(R.id.deviceName_tv);
        firmwareVersion_tv=findViewById(R.id.firmwareVersion_tv);
        calculationVersion_tv=findViewById(R.id.calculationVersion_tv);

        hzSelect_rb1=findViewById(R.id.hzSelect_rb1);
        hzSelect_rb2=findViewById(R.id.hzSelect_rb2);
        hzSelect_rb3=findViewById(R.id.hzSelect_rb3);

        showService_tv=findViewById(R.id.showService_tv);
        showService_tv.setOnClickListener(this);
        showLog_tv=findViewById(R.id.showLog_tv);
        showLog_tv.setOnClickListener(this);

        text_log=findViewById(R.id.text_log);
        log_sv=findViewById(R.id.log_sv);

        text_log.setMovementMethod(ScrollingMovementMethod.getInstance());

        deviceAddress_tv=findViewById(R.id.deviceAddress_tv);


        clearLog_tv=findViewById(R.id.clearLog_tv);
        clearLog_tv.setOnClickListener(this);


    }

    private void searchDeviceInfo(byte[] order) {


        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                text_log.append("app-->d:"+ByteUtils.toHexString(order)+" "+Utils.printTime()+"\n\n");
                log_sv.post(new Runnable() {
                    @Override
                    public void run() {
                        log_sv.smoothScrollTo(0, text_log.getBottom());
                    }
                });
            }
        });

        ble.writeByUuid(bleDevice, order, UUID_SERVER, UUID_SERVER_WRITE, new BleWriteCallback<BleDevice>() {
            @Override
            public void onWriteSuccess(BleDevice device, BluetoothGattCharacteristic characteristic) {
                Log.i("enableNotify", characteristic.getUuid() + "---成功写入通知结果"+characteristic.getValue());

            }
        });
    }

    private BleConnectCallback<BleDevice> connectCallback = new BleConnectCallback<BleDevice>() {
        @Override
        public void onConnectionChanged(BleDevice device) {
            Log.e(TAG, "onConnectionChanged: " + device.getConnectionState() + Thread.currentThread().getName());
            if (device.isConnected()) {
                actionBar.setSubtitle("已连接,时间已同步");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Utils.showToast("连接成功，时间已同步");
                    }
                });



            } else if (device.isConnecting()) {
                actionBar.setSubtitle("连接中...");
            } else if (device.isDisconnected()) {
                actionBar.setSubtitle("断开，连接中...");
                gattServices.clear();
                adapter.notifyDataSetChanged();
                //ble.connect(bleDevice, connectCallback);

                handler.postDelayed(runnable, 3000);//1秒执行一次runnable.
            }
        }

        @Override
        public void onConnectFailed(BleDevice device, int errorCode) {
            super.onConnectFailed(device, errorCode);
            Utils.showToast("连接异常，异常状态码:" + errorCode);
        }

        @Override
        public void onConnectCancel(BleDevice device) {
            super.onConnectCancel(device);
            Log.e(TAG, "onConnectCancel: " + device.getBleName());
        }

        @Override
        public void onServicesDiscovered(BleDevice device, BluetoothGatt gatt) {
            super.onServicesDiscovered(device, gatt);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    gattServices.addAll(gatt.getServices());
                    adapter.notifyDataSetChanged();
                }
            });


        }

        @Override
        public void onReady(BleDevice device) {
            super.onReady(device);
            //连接成功后，设置通知
            ble.enableNotify(device, true, new BleNotifyCallback<BleDevice>() {
                @Override
                public void onChanged(BleDevice device, BluetoothGattCharacteristic characteristic) {
                    UUID uuid = characteristic.getUuid();
                    BleLog.e(TAG, "onChanged==uuid:" + uuid.toString());
                    BleLog.e(TAG, "onChanged==data:" + ByteUtils.toHexString(characteristic.getValue()));



                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            text_log.append("d-->app:"+ByteUtils.toHexString(characteristic.getValue())+" "+Utils.printTime()+"\n\n");

                            log_sv.post(new Runnable() {
                                @Override
                                public void run() {
                                    log_sv.smoothScrollTo(0, text_log.getBottom());
                                }
                            });

                            byte [] resultBytes=characteristic.getValue();
                            switch (resultBytes[1]){
                                case 0x00:  // 频率设置结果
                                    if (resultBytes.length>5){
                                        if (resultBytes[5]==0x00){
                                            Utils.showToast(String.format("频率设置成功,设备通知数据: %s", ByteUtils.toHexString(characteristic.getValue())));

                                            currentHz=setHz;

                                            if (currentHz==25){
                                                hzSelect_rb1.setChecked(true);
                                            }else if (currentHz==50){
                                                hzSelect_rb2.setChecked(true);
                                            }else if (currentHz==100){
                                                hzSelect_rb3.setChecked(true);
                                            }

                                            //canSetHz=true;

                                        }else if (resultBytes[5]==0x01){
                                            Utils.showToast("频率设失败，请稍后再试.");

                                            if (currentHz==25){
                                                hzSelect_rb1.setChecked(true);
                                            }else if (currentHz==50){
                                                hzSelect_rb2.setChecked(true);
                                            }else if (currentHz==100){
                                                hzSelect_rb3.setChecked(true);
                                            }

                                            //canSetHz=false;


                                        }else if(resultBytes[5]==0x02) {
                                            Utils.showToast("该频率设置非法.");
                                        }
                                    }else {
                                        Utils.showToast("数据返回错误.");
                                    }
                                    break;
                                case 0x01:   // 设备信息查询
                                    if(resultBytes[5]==0x00){

                                        if (resultBytes[4]==0x03){  // 电池电量同步

                                            try{
                                                Log.e("tttt",resultBytes.length+"---");
                                                batteryNum_tv.setText("电量："+resultBytes[7]+"%");
                                                if (resultBytes[8]==0x00){
                                                    batteryState_tv.setText("充电状态：充电中");
                                                }else {
                                                    batteryState_tv.setText("充电状态：未充电");
                                                }
                                            }catch (Exception e){
                                                Log.e("tttt",resultBytes.length+"---");

                                            }

                                        }else {
                                            batteryNum_tv.setText("电量："+resultBytes[7]+"%");
                                            if (resultBytes[8]==0x00){
                                                batteryState_tv.setText("充电状态：充电中");
                                            }else {
                                                batteryState_tv.setText("充电状态：未充电");
                                            }
                                            Log.i("频率",resultBytes[9]+"---");
                                            if (resultBytes[9]==25){
                                                hzSelect_rb1.setChecked(true);
                                            }else if(resultBytes[9]==50){
                                                hzSelect_rb2.setChecked(true);
                                            }else if (resultBytes[9]==100){
                                                hzSelect_rb3.setChecked(true);
                                            }

                                            currentHz=resultBytes[9];

                                            byte[] bytes=DataTypeTools.bytesSplit(resultBytes,10);
                                            Log.i("固件版本",DataTypeTools.byte2Str(bytes)+"---字符串");
                                            String version=DataTypeTools.byte2Str(bytes);
                                            String []  tmp=version.split(",");
                                            if (tmp.length>1){
                                                String [] tmp1=tmp[0].split("\\.");
                                                calculationVersion_tv.setText("算法版本："+tmp[1]);
                                                firmwareVersion_tv.setText("固件版本："+tmp[0].replace(tmp1[0]+".",""));
                                                deviceName_tv.setText("设备型号："+tmp1[0]);
                                            }
                                        }



                                    }



                                    break;

                            }


                        }
                    });
                }

                @Override
                public void onNotifySuccess(BleDevice device) {
                    super.onNotifySuccess(device);
                    BleLog.e(TAG, "onNotifySuccess: " + device.getBleName());

                    searchDeviceInfo(OrderPacket.searchDeviceInfo);

                }
            });
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:// 点击返回图标事件
                this.finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    final Handler handler=new Handler();
    final Runnable runnable=new Runnable() {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            //要做的事情
            Log.i("ttt","断开了");
            handler.postDelayed(this,3000);
            if (bleDevice.isDisconnected()){
                ble.autoConnect(bleDevice,true);
            }
        }
    };




    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bleDevice != null) {
            if (bleDevice.isConnecting()) {
                ble.cancelConnecting(bleDevice);
            } else if (bleDevice.isConnected()) {
                ble.disconnect(bleDevice);
            }
        }

        stopService(new Intent(this,AdvertiserService.class));


        ble.cancelCallback(connectCallback);
        finish();
    }

    private boolean serviceIsShow=false;

    private boolean logIsShow=false;

    private int currentHz;
    private int setHz=0;

    private boolean canSetHz=true;


    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.showService_tv:
                if (serviceIsShow){
                    showService_tv.setText("显示服务列表");
                    recyclerView.setVisibility(View.GONE);
                    serviceIsShow=false;
                }else {
                    showService_tv.setText("隐藏服务列表");
                    recyclerView.setVisibility(View.VISIBLE);
                    serviceIsShow=true;
                }
                break;
            case R.id.showLog_tv:
                if (logIsShow){
                    showLog_tv.setText("显示日志列表");
                    log_sv.setVisibility(View.GONE);
                    logIsShow=false;
                }else {
                    showLog_tv.setText("隐藏日志列表");
                    log_sv.setVisibility(View.VISIBLE);
                    logIsShow=true;
                }
                break;

            case R.id.clearLog_tv:
                text_log.setText("");
                break;
        }
    }
}
