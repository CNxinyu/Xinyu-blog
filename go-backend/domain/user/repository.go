package user

import "context"

// Repository defines storage operations for user entities.
type Repository interface {
    GetByUsername(ctx context.Context, username string) (*Entity, error)
}
