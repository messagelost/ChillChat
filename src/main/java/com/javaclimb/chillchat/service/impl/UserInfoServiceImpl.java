package com.javaclimb.chillchat.service.impl;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.imageio.ImageIO;

import com.javaclimb.chillchat.entity.config.AppConfig;
import com.javaclimb.chillchat.entity.dto.TokenUserInfoDto;
import com.javaclimb.chillchat.entity.enums.*;
import com.javaclimb.chillchat.entity.po.GroupInfo;
import com.javaclimb.chillchat.entity.po.UserContact;
import com.javaclimb.chillchat.entity.po.UserInfoBeauty;
import com.javaclimb.chillchat.entity.query.UserContactQuery;
import com.javaclimb.chillchat.entity.query.UserInfoBeautyQuery;
import com.javaclimb.chillchat.entity.vo.UserInfoVO;
import com.javaclimb.chillchat.exception.BusinessException;
import com.javaclimb.chillchat.mappers.UserContactMapper;
import com.javaclimb.chillchat.mappers.UserInfoBeautyMapper;
import com.javaclimb.chillchat.redis.RedisComponent;
import com.javaclimb.chillchat.service.GroupInfoService;
import com.javaclimb.chillchat.service.UserContactService;
import com.javaclimb.chillchat.utils.Consts;
import com.javaclimb.chillchat.utils.CopyTools;
import jodd.util.ArraysUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.ibatis.reflection.ArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.javaclimb.chillchat.entity.query.UserInfoQuery;
import com.javaclimb.chillchat.entity.po.UserInfo;
import com.javaclimb.chillchat.entity.vo.PaginationResultVO;
import com.javaclimb.chillchat.entity.query.SimplePage;
import com.javaclimb.chillchat.mappers.UserInfoMapper;
import com.javaclimb.chillchat.service.UserInfoService;
import com.javaclimb.chillchat.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


/**
 * 用户信息 业务接口实现
 */
@Service("userInfoService")
public class UserInfoServiceImpl implements UserInfoService {

	@Resource
	private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

	@Resource
	private UserInfoBeautyMapper<UserInfoBeauty, UserInfoBeautyQuery> userInfoBeautyMapper;

	@Resource
	private UserContactMapper<UserContact, UserContactQuery> userContactMapper;

	@Resource
	private AppConfig appConfig;

	@Resource
	private RedisComponent redisComponent;

	@Resource
	private UserContactService userContactService;

