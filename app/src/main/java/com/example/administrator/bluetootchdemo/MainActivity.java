package com.example.administrator.bluetootchdemo;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.administrator.bluetootchdemo.utils.AppUtils;
import com.example.administrator.bluetootchdemo.utils.ConstantUtil;
import com.example.administrator.bluetootchdemo.view.EcgView;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 主界面
 */

public class MainActivity extends Activity implements OnClickListener {
    private final String TAG = MainActivity.class.getSimpleName();
    private int dsType = 0;

    private final float SUM_QUANTITY = 65535f;
    private final float DS_VOLTAGE = 3.3f;
    private final float DS_MULTIPLE = 0.0275f;
    private final float averageValue = 24968;

    private final static int REQUEST_CONNECT_DEVICE = 1;
    private final static String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB"; // UUID
    private String BinaryString = "00000000";
    protected static final int REQUEST_ENABLE = 0;
    private String inputString = "";
    private EcgView ecgView;
    private InputStream is; // i
    private static boolean readType = false;
    private List<Byte> oneList = new LinkedList<>();
    BluetoothDevice _device = null;  // bl device
    BluetoothSocket _socket = null;   // bl socket
    private int isTest = 0;  // 0.不在测试中 == false  1. 在测试中(求基数) == true  2.加压中
    boolean bRun = true;
    boolean bThread = false;
    boolean hex = true;
    private StringBuffer currentSB = new StringBuffer();  // 默认大小16
    public final static String FILE_NAME = "DS_Backup.txt"; // 设置文件的名称
    public final static String KEY_FILE_NAME = "DS_KeyBackup.txt"; // 设置文件的名称
    private BluetoothAdapter _bluetooth = BluetoothAdapter.getDefaultAdapter();
    private Button btnadd;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> lvAdapter;
    private String s = "";
    private StringBuilder byteString = new StringBuilder(); // 判断文字
    // ECG data
    private Queue<Byte> byteQueue = new LinkedBlockingQueue<Byte>();
    private List<Byte> CarrierList = new LinkedList<>();
    private float[] averages = new float[1000];
    private int currentAverage = -1;
    private float average = -1;
    private AppUtils appUtils;
    private int currentPressure = 0;
    private int readNums = -1;
    private ProgressDialog progressDialog;
    private TextView tvState;
    private float ecgGap;   // 峰峰值
    private float maxEcg, minEcg;  // 最大值 和最小值
    private float[] ecgGaps = new float[10];  // 一秒内取一次
    private List<Float> stageGaps = new LinkedList<>();  // 每段加压 峰峰值 数组
    private float[] ecgOnes = new float[10000];  // 单次 加压数据存放位置
    private String idealPressure = "";  // 理想加压位置
    private boolean isIdeal = false;  // 是否 在采集 30s数据

