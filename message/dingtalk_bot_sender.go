package message

import (
	"github.com/wanghuiyt/ding"
	"log"
)

type DingtalkBotSender struct {
	Token  string
	Secret string
}

// Send 发送他通知
func (sender DingtalkBotSender) Send(msg string) error {
	d := ding.Webhook{
		AccessToken: sender.Token,
		Secret:      sender.Secret,
	}
	err := d.SendMessageText(msg)
	if err != nil {
		log.Printf("钉钉机器人通知发送异常：%v", err)
		return err
	}
	return nil
}