	private static final Logger logger = LoggerFactory.getLogger(UserContactService.class);

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<UserInfo> findListByParam(UserInfoQuery param) {
		return this.userInfoMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(UserInfoQuery param) {
		return this.userInfoMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<UserInfo> findListByPage(UserInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<UserInfo> list = this.findListByParam(param);
		PaginationResultVO<UserInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(UserInfo bean) {
		return this.userInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<UserInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userInfoMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<UserInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(UserInfo bean, UserInfoQuery param) {
		StringTools.checkParam(param);
		return this.userInfoMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(UserInfoQuery param) {
		StringTools.checkParam(param);
		return this.userInfoMapper.deleteByParam(param);
	}

	/**
	 * 根据UserId获取对象
	 */
	@Override
	public UserInfo getUserInfoByUserId(String userId) {
		return this.userInfoMapper.selectByUserId(userId);
	}

	/**
	 * 根据UserId修改
	 */
	@Override
	public Integer updateUserInfoByUserId(UserInfo bean, String userId) {
		return this.userInfoMapper.updateByUserId(bean, userId);
	}

	/**
	 * 根据UserId删除
	 */
	@Override
	public Integer deleteUserInfoByUserId(String userId) {
		return this.userInfoMapper.deleteByUserId(userId);
	}

	/**
	 * 根据Email获取对象
	 */
	@Override
	public UserInfo getUserInfoByEmail(String email) {
		return this.userInfoMapper.selectByEmail(email);
	}

	/**
	 * 根据Email修改
	 */
	@Override
	public Integer updateUserInfoByEmail(UserInfo bean, String email) {
		return this.userInfoMapper.updateByEmail(bean, email);
	}

	/**
	 * 根据Email删除
	 */
	@Override
	public Integer deleteUserInfoByEmail(String email) {
		return this.userInfoMapper.deleteByEmail(email);
	}

	/**
	 * 注册
	 *
	 * @param email
	 * @param nickName
	 * @param password
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void register(String email, String nickName, String password) {

		UserInfo userInfo = this.userInfoMapper.selectByEmail(email);

		if(null!=userInfo){
			throw new BusinessException("邮箱已存在");
		}
		String userid = StringTools.getUserId();

		//加靓号
		UserInfoBeauty beautyAccount = this.userInfoBeautyMapper.selectByEmail(email);
		Boolean useBeautyAccount = null != beautyAccount && BeautyAccountStatusEnum.NO_USE.getStatus().equals(beautyAccount.getStatus());
		if(useBeautyAccount){
			userid = UserContactTypeEnum.USER.getPrefix()+beautyAccount.getUserId();
		}

		Date curDate = new Date();
		userInfo = new UserInfo();
		userInfo.setUserId(userid);
		userInfo.setNickName(nickName);
		userInfo.setEmail(email);
		userInfo.setPassword(StringTools.encodeMd5(password));
		userInfo.setCreateTime(curDate);
		userInfo.setStatus(UserStatusEnum.ENABLE.getStatus());
		userInfo.setLastOffTime(curDate.getTime());
		userInfo.setJoinType(JoinTypeEnum.APPLY.getType());
		this.userInfoMapper.insert(userInfo);

		if(useBeautyAccount){
			UserInfoBeauty updateBeauty = new UserInfoBeauty();
			updateBeauty.setStatus(BeautyAccountStatusEnum.USED.getStatus());
			this.userInfoBeautyMapper.updateByUserId(updateBeauty, beautyAccount.getUserId());
		}

		//创建机器人好友
		userContactService.addContact4Robot(userid);
	}

	/**
	 * 登录
	 *
	 * @param email
	 * @param password
	 * @return
	 */
	@Override
	public UserInfoVO login(String email, String password) {
		UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
		if(null == userInfo || !userInfo.getPassword().equals(StringTools.encodeMd5(password))){
			throw new BusinessException("账号或者密码不存在");
		}

		if(UserStatusEnum.DISABLE.getStatus().equals(userInfo.getStatus())){
			throw new BusinessException("账号已禁用");
		}

		//查询联系人
		UserContactQuery contactQuery = new UserContactQuery();
		contactQuery.setUserId(userInfo.getUserId());
		contactQuery.setStatusArray(new Integer[]{UserContactStatusEnum.FRIEND.getStatus()});
		List<UserContact> contactList = userContactMapper.selectList(contactQuery);
		List<String> contactIdList = contactList.stream().map(item -> item.getContactId()).collect(Collectors.toList());

		redisComponent.cleanUserContact(userInfo.getUserId());
		if (!contactIdList.isEmpty()) {
			redisComponent.addUserContactBatch(userInfo.getUserId(), contactIdList);
		}

		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto(userInfo);
		Long lastHeartBeat = redisComponent.getUserHeartBeat(tokenUserInfoDto.getUserId());
		if (lastHeartBeat != null) {
			throw new BusinessException("此账号已经在别处登录，请退出后再登录");
		}
		userInfo.setStatus(UserStatusEnum.ONLINE.getStatus());

		//保存登录信息到redis中
		String token = StringTools.encodeMd5(tokenUserInfoDto.getUserId() + StringTools.getRandomString(Consts.LENGTH_20));
		tokenUserInfoDto.setToken(token);
		redisComponent.saveTokenUserInfoDto(tokenUserInfoDto);

		UserInfoVO userInfoVO = CopyTools.copy(userInfo, UserInfoVO.class);
		userInfoVO.setToken(tokenUserInfoDto.getToken());
		userInfoVO.setAdmin(tokenUserInfoDto.getAdmin());
		return userInfoVO;
	}

	@Override
	public void updateUserInfo(UserInfo userInfo, MultipartFile avatarFile, MultipartFile avatarCover) throws IOException {
		if (avatarFile != null) {
			String baseFolder = appConfig.getProjectFolder() + Consts.FILE_FOLDER_FILE;
			File targetFileFolder = new File(baseFolder + Consts.FILE_FOLDER_AVATAR_NAME);
			if (!targetFileFolder.exists()) {
				targetFileFolder.mkdirs();
			}
			String filePath = targetFileFolder.getPath() + "/" + userInfo.getUserId() + Consts.IMAGE_SUFFIX;
			File dest = new File(filePath);
			try {
				avatarFile.transferTo(dest);
				// 生成缩略图
				BufferedImage thumbnail = generateThumbnail(dest, 100, 100);
				File thumbnailFile = new File(targetFileFolder.getPath() + "/" + userInfo.getUserId() +Consts.COVER_IMAGE_SUFFIX);
				ImageIO.write(thumbnail, "jpg", thumbnailFile);
			}catch (IOException e) {
				logger.error("头像上传失败", e);
				throw new BusinessException("头像上传失败");
			}
		}
		UserInfo dbInfo = this.userInfoMapper.selectByUserId(userInfo.getUserId());

		this.userInfoMapper.updateByUserId(userInfo, userInfo.getUserId());
		String contactNameUpdate=null;
		if(dbInfo.getNickName().equals(userInfo.getNickName())){
			contactNameUpdate=userInfo.getNickName();
		}
		//TODO 更新会话信息

	}
	private BufferedImage generateThumbnail(File file, int width, int height) throws IOException {
		BufferedImage originalImage = ImageIO.read(file);
		BufferedImage thumbnail = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics2D = thumbnail.createGraphics();
		graphics2D.drawImage(originalImage, 0, 0, width, height, null);
		graphics2D.dispose();
		return thumbnail;
	}


	private TokenUserInfoDto getTokenUserInfoDto(UserInfo userInfo) {
		TokenUserInfoDto tokenUserInfoDto = new TokenUserInfoDto();
		tokenUserInfoDto.setUserId(userInfo.getUserId());
		tokenUserInfoDto.setNickName(userInfo.getNickName());

		String adminEmails = appConfig.getAdminEmail();
		if (!StringTools.isEmpty(adminEmails) && ArrayUtils.contains(adminEmails.split(","), userInfo.getEmail())) {
			tokenUserInfoDto.setAdmin(true);
		} else {
			tokenUserInfoDto.setAdmin(false);
		}
		return tokenUserInfoDto;
	}

	@Override
	public void updateUserStatus(Integer status, String userId) {
		UserStatusEnum userStatusEnum = UserStatusEnum.getByStatus(status);
		if (userStatusEnum == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		UserInfo updateInfo = new UserInfo();
		updateInfo.setStatus(userStatusEnum.getStatus());
		userInfoMapper.updateByUserId(updateInfo, userId);
	}
}