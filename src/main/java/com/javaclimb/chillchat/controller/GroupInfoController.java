package com.javaclimb.chillchat.controller;

import java.io.IOException;
import java.util.List;
import com.javaclimb.chillchat.annotation.GlobalInterceptor;
import com.javaclimb.chillchat.entity.dto.TokenUserInfoDto;
import com.javaclimb.chillchat.entity.enums.GroupStatusEnum;
import com.javaclimb.chillchat.entity.enums.MessageTypeEnum;
import com.javaclimb.chillchat.entity.enums.UserContactStatusEnum;
import com.javaclimb.chillchat.entity.po.UserContact;
import com.javaclimb.chillchat.entity.query.GroupInfoQuery;
import com.javaclimb.chillchat.entity.po.GroupInfo;
import com.javaclimb.chillchat.entity.query.UserContactQuery;
import com.javaclimb.chillchat.entity.vo.GroupInfoVO;
import com.javaclimb.chillchat.entity.vo.ResponseVO;
import com.javaclimb.chillchat.exception.BusinessException;
import com.javaclimb.chillchat.service.GroupInfoService;
import com.javaclimb.chillchat.service.UserContactService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 群 Controller
 */
@RestController("groupInfoController")
@RequestMapping("/group")
@Validated
public class GroupInfoController extends ABaseController{

	@Resource
	private GroupInfoService groupInfoService;
	@Resource
	private UserContactService userContactService;

	/**
	 * 创建群组或更新群组
	 * @param request
	 * @param groupId
	 * @param groupName
	 * @param groupNotice
	 * @param joinType
	 * @param avatarFile
	 * @return
	 * @throws IOException
	 */
	@RequestMapping("/saveGroup")
	@GlobalInterceptor
	public ResponseVO saveGroup(HttpServletRequest request,
								String groupId,
								@NotEmpty String groupName,
								String groupNotice,
								@NotNull Integer joinType,
								MultipartFile avatarFile) throws IOException {
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		GroupInfo groupInfo = new GroupInfo();
		groupInfo.setGroupId(groupId);
		groupInfo.setGroupOwnerId(tokenUserInfoDto.getUserId());
		groupInfo.setGroupName(groupName);
		groupInfo.setGroupNotice(groupNotice);
		groupInfo.setStatus(GroupStatusEnum.NORMAL.getStatus());
		groupInfo.setJoinType(joinType);
		this.groupInfoService.saveGroup(groupInfo, avatarFile);
		return getSuccessResponseVO(null);
	}

	/**
	 * 加载用户创建的群组
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/loadMyGroup")
	@GlobalInterceptor
	public ResponseVO loadMyGroup(HttpServletRequest request) {
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		GroupInfoQuery infoQuery = new GroupInfoQuery();
		infoQuery.setGroupOwnerId(tokenUserInfoDto.getUserId());
		infoQuery.setOrderBy("create_time desc");
		List<GroupInfo> groupInfoList = this.groupInfoService.findListByParam(infoQuery);
		for (GroupInfo groupInfo : groupInfoList) {
			UserContactQuery userContactQuery = new UserContactQuery();
			userContactQuery.setContactId(groupInfo.getGroupId());
			Integer memberCount = this.userContactService.findCountByParam(userContactQuery);
			groupInfo.setMemberCount(memberCount);
		}
		return getSuccessResponseVO(groupInfoList);
	}

	/**
	 * 获取群组信息
	 * @param request
	 * @param groupId
	 * @return
	 */
	@RequestMapping("/getGroupInfo")
	@GlobalInterceptor
	public ResponseVO getGroupInfo(HttpServletRequest request,
								   @NotEmpty String groupId){
		GroupInfo groupInfo = getGroupDetailCommon(request, groupId);
		UserContactQuery userContactQuery = new UserContactQuery();
		userContactQuery.setContactId(groupId);
		Integer memberCount = this.userContactService.findCountByParam(userContactQuery);
		groupInfo.setMemberCount(memberCount);
		return getSuccessResponseVO(groupInfo);
	}

	private GroupInfo getGroupDetailCommon(HttpServletRequest request, String groupId) {
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		//在user_Contact中查询该群聊是否为该用户的联系人
		UserContact userContact = this.userContactService.getUserContactByUserIdAndContactId(tokenUserInfoDto.getUserId(), groupId);
		if (userContact == null || !UserContactStatusEnum.FRIEND.getStatus().equals(userContact.getStatus())) {
			throw new BusinessException("你不在群聊或者群聊不存在或已经解散");
		}
		//根据群聊id获取群聊信息
		GroupInfo groupInfo = this.groupInfoService.getGroupInfoByGroupId(groupId);
		if (groupInfo == null || !GroupStatusEnum.NORMAL.getStatus().equals(groupInfo.getStatus())) {
			throw new BusinessException("群聊不存在或已经解散");
		}
		return groupInfo;
	}

	/**
	 * 获取群成员信息
	 * @param request
	 * @param groupId
	 * @return
	 */
	@RequestMapping("/getGroupInfo4Chat")
	@GlobalInterceptor
	public ResponseVO getGroupInfo4Chat(HttpServletRequest request,
								   @NotEmpty String groupId){
		GroupInfo groupInfo = getGroupDetailCommon(request, groupId);
		UserContactQuery userContactQuery = new UserContactQuery();
		userContactQuery.setContactId(groupId);
		userContactQuery.setQueryUserInfo(true);	//关联查询
		userContactQuery.setOrderBy("create_time desc");
		userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
		List<UserContact> userContactList = this.userContactService.findListByParam(userContactQuery);
		GroupInfoVO groupInfoVo = new GroupInfoVO();
		groupInfoVo.setGroupInfo(groupInfo);
		groupInfoVo.setUserContactList(userContactList);
		return getSuccessResponseVO(groupInfoVo);
	}


	/**
	 * 退群
	 *
	 * @param request
	 * @param groupId
	 * @return
	 */
	@RequestMapping(value = "/leaveGroup")
	@GlobalInterceptor
	public ResponseVO leaveGroup(HttpServletRequest request,
								 @NotEmpty String groupId) {
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		groupInfoService.leaveGroup(tokenUserInfoDto.getUserId(), groupId, MessageTypeEnum.LEAVE_GROUP);
		return getSuccessResponseVO(null);
	}

	/**
	 * 解散群
	 *
	 * @param request
	 * @param groupId
	 * @return
	 */
	@RequestMapping(value = "/dissolutionGroup")
	@GlobalInterceptor
	public ResponseVO dissolutionGroup(HttpServletRequest request,
									   @NotEmpty String groupId) {
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		groupInfoService.dissolutionGroup(tokenUserInfoDto.getUserId(), groupId);
		return getSuccessResponseVO(null);
	}


		/**
		 * 添加或者移除人员
		 *
		 * @param request
		 * @param groupId
		 * @param selectContacts
		 * @param opType 0移除成员,1添加成员
		 * @return
		 */
		@RequestMapping(value = "/addOrRemoveGroupUser")
		@GlobalInterceptor
		public ResponseVO addOrRemoveGroupUser(HttpServletRequest request,
											   @NotEmpty String groupId,
											   @NotEmpty String selectContacts,
											   @NotNull Integer opType) {
			TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
			groupInfoService.addOrRemoveGroupUser(tokenUserInfoDto, groupId, selectContacts, opType);
			return getSuccessResponseVO(null);
		}
}