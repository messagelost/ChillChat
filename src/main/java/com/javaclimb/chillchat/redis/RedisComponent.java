package com.javaclimb.chillchat.redis;

import com.javaclimb.chillchat.entity.dto.SysSettingDto;
import com.javaclimb.chillchat.entity.dto.TokenUserInfoDto;
import com.javaclimb.chillchat.utils.Consts;
import com.javaclimb.chillchat.utils.StringTools;
import org.apache.tomcat.util.bcel.classfile.Constant;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.websocket.OnMessage;
import java.util.List;

@Component("redisComponent")
public class RedisComponent {

    @Resource
    private RedisUtils redisUtils;

    public TokenUserInfoDto getTokenUserInfoDto(String token) {
        TokenUserInfoDto tokenUserInfoDto = (TokenUserInfoDto) redisUtils.get(Consts.REDIS_KEY_WS_TOKEN + token);
        return tokenUserInfoDto;
    }
    /**
     * 获取心跳
     * @param userId
     * @return
     */

    public Long getUserHeartBeat(String userId){
        return (Long) redisUtils.get(Consts.REDIS_KEY_WS_USER_HEARTBEAT + userId);
    }

    //保存最后心跳时间
    public void saveUserHeartBeat(String userId) {
        redisUtils.set(Consts.REDIS_KEY_WS_USER_HEARTBEAT + userId, System.currentTimeMillis(), Consts.REDIS_KEY_EXPIRES_HEARTBEAT);
    }

    //删除用户心跳
    public void removeUserHeartBeat(String userId) {
        redisUtils.del(Consts.REDIS_KEY_WS_USER_HEARTBEAT + userId);
    }

    public void saveTokenUserInfoDto(TokenUserInfoDto tokenUserInfoDto) {
        redisUtils.set(Consts.REDIS_KEY_WS_TOKEN + tokenUserInfoDto.getToken(), tokenUserInfoDto, Consts.REDIS_KEY_EXPRESS_DAY * 2);
        redisUtils.set(Consts.REDIS_KEY_WS_TOKEN_USERID + tokenUserInfoDto.getUserId(), tokenUserInfoDto.getToken(), Consts.REDIS_KEY_EXPRESS_DAY * 2);
    }


    public TokenUserInfoDto getTokenUserInfoDtoByUserId(String userId) {
        String token = (String) redisUtils.get(Consts.REDIS_KEY_WS_TOKEN_USERID + userId);
        return getTokenUserInfoDto(token);
    }

    public SysSettingDto getSysSetting(){
        SysSettingDto sysSettingDto = (SysSettingDto) redisUtils.get(Consts.REDIS_KEY_SYS_SETTING);
        sysSettingDto = sysSettingDto == null ? new SysSettingDto() : sysSettingDto;
        return sysSettingDto;
    }

    //获取用户联系人
    public List<String> getUserContactList(String userId) {
        return redisUtils.getQueueList(Consts.REDIS_KEY_USER_CONTACT + userId);
    }
    //清空联系人
    public void cleanUserContact(String userId){
        redisUtils.del(Consts.REDIS_KEY_USER_CONTACT+userId);
    }
    //批量添加联系人
    public void addUserContactBatch(String userId,List<String> contactIdList){
        redisUtils.lSet(Consts.REDIS_KEY_USER_CONTACT+userId,contactIdList,Consts.REDIS_KEY_EXPRESS_DAY*2);
    }

    /**
     * 添加联系人
     * @param userId
     * @param contactId
     */
    public void addUserContact(String userId,String contactId){
        List<String> contactList = redisUtils.getQueueList(Consts.REDIS_KEY_USER_CONTACT + userId);
        if (!contactList.contains(contactId)) {
            redisUtils.lSet(Consts.REDIS_KEY_USER_CONTACT + userId, contactId, Consts.REDIS_KEY_EXPRESS_DAY * 2);
        }
    }

    /**
     * 清除token信息
     *
     * @param userId
     */
    public void cleanUserTokenByUserId(String userId) {
        String token = (String) redisUtils.get(Consts.REDIS_KEY_WS_TOKEN_USERID + userId);
        if (!StringTools.isEmpty(token)) {
            redisUtils.del(Consts.REDIS_KEY_WS_TOKEN + token);
        }
    }

    //删除用户联系人
    public void removeUserContact(String userId, String contactId) {
        redisUtils.del(Consts.REDIS_KEY_USER_CONTACT + userId, contactId);
    }
}
