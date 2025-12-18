package user

import (
    "context"
    "errors"

    "golang.org/x/crypto/bcrypt"

    userrepo "github.com/CNxinyu/Xinyu-blog/go-backend/repo/user"
    "github.com/CNxinyu/Xinyu-blog/go-backend/utils/jwt"
)

func Login(ctx context.Context, username, password string) (string, error) {
    user, err := userrepo.GetUserByUsername(ctx, username)
    if err != nil {
        return "", errors.New("user not found")
    }

    if err := bcrypt.CompareHashAndPassword(
        []byte(user.Password),
        []byte(password),
    ); err != nil {
        return "", errors.New("password incorrect")
    }

    token, err := jwt.GenerateToken(user.ID)
    if err != nil {
        return "", err
    }

    return token, nil
}
