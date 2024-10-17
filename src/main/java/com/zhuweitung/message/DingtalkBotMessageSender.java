package com.zhuweitung.message;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiRobotSendRequest;
import com.dingtalk.api.response.OapiRobotSendResponse;
import com.taobao.api.ApiException;
import com.zhuweitung.exception.MessageSendException;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

/**
 * 钉钉机器人消息发送
 *
 * @author zhuweitung
 * @since 2024/10/17
 */
@Slf4j
public class DingtalkBotMessageSender implements MessageSender {

    private static final String URL = "https://oapi.dingtalk.com/robot/send?access_token=";

    private final DingTalkClient client;

    public DingtalkBotMessageSender(String token, String secret) {
        if (ObjectUtil.hasEmpty(token, secret)) {
            throw new MessageSendException("钉钉机器人参数无效");
        }
        String url = URL + token;
        // 设置时间戳和签名
        if (StrUtil.isNotBlank(secret)) {
            long timestamp = System.currentTimeMillis();
            String sign = null;
            try {
                String stringToSign = timestamp + "\n" + secret;
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
                sign = URLEncoder.encode(Base64.encode(signData), StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.error("钉钉机器人生成签名失败");
            }
            if (StrUtil.isNotBlank(sign)) {
                url += StrUtil.format("&timestamp={}&sign={}", timestamp, sign);
            }
        }
        this.client = new DefaultDingTalkClient(url);
    }

    @Override
    public void send(String message) {
        OapiRobotSendRequest request = new OapiRobotSendRequest();
        request.setMsgtype("text");
        OapiRobotSendRequest.Text text = new OapiRobotSendRequest.Text();
        text.setContent(message);
        request.setText(text);
        try {
            OapiRobotSendResponse response = client.execute(request);
            Long errorCode = Optional.ofNullable(response).map(OapiRobotSendResponse::getErrcode).orElse(null);
            String errorMsg = Optional.ofNullable(response).map(OapiRobotSendResponse::getErrmsg).orElse(null);
            if (Objects.equals(errorCode, 0L) && Objects.equals(errorMsg, "ok")) {
                log.info("钉钉机器人消息通知成功");
            } else {
                log.error("钉钉机器人消息通知失败，errorCode：{}，errorMsg：{}", errorCode, errorMsg);
            }
        } catch (ApiException e) {
            log.error("钉钉机器人消息通知异常，errorCode：{}，errorMsg：{}", e.getErrCode(), e.getMessage());
        }
    }
}
