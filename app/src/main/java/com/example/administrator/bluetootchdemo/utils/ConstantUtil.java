package com.example.administrator.bluetootchdemo.utils;

/**
 *  Fixed constant
 * Created by Administrator on 2017/8/18.
 */

public class ConstantUtil {
    /**
     * 电机常用指令
     */
    // 电机 开始位置
    public static final String STARTING_POINT = "E00101FF00FE";
    // 电机 极限位置
    public static final String LIMIT_POINT = "E00102FF00FE";
    // 电机 连续采集
    public static final String CONTINUOUS_ACQUISITION = "E00302FF00FE";
    // 电机 结束采集
    public static final String STOP_COLLECTING = "E00300FF00FE";
    // 电机 检测位置
    public static final String DETECTION_DS = "E005000000FE";
    // 电机 封闭打开
    public static final String CLOSED_OPEN = "E006010000FE";
    // 电机 封闭关闭
    public static final String CLOSED_SHUT = "E006000000FE";
    // 电机七段加压 第一段 100 4B
    public static final String PRESSURIZED_1 = "E008006400FE";
    // 电机七段加压 第2段 125 64
    public static final String PRESSURIZED_2 = "E008007D00FE";
    // 电机七段加压 第3段 150 7D
    public static final String PRESSURIZED_3 = "E008009600FE";
    // 电机七段加压 第4段 175 96
    public static final String PRESSURIZED_4 = "E00800AF00FE";
    // 电机七段加压 第5段 200 AF
    public static final String PRESSURIZED_5 = "E00800C800FE";
    // 电机七段加压 第6段 225 C8
    public static final String PRESSURIZED_6 = "E00800E100FE";
    // 电机七段加压 第7段 250 E1
    public static final String PRESSURIZED_7 = "E00800FA00FE";

    // 结束字符
    public static final String END_LOGO = "A055AA55AAFE";

    /**
     * 电机状态标识
     */
    // 运动电机到 初始位置
    public static final int TYPE_STARTING_POINT = 0X0001;
    // 运动电机到 极限位置
    public static final int TYPE_LIMIT_POINT = 0X0002;
    // 电机 正在连续采集
    public static final int TYPE_CONTINUOUS_ACQUISITION = 0X0003;
    // 检测电机 位置
    public static final int TYPE_DETECTION_DS = 0X0004;
    // 电机 封闭 打开
    public static final int TYPE_CLOSED_OPEN = 0X0005;
    // 电机 封闭 关闭
    public static final int TYPE_CLOSED_SHUT = 0X0006;
    // 电机 结束采集
    public static final int TYPE_STOP_COLLECTING = 0X0007;
    // 电机七段加压 第1段
    public static final int TYPE_PRESSURIZED = 0X0008;


    // 电机处于极限位置
    public static final String MOTOR_LIMIT = "A005010000FE";
    // 电机处于初始
    public static final String MOTOR_START = "A005020000FE";
    // 电机处于中间
    public static final String  MOTOR_MIDDLE = "A005030000FE";

    // 第一段压力
    public static final int PRESSURE_100 = 100;
    public static final int PRESSURE_125 = 125;
    public static final int PRESSURE_150 = 150;
    public static final int PRESSURE_175 = 175;
    public static final int PRESSURE_200 = 200;
    public static final int PRESSURE_225 = 225;
    public static final int PRESSURE_250 = 250;



}
