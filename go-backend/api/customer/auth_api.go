package customer

import (
    "github.com/gin-gonic/gin"

    "github.com/CNxinyu/Xinyu-blog/go-backend/api"
    usersvc "github.com/CNxinyu/Xinyu-blog/go-backend/service/user"
)

type LoginReq struct {
    Username string `json:"username" binding:"required"`
    Password string `json:"password" binding:"required"`
}

func Login(c *gin.Context) {
    var req LoginReq
    if err := c.ShouldBindJSON(&req); err != nil {
        api.Fail(c, 40001)
        return
    }

    token, err := usersvc.Login(c.Request.Context(), req.Username, req.Password)
    if err != nil {
        api.Fail(c, 40101)
        return
    }

    api.OK(c, gin.H{
        "token": token,
    })
}
