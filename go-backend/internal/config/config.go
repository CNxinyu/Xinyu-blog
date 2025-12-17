package config

type Config struct {
	Server ServerConfig
	MySQL  MySQLConfig
	Redis  RedisConfig
	JWT    JWTConfig
}

type ServerConfig struct {
	Port int
}

type MySQLConfig struct {
	DSN string
}

type RedisConfig struct {
	Addr     string
	Password string
	DB       int
}

type JWTConfig struct {
	Secret string
	Expire int64
}

var Cfg *Config
