package config

import (
	"fmt"
	"os"

	"github.com/goccy/go-yaml"
)

var (
	Global Config
)

// Config root
type Config struct {
	Server Server `yaml:"server"`
	Mysql  Mysql  `yaml:"mysql"`
	Redis  Redis  `yaml:"redis"`
	JWT    JWT    `yaml:"jwt"` 
}

// ---------------- JWT ----------------
type JWT struct {
	Secret string `yaml:"secret"`  // 密钥
	Expire int    `yaml:"expire"`  // 过期时间（秒）
}

// ---------------- Server ----------------

type Server struct {
	HttpAddr    string `yaml:"http_addr"`   // :8080
	Env         string `yaml:"env"`         // dev / prod
	LogLevel    string `yaml:"log_level"`   // debug / info / warn / error
	EnablePprof bool   `yaml:"enable_pprof"`
}

// ---------------- Mysql ----------------

type Mysql struct {
	User     string `yaml:"user"`
	Password string `yaml:"password"`
	Host     string `yaml:"host"`
	Port     int    `yaml:"port"`
	Database string `yaml:"database"`
	Charset  string `yaml:"charset"`
	ShowSql  bool   `yaml:"show_sql"`
	MaxOpen  int    `yaml:"max_open"`
	MaxIdle  int    `yaml:"max_idle"`
}

func (m Mysql) DSN() string {
	return fmt.Sprintf(
		"%s:%s@tcp(%s:%d)/%s?charset=%s&parseTime=true&loc=Local",
		m.User,
		m.Password,
		m.Host,
		m.Port,
		m.Database,
		m.Charset,
	)
}

// ---------------- Redis ----------------

type Redis struct {
	Addr     string `yaml:"addr"`
	Password string `yaml:"password"`
	DB       int    `yaml:"db"`
	MaxIdle  int    `yaml:"max_idle"`
	MaxOpen  int    `yaml:"max_open"`
}

// ---------------- Init ----------------

// Init loads config from local yaml file
func Init(path string) error {
	content, err := os.ReadFile(path)
	if err != nil {
		return fmt.Errorf("read config failed: %w", err)
	}

	if err := yaml.Unmarshal(content, &Global); err != nil {
		return fmt.Errorf("unmarshal config failed: %w", err)
	}

	return nil
}

// ---------------- Helper ----------------

// GetString with default
func GetString(val string, def string) string {
	if val == "" {
		return def
	}
	return val
}
