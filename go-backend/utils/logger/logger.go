package logger

import (
	"fmt"
	"os"
	"strings"

	"github.com/CNxinyu/Xinyu-blog/go-backend/config"

	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
)

var (
	log  *zap.Logger
	atom zap.AtomicLevel
)

// Init must be called once at startup
func Init() {
	level := config.Global.Server.LogLevel
	if level == "" {
		level = "info"
	}

	atom = zap.NewAtomicLevel()
	if err := atom.UnmarshalText([]byte(level)); err != nil {
		fmt.Printf("invalid log level: %s, fallback to info\n", level)
		atom.SetLevel(zap.InfoLevel)
	}

	encoderCfg := zapcore.EncoderConfig{
		MessageKey: "msg",
		LevelKey:   "level",
		TimeKey:    "time",
		CallerKey:  "caller",

		EncodeTime:   zapcore.TimeEncoderOfLayout("2006-01-02 15:04:05.000"),
		EncodeLevel:  zapcore.LowercaseLevelEncoder,
		EncodeCaller: zapcore.ShortCallerEncoder,
	}

	var encoder zapcore.Encoder
	if strings.EqualFold(config.Global.Server.Env, "dev") {
		if isTerminal(os.Stdout) {
			encoderCfg.EncodeLevel = zapcore.CapitalColorLevelEncoder
		} else {
			encoderCfg.EncodeLevel = zapcore.CapitalLevelEncoder
		}
		encoder = zapcore.NewConsoleEncoder(encoderCfg)
	} else {
		encoder = zapcore.NewJSONEncoder(encoderCfg)
	}

	core := zapcore.NewCore(encoder, zapcore.AddSync(os.Stdout), atom)

	log = zap.New(
		core,
		zap.AddCaller(),
		zap.AddCallerSkip(1),
		zap.AddStacktrace(zap.ErrorLevel),
	)
}

func isTerminal(file *os.File) bool {
	stat, err := file.Stat()
	if err != nil {
		return false
	}
	return (stat.Mode() & os.ModeCharDevice) != 0
}

// Sync flushes logs
func Sync() {
	if log != nil {
		_ = log.Sync()
	}
}

// SetLevel allows dynamic log level change
func SetLevel(level string) {
	if err := atom.UnmarshalText([]byte(level)); err != nil {
		log.Warn("invalid log level", zap.String("level", level))
	}
}

// ---------- wrapper ----------

func Debug(msg string, fields ...zap.Field) {
	log.Debug(msg, fields...)
}

func Info(msg string, fields ...zap.Field) {
	log.Info(msg, fields...)
}

func Warn(msg string, fields ...zap.Field) {
	log.Warn(msg, fields...)
}

func Error(msg string, fields ...zap.Field) {
	log.Error(msg, fields...)
}

func Fatal(msg string, fields ...zap.Field) {
	log.Fatal(msg, fields...)
}
