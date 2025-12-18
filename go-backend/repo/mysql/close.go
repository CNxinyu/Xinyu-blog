package repo

func CloseDB() {
	if DB == nil {
		return
	}
	if sqlDB, err := DB.DB(); err == nil {
		_ = sqlDB.Close()
	}
}
