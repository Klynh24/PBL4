package main

import (
	"crypto/rsa"
	"crypto/x509"
	"encoding/pem"
	"fmt"
	"os"
	"time"

	"github.com/golang-jwt/jwt/v4"
)

func main() {
	// Path to the private key, relative to the project root
	privKeyPath := "configs/jwt_priv.pem"
	
	// --- You can change the User ID here if needed ---
	userID := "user1"

	// Read the private key
	privKeyBytes, err := os.ReadFile(privKeyPath)
	if err != nil {
		fmt.Printf("Error reading private key file: %v\n", err)
		os.Exit(1)
	}

	// Decode the PEM block
	block, _ := pem.Decode(privKeyBytes)
	if block == nil {
		fmt.Println("Error: Failed to decode PEM block containing private key.")
		os.Exit(1)
	}

	// Parse the private key (supports PKCS#1 and PKCS#8)
	key, err := x509.ParsePKCS1PrivateKey(block.Bytes)
	if err != nil {
		// If parsing as PKCS#1 fails, try PKCS#8
		keyInterface, err2 := x509.ParsePKCS8PrivateKey(block.Bytes)
		if err2 != nil {
			fmt.Printf("Error parsing private key: %v\n", err2)
			os.Exit(1)
		}
		var ok bool
		key, ok = keyInterface.(*rsa.PrivateKey)
		if !ok {
			fmt.Println("Error: Private key is not an RSA key.")
			os.Exit(1)
		}
	}

	// Create the claims
	claims := jwt.MapClaims{
		"sub": userID,
		"iat": time.Now().Unix(),
		"exp": time.Now().Add(time.Hour * 8).Unix(), // Token is valid for 8 hours
	}

	// Create the token with RS256 signing method
	token := jwt.NewWithClaims(jwt.SigningMethodRS256, claims)

	// Sign the token with the private key
	signedToken, err := token.SignedString(key)
	if err != nil {
		fmt.Printf("Error signing token: %v\n", err)
		os.Exit(1)
	}

	// Print the generated token
	fmt.Println("Generated JWT Token (valid for 8 hours):")
	fmt.Println(signedToken)
}
