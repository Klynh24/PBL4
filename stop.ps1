Write-Host "Stopping and closing all server processes..." -ForegroundColor Red
Get-Process -Name caddy -ErrorAction SilentlyContinue | Stop-Process -Force
Get-Process -Name go -ErrorAction SilentlyContinue | Stop-Process -Force