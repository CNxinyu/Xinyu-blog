package app

import (
	"os"
	"os/signal"
	"syscall"

	"github.com/CNxinyu/Xinyu-blog/go-backend/adaptor/redis"
	"github.com/CNxinyu/Xinyu-blog/go-backend/utils/logger"
	"github.com/CNxinyu/Xinyu-blog/go-backend/repo/mysql"
)

func (a *App) waitForShutdown() {
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)

	<-quit
	logger.Info("shutdown signal received")

	// 1. 通知所有 goroutine
	a.cancel()

	// 2. 优雅关闭 HTTP Server
	a.shutdownHTTPServer()

	// 3. 关闭基础设施
	redis.Close()
	repo.CloseDB()

	// 4. 等待 goroutine 退出
	a.wg.Wait()
	logger.Info("app shutdown complete")

	// 5. flush logger
	logger.Sync()
}
