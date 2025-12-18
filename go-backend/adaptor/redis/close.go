package redis

func Close() {
	if Client != nil {
		_ = Client.Close()
	}
}