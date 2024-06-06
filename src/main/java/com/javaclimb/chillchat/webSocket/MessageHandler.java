package com.javaclimb.chillchat.webSocket;

import com.alibaba.fastjson.JSON;
import com.javaclimb.chillchat.entity.dto.MessageSendDto;
import com.javaclimb.chillchat.entity.enums.UserContactTypeEnum;
import com.javaclimb.chillchat.entity.enums.UserStatusEnum;
import com.javaclimb.chillchat.entity.po.UserInfo;
import com.javaclimb.chillchat.entity.query.UserInfoQuery;
import com.javaclimb.chillchat.mappers.UserInfoMapper;
import com.javaclimb.chillchat.utils.Consts;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Component("messageHandler")
public class MessageHandler<T> {

    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    private static final String MESSAGE_TOPIC = "message.topic";

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private ChannelContextUtils channelContextUtils;

    @PostConstruct
    public void lisMessage() {
        RTopic rTopic = redissonClient.getTopic(MESSAGE_TOPIC);
        rTopic.addListener(MessageSendDto.class, (MessageSendDto, sendDto) -> {
            if(sendDto.getContactId().startsWith(UserContactTypeEnum.USER.getPrefix())&&(sendDto.getSendUserId()!= Consts.ROBOT_UID)&&(sendDto.getSendUserId()!=null)){
                logger.info("用户{}向{}发送信息：{}",sendDto.getSendUserId(),sendDto.getContactId(),sendDto.getMessageContent());
            }else{
                logger.info("用户{}在群聊{}发送信息：{}",sendDto.getSendUserId(),sendDto.getContactId(),sendDto.getMessageContent());
            }
            logger.info("收到广播消息:{}", JSON.toJSONString(sendDto));
            channelContextUtils.sendMessage(sendDto);
        });
    }

    public void sendMessage(MessageSendDto sendDto) {
        RTopic rTopic = redissonClient.getTopic(MESSAGE_TOPIC);
        rTopic.publish(sendDto);
    }
}
