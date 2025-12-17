package bootstrap

import (
	"gorm.io/driver/mysql"
	"gorm.io/gorm"

	"github.com/CNxinyu/Xinyu-blog/go-backend/internal/config"
)

var DB *gorm.DB

func InitDB() {
	db, err := gorm.Open(mysql.Open(config.Cfg.MySQL.DSN), &gorm.Config{})
	if err != nil {
		panic(err)
	}
	DB = db
}
