# Tier 3: Core Logic Server

## Overview

The core server is implemented using **ONLY** pure Java sockets from the `java.net` package. No frameworks, no Spring, no RMI - just raw TCP and UDP.

## Architecture

### Main Components

1. **CoreServer** - Main entry point
   - Initializes TCP ServerSocket on port 9000
   - Initializes UDP DatagramSocket on port 9001
   - Spawns threads for handling connections

2. **ClientHandler** (per connection)
   - Manages TCP connection from proxy
   - Handles signaling commands (LOGIN, JOIN_ROOM, CHAT, etc.)
   - Stores UDP address for media broadcasting
   - Thread-safe communication

3. **UDPMediaHandler** (single instance)
   - Listens on UDP port 9001
   - Receives media packets from all proxies
   - Broadcasts to room members (excluding sender)
   - Identifies sender and room from packet

4. **RoomManager** (singleton)
   - Thread-safe room state management
   - Uses ConcurrentHashMap
   - Maintains room membership (RoomID → Set<ClientID>)

5. **UserManager** (singleton)
   - User authentication
   - Username/password storage (in-memory)

## Ports

- **TCP 9000** - Signaling and commands
- **UDP 9001** - Media streaming (voice, screen)

## Building

**Windows:**
```cmd
compile.bat
```

**Linux/Mac:**
```bash
chmod +x compile.sh
./compile.sh
```

**Manual:**
```bash
mkdir -p bin
javac -d bin -sourcepath src src/com/tutoring/core/*.java
```

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
java -cp bin com.tutoring.core.CoreServer
```

## State Management

### The Room Manager

The RoomManager is the heart of the state management:

```java
ConcurrentHashMap<String, Set<String>> rooms;
// RoomName → Set of ClientIDs
```

**Thread Safety:**
- Uses ConcurrentHashMap for room map
- Synchronized Set for room members
- Synchronized methods for mutations

### Client Handler Lookup

```java
ConcurrentHashMap<String, ClientHandler> clientHandlers;
// ClientID → ClientHandler
```

Each ClientHandler stores:
- TCP Socket (for signaling)
- UDP Address (InetSocketAddress for media)
- Current room
- Username

## Media Broadcasting Logic

### Registration Flow

1. Proxy connects via TCP
2. ClientHandler created with unique ID
3. Proxy sends: `REGISTER_UDP:[port]`
4. Handler stores: `udpAddress = new InetSocketAddress(socket.getInetAddress(), port)`

### Broadcasting Flow

1. UDP packet arrives at port 9001
2. Parse packet to extract: `clientId, roomId, mediaType, data`
3. Look up: `roomManager.getRoomMembers(roomId)` → Set<ClientID>
4. For each member (except sender):
   - Get `ClientHandler handler = clientHandlers.get(memberId)`
   - Get `InetSocketAddress addr = handler.getUdpAddress()`
   - Create new DatagramPacket with media data
   - `socket.send(packet)` to that address

### Key Insight

The Core Server uses **ONE** UDP socket for all communication:
- `receive()` from any proxy
- `send()` to specific proxy addresses

This is efficient and leverages UDP's connectionless nature.

## Protocol

See [Protocol Specifications](../README.md#protocol-specifications) in main README.

## Threading Model

- **Main Thread:** Accepts TCP connections
- **ClientHandler Threads:** One per TCP connection
- **UDPMediaHandler Thread:** Single thread for all UDP
- **All threads** coordinate via RoomManager

## Default Users

For testing, default users are created:
- `teacher1` / `pass123`
- `student1` / `pass123`

## Logging

The server logs to stdout:
- Connection events
- Command processing
- Room operations
- Media broadcasts

Example:
```
[Core Server] TCP Server started on port 9000
[ClientHandler abc123] User logged in: teacher1
[ClientHandler abc123] Room created: Math101
[UDP Handler] Broadcast VOICE from abc123 in room Math101 to 2 recipients (4096 bytes)
```

## Troubleshooting

**Port already in use:**
```
Address already in use: JVM_Bind
```
- Another instance is running
- Use `netstat -ano | findstr 9000` (Windows)
- Use `lsof -i :9000` (Linux/Mac)

**OutOfMemoryError:**
- Too many connections
- Increase heap: `java -Xmx512m -cp bin com.tutoring.core.CoreServer`

**No response from client:**
- Check ClientHandler thread is running
- Verify TCP socket is open
- Check for exceptions in logs

## Security Considerations

⚠️ This is an educational implementation. For production:
- Use TLS/SSL for TCP
- Hash passwords with bcrypt
- Add authentication tokens
- Rate limiting
- Input validation
- Proper error handling

## Extending

To add new commands:
1. Define protocol in `handleMessage()` in ClientHandler
2. Add case in switch statement
3. Implement handler method
4. Update RoomManager/UserManager if needed

To add new media types:
1. Define new type constant (e.g., 3 = Video)
2. Update UDPMediaHandler parsing
3. Update client to send new type

