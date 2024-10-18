package utils

import (
	"fmt"
	"github.com/zhuweitung/jd-stock/models"
	"gopkg.in/yaml.v3"
	"os"
	"sync"
)

// 全局变量保存配置实例
var (
	configInstance *models.Config
	configOnce     sync.Once // 确保配置只加载一次
)

// LoadConfig 加载 YAML 配置文件，并保存到全局变量
func LoadConfig(path string) (*models.Config, error) {
	var err error

	configOnce.Do(func() {
		// 打开 YAML 文件
		file, e := os.Open(path)
		if e != nil {
			err = fmt.Errorf("未找到配置文件，请下载config.yaml.example到config目录")
			return
		}
		defer file.Close()

		// 创建 Config 实例
		var cfg models.Config

		// 解析 YAML 文件
		decoder := yaml.NewDecoder(file)
		if e := decoder.Decode(&cfg); e != nil {
			err = fmt.Errorf("解析配置文件失败: %w", e)
			return
		}

		configInstance = &cfg // 保存到全局变量
	})

	return configInstance, err
}

// GetConfig 返回全局配置实例
func GetConfig() *models.Config {
	return configInstance
}

// GetEveryMinutes 获取间隔执行分钟
func GetEveryMinutes() int {
	if configInstance != nil && configInstance.EveryMinutes > 0 {
		return configInstance.EveryMinutes
	}
	return 5
}

// GetDelay 获取 Delay 值
func GetDelay() int {
	if configInstance != nil && configInstance.Delay >= 5000 {
		return configInstance.Delay
	}
	return 5000 // 返回最小值 5000 毫秒
}
