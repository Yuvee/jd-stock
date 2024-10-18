package models

import "encoding/json"

// AreaInfo 地区信息
type AreaInfo struct {
	ID    json.Number `json:"id"`    // 地区ID
	PID   json.Number `json:"pid"`   // 父级ID
	Level int         `json:"level"` // 层级
	Name  string      `json:"name"`  // 地区名称
}

// SkuInfo 商品信息
type SkuInfo struct {
	FreshEdi       *string `json:"freshEdi"`
	SidDely        string  `json:"sidDely"`
	Channel        int     `json:"channel"`
	Rid            *string `json:"rid"`
	Sid            string  `json:"sid"`
	DcId           string  `json:"dcId"`
	IsPurchase     bool    `json:"IsPurchase"`
	Eb             string  `json:"eb"`
	Ec             string  `json:"ec"`
	StockState     int     `json:"StockState"`
	Ab             string  `json:"ab"`
	CanAddCart     string  `json:"canAddCart"`
	Ac             string  `json:"ac"`
	Ad             string  `json:"ad"`
	Ae             string  `json:"ae"`
	SkuState       int     `json:"skuState"`
	PopType        int     `json:"PopType"`
	Af             string  `json:"af"`
	Ag             string  `json:"ag"`
	StockStateName string  `json:"StockStateName"`
	M              string  `json:"m"`
	Rfg            int     `json:"rfg"`
	ArrivalDate    string  `json:"ArrivalDate"`
	V              string  `json:"v"`
	Rn             int     `json:"rn"`
	Dc             string  `json:"dc"`
}

// Config 定义模型结构体，映射 YAML 配置文件
type Config struct {
	EveryMinutes int         `yaml:"everyMinutes"` // 每隔N分钟执行
	Provinces    []string    `yaml:"provinces"`    // 库存省份
	SkuIds       []string    `yaml:"skuIds"`       // 商品ID列表
	Delay        int         `yaml:"delay"`        // 查询延迟（毫秒）
	Ua           string      `yaml:"ua"`           // 用户代理字符串
	EnableNotify bool        `yaml:"enableNotify"` // 是否启用通知
	NotifyType   string      `yaml:"notifyType"`   // 通知方式
	DingtalkBot  DingtalkBot `yaml:"dingtalkBot"`  // 钉钉机器人配置
}

// DingtalkBot 钉钉机器人配置结构体
type DingtalkBot struct {
	Token  string `yaml:"token"`  // 钉钉机器人 token
	Secret string `yaml:"secret"` // 钉钉机器人 secret
}
