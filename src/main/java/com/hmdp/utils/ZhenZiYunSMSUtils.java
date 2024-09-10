package com.hmdp.utils;

import com.zhenzi.sms.ZhenziSmsClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/*
验证码发送平台 榛子云
 */
@Data
@Component
@ConfigurationProperties(prefix = "zhenzi")
public class ZhenZiYunSMSUtils {

    /**
     * 配置文件，别乱改
     */
    private String apiUrl = "https://sms_developer.zhenzikj.com";          //apiUrl
    private String appId = "113981";           //应用id
    private String appSecret = "fc81cd45-08a3-4086-bf38-3103207ab9c6";       //应用secret
    private String templateId = "13097";      //模板id
    private String invalidTimer = "2";    //失效时间

    /**
     * 发送短信验证码
     *
     * @param telNumber 接收者手机号码
     * @param validateCode 随机验证码（四位或六位）
     * @return
     */
    public String sendSMS(String telNumber, String validateCode) throws Exception {
        //榛子云短信 客户端
        //请求地址，个人开发者使用https://sms_developer.zhenzikj.com，企业开发者使用https://sms.zhenzikj.com
        ZhenziSmsClient client = new ZhenziSmsClient(apiUrl, appId, appSecret);
        //存放请求参数的map集合
        Map<String, Object> params = new HashMap<String, Object>();
        //接收者手机号码
        params.put("number", telNumber);
        //短信模板ID
        params.put("templateId", templateId);
        //短信模板参数
        String[] templateParams = new String[2];
        templateParams[0] = validateCode;
        templateParams[1] = invalidTimer;
        params.put("templateParams", templateParams);
        /**
         * 1.send方法用于单条发送短信,所有请求参数需要封装到Map中;
         * 2.返回结果为json串：{ "code":0,"data":"发送成功"}
         * 3.备注：（code: 发送状态，0为成功。非0为发送失败，可从data中查看错误信息）
         */
        String result = client.send(params);
        return result;
    }

}
