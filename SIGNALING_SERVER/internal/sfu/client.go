package sfu

import (
	"context"
	"time"

	"google.golang.org/grpc"

	"tangthetoan.com/signaling-server/proto"
)

const (
	createPeerTimeout   = 10 * time.Second
	addCandidateTimeout = 3 * time.Second
)

type Client struct {
	conn   *grpc.ClientConn
	client proto.IonSFUClient
}

func NewClient(conn *grpc.ClientConn) *Client {
	return &Client{
		conn:   conn,
		client: proto.NewIonSFUClient(conn),
	}
}

func (c *Client) CreatePeer(room, userID, sdp string) (*proto.CreatePeerResponse, error) {
	ctx, cancel := context.WithTimeout(context.Background(), createPeerTimeout)
	defer cancel()

	return c.client.CreatePeer(ctx, &proto.CreatePeerRequest{
		Room:   room,
		UserId: userID,
		Sdp:    sdp,
	})
}

func (c *Client) AddICECandidate(peerID, candidate string) (*proto.Ack, error) {
	ctx, cancel := context.WithTimeout(context.Background(), addCandidateTimeout)
	defer cancel()

	return c.client.AddICECandidate(ctx, &proto.AddCandidateRequest{
		PeerId:    peerID,
		Candidate: candidate,
	})
}
