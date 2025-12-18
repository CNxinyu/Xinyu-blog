package redis

import (
	"context"
	"fmt"
	"time"

	"github.com/CNxinyu/Xinyu-blog/go-backend/config"

	"github.com/redis/go-redis/v9"
)

var (
	Client *redis.Client
)

func InitRedis() error {
	cfg := config.Global.Redis

	Client = redis.NewClient(&redis.Options{
		Addr:         cfg.Addr,
		Password:     cfg.Password,
		DB:           cfg.DB,
		PoolSize:     cfg.MaxOpen,
		MinIdleConns: cfg.MaxIdle,
		DialTimeout:  5 * time.Second,
		ReadTimeout:  3 * time.Second,
		WriteTimeout: 3 * time.Second,
	})

	ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
	defer cancel()

	if err := Client.Ping(ctx).Err(); err != nil {
		return fmt.Errorf("ping redis failed: %w", err)
	}

	return nil
}
