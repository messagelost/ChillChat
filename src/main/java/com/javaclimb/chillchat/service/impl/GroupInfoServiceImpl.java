package com.javaclimb.chillchat.service.impl;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.imageio.ImageIO;

import com.javaclimb.chillchat.controller.AccountController;
import com.javaclimb.chillchat.entity.config.AppConfig;
import com.javaclimb.chillchat.entity.dto.MessageSendDto;
import com.javaclimb.chillchat.entity.dto.SysSettingDto;
import com.javaclimb.chillchat.entity.dto.TokenUserInfoDto;
import com.javaclimb.chillchat.entity.enums.*;
import com.javaclimb.chillchat.entity.po.*;
import com.javaclimb.chillchat.entity.query.*;
import com.javaclimb.chillchat.exception.BusinessException;
import com.javaclimb.chillchat.mappers.*;
import com.javaclimb.chillchat.redis.RedisComponent;
import com.javaclimb.chillchat.redis.RedisUtils;
import com.javaclimb.chillchat.service.ChatSessionUserService;
import com.javaclimb.chillchat.service.UserContactService;
import com.javaclimb.chillchat.utils.Consts;
import com.javaclimb.chillchat.utils.CopyTools;
import com.javaclimb.chillchat.webSocket.ChannelContextUtils;
import com.javaclimb.chillchat.webSocket.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.javaclimb.chillchat.entity.vo.PaginationResultVO;
import com.javaclimb.chillchat.service.GroupInfoService;
import com.javaclimb.chillchat.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


/**
 * 群 业务接口实现
 */
@Service("groupInfoService")
public class GroupInfoServiceImpl implements GroupInfoService {

	private static final Logger logger = LoggerFactory.getLogger(AccountController.class);
	@Resource
	private GroupInfoMapper<GroupInfo, GroupInfoQuery> groupInfoMapper;

	@Resource
	private UserContactMapper<UserContact, UserContactQuery> userContactMapper;

	@Resource
	private ChatSessionMapper<ChatSession, ChatSessionQuery> chatSessionMapper;

	@Resource
	private RedisComponent redisComponent;
	@Resource
	private AppConfig appConfig;
	@Resource
	private ChatSessionUserService chatSessionUserService;
	@Resource
	private ChannelContextUtils channelContextUtils;
	@Resource
	private ChatMessageMapper<ChatMessage, ChatMessageQuery> chatMessageMapper;
	@Resource
	private MessageHandler messageHandler;

	@Resource
	private UserInfoMapper<UserInfo,UserInfoQuery> userInfoMapper;


