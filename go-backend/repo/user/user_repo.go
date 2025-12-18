package user

import (
	"context"

	"github.com/CNxinyu/Xinyu-blog/go-backend/repo/model"
	repo "github.com/CNxinyu/Xinyu-blog/go-backend/repo/mysql"
)

func GetUserByUsername(ctx context.Context, username string) (*model.User, error) {
	var user model.User
	err := repo.DB.WithContext(ctx).
		Where("username = ?", username).
		First(&user).Error
	if err != nil {
		return nil, err
	}
	return &user, nil
}
