package app

import (
	"github.com/CNxinyu/Xinyu-blog/go-backend/adaptor/redis"
	"github.com/CNxinyu/Xinyu-blog/go-backend/config"
	repo "github.com/CNxinyu/Xinyu-blog/go-backend/repo/mysql"
	"github.com/CNxinyu/Xinyu-blog/go-backend/utils/logger"
)

func (a *App) init() error {
	// config
	if err := config.Init("config/config.yaml"); err != nil {
		return err
	}

	// logger
	logger.Init()

	// infra
	if err := repo.InitDB(); err != nil {
		return err
	}
	if err := redis.InitRedis(); err != nil {
		return err
	}

	// server config
	a.httpAddr = config.Global.Server.HttpAddr
	if a.httpAddr == "" {
		a.httpAddr = ":8080"
	}

	a.initHTTPServer()
	return nil
}
