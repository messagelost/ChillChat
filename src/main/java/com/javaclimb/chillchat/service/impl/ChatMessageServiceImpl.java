package com.javaclimb.chillchat.service.impl;

import java.io.File;
import java.util.List;

import javax.annotation.Resource;

import com.javaclimb.chillchat.entity.dto.MessageSendDto;
import com.javaclimb.chillchat.entity.dto.SysSettingDto;
import com.javaclimb.chillchat.entity.dto.TokenUserInfoDto;
import com.javaclimb.chillchat.entity.enums.*;
import com.javaclimb.chillchat.entity.po.ChatSession;
import com.javaclimb.chillchat.exception.BusinessException;
import com.javaclimb.chillchat.mappers.ChatSessionMapper;
import com.javaclimb.chillchat.redis.RedisComponent;
import com.javaclimb.chillchat.utils.Consts;
import com.javaclimb.chillchat.utils.CopyTools;
import com.javaclimb.chillchat.webSocket.MessageHandler;
import jodd.util.ArraysUtil;
import org.springframework.stereotype.Service;

import com.javaclimb.chillchat.entity.query.ChatMessageQuery;
import com.javaclimb.chillchat.entity.po.ChatMessage;
import com.javaclimb.chillchat.entity.vo.PaginationResultVO;
import com.javaclimb.chillchat.entity.query.SimplePage;
import com.javaclimb.chillchat.mappers.ChatMessageMapper;
import com.javaclimb.chillchat.service.ChatMessageService;
import com.javaclimb.chillchat.utils.StringTools;
import org.springframework.web.multipart.MultipartFile;


/**
 * 聊天消息表 业务接口实现
 */
@Service("chatMessageService")
public class ChatMessageServiceImpl implements ChatMessageService {

	@Resource
	private ChatMessageMapper<ChatMessage, ChatMessageQuery> chatMessageMapper;

	@Resource
	private RedisComponent redisComponent;

	@Resource
	private ChatSessionMapper chatSessionMapper;

