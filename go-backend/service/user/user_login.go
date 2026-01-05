package user

import (
	"context"

	"golang.org/x/crypto/bcrypt"

	"github.com/CNxinyu/Xinyu-blog/go-backend/common/errorx"
	userrepo "github.com/CNxinyu/Xinyu-blog/go-backend/repo/user"
	"github.com/CNxinyu/Xinyu-blog/go-backend/utils/jwt"
)

// 已弃用：认证逻辑已集中到 `service/auth`，请改用 `auth.LoginService`。
func Login(ctx context.Context, username, password string) (string, error) {
	user, err := userrepo.GetUserByUsername(ctx, username)
	if err != nil {
		return "", errorx.ErrNotFound
	}

	if err := bcrypt.CompareHashAndPassword(
		[]byte(user.Password),
		[]byte(password),
	); err != nil {
		return "", errorx.ErrUnauthorized
	}

	token, err := jwt.GenerateToken(user.ID)
	if err != nil {
		return "", err
	}

	return token, nil
}
