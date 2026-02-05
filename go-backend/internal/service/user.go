package service

import (
	"context"
	"log"
	"regexp"
	"time"

	userpb "go-backend/api/user/v1"
	verifypb "go-backend/api/verify/v1"

	kgrpc "github.com/go-kratos/kratos/v2/transport/grpc"
)

type UserService struct {
	userpb.UnimplementedUserServer
}

func NewUserService() *UserService {
	return &UserService{}
}

func (s *UserService) GetVerifyCode(ctx context.Context, req *userpb.GetVerifyCodeRequest) (*userpb.GetVerifyCodeReply, error) {
	phoneRegex := regexp.MustCompile(`^1[3-9]\d{9}$`)
	if !phoneRegex.MatchString(req.GetTelephone()) {
		return &userpb.GetVerifyCodeReply{
			Code:    400,
			Message: "手机格式错误",
		}, nil
	}

	conn, err := kgrpc.DialInsecure(
		context.Background(),
		kgrpc.WithEndpoint("127.0.0.1:9090"),
	)
	if err != nil {
		log.Printf("dial verify-code service failed: %v", err)
		return &userpb.GetVerifyCodeReply{
			Code:    500,
			Message: "connect verify-code service failed",
		}, nil
	}
	defer func() { _ = conn.Close() }()

	vcClient := verifypb.NewVerifyCodeServiceClient(conn)

	callCtx, cancel := context.WithTimeout(ctx, 3*time.Second)
	defer cancel()

	resp, err := vcClient.GetVerifyCode(callCtx, &verifypb.GetVerifyCodeRequest{
		Length: 6,
		Type:   verifypb.TYPE_DIGIT,
	})
	if err != nil {
		log.Printf("call VerifyCode.GetVerifyCode failed: %v", err)
		return &userpb.GetVerifyCodeReply{
			Code:    500,
			Message: "verify code service call failed: " + err.Error(),
		}, nil
	}

	return &userpb.GetVerifyCodeReply{
		Code:           resp.Code,
		Message:        resp.Message,
		VerifyCode:     resp.VerifyCode,
		VerifyCodeTime: resp.VerifyCodeTime,
		VerifyCodeLife: resp.VerifyCodeLife,
	}, nil

}
