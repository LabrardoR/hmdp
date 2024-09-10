package com.hmdp.dto;

import lombok.Data;

/**
 * 既支持验证码登录，又支持密码登录
 */
@Data
public class LoginFormDTO {
    private String phone;
    private String code;
    private String password;
}
