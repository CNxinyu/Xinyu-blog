package app

import (
	"context"
	"net"
	"net/http"
	"time"

	"go.uber.org/zap"

	"github.com/CNxinyu/Xinyu-blog/go-backend/router"
	"github.com/CNxinyu/Xinyu-blog/go-backend/utils/logger"
)

func (a *App) initHTTPServer() {
	a.httpServer = &http.Server{
		Addr:              a.httpAddr,
		Handler:           router.NewEngine(),
		ReadHeaderTimeout: 5 * time.Second,
	}
}

func (a *App) startHTTPServer() {
	ln, err := net.Listen("tcp", a.httpAddr)
	if err != nil {
		logger.Error(
			"HTTP server failed to listen",
			zap.String("addr", a.httpAddr),
			zap.Error(err),
		)
		a.cancel()
		return
	}

	logger.Info(
		"HTTP server listening",
		zap.String("addr", ln.Addr().String()),
	)

	if err := a.httpServer.Serve(ln); err != nil && err != http.ErrServerClosed {
		logger.Error(
			"HTTP server crashed",
			zap.Error(err),
		)
		a.cancel()
	}
}

func (a *App) shutdownHTTPServer() {
	if a.httpServer == nil {
		return
	}

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	logger.Info("http server shutting down")
	_ = a.httpServer.Shutdown(ctx)
}
