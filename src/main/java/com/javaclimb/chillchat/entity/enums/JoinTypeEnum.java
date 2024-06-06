package com.javaclimb.chillchat.entity.enums;

import com.javaclimb.chillchat.utils.StringTools;

public enum JoinTypeEnum {
    JOIN(0,"直接加入"),
    APPLY(1,"需要审核");

    private Integer type;
    private String desc;

    JoinTypeEnum(Integer type,String desc){
        this.type = type;
        this.desc = desc;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public static JoinTypeEnum getByName(String name){
        try {
            if(StringTools.isEmpty(name)){
                return null;
            }
            return JoinTypeEnum.valueOf(name.toUpperCase());
        }catch (Exception e){
            return null;
        }
    }

    public static JoinTypeEnum getByType(Integer type){
        for(JoinTypeEnum item:JoinTypeEnum.values()){
            if(item.getType().equals(type)){
                return item;
            }
        }
        return null;
    }
}
