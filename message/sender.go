package message

type Sender interface {
	// Send 发送通知
	Send(message string)
}