	@Resource
	private UserContactService userContactService;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<GroupInfo> findListByParam(GroupInfoQuery param) {
		return this.groupInfoMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(GroupInfoQuery param) {
		return this.groupInfoMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<GroupInfo> findListByPage(GroupInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<GroupInfo> list = this.findListByParam(param);
		PaginationResultVO<GroupInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(GroupInfo bean) {
		return this.groupInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<GroupInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.groupInfoMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<GroupInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.groupInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(GroupInfo bean, GroupInfoQuery param) {
		StringTools.checkParam(param);
		return this.groupInfoMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(GroupInfoQuery param) {
		StringTools.checkParam(param);
		return this.groupInfoMapper.deleteByParam(param);
	}

	/**
	 * 根据GroupId获取对象
	 */
	@Override
	public GroupInfo getGroupInfoByGroupId(String groupId) {
		return this.groupInfoMapper.selectByGroupId(groupId);
	}

	/**
	 * 根据GroupId修改
	 */
	@Override
	public Integer updateGroupInfoByGroupId(GroupInfo bean, String groupId) {
		return this.groupInfoMapper.updateByGroupId(bean, groupId);
	}

	/**
	 * 根据GroupId删除
	 */
	@Override
	public Integer deleteGroupInfoByGroupId(String groupId) {
		return this.groupInfoMapper.deleteByGroupId(groupId);
	}

	/**
	 * 创建群组与修改群组
	 *
	 * @param groupInfo
	 * @param avatarFile
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveGroup(GroupInfo groupInfo, MultipartFile avatarFile) throws IOException {
		Date curdate = new Date();
		//新增
		if(StringTools.isEmpty(groupInfo.getGroupId())){
			GroupInfoQuery groupInfoQuery = new GroupInfoQuery();
			groupInfoQuery.setGroupOwnerId(groupInfo.getGroupOwnerId());
			Integer count = this.groupInfoMapper.selectCount(groupInfoQuery);
			SysSettingDto sysSettingDto = redisComponent.getSysSetting();
			if(count>sysSettingDto.getMaxGroupCount()){
				throw new BusinessException("最多支持创建" + sysSettingDto.getMaxGroupCount() + "个群聊");
			}

			if(null==avatarFile){
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			}

			groupInfo.setCreateTime(curdate);
			groupInfo.setGroupId(StringTools.getGroupId());
			this.groupInfoMapper.insert(groupInfo);

			//将群组添加为联系人
			UserContact userContact = new UserContact();
			userContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
			userContact.setContactType(UserContactTypeEnum.GROUP.getType());
			userContact.setContactId(groupInfo.getGroupId());
			userContact.setUserId(groupInfo.getGroupOwnerId());
			userContact.setCreateTime(curdate);
			this.userContactMapper.insert(userContact);

			Date curDate = new Date();
			//创建会话
			String sessionId = StringTools.getChatSessionId4Group(groupInfo.getGroupId());
			ChatSession chatSession = new ChatSession();
			chatSession.setSessionId(sessionId);
			chatSession.setLastMessage(MessageTypeEnum.GROUP_CREATE.getInitMessage());
			chatSession.setLastReceiveTime(curDate.getTime());
			this.chatSessionMapper.insert(chatSession);

			//创建群主会话
			ChatSessionUser chatSessionUser = new ChatSessionUser();
			chatSessionUser.setUserId(groupInfo.getGroupOwnerId());
			chatSessionUser.setContactId(groupInfo.getGroupId());
			chatSessionUser.setContactName(groupInfo.getGroupName());
			chatSessionUser.setSessionId(sessionId);
			this.chatSessionUserService.add(chatSessionUser);

			//添加为联系人
			redisComponent.addUserContact(groupInfo.getGroupOwnerId(), groupInfo.getGroupId());


			channelContextUtils.addUser2Group(groupInfo.getGroupOwnerId(), groupInfo.getGroupId());

			//创建消息
			ChatMessage chatMessage = new ChatMessage();
			chatMessage.setSessionId(sessionId);
			chatMessage.setMessageType(MessageTypeEnum.GROUP_CREATE.getType());
			chatMessage.setMessageContent(MessageTypeEnum.GROUP_CREATE.getInitMessage());
			chatMessage.setSendUserId(null);
			chatMessage.setSendUserNickName(null);
			chatMessage.setSendTime(curDate.getTime());
			chatMessage.setContactId(groupInfo.getGroupId());
			chatMessage.setContactType(UserContactTypeEnum.GROUP.getType());
			chatMessage.setStatus(MessageStatusEnum.SENDED.getStatus());
			chatMessageMapper.insert(chatMessage);
			//发送WS消息
			chatSessionUser.setLastMessage(MessageTypeEnum.GROUP_CREATE.getInitMessage());
			chatSessionUser.setLastReceiveTime(curDate.toString());
			chatSessionUser.setMemberCount(1);

			MessageSendDto messageSend = CopyTools.copy(chatMessage, MessageSendDto.class);
			messageSend.setExtendData(chatSessionUser);
			messageSend.setLastMessage(chatSessionUser.getLastMessage());
			messageHandler.sendMessage(messageSend);
		}else {
			//修改
			GroupInfo dbInfo = this.groupInfoMapper.selectByGroupId(groupInfo.getGroupId());
			if(!dbInfo.getGroupOwnerId().equals(groupInfo.getGroupOwnerId())){
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			}
			this.groupInfoMapper.updateByGroupId(groupInfo,groupInfo.getGroupId());
			//更新相关表冗余的字段
			String contactNameUpdate = null;
			if (!dbInfo.getGroupName().equals(groupInfo.getGroupName())) {
				contactNameUpdate = groupInfo.getGroupName();
			}
			chatSessionUserService.updateRedundanceInfo(contactNameUpdate, groupInfo.getGroupId());
		}
		if (null == avatarFile) {
			return;
		}
		String baseFolder = appConfig.getProjectFolder() + Consts.FILE_FOLDER_FILE;
		File targetFileFolder = new File(baseFolder + Consts.FILE_FOLDER_AVATAR_NAME);
		if (!targetFileFolder.exists()) {
			targetFileFolder.mkdirs();
		}
		String filePath = targetFileFolder.getPath() + "/" + groupInfo.getGroupId() + Consts.IMAGE_SUFFIX;
		File dest = new File(filePath);
		try {
			avatarFile.transferTo(dest);
			// 生成缩略图
			BufferedImage thumbnail = generateThumbnail(dest, 100, 100);
			File thumbnailFile = new File(targetFileFolder.getPath() + "/" + groupInfo.getGroupId() +Consts.COVER_IMAGE_SUFFIX);
			ImageIO.write(thumbnail, "jpg", thumbnailFile);
		} catch (IOException e) {
			logger.error("头像上传失败", e);
			throw new BusinessException("头像上传失败");
		}

	}

	private BufferedImage generateThumbnail(File file, int width, int height) throws IOException {
		BufferedImage originalImage = ImageIO.read(file);
		BufferedImage thumbnail = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics2D = thumbnail.createGraphics();
		graphics2D.drawImage(originalImage, 0, 0, width, height, null);
		graphics2D.dispose();
		return thumbnail;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void dissolutionGroup(String userId, String groupId) {
		GroupInfo dbInfo = this.groupInfoMapper.selectByGroupId(groupId);
		if (null == groupId || !dbInfo.getGroupOwnerId().equals(userId)) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		//删除群组
		GroupInfo updateInfo = new GroupInfo();
		updateInfo.setStatus(GroupStatusEnum.DISSOLUTION.getStatus());
		this.groupInfoMapper.updateByGroupId(updateInfo, groupId);

		UserContactQuery userContactQuery = new UserContactQuery();
		userContactQuery.setContactId(groupId);
		userContactQuery.setContactType(UserContactTypeEnum.GROUP.getType());

		UserContact updateUserContact = new UserContact();
		updateUserContact.setStatus(UserContactStatusEnum.DEL.getStatus());
		userContactMapper.updateByParam(updateUserContact, userContactQuery);

		List<UserContact> userContactList = this.userContactMapper.selectList(userContactQuery);
		for (UserContact userContact : userContactList) {
			redisComponent.removeUserContact(userContact.getUserId(), userContact.getContactId());
		}
		String sessionId = StringTools.getChatSessionId4Group(groupId);
		Date curTime = new Date();
		String messageContent = MessageTypeEnum.DISSOLUTION_GROUP.getInitMessage();
		//更新会话消息
		ChatSession chatSession = new ChatSession();
		chatSession.setLastMessage(messageContent);
		chatSession.setLastReceiveTime(curTime.getTime());
		chatSessionMapper.updateBySessionId(chatSession, sessionId);
		//记录消息消息表
		ChatMessage chatMessage = new ChatMessage();
		chatMessage.setSessionId(sessionId);
		chatMessage.setSendTime(curTime.getTime());
		chatMessage.setContactType(UserContactTypeEnum.GROUP.getType());
		chatMessage.setStatus(MessageStatusEnum.SENDED.getStatus());
		chatMessage.setMessageType(MessageTypeEnum.DISSOLUTION_GROUP.getType());
		chatMessage.setContactId(groupId);
		chatMessage.setMessageContent(messageContent);
		chatMessageMapper.insert(chatMessage);
		//发送解散群消息
		MessageSendDto messageSendDto = CopyTools.copy(chatMessage, MessageSendDto.class);
		messageHandler.sendMessage(messageSendDto);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void leaveGroup(String userId, String groupId, MessageTypeEnum messageTypeEnum) {
		GroupInfo groupInfo = groupInfoMapper.selectByGroupId(groupId);
		if (groupInfo == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		//创建者不能退出群聊，只能解散群
		if (userId.equals(groupInfo.getGroupOwnerId())) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		Integer count = userContactMapper.deleteByUserIdAndContactId(userId, groupId);
		if (count == 0) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		UserInfo userInfo = userInfoMapper.selectByUserId(userId);

		String sessionId = StringTools.getChatSessionId4Group(groupId);
		Date curTime = new Date();
		String messageContent = String.format(messageTypeEnum.getInitMessage(), userInfo.getNickName());
		//更新会话消息
		ChatSession chatSession = new ChatSession();
		chatSession.setLastMessage(messageContent);
		chatSession.setLastReceiveTime(curTime.getTime());
		chatSessionMapper.updateBySessionId(chatSession, sessionId);
		//记录消息消息表
		ChatMessage chatMessage = new ChatMessage();
		chatMessage.setSessionId(sessionId);
		chatMessage.setSendTime(curTime.getTime());
		chatMessage.setContactType(UserContactTypeEnum.GROUP.getType());
		chatMessage.setStatus(MessageStatusEnum.SENDED.getStatus());
		chatMessage.setMessageType(messageTypeEnum.getType());
		chatMessage.setContactId(groupId);
		chatMessage.setMessageContent(messageContent);
		chatMessageMapper.insert(chatMessage);

		UserContactQuery userContactQuery = new UserContactQuery();
		userContactQuery.setContactId(groupId);
		userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
		Integer memberCount = this.userContactMapper.selectCount(userContactQuery);

		MessageSendDto messageSendDto = CopyTools.copy(chatMessage, MessageSendDto.class);
		messageSendDto.setExtendData(userId);
		messageSendDto.setMemberCount(memberCount);
		messageHandler.sendMessage(messageSendDto);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)

	public void addOrRemoveGroupUser(TokenUserInfoDto tokenUserInfoDto, String groupId, String contactIds, Integer opType) {
		GroupInfo groupInfo = groupInfoMapper.selectByGroupId(groupId);
		if (null == groupInfo || !groupInfo.getGroupOwnerId().equals(tokenUserInfoDto.getUserId())) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		String[] contactIdList = contactIds.split(",");
		for (String contactId : contactIdList) {
			//移除群员
			if (Consts.ZERO.equals(opType)) {
				leaveGroup(contactId, groupId, MessageTypeEnum.REMOVE_GROUP);
			} else {
				userContactService.addContact(contactId, null, groupId, UserContactTypeEnum.GROUP.getType(), null);
			}
		}
	}
}