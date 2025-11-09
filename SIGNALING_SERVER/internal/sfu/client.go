package sfu

import (
	"context"
	"time"

	"github.com/rs/zerolog/log"
	"google.golang.org/grpc"
	"tangthetoan.com/signaling-server/proto"
)

type Client struct {
	conn *grpc.ClientConn
	c    proto.IonSFUClient
	addr string
}

func NewClient(ctx context.Context, target string) (*Client, error) {
	conn, err := grpc.DialContext(ctx, target, grpc.WithInsecure(), grpc.WithBlock(), grpc.WithTimeout(5*time.Second))
	if err != nil {
		return nil, err
	}
	c := proto.NewIonSFUClient(conn)
	return &Client{
		conn: conn,
		c:    c,
		addr: target,
	}, nil
}
func (cl *Client) Close() error {
	return cl.conn.Close()
}
func (cl *Client) Addr() string {
	return cl.addr
}
func (cl *Client) CreatePeer(ctx context.Context, room, userId, sdp string) (string, string, error) {
	ctx, cancel := context.WithTimeout(ctx, 5*time.Second)
	defer cancel()
	resp, err := cl.c.CreatePeer(ctx, &proto.CreatePeerRequest{
		Room:   room,
		UserId: userId,
		Sdp:    sdp,
	})
	if err != nil {
		log.Error().Err(err).Msg("create peer failed")
		return "", "", err
	}
	return resp.PeerId, resp.AnswerSdp, nil
}
func (cl *Client) AddICECandidate(ctx context.Context, peerId, candidate string) error {
	ctx, cancel := context.WithTimeout(ctx, 3*time.Second)
	defer cancel()
	_, err := cl.c.AddICECandidate(ctx, &proto.AddCandidateRequest{
		PeerId:    peerId,
		Candidate: candidate,
	})
	if err != nil {
		log.Error().Err(err).Str("sfu", cl.addr).Msg("add candidate failed")
	}
	return err
}
