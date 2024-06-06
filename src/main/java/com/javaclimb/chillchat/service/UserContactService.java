package com.javaclimb.chillchat.service;

import java.util.List;

import com.javaclimb.chillchat.entity.dto.TokenUserInfoDto;
import com.javaclimb.chillchat.entity.dto.UserContactSearchResultDto;
import com.javaclimb.chillchat.entity.enums.UserContactStatusEnum;
import com.javaclimb.chillchat.entity.query.UserContactQuery;
import com.javaclimb.chillchat.entity.po.UserContact;
import com.javaclimb.chillchat.entity.vo.PaginationResultVO;


/**
 * 联系人 业务接口
 */
public interface UserContactService {

	/**
	 * 根据条件查询列表
	 */
	List<UserContact> findListByParam(UserContactQuery param);

	/**
	 * 根据条件查询列表
	 */
	Integer findCountByParam(UserContactQuery param);

	/**
	 * 分页查询
	 */
	PaginationResultVO<UserContact> findListByPage(UserContactQuery param);

	/**
	 * 新增
	 */
	Integer add(UserContact bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<UserContact> listBean);

	/**
	 * 批量新增/修改
	 */
	Integer addOrUpdateBatch(List<UserContact> listBean);

	/**
	 * 多条件更新
	 */
	Integer updateByParam(UserContact bean,UserContactQuery param);

	/**
	 * 多条件删除
	 */
	Integer deleteByParam(UserContactQuery param);

	/**
	 * 根据UserIdAndContactId查询对象
	 */
	UserContact getUserContactByUserIdAndContactId(String userId,String contactId);


	/**
	 * 根据UserIdAndContactId修改
	 */
	Integer updateUserContactByUserIdAndContactId(UserContact bean,String userId,String contactId);


	/**
	 * 根据UserIdAndContactId删除
	 */
	Integer deleteUserContactByUserIdAndContactId(String userId,String contactId);

	/**
	 * 查找联系人
	 * @param userId
	 * @param contactId
	 */
	UserContactSearchResultDto searchContact(String userId, String contactId);

	/**
	 * 添加联系人
	 * @param applyUserId
	 * @param receiveUserId
	 * @param contactId
	 * @param contactType
	 * @param applyInfo
	 */
	void addContact(String applyUserId,String receiveUserId,String contactId,Integer contactType,String applyInfo);

	void removeUserContact(String userId, String contactId, UserContactStatusEnum statusEnum);

	void addContact4Robot(String userId);

}