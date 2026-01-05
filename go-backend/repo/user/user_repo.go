package user

import (
	"context"

	"github.com/CNxinyu/Xinyu-blog/go-backend/repo/tx"
)

// User 为 repo 层使用的用户模型
type User struct {
	ID       uint
	Username string
	Password string
}

// Repo 定义了服务使用的最小接口，便于在测试中注入 mock
type Repo interface {
	FindByUsername(ctx context.Context, username string) (*User, error)
}

// UserRepo 为基于 GORM 的生产实现
type UserRepo struct{}

func NewUserRepo() *UserRepo {
	return &UserRepo{}
}

func (r *UserRepo) FindByUsername(ctx context.Context, username string) (*User, error) {
	db := tx.GetTx(ctx)

	var user User
	if err := db.Where("username = ?", username).First(&user).Error; err != nil {
		return nil, err
	}
	return &user, nil
}

// 保留向后兼容的辅助函数，供旧的调用方使用
func GetUserByUsername(ctx context.Context, username string) (*User, error) {
	r := NewUserRepo()
	return r.FindByUsername(ctx, username)
}
