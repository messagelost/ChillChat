package com.javaclimb.chillchat.entity.enums;

import com.javaclimb.chillchat.utils.StringTools;

public enum UserContactTypeEnum {
    USER(0,"U","好友"),
    GROUP(1,"G","群组");
    private Integer type;
    private String prefix;
    private String desc;

    UserContactTypeEnum(Integer type, String prefix, String desc){
        this.type = type;
        this.prefix = prefix;
        this.desc = desc;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public static UserContactTypeEnum getByName(String name){
        try {
            if(StringTools.isEmpty(name)){
                return null;
            }
            return UserContactTypeEnum.valueOf(name.toUpperCase());
        }catch (Exception e){
            return null;
        }
    }

    public static UserContactTypeEnum getByPrefix(String prefix){
        try {
            if(StringTools.isEmpty(prefix) || prefix.trim().length()==0){
                return null;
            }
            prefix = prefix.substring(0,1);
            for(UserContactTypeEnum typeEnum : UserContactTypeEnum.values()){
                if(typeEnum.getPrefix().equals(prefix)){
                    return typeEnum;
                }
            }
            return null;
        }catch (Exception e){
            return null;
        }
    }


}
