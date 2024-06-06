package com.javaclimb.chillchat.utils;

import com.javaclimb.chillchat.entity.enums.UserContactTypeEnum;

/**
 * 常量
 */
public class Consts {
//    /*登录名*/
//    public static final String NAME = "name";
//    /*返回码*/
//    public static final String CODE = "code";
//    /*返回信息*/
//    public static final String MSG = "msg";

    public static final Integer ZERO = 0;

    public static final String REDIS_KEY_CHECK_CODE = "chillchat:checkCodeKey";
    public static final String REDIS_KEY_WS_USER_HEARTBEAT = "chillchat:ws:user:heartbeat";
    public static final Integer REDIS_KEY_EXPIRES_HEARTBEAT = 6;
    public static final String REDIS_KEY_WS_TOKEN_USERID = "chillchat:ws:token:userid";
    public static final String REDIS_KEY_WS_TOKEN = "chillchat:ws:token";
    public static final Integer REDIS_TIME_1MIN = 60;
    public static final Integer REDIS_KEY_EXPRESS_DAY = REDIS_TIME_1MIN * 60 * 24;
    public static final String REDIS_KEY_SYS_SETTING = "chillchat:syssetting";
    public static final String FILE_FOLDER_FILE = "/file/";
    public static final String FILE_FOLDER_AVATAR_NAME = "avatar/";
    public static final String IMAGE_SUFFIX = ".png";
    public static final String COVER_IMAGE_SUFFIX = "_cover.png";

    //正则
    public static final String REGEX_PASSWORD = "^(?=.*\\d)(?=.*[a-zA-Z])[\\da-zA-Z~!@#$%^&*_]{8,18}$";

    public static final String APPLY_INFO_TEMPLATE = "我是%s";

    public static final Integer LENGTH_11 = 11;
    public static final Integer LENGTH_20 = 20;

    public static final String ROBOT_UID = UserContactTypeEnum.USER.getPrefix()+"robot";

    public static final String REDIS_KEY_USER_CONTACT = "chillchat:ws:user:contact:";

    public static final Long MILLISECOND_3DAYS_AGO = 3 * 24 * 60 * 60 * 1000L;

    public static final String FILE_FOLDER_TEMP = "/temp/";


}
