package router

import "github.com/gin-gonic/gin"

func registerCustomerRoutes(r *gin.Engine) {
	api := r.Group("/api")
	{
		api.GET("/ping", func(c *gin.Context) {
			c.JSON(200, gin.H{"msg": "pong"})
		})
	}
}
