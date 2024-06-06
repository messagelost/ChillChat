package com.javaclimb.chillchat.controller;

import com.javaclimb.chillchat.annotation.GlobalInterceptor;
import com.javaclimb.chillchat.entity.dto.TokenUserInfoDto;
import com.javaclimb.chillchat.entity.po.UserInfo;
import com.javaclimb.chillchat.entity.vo.ResponseVO;
import com.javaclimb.chillchat.entity.vo.UserInfoVO;
import com.javaclimb.chillchat.exception.BusinessException;
import com.javaclimb.chillchat.redis.RedisComponent;
import com.javaclimb.chillchat.redis.RedisUtils;
import com.javaclimb.chillchat.service.UserInfoService;
import com.javaclimb.chillchat.utils.Consts;
import com.javaclimb.chillchat.utils.CopyTools;
import com.wf.captcha.ArithmeticCaptcha;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.xml.ws.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 注册
 */
@RestController("accountController")
@RequestMapping(value = "/account")
public class AccountController extends ABaseController{

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    @Resource
    private UserInfoService userInfoService;
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private RedisComponent redisComponent;

    @RequestMapping("/checkCode")
    /**
     * 生成图形验证码
     */
    public ResponseVO checkCode(){
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(100,42);
        String code = captcha.text();
        String checkCodeBase64 = captcha.toBase64();
        String checkCodeKey = UUID.randomUUID().toString();
        redisUtils.set(Consts.REDIS_KEY_CHECK_CODE + checkCodeKey,code,Consts.REDIS_TIME_1MIN*10);
        Map<String, String> result = new HashMap<>();
        result.put("checkCode",checkCodeBase64);
        result.put("checkCodeKey",checkCodeKey);
        return getSuccessResponseVO(result);
    }

    /**
     * 注册
     * @param checkCodeKey
     * @param email
     * @param password
     * @param nickName
     * @param checkCode
     * @return
     */
    @RequestMapping("/register")
    public ResponseVO register(@NotEmpty String checkCodeKey,
                               @NotEmpty @Email String email,
                               @NotEmpty String password,
                               @NotEmpty String nickName,
                               @NotEmpty String checkCode) {
        try {
            if(!checkCode.equalsIgnoreCase((String) redisUtils.get(Consts.REDIS_KEY_CHECK_CODE + checkCodeKey))){
                throw new BusinessException("图片验证码不正确");
            }
            userInfoService.register(email,nickName,password);
            return getSuccessResponseVO(null);
        }finally {
            redisUtils.del(Consts.REDIS_KEY_CHECK_CODE + checkCodeKey);
        }

    }

    /**
     * 登录
     * @param checkCodeKey
     * @param email
     * @param password
     * @param checkCode
     * @return
     */
    @RequestMapping("/login")
    public ResponseVO login(@NotEmpty String checkCodeKey,
                            @NotEmpty @Email String email,
                            @NotEmpty String password,
                            @NotEmpty String checkCode){
        try {
            if(!checkCode.equalsIgnoreCase((String) redisUtils.get(Consts.REDIS_KEY_CHECK_CODE + checkCodeKey))){
                throw new BusinessException("图片验证码不正确");
            }
            UserInfoVO userInfoVO = userInfoService.login(email,password);
            return getSuccessResponseVO(userInfoVO);
        }finally {
            redisUtils.del(Consts.REDIS_KEY_CHECK_CODE + checkCodeKey);
        }
    }


    @GlobalInterceptor
    @RequestMapping("/getSystemSetting")
    public ResponseVO getSystemSetting(){
        return getSuccessResponseVO(redisComponent.getSysSetting());
    }
}
