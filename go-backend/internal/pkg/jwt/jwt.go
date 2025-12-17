package jwt

import (
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/CNxinyu/Xinyu-blog/go-backend/internal/config"
)

func GenerateToken(uid uint) (string, error) {
	claims := jwt.MapClaims{
		"uid": uid,
		"exp": time.Now().Add(time.Duration(config.Cfg.JWT.Expire) * time.Second).Unix(),
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString([]byte(config.Cfg.JWT.Secret))
}
