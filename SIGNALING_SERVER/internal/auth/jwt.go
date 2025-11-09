package auth

import (
	"crypto/rsa"
	"crypto/x509"
	"encoding/pem"
	"errors"
	"os"

	"github.com/golang-jwt/jwt/v4"
)

type Verifier struct {
	pub *rsa.PublicKey
}

func NewVerifierFromFile(path string) (*Verifier, error) {
	b, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}
	blk, _ := pem.Decode(b)
	if blk == nil {
		return nil, errors.New("invalid pem")
	}
	pubIfc, err := x509.ParsePKIXPublicKey(blk.Bytes)
	if err != nil {
		return nil, err
	}
	pk, ok := pubIfc.(*rsa.PublicKey)
	if !ok {
		return nil, errors.New("not rsa public key")
	}
	return &Verifier{pub: pk}, nil
}
func (v *Verifier) Verify(tokenStr string) (string, error) {
	if tokenStr == "" {
		return "", errors.New("empty token")
	}
	t, err := jwt.Parse(tokenStr, func(t *jwt.Token) (interface{}, error) {
		if _, ok := t.Method.(*jwt.SigningMethodRSA); !ok {
			return nil, errors.New("unexpected signing method")
		}
		return v.pub, nil
	})
	if err != nil {
		return "", err
	}
	if !t.Valid {
		return "", errors.New("token invalid")
	}
	claims, ok := t.Claims.(jwt.MapClaims)
	if !ok {
		return "", errors.New("invalid claims")
	}
	uid, ok := claims["sub"].(string)
	if !ok {
		return "", errors.New("no sub claim")
	}
	return uid, nil
}