	@Resource
	private MessageHandler messageHandler;


	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<ChatMessage> findListByParam(ChatMessageQuery param) {
		return this.chatMessageMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(ChatMessageQuery param) {
		return this.chatMessageMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<ChatMessage> findListByPage(ChatMessageQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<ChatMessage> list = this.findListByParam(param);
		PaginationResultVO<ChatMessage> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(ChatMessage bean) {
		return this.chatMessageMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<ChatMessage> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.chatMessageMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<ChatMessage> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.chatMessageMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(ChatMessage bean, ChatMessageQuery param) {
		StringTools.checkParam(param);
		return this.chatMessageMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(ChatMessageQuery param) {
		StringTools.checkParam(param);
		return this.chatMessageMapper.deleteByParam(param);
	}

	/**
	 * 根据MessageId获取对象
	 */
	@Override
	public ChatMessage getChatMessageByMessageId(Long messageId) {
		return this.chatMessageMapper.selectByMessageId(messageId);
	}

	/**
	 * 根据MessageId修改
	 */
	@Override
	public Integer updateChatMessageByMessageId(ChatMessage bean, Long messageId) {
		return this.chatMessageMapper.updateByMessageId(bean, messageId);
	}

	/**
	 * 根据MessageId删除
	 */
	@Override
	public Integer deleteChatMessageByMessageId(Long messageId) {
		return this.chatMessageMapper.deleteByMessageId(messageId);
	}

	@Override
	public MessageSendDto saveMessage(ChatMessage chatMessage, TokenUserInfoDto tokenUserInfoDto) {
		//不是机器人回复，判断好友状态
		if (!Consts.ROBOT_UID.equals(tokenUserInfoDto.getUserId())) {
			List<String> contactList = redisComponent.getUserContactList(tokenUserInfoDto.getUserId());
			if (!contactList.contains(chatMessage.getContactId())) {
				UserContactTypeEnum userContactTypeEnum = UserContactTypeEnum.getByPrefix(chatMessage.getContactId());
				if (UserContactTypeEnum.USER == userContactTypeEnum) {
					throw new BusinessException(ResponseCodeEnum.CODE_902);
				} else {
					throw new BusinessException(ResponseCodeEnum.CODE_903);
				}
			}
		}
		String sessionId = null;
		String sendUserId = tokenUserInfoDto.getUserId();
		String contactId = chatMessage.getContactId();
		Long curTime = System.currentTimeMillis();
		UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByPrefix(contactId);
		MessageTypeEnum messageTypeEnum = MessageTypeEnum.getByType(chatMessage.getMessageType());
		String lastMessage = chatMessage.getMessageContent();
		String messageContent = StringTools.resetMessageContent(chatMessage.getMessageContent());
		chatMessage.setMessageContent(messageContent);
		Integer status = MessageTypeEnum.MEDIA_CHAT == messageTypeEnum ? MessageStatusEnum.SENDING.getStatus() : MessageStatusEnum.SENDED.getStatus();
		if (ArraysUtil.contains(new Integer[]{
				MessageTypeEnum.CHAT.getType(),
				MessageTypeEnum.GROUP_CREATE.getType(),
				MessageTypeEnum.ADD_FRIEND.getType(),
				MessageTypeEnum.MEDIA_CHAT.getType()
		}, messageTypeEnum.getType())) {
			if (UserContactTypeEnum.USER == contactTypeEnum) {
				sessionId = StringTools.getChatSessionId4User(new String[]{sendUserId, contactId});
			} else {
				sessionId = StringTools.getChatSessionId4Group(contactId);
			}
			//更新会话消息
			ChatSession chatSession = new ChatSession();
			chatSession.setLastMessage(messageContent);
			if (UserContactTypeEnum.GROUP == contactTypeEnum && !MessageTypeEnum.GROUP_CREATE.getType().equals(messageTypeEnum.getType())) {
				chatSession.setLastMessage(tokenUserInfoDto.getNickName() + "：" + messageContent);
			}
			lastMessage = chatSession.getLastMessage();
			//如果是媒体文件
			chatSession.setLastReceiveTime(curTime);
			chatSessionMapper.updateBySessionId(chatSession, sessionId);
			//记录消息消息表
			chatMessage.setSessionId(sessionId);
			chatMessage.setSendUserId(sendUserId);
			chatMessage.setSendUserNickName(tokenUserInfoDto.getNickName());
			chatMessage.setSendTime(curTime);
			chatMessage.setContactType(contactTypeEnum.getType());
			chatMessage.setStatus(status);
			chatMessageMapper.insert(chatMessage);
		}
		MessageSendDto messageSend = CopyTools.copy(chatMessage, MessageSendDto.class);
		if (Consts.ROBOT_UID.equals(contactId)) {
			SysSettingDto sysSettingDto = redisComponent.getSysSetting();
			TokenUserInfoDto robot = new TokenUserInfoDto();
			robot.setUserId(sysSettingDto.getRobotUid());
			robot.setNickName(sysSettingDto.getRobotNickName());
			ChatMessage robotChatMessage = new ChatMessage();
			robotChatMessage.setContactId(sendUserId);
			//这里可以对接Ai 根据输入的信息做出回答
			robotChatMessage.setMessageContent("我只是一个机器人无法识别你的消息");
			robotChatMessage.setMessageType(MessageTypeEnum.CHAT.getType());
			saveMessage(robotChatMessage, robot);
		} else {
			messageHandler.sendMessage(messageSend);
		}
		return messageSend;
	}

	@Override
	public void saveMessageFile(String userId, Long messageId, MultipartFile file, MultipartFile cover) {

	}

	@Override
	public File downloadFile(TokenUserInfoDto userInfoDto, Long messageId, Boolean cover) {
		return null;
	}
}