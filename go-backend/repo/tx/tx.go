package tx

import (
	"context"
	"fmt"

	mysqlrepo "github.com/CNxinyu/Xinyu-blog/go-backend/repo/mysql"
	"gorm.io/gorm"
)

type txKeyType struct{}

var txKey = txKeyType{}

// RunInTx 在事务中运行 fn
func RunInTx(ctx context.Context, fn func(ctx context.Context) error) error {
	// 如果 ctx 里已经有 tx，说明是嵌套调用，直接复用
	if existingTx := getTx(ctx); existingTx != nil {
		return fn(ctx)
	}

	db := mysqlrepo.DB
	if db == nil {
		return fmt.Errorf("db not initialized")
	}

	tx := db.Begin()
	if tx.Error != nil {
		return tx.Error
	}

	// 把 tx 放进 context
	ctx = context.WithValue(ctx, txKey, tx)

	defer func() {
		if r := recover(); r != nil {
			tx.Rollback()
			panic(r)
		}
	}()

	if err := fn(ctx); err != nil {
		tx.Rollback()
		return err
	}

	return tx.Commit().Error
}

// GetTx 从 context 中获取事务；没有则返回 DB
func GetTx(ctx context.Context) *gorm.DB {
	if tx := getTx(ctx); tx != nil {
		return tx
	}
	return mysqlrepo.DB
}

func getTx(ctx context.Context) *gorm.DB {
	tx, ok := ctx.Value(txKey).(*gorm.DB)
	if !ok {
		return nil
	}
	return tx
}
