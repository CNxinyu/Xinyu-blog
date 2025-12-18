package model

import "time"

type User struct {
    ID        uint      `gorm:"primaryKey"`
    Username  string    `gorm:"type:varchar(64);uniqueIndex"`
    Password  string    `gorm:"type:varchar(255)"`
    CreatedAt time.Time
    UpdatedAt time.Time
}
