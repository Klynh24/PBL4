# Quick Test Guide - UDP Fragmentation Fix

## 🚀 Quick Start (5 Minutes)

### Step 1: Rebuild Proxy (30 seconds)
```bash
cd tier2-proxy-server
mvn compile  # Compiles the new fragmentation code
```

**Expected Output**:
```
[INFO] BUILD SUCCESS
[INFO] Total time:  3.860 s
```

---

### Step 2: Stop Old Proxy
Press `Ctrl+C` in the proxy server console window.

---

### Step 3: Start Fixed Proxy (15 seconds)
```bash
.\run.bat  # Windows
./run.sh   # Linux/Mac
```

**Look For**:
```
[INFO] WebSocket server started.
WebSocket Registered apps: URLs all start with ws://0.0.0.0:8089
```

---

### Step 4: Test Screen Sharing (2 minutes)

#### Browser 1 (Teacher)
1. Open: http://localhost:8081 (or wherever web client is hosted)
2. Login: `teacher1` / `pass123`
3. Create/join room: `123`
4. Click **"Share Screen"**
5. Select screen/window to share
6. Click **"Share"**

**Check Proxy Logs**:
```
✅ [Proxy xxx] Fragmented 153847 bytes into 113 UDP packets (frame #0)
```

**If you see this** → Fragmentation is working! ✅

---

#### Browser 2 (Student)
1. Open http://localhost:8081 in **different browser or incognito**
2. Login: `student1` / `pass123`
3. Join room: `123`
4. **VERIFY**: You see teacher's screen (not black!)

**Success Indicators**:
- ✅ Screen is visible
- ✅ Updates every ~1 second
- ✅ No "No screen sharing active" message

---

### Step 5: Verify Logs (30 seconds)

#### Proxy Server Console
**Before Fix (BAD)**:
```
❌ [Proxy] Error forwarding binary data: The message is larger than...
❌ [Proxy] Error forwarding binary data: The message is larger than...
❌ (repeated many times)
```

**After Fix (GOOD)**:
```
✅ [Proxy d100e1a6] Fragmented 153847 bytes into 113 UDP packets (frame #0)
✅ [Proxy d100e1a6] Fragmented 149234 bytes into 109 UDP packets (frame #1)
✅ [Proxy d100e1a6] Fragmented 151092 bytes into 111 UDP packets (frame #50)
```

#### Core Server Console
**Before Fix (BAD)**:
```
❌ (No UDP packets received - silence)
```

**After Fix (GOOD)**:
```
✅ [UDP Handler] Received packet from /127.0.0.1:53356
✅ [UDP Handler] Magic: 0x53435245, Frame: 0, Fragment: 0/113
✅ [UDP Handler] Frame complete, broadcasting to room
```

---

## Expected Results

### Proxy Server
```
[Proxy d100e1a6-0ca6-4850-baaf-a39b021908b4] New WebSocket connection
[Proxy d100e1a6] TCP connected to Core Server
[Proxy d100e1a6] UDP socket bound to port 53356
[Proxy d100e1a6] Fragmented 153847 bytes into 113 UDP packets (frame #0)
[Proxy d100e1a6] Fragmented 149234 bytes into 109 UDP packets (frame #1)
[Proxy d100e1a6] Fragmented 151092 bytes into 111 UDP packets (frame #2)
```

### Core Server
```
[ClientHandler xxx] User logged in: teacher1
[ClientHandler xxx] Room created: 123
[UDP Handler] Received packet from /127.0.0.1:53356
[UDP Handler] Processing fragmented frame (113 packets expected)
[UDP Handler] Frame 0 complete - broadcasting to room 123
```

### Web Client (Browser Console F12)
```
[Reassembler] Frame complete: 153847 bytes
[App] Displaying screen frame
✅ Image rendered successfully
```

---

## Troubleshooting

### Issue: Still See "Message Too Large" Error

**Cause**: Old proxy code still running

**Solution**:
1. Stop proxy completely
2. Check task manager (kill java.exe if needed)
3. Rebuild: `mvn clean compile`
4. Restart proxy

---

### Issue: No UDP Packets at Core Server

**Cause**: Firewall or wrong port

**Check**:
```bash
netstat -an | grep 9001  # Core Server UDP port
```

**Solution**:
- Verify Core Server is on port 9001
- Check Windows Firewall settings
- Verify proxy connects to localhost:9001

---

### Issue: Screen Shows But Is Black

**Cause**: Fragmentation working, but reassembly failing

**Check Core Server Logs**:
```
[UDP Handler] Frame incomplete - missing fragments
```

**Solution**:
- Check for packet loss
- Verify all 28-byte headers are correct
- Check magic number validation in Core Server

---

### Issue: "BUILD FAILURE" When Compiling

**Error**: `Failed to delete target/proxy-server-1.0.0.jar`

**Cause**: Proxy still running (JAR in use)

**Solution**:
```bash
# Stop proxy first
# Then compile
mvn compile  # Skip clean, just compile
```

---

## Performance Check

### Normal Operation

**Frame Size**: 50KB - 200KB (varies with screen content)  
**Fragments**: 37 - 150 packets per frame  
**Frame Rate**: 5 FPS (one frame every ~200ms)  
**Bandwidth**: 2-10 Mbps  
**Overhead**: ~2% (28-byte headers)  

### Monitoring

Watch proxy logs for:
```
[Proxy xxx] Fragmented 153847 bytes into 113 UDP packets (frame #0)
```

**If fragments > 200**: Frame is unusually large, may indicate:
- Very high resolution (4K)
- Complex screen content
- JPEG compression not working

---

## Success Criteria

After the fix, you should have:

- [x] Proxy compiles successfully
- [x] Proxy starts without errors
- [x] Web client connects to proxy
- [x] Screen sharing button works
- [x] **Proxy logs show fragmentation** (key indicator!)
- [x] Core Server receives UDP packets
- [x] Student sees teacher's screen
- [x] No "message too large" errors
- [x] Screen updates smoothly

**If all checkboxes pass** → ✅ **FIX IS WORKING!**

---

## One-Command Test

For quick verification:

```bash
# Terminal 1: Core Server
cd tier3-core-server && .\run.bat

# Terminal 2: Proxy (with fix)
cd tier2-proxy-server && mvn compile && .\run.bat

# Browser 1: http://localhost:8081
# Login: teacher1, share screen

# Browser 2: http://localhost:8081 (incognito)
# Login: student1, join same room
# VERIFY: Screen visible!
```

**Total Time**: ~3 minutes

---

## Revert Instructions (If Needed)

If something goes wrong:

```bash
cd tier2-proxy-server

# Restore from git
git checkout HEAD WebSocketEndpoint.java
git checkout HEAD ConnectionContext.java
rm src/main/java/com/tutoring/proxy/FrameFragmenter.java

# Recompile
mvn compile

# Restart
.\run.bat
```

**Note**: Old code will have the "message too large" error again.

---

**Fix Status**: ✅ Ready for Testing  
**Compilation**: ✅ Success  
**Estimated Testing Time**: 5 minutes  

**Go ahead and test - screen sharing should work now!** 🎉

