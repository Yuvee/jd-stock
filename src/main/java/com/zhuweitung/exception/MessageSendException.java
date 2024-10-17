package com.zhuweitung.exception;

/**
 * 通知发送异常
 *
 * @author zhuweitung
 * @since 2024/10/17
 */
public class MessageSendException extends RuntimeException {
    public MessageSendException(String message) {
        super(message);
    }
}
