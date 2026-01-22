package service

import (
	"context"
	"crypto/rand"
	"math/big"

	pb "go-backend/api/test"
)

type TestService struct {
	pb.UnimplementedTestServer
}

func NewTestService() *TestService {
	return &TestService{}
}

func (s *TestService) GetTest(ctx context.Context, req *pb.GetTestRequest) (*pb.GetTestReply, error) {
    return &pb.GetTestReply{
		Code: RandCode(int(req.Length), req.Type),
	}, nil
}

func RandCode(length int, codeType pb.TYPE) string {
	if length <= 0 {
		return ""
	}

	var charset string
	switch codeType {
	case pb.TYPE_DIGIT:
		charset = "0123456789"
	case pb.TYPE_LETTER:
		charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
	case pb.TYPE_MIXED:
		charset = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
	default:
		charset = "0123456789"
	}

	bytes := make([]byte, length)
	charsetLen := big.NewInt(int64(len(charset)))

	for i := 0; i < length; i++ {
		n, err := rand.Int(rand.Reader, charsetLen)
		if err != nil {
			return ""
		}
		bytes[i] = charset[n.Int64()]
	}

	return string(bytes)
}

