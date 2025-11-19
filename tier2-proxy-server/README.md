# Tier 2: WebSocket Proxy Server

## Overview

The proxy server acts as a translator between WebSocket (browser) and TCP/UDP (core server). It's allowed to use WebSocket libraries (Tyrus, javax.websocket).

## Architecture

### Components

1. **ProxyServer** - Main entry point
   - Starts Tyrus WebSocket server on port 8080
   - Endpoint: `/connect`

2. **WebSocketEndpoint** - WebSocket handler
   - `@OnOpen`: Create TCP + UDP connections to core server
   - `@OnMessage(Text)`: Translate JSON → Core protocol
   - `@OnMessage(Binary)`: Forward media via UDP
   - `@OnClose`: Cleanup connections

3. **ConnectionContext** - Session state
   - Maps WebSocket session to TCP Socket + UDP DatagramSocket
   - Stores readers/writers for TCP

4. **TCPListener** - Background thread
   - Listens on TCP socket from core server
   - Translates core messages → JSON
   - Sends to WebSocket client

5. **UDPListener** - Background thread
   - Listens on UDP socket (ephemeral port)
   - Receives media from core server
   - Forwards to WebSocket as binary

## Ports

- **8080** - WebSocket server (incoming from browsers)
- **Ephemeral** - UDP sockets (one per WebSocket session)
- **Connects to Core:** TCP 9000, UDP 9001

## Building

**All Platforms:**
```bash
mvn clean package
```

This creates: `target/proxy-server-1.0.0.jar`

## Running

**Windows:**
```cmd
run.bat
```

**Linux/Mac:**
```bash
chmod +x run.sh
./run.sh
```

**Manual:**
```bash
java -jar target/proxy-server-1.0.0.jar
```

## Dependencies (Maven)

- **javax.websocket-api** - WebSocket API
- **Tyrus** - WebSocket implementation (Glassfish)
- **org.json** - JSON parsing

See `pom.xml` for versions.

## Session Management

### Per WebSocket Connection:

```java
WebSocket Session (Browser Tab)
    ├─ TCP Socket → Core Server:9000
    │    ├─ PrintWriter (out)
    │    └─ BufferedReader (in)
    └─ UDP DatagramSocket (ephemeral port)
         └─ Registered with Core Server
```

### Lifecycle:

1. **@OnOpen:**
   - Create TCP socket
   - Create UDP socket (auto-bound to random port)
   - Send `REGISTER_UDP:[port]` via TCP
   - Start TCPListener thread
   - Start UDPListener thread

2. **@OnMessage:**
   - Text: Parse JSON, translate, send via TCP
   - Binary: Forward directly via UDP to core

3. **@OnClose:**
   - Send `DISCONNECT` via TCP
   - Close TCP socket
   - Close UDP socket
   - Threads automatically terminate

## Message Translation

### Browser → Core (Text)

JSON from browser:
```json
{
  "type": "login",
  "username": "john",
  "password": "pass123"
}
```

Translated to Core protocol:
```
LOGIN:john:pass123
```

### Core → Browser (Text)

Core protocol:
```
CHAT:john:Hello everyone
```

Translated to JSON:
```json
{
  "type": "chat",
  "username": "john",
  "message": "Hello everyone"
}
```

### Binary (Media) - Pass-through

Binary data is forwarded as-is between WebSocket and UDP.

## Threading

- **Main Thread:** WebSocket server event loop
- **Per Connection:**
  - WebSocket handler thread (Tyrus managed)
  - TCPListener thread (reads from core server)
  - UDPListener thread (reads media from core server)

## Configuration

To change core server address, edit `WebSocketEndpoint.java`:

```java
private static final String CORE_SERVER_HOST = "localhost";
private static final int CORE_SERVER_TCP_PORT = 9000;
private static final int CORE_SERVER_UDP_PORT = 9001;
```

## Logging

The proxy logs to stdout:
- WebSocket connections/disconnections
- TCP/UDP connection establishment
- Message translation
- Media forwarding

Example:
```
[Proxy] New WebSocket connection: abc-123
[Proxy abc-123] TCP connected to Core Server
[Proxy abc-123] UDP socket bound to port 50123
[Proxy abc-123] Sent to Core: LOGIN:teacher1:pass123
[TCP Listener abc-123] Received from Core: LOGIN_SUCCESS:teacher1
[UDP Listener abc-123] Forwarded 4096 bytes to client
```

## Troubleshooting

**Failed to connect to Core Server:**
- Ensure Tier 3 is running first
- Check core server is on localhost:9000/9001
- Verify firewall not blocking connections

**Port 8080 already in use:**
- Another service is using port 8080
- Change port in ProxyServer.java
- Update client's app.js to match

**WebSocket closes immediately:**
- Check core server is reachable
- Look for exceptions in console
- Verify network connectivity

**Maven build fails:**
- Check Maven version: `mvn -version` (need 3.6+)
- Clear Maven cache: `mvn clean`
- Delete `target/` folder and rebuild

**OutOfMemoryError:**
- Increase heap: `java -Xmx512m -jar target/proxy-server-1.0.0.jar`

## Security Considerations

⚠️ Educational implementation. For production:
- Use WSS (WebSocket Secure) instead of WS
- Add authentication/authorization
- Validate all incoming messages
- Rate limiting per session
- Proper error handling
- Logging to files (not stdout)

## Extending

To add new message types:
1. Add case in `translateToCore()` (Browser → Core)
2. Add case in `translateToClient()` (Core → Browser)
3. Update client's app.js to send/handle new type

To change WebSocket library:
- Replace Tyrus with another (e.g., Jetty, Spring WebSocket)
- Keep same endpoint contract
- Maintain TCP/UDP connections to core server

