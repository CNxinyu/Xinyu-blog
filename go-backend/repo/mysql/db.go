package repo

import (
	"fmt"
	"time"

	"github.com/CNxinyu/Xinyu-blog/go-backend/config"

	"gorm.io/driver/mysql"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

var (
	DB *gorm.DB
)

func InitDB() error {
	cfg := config.Global.Mysql

	dsn := cfg.DSN()

	gormLogger := logger.Default
	if cfg.ShowSql {
		gormLogger = logger.Default.LogMode(logger.Info)
	}

	db, err := gorm.Open(mysql.Open(dsn), &gorm.Config{
		Logger: gormLogger,
	})
	if err != nil {
		return fmt.Errorf("open mysql failed: %w", err)
	}

	sqlDB, err := db.DB()
	if err != nil {
		return fmt.Errorf("get sql db failed: %w", err)
	}

	sqlDB.SetMaxOpenConns(cfg.MaxOpen)
	sqlDB.SetMaxIdleConns(cfg.MaxIdle)
	sqlDB.SetConnMaxLifetime(time.Hour)

	// 可选：启动时 ping 一次，确保 DB 可用
	if err := sqlDB.Ping(); err != nil {
		return fmt.Errorf("ping mysql failed: %w", err)
	}

	DB = db
	return nil
}
