package main

import (
	"log"

	"github.com/CNxinyu/Xinyu-blog/go-backend/internal/app"
)

func main() {
	if err := app.NewApp().Run(); err != nil {
		log.Fatal(err)
	}
}
