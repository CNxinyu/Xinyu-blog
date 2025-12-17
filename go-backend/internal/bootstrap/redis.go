package bootstrap

import (
	"context"

	"github.com/go-redis/redis/v8"
	"github.com/CNxinyu/Xinyu-blog/go-backend/internal/config"
)

var RDB *redis.Client
var Ctx = context.Background()

func InitRedis() {
	RDB = redis.NewClient(&redis.Options{
		Addr:     config.Cfg.Redis.Addr,
		Password: config.Cfg.Redis.Password,
		DB:       config.Cfg.Redis.DB,
	})
}
