package middleware

import (
	"github.com/gin-gonic/gin"
)

func JWTAuth() gin.HandlerFunc {
	return func(c *gin.Context) {
		// 解析 token
		c.Next()
	}
}
