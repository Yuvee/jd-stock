package com.zhuweitung.utils;

import cn.hutool.core.util.StrUtil;
import com.zhuweitung.exception.MessageSendException;
import com.zhuweitung.message.DingtalkBotMessageSender;
import com.zhuweitung.message.MessageSender;
import com.zhuweitung.model.Config;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * 消息发送工具类
 *
 * @author zhuweitung
 * @since 2024/10/17
 */
@Slf4j
public class MessageUtils {

    /**
     * 发送通知
     *
     * @param message 消息内容
     */
    public static void send(String message) {
        Config config = ConfigUtils.get();
        if (!config.isEnableNotify()) {
            return;
        }
        String notifyType = config.getNotifyType();
        if (StrUtil.isBlank(notifyType)) {
            log.warn("请先配置通知方式");
            return;
        }
        try {
            MessageSender sender;
            if (Objects.equals(notifyType, "dingtalk_bot")) {
                Config.DingtalkBot dingtalkBot = config.getDingtalkBot();
                if (Objects.isNull(dingtalkBot)) {
                    log.error("请先配置钉钉机器人参数");
                    return;
                }
                sender = new DingtalkBotMessageSender(dingtalkBot.getToken(), dingtalkBot.getSecret());
            } else {
                log.error("无效通知方式");
                return;
            }
            sender.send(message);

        } catch (MessageSendException e) {
            log.error(e.getMessage());
        } catch (Exception e) {
            log.error("通知发送异常", e);
        }

    }

}
