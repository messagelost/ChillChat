package com.javaclimb.chillchat.controller;

import com.javaclimb.chillchat.annotation.GlobalInterceptor;
import com.javaclimb.chillchat.entity.dto.TokenUserInfoDto;
import com.javaclimb.chillchat.entity.dto.UserContactSearchResultDto;
import com.javaclimb.chillchat.entity.enums.PageSize;
import com.javaclimb.chillchat.entity.enums.ResponseCodeEnum;
import com.javaclimb.chillchat.entity.enums.UserContactStatusEnum;
import com.javaclimb.chillchat.entity.enums.UserContactTypeEnum;
import com.javaclimb.chillchat.entity.po.UserContact;
import com.javaclimb.chillchat.entity.po.UserInfo;
import com.javaclimb.chillchat.entity.query.UserContactApplyQuery;
import com.javaclimb.chillchat.entity.query.UserContactQuery;
import com.javaclimb.chillchat.entity.vo.PaginationResultVO;
import com.javaclimb.chillchat.entity.vo.ResponseVO;
import com.javaclimb.chillchat.entity.vo.UserInfoVO;
import com.javaclimb.chillchat.exception.BusinessException;
import com.javaclimb.chillchat.service.UserContactApplyService;
import com.javaclimb.chillchat.service.UserContactService;
import com.javaclimb.chillchat.service.UserInfoService;
import com.javaclimb.chillchat.utils.CopyTools;
import jodd.util.ArraysUtil;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/contact")
public class UserContactController extends ABaseController {

    @Resource
    private UserContactService userContactService;

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private UserContactApplyService userContactApplyService;

    /**
     * 查找联系人
     * @param request
     * @param contactId
     * @return
     */
    @RequestMapping("/search")
    @GlobalInterceptor
    public ResponseVO search(HttpServletRequest request, @NotEmpty String contactId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        UserContactSearchResultDto resultDto = userContactService.searchContact(tokenUserInfoDto.getUserId(), contactId);
        return getSuccessResponseVO(resultDto);
    }

    @RequestMapping("/applyAdd")
    @GlobalInterceptor
    public ResponseVO applyAdd(HttpServletRequest request,
                               @NotEmpty String contactId,
                               String applyInfo) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        Integer joinType = userContactApplyService.applyAdd(tokenUserInfoDto,contactId,applyInfo);
        return getSuccessResponseVO(joinType);
    }

    @RequestMapping("/loadApply")
    @GlobalInterceptor
    public ResponseVO loadApply(HttpServletRequest request, Integer pageNo) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        UserContactApplyQuery userContactApplyQuery = new UserContactApplyQuery();
        userContactApplyQuery.setOrderBy("last_apply_time desc");
        userContactApplyQuery.setReceiveUserId(tokenUserInfoDto.getUserId());
        userContactApplyQuery.setQueryContactInfo(true);
        userContactApplyQuery.setPageNo(pageNo);
        userContactApplyQuery.setPageSize(PageSize.SIZE15.getSize());
        PaginationResultVO resultVO = userContactApplyService.findListByPage(userContactApplyQuery);
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/dealWithApply")
    @GlobalInterceptor
    public ResponseVO dealWithApply(HttpServletRequest request,
                                    @NotNull Integer applyId,
                                    @NotNull Integer status) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        userContactApplyService.dealWithApply(tokenUserInfoDto.getUserId(), applyId, status);
        return getSuccessResponseVO(null);
    }


    @RequestMapping("/loadContact")
    @GlobalInterceptor
    public ResponseVO loadContact(HttpServletRequest request,
                                  @NotEmpty String contactType) {
        UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByName(contactType);
        if (null == contactTypeEnum) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        UserContactQuery contactQuery = new UserContactQuery();
        contactQuery.setUserId(tokenUserInfoDto.getUserId());
        contactQuery.setContactType(contactTypeEnum.getType());
        if (UserContactTypeEnum.USER == contactTypeEnum) {
            contactQuery.setQueryContactUserInfo(true);
        } else if (UserContactTypeEnum.GROUP == contactTypeEnum) {
            contactQuery.setQueryGroupInfo(true);
            contactQuery.setExcludeMyGroup(true);
        }
        contactQuery.setStatusArray(new Integer[]{
                UserContactStatusEnum.FRIEND.getStatus(),
                UserContactStatusEnum.DEL_BE.getStatus(),
                UserContactStatusEnum.BLOCK_BE.getStatus()});
        contactQuery.setOrderBy("last_update_time desc");
        List<UserContact> contactList = userContactService.findListByParam(contactQuery);
        return getSuccessResponseVO(contactList);
    }

    /**
     * 获取联系人的基本信息（不一定是好友）
     * @param request
     * @param contactId
     * @return
     */
    @RequestMapping("/getContactInfo")
    @GlobalInterceptor
    public ResponseVO getContactInfo(HttpServletRequest request,
                                     @NotEmpty String contactId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        UserInfo userInfo = userInfoService.getUserInfoByUserId(contactId);
        UserInfoVO userInfoVO = CopyTools.copy(userInfo, UserInfoVO.class);
        userInfoVO.setContactStatus(UserContactStatusEnum.NOT_FRIEND.getStatus().toString());
        //判断是否是联系人
        UserContact userContact = userContactService.getUserContactByUserIdAndContactId(tokenUserInfoDto.getUserId(), contactId);
        if (userContact != null) {
            userInfoVO.setContactStatus(userContact.getStatus().toString());
        }
        return getSuccessResponseVO(userInfoVO);
    }

    /**
     * 获取好友联系人信息
     * @param request
     * @param contactId
     * @return
     */
    @RequestMapping("/getContactUserInfo")
    @GlobalInterceptor
    public ResponseVO getContactUserInfo(HttpServletRequest request, @NotEmpty String contactId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        UserContact userContact = this.userContactService.getUserContactByUserIdAndContactId(tokenUserInfoDto.getUserId(), contactId);
        if (null == userContact || !ArraysUtil.contains(new Integer[]{
                UserContactStatusEnum.FRIEND.getStatus(),
                UserContactStatusEnum.DEL_BE.getStatus(),
                UserContactStatusEnum.BLOCK_BE.getStatus(),
                UserContactStatusEnum.BLOCK_BE_FIRST.getStatus()}, userContact.getStatus())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        UserInfo userInfo = userInfoService.getUserInfoByUserId(contactId);
        UserInfoVO userInfoVO = CopyTools.copy(userInfo, UserInfoVO.class);
        return getSuccessResponseVO(userInfoVO);
    }

    /**
     * 删除联系人
     *
     * @param request
     * @param contactId
     * @return
     */
    @RequestMapping("/delContact")
    @GlobalInterceptor
    public ResponseVO delContact(HttpServletRequest request, @NotEmpty String contactId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        userContactService.removeUserContact(tokenUserInfoDto.getUserId(), contactId, UserContactStatusEnum.DEL);
        return getSuccessResponseVO(null);
    }

    /**
     * 添加到黑名单
     *
     * @param request
     * @param contactId
     * @return
     */
    @RequestMapping("/addContact2BlackList")
    @GlobalInterceptor
    public ResponseVO addContact2BlackList(HttpServletRequest request, @NotEmpty String contactId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        userContactService.removeUserContact(tokenUserInfoDto.getUserId(), contactId, UserContactStatusEnum.BLOCK);
        return getSuccessResponseVO(null);
    }



}