    Handler handler = new Handler() {
        public void handleMessage(Message msg) {  // byteString
            super.handleMessage(msg);
            switch (msg.what) {
                case 1: // 显示消息弹框
                    if (!progressDialog.isShowing()) {
                        progressDialog.show();
                    }
                    progressDialog.setMessage(msg.obj.toString());
                    break;
                case 2:  // 隐藏消息框
                    if (progressDialog.isShowing()) {
                        progressDialog.setMessage("");
                        progressDialog.dismiss();
                    }
                    break;
                case 3:
                    appUtils.error(msg.obj.toString() + "长度");
                    break;
                case 4:
                    lvAdapter.add(msg.obj.toString());
                    break;
                case 5:
                    tvState.setText(msg.obj.toString());
                    break;
            }
            if (s.length() != 0)
                appUtils.error("遗漏数据：" + s);
        }
    };
    private ListView lv;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
//        Runtime rt=Runtime.getRuntime();
//        long maxMemory=rt.maxMemory();
//        appUtils.error("最大运行内存"+Long.toString(maxMemory/(1024*1024)));
        // 判断权限 如果 大于 6.0 开启定位权限  并且最好关闭 wifi
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 10);
            }
        }
        if (_bluetooth == null) {
            appUtils.ToastString(this, "该设备不支持蓝牙");
            finish();
            return;
        }
        if (!_bluetooth.isEnabled()) {
            appUtils.ToastString(MainActivity.this, "蓝牙启动");
            new Thread() {
                public void run() {
                    if (!_bluetooth.isEnabled()) {
                        _bluetooth.enable();
                    }
                }
            }.start();
        }

        if (!_bluetooth.isEnabled()) {
            appUtils.ToastString(MainActivity.this, "打开蓝牙中");
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!_bluetooth.isEnabled()) {
                        Intent enabler = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enabler, REQUEST_ENABLE);
                    } else {
                        connect();
                    }
                }
            }, 5000);
        } else {
            connect();
        }
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                disconnect();
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getExtras().getString(
                            DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // TODO: 2017/10/20 可以放在线程中进行蓝牙的连接和socket 获取
                    _device = _bluetooth.getRemoteDevice(address);
                    try {
                        Method m = _device.getClass().getMethod("createRfcommSocket", int.class);
                        _socket = (BluetoothSocket) m.invoke(_device, 1);
                    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    try {
                        _socket.connect();
                        Toast.makeText(this, "连接 " + _device.getName() + "成功",
                                Toast.LENGTH_SHORT).show();
                        mPairedDevicesArrayAdapter.add(_device.getName() + "         "
                                + _device.getAddress());
                        SharedPreferences.Editor sharedata = getSharedPreferences("Add", 0).edit();
                        sharedata.putString(String.valueOf(0), _device.getName());
                        sharedata.putString(String.valueOf(1), _device.getAddress());
                        sharedata.apply();
                        btnadd.setText(getResources().getString(R.string.delete));
                        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
                        this.registerReceiver(mReceiver, filter);
                    } catch (IOException e) {
                        btnadd.setText(getResources().getString(R.string.add));
                        try {
                            Toast.makeText(this, "connection" + _device.getName() + " err ：" + e, Toast.LENGTH_SHORT).show();
                            _socket.close();
                            _socket = null;
                        } catch (IOException ignored) {
                        }
                        return;
                    }

                    try {
                        is = _socket.getInputStream();
                    } catch (IOException e) {
                        Toast.makeText(this, "Get is err" + e, Toast.LENGTH_SHORT).show();
                        btnadd.setText(getResources().getString(R.string.add));
                        return;
                    }
                    if (!bThread) {
                        {
                            ReadThread.start();
                            bThread = true;
                        }
                    } else {
                        bRun = true;
                    }
                }
                break;
            default:
                break;
        }
    }

    Thread ReadThread = new Thread() {

        public void run() {
            bRun = true;
            try {
                while (true) {
                    if (readType) {   // 如果是 多数据
                        int count = 0;
                        while (count == 0) {
                            count = is.available();
                        }
                        byte[] buffer = new byte[count];
                        is.read(buffer);
                        for (byte b : buffer) {
                            byteQueue.offer(b);
                        }
                        for (int i = 0; i < byteQueue.size(); i++) {
                            Byte byteOne = byteQueue.poll();
                            CarrierList.add(byteOne);
                            if (String.format("%02X ", byteOne).equals("FE ")) {// 如果出错，后边的数据都会错误
                                if (CarrierList.size() == 6 && String.format("%02X ", CarrierList.get(0)).equals("A0 ")
                                        && String.format("%02X ", CarrierList.get(5)).equals("FE ")) {
                                    int high = CarrierList.get(3) & 0xff;
                                    int low = CarrierList.get(4) & 0xff;
                                    int staticHigh = CarrierList.get(1) & 0xff;
                                    int staticLow = CarrierList.get(2) & 0xff;
                                    CarrierList.clear();
                                    float dd = (high << 8) | low;
                                    if (isTest == 1) {
                                        currentAverage++;
                                        averages[currentAverage] = dd;
                                        if (999 == currentAverage) {
                                            average = average(averages);
                                            sendString(ConstantUtil.STOP_COLLECTING, true);
                                            averages = new float[1000];
                                            currentAverage = -1;
                                        }
                                    } else if (isTest == 2) {
                                        if (average != -1) {
                                            dd = ((dd - average) / SUM_QUANTITY * DS_VOLTAGE) / DS_MULTIPLE;
                                            EcgView.addEcgData0(dd);
                                            readNums++;
                                            if (!isIdeal) {
                                                ecgOnes[readNums] = dd;
                                            }
                                            String spHigt = Integer.toBinaryString(high);
                                            String spLow = Integer.toBinaryString(low);
                                            spHigt = BinaryString.substring(0, 8 - spHigt.length()) + spHigt;
                                            spLow = BinaryString.substring(0, 8 - spLow.length()) + spLow;
                                            currentSB.append(spHigt).append(spLow).append(",");
                                            if (isIdeal) {
                                                if (readNums == 30000) {  // 采取30S
                                                    sendString(ConstantUtil.STOP_COLLECTING, true);
                                                }
                                            } else {
                                                if (readNums == 8000) {  // 模拟运行了8s (7999)
                                                    minEcg = maxEcg = ecgOnes[0];
                                                    for (int j = 0; j < ecgOnes.length; j++) {
                                                        if (ecgOnes[i] > maxEcg)   // 判断最大值
                                                            maxEcg = ecgOnes[i];
                                                        if (ecgOnes[i] < minEcg)   // 判断最小值
                                                            minEcg = ecgOnes[i];
                                                        if (j != 0 && j % 1000 == 0) {
                                                            ecgGap = maxEcg - minEcg;
                                                            ecgGaps[j / 1000] = ecgGap;
                                                        }
                                                        if (j == ecgOnes.length - 1) {
                                                            stageGaps.add(average(ecgGaps));
                                                        }
                                                    }
                                                    ecgOnes = new float[10000];
                                                    sendString(ConstantUtil.STOP_COLLECTING, true);
                                                }
                                            }
                                        }
                                    }
                                } else if (CarrierList.size() < 6) {  // 个数小于6  代表 FE 为中间数据
                                    if (String.format("%02X ", CarrierList.get(0)).equals("A0 ")) {  // 包头正常

                                    } else {  // 包头 不正常
                                        for (byte by : CarrierList) {
                                            s += String.format("%02X ", by);
                                        }
                                        CarrierList.clear();
                                    }
                                } else {
                                    for (byte by : CarrierList) {
                                        s += String.format("%02X ", by);
                                    }
                                    CarrierList.clear();
                                }
                            }
                        }
                    } else {   // 如果是短 数据
                        int count = 0;
                        while (count == 0) {
                            count = is.available();
                        }
                        byte[] buffer = new byte[count];
                        is.read(buffer);
                        for (byte b : buffer) {
                            oneList.add(b);
                            byteString.append(String.format("%02X ", b).replaceAll(" ", ""));
                        }
                        if (dsType == ConstantUtil.TYPE_DETECTION_DS && oneList.size() >= 6) {  // 判断电机位置
                            appUtils.error("判断位置：" + Arrays.toString(buffer));
                            if (byteString.toString().contains(ConstantUtil.MOTOR_LIMIT)) {
                                oneList.clear();
                                addOneLists(oneList, (byte) 1);
                                analyzeData(oneList);
                                oneList.clear();
                            } else if (byteString.toString().contains(ConstantUtil.MOTOR_START)) {
                                oneList.clear();
                                addOneLists(oneList, (byte) 2);
                                analyzeData(oneList);
                                oneList.clear();
                            } else if (byteString.toString().contains(ConstantUtil.MOTOR_MIDDLE)) {
                                oneList.clear();
                                addOneLists(oneList, (byte) 3);
                                analyzeData(oneList);
                                oneList.clear();
                            }
                        } else if (dsType == ConstantUtil.TYPE_PRESSURIZED || dsType == ConstantUtil.TYPE_STOP_COLLECTING) {
                            if (oneList.size() >= 6 && byteString.toString().contains(ConstantUtil.END_LOGO)) {
                                analyzeData(oneList);
                                oneList.clear();
                            }
                        } else if (oneList.size() >= 6 && dsType == ConstantUtil.TYPE_STARTING_POINT) {  // 恢复到起始位
                            if (oneList.size() >= 6 && byteString.toString().contains(ConstantUtil.END_LOGO)) {
                                analyzeData(oneList);
                                oneList.clear();
                            }
                        } else if (oneList.size() == 6 && String.format("%02X", oneList.get(0)).equals("A0")
                                && String.format("%02X", oneList.get(5)).equals("FE") && dsType != 0) {  // 不是在加压中 都是短数据
                            analyzeData(oneList);
                            oneList.clear();
                        } else if (dsType == 0) {
                            oneList.clear();
                        }
                    }
                }
            } catch (IOException ignored) {
            }
        }
    };

    // 分析数据
    private void analyzeData(List<Byte> oneList) {
        switch (dsType) {
            case ConstantUtil.TYPE_DETECTION_DS:  // 检测电机是否在 初始位置
                if (String.format("%02X ", oneList.get(1)).equals("05 ")) {
                    if (String.format("%02X ", oneList.get(2)).equals("01 ")
                            || String.format("%02X ", oneList.get(2)).equals("03 ")) {  // 不在初始位置发送指令移动到初始位置
                        sendString(ConstantUtil.STARTING_POINT, true);
                        sendMessage2Handler(1, "回到起始位置...", handler);
                    } else if (String.format("%02X ", oneList.get(2)).equals("02 ")) {  // 封闭 开关
                        sendString(ConstantUtil.CLOSED_OPEN, true);
                    }
                }
                break;
            case ConstantUtil.TYPE_CLOSED_OPEN: // 封闭 打开
                if (String.format("%02X ", oneList.get(1)).equals("06 ")) {
                    if(isIdeal){  // 打开后加压
                        sendMessage2Handler(1, "加压中...", handler);
                        SystemClock.sleep(3000);  // 睡眠
                        sendString(idealPressure, true);
                    }else{
                        sendMessage2Handler(1, "电机处理中...", handler);
                        SystemClock.sleep(3000);  // 睡眠
                        sendString(ConstantUtil.CLOSED_SHUT, true);
                    }
                }
                break;
            case ConstantUtil.TYPE_CLOSED_SHUT:  // 封闭关闭
                SystemClock.sleep(3000);  // 睡眠
                if (isIdeal) {  // 30s 数据采集
                    readNums = -1;
                    sendMessage2Handler(2, "", handler);
                    sendString(ConstantUtil.CONTINUOUS_ACQUISITION, true);
                    EcgView.isRead = false;
                    ecgView.startThread();
                } else {
                    if (isTest == 0) {  // 还没有进入 七段加压阶段
                        isTest = 1;
                        sendMessage2Handler(1, "电机处理中...", handler);
                        sendString(ConstantUtil.CONTINUOUS_ACQUISITION, true);
                    } else if (isTest == 2) {
                        currentSB.setLength(0);
                        sendMessage2Handler(2, "", handler);
                        switch (currentPressure) {
                            case 0:  // 第一段加压
                                sendMsg(ConstantUtil.PRESSURE_100);
                                break;
                            case 100:  // 第2段加压
                                sendMsg(ConstantUtil.PRESSURE_125);
                                break;
                            case 125:  // 第3段加压
                                sendMsg(ConstantUtil.PRESSURE_150);
                                break;
                            case 150:  // 第4段加压
                                sendMsg(ConstantUtil.PRESSURE_175);
                                break;
                            case 175:  // 第5段加压
                                sendMsg(ConstantUtil.PRESSURE_200);
                                break;
                            case 200:  // 第6段加压
                                sendMsg(ConstantUtil.PRESSURE_225);
                                break;
                            case 225:  // 第7段加压
                                sendMsg(ConstantUtil.PRESSURE_250);
                                break;
                        }
                        break;
                    }
                }
                break;
            case ConstantUtil.TYPE_STOP_COLLECTING:  // 关闭采集
                EcgView.isRead = false;
                ecgView.stopThread();
                if (isIdeal) {
                    isIdeal = false;
                } else {
                    if (isTest == 1 && average != 0) {  // 测试完毕 已求出平均值
                        sendMessage2Handler(1, "求基数完成，加压中...", handler);
                        sendString(ConstantUtil.PRESSURIZED_1, true);  // 加压到自己想要的位置
                    } else {  // 开始加压
                        if (currentPressure == ConstantUtil.PRESSURE_100) {
                            sendString(ConstantUtil.PRESSURIZED_2, true);
                        } else if (currentPressure == ConstantUtil.PRESSURE_125) {
                            sendString(ConstantUtil.PRESSURIZED_3, true);
                        } else if (currentPressure == ConstantUtil.PRESSURE_150) {
                            sendString(ConstantUtil.PRESSURIZED_4, true);
                        } else if (currentPressure == ConstantUtil.PRESSURE_175) {
                            sendString(ConstantUtil.PRESSURIZED_5, true);
                        } else if (currentPressure == ConstantUtil.PRESSURE_200) {
                            sendString(ConstantUtil.PRESSURIZED_6, true);
                        } else if (currentPressure == ConstantUtil.PRESSURE_225) {
                            sendString(ConstantUtil.PRESSURIZED_7, true);
                        } else if (currentPressure == ConstantUtil.PRESSURE_250) {  // 七段加压全部走完 分析数据 采取幅度大 30a
                            sendString(ConstantUtil.STOP_COLLECTING, false);
                            float max = Collections.max(stageGaps);
                            int i = stageGaps.indexOf(max);
                            switch (i) {
                                case 0:
                                    idealPressure = ConstantUtil.PRESSURIZED_1;
                                    break;
                                case 1:
                                    idealPressure = ConstantUtil.PRESSURIZED_2;
                                    break;
                                case 2:
                                    idealPressure = ConstantUtil.PRESSURIZED_3;
                                    break;
                                case 3:
                                    idealPressure = ConstantUtil.PRESSURIZED_4;
                                    break;
                                case 4:
                                    idealPressure = ConstantUtil.PRESSURIZED_5;
                                    break;
                                case 5:
                                    idealPressure = ConstantUtil.PRESSURIZED_6;
                                    break;
                                case 6:
                                    idealPressure = ConstantUtil.PRESSURIZED_7;
                                    break;
                            }
                            sendString(ConstantUtil.STOP_COLLECTING, false);
                            isTest = 2;
                            currentSB.setLength(0);  // 清空之前保存的数据
                            isIdeal = true;
//                            sendMessage2Handler(1, "获取电机状态及第"+(i+1)+"段压力系数...", handler);
                            sendString(ConstantUtil.DETECTION_DS, true);
                        }
                    }
                }
                break;
            case ConstantUtil.TYPE_PRESSURIZED:  // 加压到理想位置 封闭打开
                if (isIdeal) {  // 加压到理想位置  关闭封闭
                    sendMessage2Handler(1, "电机处理中...", handler);
                    SystemClock.sleep(3000);  // 睡眠
                    sendString(ConstantUtil.CLOSED_SHUT, true);
                } else {
                    isTest = 2;
                    sendString(ConstantUtil.CLOSED_OPEN, true);
                }
                break;
            case ConstantUtil.TYPE_STARTING_POINT:  // 回到起始位置
                sendString(ConstantUtil.DETECTION_DS, true);
//                sendString(ConstantUtil.CLOSED_OPEN, true);
                break;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            /**
             * 1. 检测是否在初始位 （如果不在移动到初始位） -->  打开封闭，关闭封闭
             * 2. 采1s数据，求出平均值。（基数）
             * 3. 电机 七段加压
             */
            case R.id.button_acquistion:  // 连接
                sendString(ConstantUtil.STOP_COLLECTING, false);
                if (!progressDialog.isShowing()) {
                    progressDialog.setMessage("连接中，如卡死请返回重试");
                    progressDialog.show();
                }
                isTest = 0;
                average = 0;
                isIdeal = false;
                currentSB.setLength(0);  // 清空之前保存的数据
                currentPressure = 0;  // 重置加压
                sendString(ConstantUtil.DETECTION_DS, true);
                break;
            case R.id.button_stop:  // 暂停
                sendString(ConstantUtil.STOP_COLLECTING, false);
                if (EcgView.isRunning) {
                    EcgView.isRead = false;
                    ecgView.stopThread();
                }
                sendString(ConstantUtil.STARTING_POINT, false);
                break;
            case R.id.button_limit:  // 极限位置
                EcgView.isRead = false;
                sendString(ConstantUtil.LIMIT_POINT, false);
                break;
            case R.id.button_start:  // 开始位置
                EcgView.isRead = false;
                sendString(ConstantUtil.STARTING_POINT, false);
                break;
            case R.id.add:  // 连接
                connect();
                break;
            case R.id.button_save:  // 文件保存
                if (currentSB.length() != 0 && !Float.toString(average).equals("0")) {
                    if (existsFile(FILE_NAME)) {
                        deleteDir(getFilesDir().getPath());
                    }
                    saveFile(currentSB.toString(), FILE_NAME);
                    saveFile(Float.toString(average), KEY_FILE_NAME);
                } else {
                    appUtils.ToastString(MainActivity.this, "请采取新的波段进行保存");
                }
                break;
            case R.id.button_read:  // 读取
                // 测试代码
                InputStream is = null;
                try {
                    is = getAssets().open("text.txt");
                    int lenght = is.available();
                    byte[]  buffer = new byte[lenght];
                    is.read(buffer);
                    String result = new String(buffer, "utf8").replaceAll(" ","");
                    String[] reads = result.split(",");
                    for (String readBean : reads) {  // Float.parseFloat(readBean)
                        int readHigt = Integer.parseInt(readBean.substring(0, 8), 2);
                        int readlow = Integer.parseInt(readBean.substring(8, 16), 2);
                        float readdd = (readHigt << 8) | readlow;
                        readdd =  (((readdd - averageValue) / SUM_QUANTITY * DS_VOLTAGE) / DS_MULTIPLE);
                        EcgView.addEcgData0(readdd);
                    }
                    ecgView.startThread();
                } catch (IOException e) {
                    e.printStackTrace();
                }

//                if (existsFile(FILE_NAME)) {
//                    if (EcgView.isRunning) {  // 暂停脉象图的刷新
//                        sendString(ConstantUtil.STOP_COLLECTING, false);
//                        ecgView.stopThread();
//                    }
//                    EcgView.clearEcgData0();
//                    EcgView.isRead = true;
//                    appUtils.ToastString(MainActivity.this, "读取数据中...");
//                    StringBuffer readSB = readFile(MainActivity.this, FILE_NAME);
//                    appUtils.error(readSB.length() + "长度");
//                    String[] reads = readSB.toString().split(",");
//                    appUtils.error(Arrays.toString(reads));
//                    for (String readBean : reads) {  // Float.parseFloat(readBean)
//                        int readHigt = Integer.parseInt(readBean.substring(0, 8), 2);
//                        int readlow = Integer.parseInt(readBean.substring(8, 16), 2);
//                        float readdd = (readHigt << 8) | readlow;
//                        readdd = (float) (((readdd - averageValue) / SUM_QUANTITY * DS_VOLTAGE) / DS_MULTIPLE);
//                        EcgView.addEcgData0(readdd);
//                    }
//                    ecgView.startThread();
//                } else {
//                    appUtils.ToastString(MainActivity.this, "请先进行保存");
//                }
                break;
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (EcgView.isRunning) {
            sendString(ConstantUtil.STOP_COLLECTING, false);
            EcgView.isRead = false;
            ecgView.stopThread();
        }
        if (_socket != null)
            try {
                _socket.close();
            } catch (IOException e) {
            }
        Process.killProcess(Process.myPid());
    }

    /**
     * 初始化 控件
     */
    private void initView() {
        appUtils = AppUtils.getInstance();
        lv = findViewById(R.id.lv_content);
        Button btnAcquistion = findViewById(R.id.button_acquistion);
        Button btnStop = findViewById(R.id.button_stop);
        Button btnStart = findViewById(R.id.button_start);
        Button btnLimit = findViewById(R.id.button_limit);
        Button btnSave = findViewById(R.id.button_save);
        btnadd = findViewById(R.id.add);
        ecgView = findViewById(R.id.ecg_view);
        Button btnRead = findViewById(R.id.button_read);
        tvState = findViewById(R.id.tv_state);

        mPairedDevicesArrayAdapter = new ArrayAdapter<>(this,
                R.layout.device_name);
        ListView pairedListView = findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        lvAdapter = new ArrayAdapter<>(this, R.layout.device_name);
        lv.setAdapter(lvAdapter);

        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle("开始采集");
        progressDialog.setMessage("错误，请返回重试...");
        progressDialog.setCancelable(true);

        btnStart.setOnClickListener(this);
        btnLimit.setOnClickListener(this);
        btnAcquistion.setOnClickListener(this);
        btnSave.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        btnadd.setOnClickListener(this);
        btnRead.setOnClickListener(this);
    }

    // Write the file
    public void saveFile(String data, String fileName) {
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(data.getBytes());
            outputStream.close();
            appUtils.ToastString(MainActivity.this, "已保存 " + getFilesDir().getPath() + "/" + fileName);
        } catch (Exception e) {
            e.printStackTrace();
            appUtils.ToastString(MainActivity.this, "文件保存失败，请重试");
        }
    }

    // To determine whether the file exists
    public boolean existsFile(String fileName) {
        String path = this.getFilesDir().getPath() + "//";
        File file = new File(path + fileName);
        if (file.exists()) {
            return true;
        }
        return false;
    }

    // Read the file
    public StringBuffer readFile(Context context, String FILE_NAME) {
        FileInputStream in = null;
        Scanner s = null;
        StringBuffer sb = new StringBuffer();
        try {
            in = context.openFileInput(FILE_NAME);
            s = new Scanner(in);
            while (s.hasNext()) {
                sb.append(s.next());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb;
    }

    // Delete the contents of the folder and folder under the specified path
    public static void deleteDir(final String pPath) {
        File dir = new File(pPath);
        deleteDirWihtFile(dir);
    }

    // Delete the contents of the folder and folder under the specified path
    public static void deleteDirWihtFile(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory())
            return;
        for (File file : dir.listFiles()) {
            if (file.isFile())
                file.delete(); // 删除所有文件
            else if (file.isDirectory())
                deleteDirWihtFile(file); // 递规的方式删除文件夹
        }
        dir.delete();// 删除目录本身
    }

    public void disconnect() {
        this.unregisterReceiver(mReceiver);
        SharedPreferences.Editor sharedata = getSharedPreferences("Add", 0).edit();
        sharedata.clear();
        sharedata.commit();
        mPairedDevicesArrayAdapter.clear();
        Toast.makeText(this, "设备已经断开", Toast.LENGTH_SHORT).show();
        try {
            bRun = false;
            is.close();
            _socket.close();
            _socket = null;
            btnadd.setText(getResources().getString(R.string.add));
        } catch (IOException e) {
        }
    }

    public void connect() {
        if (!_bluetooth.isEnabled()) {
            Intent enabler = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enabler, REQUEST_ENABLE);
            return;
        }
        if (_socket == null) {
            mPairedDevicesArrayAdapter.clear();
            Intent serverIntent = new Intent(MainActivity.this,
                    DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        } else {
            disconnect();
        }
        return;
    }

    public byte[] hexStringToBytes(String hexString) {
        hexString = hexString.replaceAll(" ", "");
        if ((hexString == null) || (hexString.equals(""))) {
            return null;
        }
        hexString = hexString.toUpperCase();
        inputString = hexString;
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; ++i) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[(pos + 1)]));
        }
        return d;
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    public boolean sendString(String str, boolean isProcess) {
        if (_socket == null) {
            Toast.makeText(this, "Blue socket disconnect", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (str == null) {
            Toast.makeText(this, "Please enter the content", Toast.LENGTH_SHORT).show();
            return false;
        }
        byteString.setLength(0);
        if (isProcess) {
            if (str.equals(ConstantUtil.CONTINUOUS_ACQUISITION)) {
                readType = true;
                dsType = ConstantUtil.TYPE_CONTINUOUS_ACQUISITION;
            } else {
                readType = false;
                oneList.clear();
                if (str.equals(ConstantUtil.STARTING_POINT)) {  // 运动到起始位置
                    dsType = ConstantUtil.TYPE_STARTING_POINT;
                } else if (str.equals(ConstantUtil.LIMIT_POINT)) {  // 运动到极限位置
                    dsType = ConstantUtil.TYPE_LIMIT_POINT;
                } else if (str.equals(ConstantUtil.DETECTION_DS)) {  // 检测电机 位置
                    dsType = ConstantUtil.TYPE_DETECTION_DS;
                } else if (str.equals(ConstantUtil.CLOSED_OPEN)) {  // 电机 封闭 打开
                    dsType = ConstantUtil.TYPE_CLOSED_OPEN;
                } else if (str.equals(ConstantUtil.CLOSED_SHUT)) {  // 电机 封闭 关闭
                    dsType = ConstantUtil.TYPE_CLOSED_SHUT;
                } else if (str.equals(ConstantUtil.STOP_COLLECTING)) {  // 电机结束采集
                    dsType = ConstantUtil.TYPE_STOP_COLLECTING;
                } else {  // 七段加压 压力
                    dsType = ConstantUtil.TYPE_PRESSURIZED;
                }
            }
        } else {
            dsType = 0;
        }
        try {
            OutputStream os = _socket.getOutputStream();
            if (hex) {
                byte[] bos_hex = hexStringToBytes(str);
                os.write(bos_hex);
            } else {
                byte[] bos = str.getBytes("GB2312");
                os.write(bos);
            }

        } catch (IOException e) {
        }
        return true;
    }

    // 求 平均值
    public float average(float[] array) {
        float sum = 0;
        for (int i = 0; i < array.length; i++) {
            sum = sum + array[i];
        }
        return sum / array.length;
    }

    private void sendMsg(int nums) {
        if (!EcgView.isRunning) {
            currentPressure = nums;
            readNums = -1;
            sendString(ConstantUtil.CONTINUOUS_ACQUISITION, true);
            EcgView.isRead = false;
            ecgView.startThread();
        }
    }

    // 发送消息给 handler
    private void sendMessage2Handler(int what, String obj, Handler handler) {
        Message msg = new Message();
        msg.obj = obj;
        msg.what = what;
        handler.sendMessage(msg);
    }

    // 填充集合数据   01极限；02初始；03中间
    private void addOneLists(List<Byte> oneList, byte b) {
        oneList.add((byte) -96);
        oneList.add((byte) 5);
        oneList.add(b);
        oneList.add((byte) 0);
        oneList.add((byte) 0);
        oneList.add((byte) -2);
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }
}
