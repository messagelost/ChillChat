package com.javaclimb.chillchat.controller;

import com.javaclimb.chillchat.annotation.GlobalInterceptor;
import com.javaclimb.chillchat.entity.dto.TokenUserInfoDto;
import com.javaclimb.chillchat.entity.enums.UserStatusEnum;
import com.javaclimb.chillchat.entity.po.UserInfo;
import com.javaclimb.chillchat.entity.vo.ResponseVO;
import com.javaclimb.chillchat.entity.vo.UserInfoVO;
import com.javaclimb.chillchat.service.UserInfoService;
import com.javaclimb.chillchat.utils.Consts;
import com.javaclimb.chillchat.utils.CopyTools;
import com.javaclimb.chillchat.utils.StringTools;
import com.javaclimb.chillchat.webSocket.ChannelContextUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.io.IOException;

/**
 * 账号信息 Controller
 */
@RestController("userInfoController")
@RequestMapping("/userInfo")
public class UserInfoController extends ABaseController{

    @Resource
    private UserInfoService userInfoService;
    @Resource
    private ChannelContextUtils channelContextUtils;

    /**
     * 获取用户信息
     * @param request
     * @return
     */
    @RequestMapping("/getUserInfo")
    @GlobalInterceptor
    public ResponseVO getUserInfo(HttpServletRequest request) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        UserInfo userInfo = userInfoService.getUserInfoByUserId(tokenUserInfoDto.getUserId());
        UserInfoVO userInfoVO = CopyTools.copy(userInfo, UserInfoVO.class);
        userInfoVO.setAdmin(tokenUserInfoDto.getAdmin());
        return getSuccessResponseVO(userInfoVO);
    }

    @RequestMapping("/saveUserInfo")
    @GlobalInterceptor
    public ResponseVO saveUserInfo(HttpServletRequest request,
                                   @RequestPart UserInfo userInfo,
                                   MultipartFile avatarFile,
                                   MultipartFile avatarCover) throws IOException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        userInfo.setUserId(tokenUserInfoDto.getUserId());
        userInfo.setPassword(null);
        userInfo.setStatus(null);
        userInfo.setCreateTime(null);
        userInfo.setLastLoginTime(null);
        this.userInfoService.updateUserInfo(userInfo, avatarFile, avatarCover);
        if (!tokenUserInfoDto.getNickName().equals(userInfo.getNickName())) {
            tokenUserInfoDto.setNickName(userInfo.getNickName());
            resetTokenUserInfo(request, tokenUserInfoDto);
        }
        return getUserInfo(request);
    }

    @RequestMapping("/updatePassword")
    @GlobalInterceptor
    public ResponseVO updatePassword(HttpServletRequest request,
                                     @NotEmpty @Pattern(regexp = Consts.REGEX_PASSWORD) String password) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        UserInfo userInfo = new UserInfo();
        userInfo.setPassword(StringTools.encodeMd5(password));
        this.userInfoService.updateUserInfoByUserId(userInfo, tokenUserInfoDto.getUserId());
        channelContextUtils.closeContext(tokenUserInfoDto.getUserId());
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/logout")
    @GlobalInterceptor
    public ResponseVO logout(HttpServletRequest request) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        this.userInfoService.updateUserStatus(UserStatusEnum.OFFLINE.getStatus(), tokenUserInfoDto.getUserId());
        channelContextUtils.closeContext(tokenUserInfoDto.getUserId());
        return getSuccessResponseVO(null);
    }
}
