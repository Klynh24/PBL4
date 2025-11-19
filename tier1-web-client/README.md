# Tier 1: Web Client

## Overview

The web client is a browser-based application that provides the user interface for the online tutoring system.

## Files

- `index.html` - Login and registration page
- `room.html` - Main room interface with chat, screen sharing, and voice
- `css/style.css` - Styling and responsive design
- `js/auth.js` - Authentication form handling
- `js/app.js` - Main application logic, WebSocket communication, media handling

## Running

### Option 1: Direct File Access
Simply open `index.html` in your browser.

### Option 2: Local Web Server (Recommended for full functionality)

**Python 3:**
```bash
python -m http.server 3000
```

**Node.js:**
```bash
npx http-server -p 3000
```

Then open: http://localhost:3000

## Configuration

To change the WebSocket proxy server address, edit `js/app.js`:

```javascript
const PROXY_SERVER_URL = 'ws://localhost:8080/connect';
```

## Browser Requirements

- Modern browser with WebRTC support (Chrome 90+, Firefox 88+, Edge 90+)
- JavaScript enabled
- Microphone access for voice chat
- Screen sharing permission for screen share feature

## Features

### Authentication
- Tab-based login/register interface
- Session storage for credentials
- Demo accounts provided

### Room Management
- Live room list with participant counts
- Create and join rooms
- Leave room functionality

### Text Chat
- Real-time messaging
- System notifications
- Scrollable message history

### Voice Chat
- Microphone access via getUserMedia
- Real-time audio streaming
- Volume level indicator
- Enable/disable toggle

### Screen Sharing
- Screen/window capture via getDisplayMedia
- Real-time frame capture
- JPEG compression
- Canvas-based display

## Media Packet Format

When sending voice/screen data via WebSocket (binary):

```
[ClientID_Length (4 bytes)]
[ClientID (UTF-8 string)]
[RoomID_Length (4 bytes)]
[RoomID (UTF-8 string)]
[MediaType (1 byte)]  // 1=Voice, 2=Screen
[Media Data]          // Int16 PCM for voice, JPEG for screen
```

## Troubleshooting

**Screen sharing not working:**
- Browser security requires HTTPS or localhost
- Grant permissions when prompted
- Try Chrome (best WebRTC support)

**Voice not working:**
- Grant microphone permission
- Check device isn't used by another app
- Verify input device in browser settings

**Can't connect:**
- Ensure proxy server is running on port 8080
- Check WebSocket URL in app.js
- Open browser console (F12) for errors

## Development

To modify the UI:
1. Edit HTML files for structure
2. Edit CSS for styling
3. Edit JS for behavior

To add features:
- Add UI elements in `room.html`
- Add styling in `css/style.css`
- Add logic in `js/app.js`
- Update message handling in `handleTextMessage()`

## Security Notes

⚠️ This is an educational project. For production:
- Use HTTPS/WSS instead of HTTP/WS
- Implement proper authentication tokens
- Add input validation and sanitization
- Use secure session management

