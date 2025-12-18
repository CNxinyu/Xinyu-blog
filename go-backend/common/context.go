package common

import "context"

type ContextKey string

func WithValue(ctx context.Context, key ContextKey, val any) context.Context {
    return context.WithValue(ctx, key, val)
}
