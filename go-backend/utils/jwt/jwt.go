package jwt

import (
	"fmt"
	"time"

	"github.com/golang-jwt/jwt/v5"

	"github.com/CNxinyu/Xinyu-blog/go-backend/config"
)

// GenerateToken 生成 JWT token
func GenerateToken(uid uint) (string, error) {
	// 获取配置
	secret := config.Global.JWT.Secret
	if secret == "" {
		return "", fmt.Errorf("JWT secret is not configured")
	}

	// 配置 token 的过期时间
	expirationTime := time.Now().Add(time.Duration(config.Global.JWT.Expire) * time.Second)

	// 创建声明
	claims := jwt.MapClaims{
		"uid": uid,
		"exp": expirationTime.Unix(),  // 使用 Unix 时间戳
	}

	// 创建 Token
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)

	// 使用配置的密钥签名
	signedToken, err := token.SignedString([]byte(secret))
	if err != nil {
		return "", fmt.Errorf("failed to sign token: %w", err)
	}

	return signedToken, nil
}
