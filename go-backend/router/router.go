package router

import (
	"net/http"

	"github.com/gin-gonic/gin"


)

func NewEngine() *gin.Engine {
	r := gin.New()

	// 基础中间件
	// r.Use(
	// 	gin.Recovery(),
	// 	middleware.Logger(),
	// )

	// 健康检查（必须有）
	r.GET("/healthz", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "ok"})
	})

	// 业务路由
	registerAdminRoutes(r)
	registerCustomerRoutes(r)

	return r
}
