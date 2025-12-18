package app

import (
	"context"
	"sync"
	"net/http"
)

type App struct {
	ctx        context.Context
	cancel     context.CancelFunc
	wg         sync.WaitGroup
	httpServer *http.Server
	httpAddr   string
}

// NewApp 创建一个新的应用实例
func NewApp() *App {
	ctx, cancel := context.WithCancel(context.Background())
	return &App{
		ctx:    ctx,
		cancel: cancel,
	}
}

// Run 启动应用服务
func (a *App) Run() error {
	if err := a.init(); err != nil {
		return err
	}

	// 启动 HTTP Server
	a.wg.Add(1)
	go func() {
		defer a.wg.Done()
		a.startHTTPServer()
	}()

	// 等待退出信号
	a.waitForShutdown()
	return nil
}

