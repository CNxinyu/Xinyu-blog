package user

// Service defines domain behavior for user.
type Service interface {
    Authenticate(username, password string) (string, error)
}
