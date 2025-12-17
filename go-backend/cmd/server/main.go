package main

import (
	"github.com/CNxinyu/Xinyu-blog/go-backend/internal/bootstrap"
	"github.com/CNxinyu/Xinyu-blog/go-backend/internal/config"
	"github.com/CNxinyu/Xinyu-blog/go-backend/internal/pkg/logger"
	"github.com/CNxinyu/Xinyu-blog/go-backend/internal/router"
)

func main() {
    // 1. 加载配置
    config.Init()

    // 2. 初始化日志
    logger.Init()

    // 3. 初始化数据库
    bootstrap.InitDB()

    // 4. 初始化 Redis
    bootstrap.InitRedis()

    // 5. 启动 HTTP Server
    router.Run()
}
