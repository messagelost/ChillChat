package com.javaclimb.chillchat.entity.vo;

import com.javaclimb.chillchat.entity.po.GroupInfo;
import com.javaclimb.chillchat.entity.po.UserContact;

import java.util.List;


public class GroupInfoVO {
    private GroupInfo groupInfo;
    private List<UserContact> userContactList;

    public List<UserContact> getUserContactList() {
        return userContactList;
    }

    public void setUserContactList(List<UserContact> userContactList) {
        this.userContactList = userContactList;
    }

    public GroupInfo getGroupInfo() {
        return groupInfo;
    }

    public void setGroupInfo(GroupInfo groupInfo) {
        this.groupInfo = groupInfo;
    }
}
