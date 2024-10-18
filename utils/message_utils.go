package utils

import (
	"github.com/zhuweitung/jd-stock/message"
	"log"
)

// SendMessage 发送通知
func SendMessage(msg string) {
	config := GetConfig()
	if !config.EnableNotify {
		// 未开启通知，跳过
		return
	}
	if "dingtalk_bot" == config.NotifyType {
		sender := message.DingtalkBotSender{Token: config.DingtalkBot.Token, Secret: config.DingtalkBot.Secret}
		sender.Send(msg)
	} else {
		log.Printf("通知方式%s未实现，欢迎 pull request\n", config.NotifyType)
	}
}
