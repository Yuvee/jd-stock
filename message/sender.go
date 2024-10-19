package message

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
)

// Sender 通知消息发送客户端
type Sender interface {
	// GetName 获取发送客户端名称
	GetName() string

	// Send 发送通知
	Send(message string) error
}

// SendPost 发送post请求
func SendPost(senderName string, url string, body map[string]interface{}) error {
	data, err := json.Marshal(body)
	if err != nil {
		return err
	}
	resp, err := http.Post(url, "application/json", bytes.NewBuffer(data))
	if err != nil {
		return fmt.Errorf("%s通知发送异常：%v", senderName, err)
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		return fmt.Errorf("%s通知发送异常，状态码: %d", senderName, resp.StatusCode)
	}
	return nil
}
