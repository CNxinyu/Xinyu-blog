package api

import "github.com/gin-gonic/gin"

func OK(c *gin.Context, data interface{}) {
    c.JSON(200, gin.H{
        "code": 0,
        "msg":  "success",
        "data": data,
    })
}

func Fail(c *gin.Context, code int) {
    c.JSON(200, gin.H{
        "code": code,
        "msg":  "error",
    })
}
