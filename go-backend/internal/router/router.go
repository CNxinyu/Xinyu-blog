package router

import (
	"github.com/gin-gonic/gin"
)

func Run() {
	r := gin.Default()

	r.GET("/get", func(c *gin.Context) {
		c.String(200, "值:%v", "get请求")
	})

	r.Run(":8080")
}
