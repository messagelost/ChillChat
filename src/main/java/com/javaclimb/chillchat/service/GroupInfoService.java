package com.javaclimb.chillchat.service;

import java.io.IOException;
import java.util.List;

import com.javaclimb.chillchat.entity.dto.TokenUserInfoDto;
import com.javaclimb.chillchat.entity.enums.MessageTypeEnum;
import com.javaclimb.chillchat.entity.query.GroupInfoQuery;
import com.javaclimb.chillchat.entity.po.GroupInfo;
import com.javaclimb.chillchat.entity.vo.PaginationResultVO;
import org.springframework.web.multipart.MultipartFile;


/**
 * 群 业务接口
 */
public interface GroupInfoService {

	/**
	 * 根据条件查询列表
	 */
	List<GroupInfo> findListByParam(GroupInfoQuery param);

	/**
	 * 根据条件查询列表
	 */
	Integer findCountByParam(GroupInfoQuery param);

	/**
	 * 分页查询
	 */
	PaginationResultVO<GroupInfo> findListByPage(GroupInfoQuery param);

	/**
	 * 新增
	 */
	Integer add(GroupInfo bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<GroupInfo> listBean);

	/**
	 * 批量新增/修改
	 */
	Integer addOrUpdateBatch(List<GroupInfo> listBean);

	/**
	 * 多条件更新
	 */
	Integer updateByParam(GroupInfo bean,GroupInfoQuery param);

	/**
	 * 多条件删除
	 */
	Integer deleteByParam(GroupInfoQuery param);

	/**
	 * 根据GroupId查询对象
	 */
	GroupInfo getGroupInfoByGroupId(String groupId);


	/**
	 * 根据GroupId修改
	 */
	Integer updateGroupInfoByGroupId(GroupInfo bean,String groupId);


	/**
	 * 根据GroupId删除
	 */
	Integer deleteGroupInfoByGroupId(String groupId);

	/**
	 * 创建群组与修改群组
	 * @param groupInfo
	 * @param avatarFile
	 */
	void saveGroup(GroupInfo groupInfo, MultipartFile avatarFile) throws IOException;

	/**
	 * 解散群聊
	 * @param userId
	 * @param groupId
	 */
	void dissolutionGroup(String userId, String groupId);

	/**
	 * 退出群聊
	 * @param userId
	 * @param groupId
	 * @param messageTypeEnum
	 */
	void leaveGroup(String userId, String groupId, MessageTypeEnum messageTypeEnum);

	/**
	 * 添加或移除成员
	 * @param tokenUserInfoDto
	 * @param groupId
	 * @param contactIds
	 * @param opType
	 */
	void addOrRemoveGroupUser(TokenUserInfoDto tokenUserInfoDto, String groupId, String contactIds, Integer opType);
}