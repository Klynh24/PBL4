Write-Host "Starting all servers..." -ForegroundColor Green

# Start Caddy
Start-Process powershell -ArgumentList "-NoExit", "-Command", "caddy run --config Caddyfile"
Start-Sleep -Seconds 2

# Start SIGNALING_SERVER
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd SIGNALING_SERVER; go run cmd/server/main.go"
Start-Sleep -Seconds 2

# Start SFU_SERVER
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd SFU_SERVER; go run cmd/server/main.go"
Start-Sleep -Seconds 2

# Start STUN_SERVER
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd STUN_SERVER; go run main.go"