// Main application logic for room page

const PROXY_SERVER_URL = "wss://192.168.98.93/connect";

class TutoringApp {
  constructor() {
    this.ws = null;
    this.username = sessionStorage.getItem("username");
    this.password = sessionStorage.getItem("password");
    this.authType = sessionStorage.getItem("authType");
    this.currentRoom = null;
    this.clientId = this.generateClientId();

    // Media streams
    this.voiceStream = null;
    this.screenStream = null;
    this.isVoiceActive = false;
    this.isScreenSharing = false;

    // Audio context for voice
    this.audioContext = null;
    this.audioDestination = null;

    // ✅ NEW: Jitter buffer for voice
    this.jitterBuffer = null;

    // Chat history loading state
    this.isLoadingHistory = false;

    // Frame reassembler for advanced screen sharing
    this.frameReassembler = null;

    // ✅ FIX: Capture interval ID for dynamic quality adjustment
    this.captureIntervalId = null;
    this.captureVideoElement = null;
    this.captureCanvas = null;

    // ✅ NEW: Priority sender for WebSocket binary messages
    this.prioritySender = null;
    this.captureCtx = null;

    // ✅ NEW: H.264 Hardware Decoder (WebCodecs API)
    this.h264Decoder = null;
    this.videoCanvas = null;
    this.useH264Decoding = false;

    // ✅ PERFORMANCE OPTIMIZATION: Lower default quality for low-spec/localhost testing
    // Reduced resolution, FPS, and JPEG quality to reduce CPU usage
    this.currentQuality = {
      level: "HIGH",
      width: 1280, // Max 720p (was 1920)
      height: 720, // Max 720p (was 1080)
      fps: 10, // 10 FPS target (was 15)
      jpegQuality: 0.5, // Medium quality (was 0.85)
    };

    // Media device availability (fix for missing camera/microphone)
    this.mediaDevicesAvailable = false;
    this.audioInputAvailable = false;
    this.checkMediaDeviceAvailability();

    // Check if user is logged in
    if (!this.username || !this.password) {
      window.location.href = "index.html";
      return;
    }

    this.initializeUI();
    this.connectToServer();
  }

  generateClientId() {
    return "client_" + Math.random().toString(36).substr(2, 9);
  }

  /**
   * Check if media devices are available (fix for missing camera/mic hardware)
   */
  async checkMediaDeviceAvailability() {
    try {
      // Check if getUserMedia API exists
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        console.warn("[Media] getUserMedia not supported by browser");
        this.mediaDevicesAvailable = false;
        this.audioInputAvailable = false;
        return;
      }

      this.mediaDevicesAvailable = true;

      // Try to enumerate devices to check for audio input
      if (navigator.mediaDevices.enumerateDevices) {
        const devices = await navigator.mediaDevices.enumerateDevices();
        this.audioInputAvailable = devices.some(
          (device) => device.kind === "audioinput"
        );

        if (!this.audioInputAvailable) {
          console.warn("[Media] No audio input devices found");
        } else {
          console.log("[Media] Audio input devices available");
        }
      } else {
        // If we can't enumerate, assume devices are available
        this.audioInputAvailable = true;
      }
    } catch (error) {
      console.warn("[Media] Error checking device availability:", error);
      this.mediaDevicesAvailable = false;
      this.audioInputAvailable = false;
    }
  }

  initializeUI() {
    document.getElementById("username-display").textContent = this.username;

    // Button handlers
    document
      .getElementById("logoutBtn")
      .addEventListener("click", () => this.logout());
    document
      .getElementById("refreshRoomsBtn")
      .addEventListener("click", () => this.refreshRooms());
    document
      .getElementById("createRoomBtn")
      .addEventListener("click", () => this.createRoom());
    document
      .getElementById("leaveRoomBtn")
      .addEventListener("click", () => this.leaveRoom());
    document
      .getElementById("shareScreenBtn")
      .addEventListener("click", () => this.startScreenShare());
    document
      .getElementById("stopScreenBtn")
      .addEventListener("click", () => this.stopScreenShare());
    document
      .getElementById("toggleVoiceBtn")
      .addEventListener("click", () => this.toggleVoice());
    document
      .getElementById("sendChatBtn")
      .addEventListener("click", () => this.sendChat());

    // Chat input enter key
    document.getElementById("chatInput").addEventListener("keypress", (e) => {
      if (e.key === "Enter") {
        this.sendChat();
      }
    });
  }

  connectToServer() {
    this.updateStatus("Connecting...", false);

    try {
      this.ws = new WebSocket(PROXY_SERVER_URL);

      this.ws.onopen = () => {
        console.log("WebSocket connected");
        this.updateStatus("Connected", true);

        // ✅ ADVANCED PIPELINE: Enable FrameReassembler
        // Proxy Server now ALWAYS fragments packets using FrameFragmenter
        // This allows sending high-quality frames > 65KB
        // FrameReassembler will reassemble fragments on client side
        if (typeof FrameReassembler !== "undefined") {
          this.frameReassembler = new FrameReassembler(this.ws);
          // ✅ NEW: Set callback for frame-ready events (with rate limiting)
          this.frameReassembler.setOnFrameReady((frame) => {
            this.displayScreenFrame(frame);
          });
          console.log(
            "[App] FrameReassembler ENABLED for advanced streaming with rate limiting"
          );
        } else {
          console.warn(
            "[App] FrameReassembler not loaded - using fallback mode"
          );
          this.frameReassembler = null;
        }

        // ✅ NEW: Initialize H.264 Hardware Decoder (WebCodecs API)
        this.initH264Decoder();

        // ✅ NEW: Initialize Priority Sender for packet prioritization
        if (typeof PrioritySender !== "undefined") {
          this.prioritySender = new PrioritySender(this.ws);
          console.log(
            "[App] ✅ PrioritySender ENABLED for packet prioritization"
          );
        } else {
          console.warn("[App] PrioritySender not loaded - using direct send");
          this.prioritySender = null;
        }

        this.authenticate();
      };

      this.ws.onmessage = (event) => {
        if (typeof event.data === "string") {
          this.handleTextMessage(event.data);
        } else {
          this.handleBinaryMessage(event.data);
        }
      };

      this.ws.onerror = (error) => {
        console.error("WebSocket error:", error);
        this.updateStatus("Connection error", false);
      };

      this.ws.onclose = () => {
        console.log("WebSocket closed");
        this.updateStatus("Disconnected", false);
        setTimeout(() => this.connectToServer(), 3000);
      };
    } catch (error) {
      console.error("Failed to connect:", error);
      this.updateStatus("Connection failed", false);
    }
  }

  authenticate() {
    const message = {
      type: this.authType,
      username: this.username,
      password: this.password,
    };
    this.sendMessage(message);
  }

  handleTextMessage(data) {
    try {
      const message = JSON.parse(data);
      console.log("Received:", message);

      switch (message.type) {
        case "login_success":
          console.log("Login successful");
          this.refreshRooms();
          break;

        case "register_success":
          console.log("Registration successful");
          this.refreshRooms();
          break;

        case "room_list":
          this.updateRoomList(message.rooms);
          break;

        case "room_created":
        case "room_joined":
          this.currentRoom = message.room;
          this.showRoomUI();
          this.addSystemMessage("Joined room: " + message.room);
          // ✅ FIX: Reset frame reassembler to wait for keyframe
          // Client mới sẽ nhận keyframe từ sender (nếu có sender đang share)
          if (this.frameReassembler) {
            this.frameReassembler.resetStats();
          }
          break;

        case "room_left":
          this.currentRoom = null;
          this.hideRoomUI();
          break;

        case "chat_history_start":
          this.isLoadingHistory = true;
          console.log("Loading chat history...");
          break;

        case "chat_history_end":
          this.isLoadingHistory = false;
          console.log("Chat history loaded");
          this.addSystemMessage("📜 Chat history loaded");
          break;

        case "chat":
          if (this.isLoadingHistory) {
            // Historical message
            this.addChatMessage(message.username, message.message, false, true);
          } else {
            // New real-time message
            this.addChatMessage(message.username, message.message);
          }
          break;

        case "user_joined":
          this.addSystemMessage(message.username + " joined the room");
          this.refreshRooms(); // ✅ FIX: Refresh room list to update participant count
          break;

        case "user_left":
          this.addSystemMessage(message.username + " left the room");
          this.refreshRooms(); // ✅ FIX: Refresh room list to update participant count
          break;

        case "set_quality":
          this.handleQualityChange(message);
          break;

        case "error":
          alert("Error: " + message.error);
          break;
      }
    } catch (error) {
      console.error("Error parsing message:", error);
    }
  }

  async handleBinaryMessage(data) {
    try {
      // Received media data from server
      const arrayBuffer = await data.arrayBuffer();

      if (arrayBuffer.byteLength === 0) return;

      // ✅ ADVANCED PIPELINE: Try frame reassembler first (for fragmented packets)
      if (this.frameReassembler) {
        // ✅ FIX: processPacket no longer returns frame directly
        // Frames are rendered via callback with rate limiting
        this.frameReassembler.processPacket(arrayBuffer);
        return; // Don't fallback - frame will be rendered via callback when ready
      }

      // ✅ FALLBACK: Direct display for non-fragmented packets (if reassembler disabled)
      // This handles case when FrameReassembler script not loaded
      try {
        const blob = new Blob([arrayBuffer], { type: "image/jpeg" });
        const url = URL.createObjectURL(blob);

        const sharedScreenEl = document.getElementById("sharedScreen");
        const noScreenEl = document.getElementById("noScreen");

        if (sharedScreenEl) {
          const img = new Image();
          img.onload = () => {
            // Hide "No screen sharing" message
            if (noScreenEl) {
              noScreenEl.style.display = "none";
            }

            // Clear previous content
            sharedScreenEl.innerHTML = "";
            sharedScreenEl.appendChild(img);

            // Revoke old URL to prevent memory leaks
            URL.revokeObjectURL(url);
          };
          img.onerror = () => {
            // Not a valid JPEG, might be audio data
            console.log("[Binary] Not an image, trying as audio");
            this.playReceivedAudio(arrayBuffer);
            URL.revokeObjectURL(url);
          };
          img.src = url;
          img.style.width = "100%";
          img.style.height = "auto";
        } else {
          // sharedScreen element not found, might be audio
          this.playReceivedAudio(arrayBuffer);
        }
      } catch (error) {
        // Failed to display as image, try as audio
        console.log("[Binary] Display failed, trying as audio:", error);
        this.playReceivedAudio(arrayBuffer);
      }
    } catch (error) {
      console.error("Error handling binary message:", error);
    }
  }

  /**
   * ✅ NEW: Initialize H.264 Hardware Decoder
   */
  initH264Decoder() {
    try {
      // Check if H264Decoder class is available
      if (typeof H264Decoder === "undefined") {
        console.warn(
          "[H264Decoder] H264Decoder class not loaded - falling back to JPEG"
        );
        this.useH264Decoding = false;
        return;
      }

      // Get canvas element
      const canvas = document.getElementById("screenCanvas");
      if (!canvas) {
        console.warn(
          "[H264Decoder] Canvas element not found - falling back to JPEG"
        );
        this.useH264Decoding = false;
        return;
      }

      // Initialize decoder
      this.h264Decoder = new H264Decoder(canvas, (frame, error) => {
        if (error) {
          console.error("[H264Decoder] Decode error:", error);
          // Don't immediately disable - might be temporary error
          // Only disable if decoder is in error state
          if (
            this.h264Decoder &&
            this.h264Decoder.decoder &&
            this.h264Decoder.decoder.state === "closed"
          ) {
            console.warn("[H264Decoder] Decoder closed, falling back to JPEG");
            this.useH264Decoding = false;
          }
        } else {
          // Frame decoded successfully - hide "No screen" message
          const noScreenEl = document.getElementById("noScreen");
          if (noScreenEl) {
            noScreenEl.style.display = "none";
          }
        }
      });

      if (this.h264Decoder.isSupported) {
        this.useH264Decoding = true;
        this.videoCanvas = canvas;

        // Setup canvas styling for video display
        canvas.style.width = "100%";
        canvas.style.height = "auto";
        canvas.style.objectFit = "contain";
        canvas.style.display = "none"; // Hidden by default, shown when H.264 frames arrive

        // Initialize decoder asynchronously
        this.h264Decoder
          .init()
          .then(() => {
            console.log(
              "[H264Decoder] ✅ Hardware-accelerated H.264 decoder initialized and ready"
            );
          })
          .catch((error) => {
            console.error("[H264Decoder] Initialization failed:", error);
            this.useH264Decoding = false;
          });
      } else {
        console.warn(
          "[H264Decoder] WebCodecs not supported - falling back to JPEG"
        );
        this.useH264Decoding = false;
      }
    } catch (error) {
      console.error("[H264Decoder] Initialization error:", error);
      this.useH264Decoding = false;
    }
  }

  displayScreenFrame(frame) {
    try {
      // ✅ FIX: Skip displaying frames if we're currently sharing (have local preview)
      // Teacher who is sharing already sees local preview, don't overwrite with received frames
      if (this.isScreenSharing && this.screenStream) {
        // We're the sender - skip displaying our own frames
        // Local preview is already showing
        return;
      }

      // Extract frame data (remove media type byte if present)
      let frameData = frame.data;
      const dataView = new DataView(frame.data.buffer || frame.data);
      const firstByte = dataView.getUint8(0);

      if (firstByte === 2) {
        // Remove media type byte
        frameData = frame.data.slice(1);
      }

      // ✅ NEW: Auto-detect format and decode accordingly
      // Check if data is JPEG (magic number: 0xFF 0xD8)
      const isJPEG =
        frameData.byteLength >= 2 &&
        dataView.getUint8(0) === 0xff &&
        dataView.getUint8(1) === 0xd8;

      // Try H.264 decoding if:
      // 1. H.264 decoder is enabled and initialized
      // 2. Data is NOT JPEG (could be H.264)
      // 3. Data looks like H.264 (check if WebCodecs is supported)
      if (
        !isJPEG &&
        this.useH264Decoding &&
        this.h264Decoder &&
        this.h264Decoder.initialized &&
        typeof H264Decoder !== "undefined" &&
        H264Decoder.isH264(frameData)
      ) {
        // Decode H.264 frame
        this.decodeH264Frame(frameData, frame);
        return;
      }

      // ✅ FALLBACK: JPEG decoding (original method or if H.264 decode failed)
      this.displayJPEGFrame(frameData);
    } catch (error) {
      console.error("Error displaying screen frame:", error);
    }
  }

  /**
   * ✅ NEW: Decode and display H.264 frame
   */
  decodeH264Frame(frameData, frame) {
    try {
      const isKeyframe = frame.isKeyframe || false;
      const timestamp = frame.timestamp
        ? frame.timestamp * 1000
        : Date.now() * 1000; // Convert to microseconds

      // Hide JPEG container, show canvas for H.264
      const sharedScreenEl = document.getElementById("sharedScreen");
      const noScreenEl = document.getElementById("noScreen");
      if (sharedScreenEl) {
        // Hide all img elements in JPEG container
        const imgElements = sharedScreenEl.querySelectorAll("img");
        imgElements.forEach((img) => (img.style.display = "none"));
      }
      if (this.videoCanvas) {
        this.videoCanvas.style.display = "block"; // Show H.264 canvas
      }

      // Configure decoder on keyframe
      if (isKeyframe && this.h264Decoder && this.h264Decoder.initialized) {
        try {
          const config = H264Decoder.extractConfig(frameData);
          if (config && config.description) {
            this.h264Decoder.configure(config);
            console.log("[H264Decoder] ✅ Decoder configured with keyframe");
          }
        } catch (configError) {
          console.warn("[H264Decoder] Config extraction failed:", configError);
        }
      }

      // Decode frame
      if (this.h264Decoder && this.h264Decoder.decoder) {
        const success = this.h264Decoder.decode(
          frameData,
          isKeyframe,
          timestamp
        );

        if (!success && this.h264Decoder.decoder.state === "configured") {
          // Decode failed but decoder is configured - might be temporary error
          // Don't fallback immediately, decoder might recover
          console.warn(
            "[H264Decoder] Decode returned false, but decoder is configured"
          );
        } else if (!success) {
          // Decoder not configured or serious error - fallback to JPEG
          console.warn("[H264Decoder] Decode failed, falling back to JPEG");
          this.useH264Decoding = false;
          this.displayJPEGFrame(frameData);
        } else {
          // Success - hide "No screen" message
          if (noScreenEl) {
            noScreenEl.style.display = "none";
          }
        }
      } else {
        // Decoder not ready - fallback
        this.displayJPEGFrame(frameData);
      }
    } catch (error) {
      console.error("[H264Decoder] Error decoding frame:", error);
      // Fallback to JPEG
      this.useH264Decoding = false;
      this.displayJPEGFrame(frameData);
    }
  }

  /**
   * ✅ NEW: Display JPEG frame (fallback method)
   */
  displayJPEGFrame(jpegData) {
    try {
      // Hide H.264 canvas, show JPEG image
      if (this.videoCanvas) {
        this.videoCanvas.style.display = "none";
      }

      // Show JPEG image container
      const sharedScreenEl = document.getElementById("sharedScreen");
      const noScreenEl = document.getElementById("noScreen");

      if (sharedScreenEl) {
        // Create or reuse img element
        let img = sharedScreenEl.querySelector("img");
        if (!img) {
          img = new Image();
          img.style.width = "100%";
          img.style.height = "auto";
          img.style.objectFit = "contain";
          img.style.display = "block";
          sharedScreenEl.appendChild(img);
        }

        // Create blob and display
        const blob = new Blob([jpegData], { type: "image/jpeg" });
        const url = URL.createObjectURL(blob);

        img.onload = () => {
          if (noScreenEl) {
            noScreenEl.style.display = "none";
          }
          setTimeout(() => URL.revokeObjectURL(url), 100);
        };

        img.onerror = () => {
          console.error(
            "[FrameDisplay] Failed to load JPEG image, size:",
            jpegData.byteLength
          );
          const bytes = new Uint8Array(jpegData.slice(0, 10));
          console.error(
            "[FrameDisplay] First bytes:",
            Array.from(bytes)
              .map((b) => "0x" + b.toString(16).padStart(2, "0"))
              .join(" ")
          );
          URL.revokeObjectURL(url);
        };

        img.src = url;
      }
    } catch (error) {
      console.error("Error displaying JPEG frame:", error);
    }
  }

  sendMessage(message) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
    }
  }

  sendBinary(data) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      return;
    }

    // ✅ NEW: Use priority sender if available
    if (this.prioritySender) {
      // Determine priority from packet data
      const priority = PacketPriority.fromPacket(data);
      this.prioritySender.send(data, priority);
    } else {
      // Fallback: direct send
      this.ws.send(data);
    }
  }

  refreshRooms() {
    this.sendMessage({ type: "list_rooms" });
  }

  updateRoomList(roomsString) {
    const roomList = document.getElementById("roomList");
    roomList.innerHTML = "";

    if (!roomsString || roomsString === "") {
      roomList.innerHTML = '<p class="loading">No rooms available</p>';
      return;
    }

    const rooms = roomsString.split(",").filter((r) => r.trim() !== "");

    rooms.forEach((roomStr) => {
      const match = roomStr.match(/(.+)\((\d+)\)/);
      if (match) {
        const roomName = match[1];
        const userCount = match[2];

        const roomItem = document.createElement("div");
        roomItem.className = "room-item";
        if (roomName === this.currentRoom) {
          roomItem.classList.add("active");
        }

        roomItem.innerHTML = `
                    <div class="room-name">${roomName}</div>
                    <div class="room-users">👥 ${userCount} user(s)</div>
                `;

        roomItem.addEventListener("click", () => this.joinRoom(roomName));
        roomList.appendChild(roomItem);
      }
    });
  }

  createRoom() {
    const roomName = document.getElementById("roomNameInput").value.trim();
    if (!roomName) {
      alert("Please enter a room name");
      return;
    }

    this.sendMessage({
      type: "create_room",
      room: roomName,
    });

    document.getElementById("roomNameInput").value = "";
  }

  joinRoom(roomName) {
    if (this.currentRoom === roomName) return;

    if (this.currentRoom) {
      this.leaveRoom();
    }

    this.sendMessage({
      type: "join_room",
      room: roomName,
    });
  }

  leaveRoom() {
    if (!this.currentRoom) return;

    this.stopScreenShare();
    this.stopVoice();

    this.sendMessage({ type: "leave_room" });
  }

  showRoomUI() {
    document.getElementById("notInRoom").style.display = "none";
    document.getElementById("inRoom").style.display = "flex";
    document.getElementById("currentRoomName").textContent = this.currentRoom;
    document.getElementById("chatInput").disabled = false;
    document.getElementById("sendChatBtn").disabled = false;

    // Clear chat
    const chatMessages = document.getElementById("chatMessages");
    chatMessages.innerHTML = "";

    this.refreshRooms();
  }

  hideRoomUI() {
    document.getElementById("notInRoom").style.display = "flex";
    document.getElementById("inRoom").style.display = "none";
    document.getElementById("chatInput").disabled = true;
    document.getElementById("sendChatBtn").disabled = true;

    this.refreshRooms();
  }

  sendChat() {
    const input = document.getElementById("chatInput");
    const message = input.value.trim();

    if (!message) return;

    this.sendMessage({
      type: "chat",
      message: message,
    });

    // Add own message to chat
    this.addChatMessage(this.username, message, true);
    input.value = "";
  }

  addChatMessage(username, text, isOwn = false, isHistory = false) {
    const chatMessages = document.getElementById("chatMessages");
    const messageEl = document.createElement("div");

    let className = "chat-message";
    if (isOwn) className += " own";
    if (isHistory) className += " history";

    messageEl.className = className;

    const time = new Date().toLocaleTimeString([], {
      hour: "2-digit",
      minute: "2-digit",
    });

    messageEl.innerHTML = `
            <div class="chat-username">${username}</div>
            <div class="chat-text">${this.escapeHtml(text)}</div>
            <div class="chat-time">${time}</div>
        `;

    chatMessages.appendChild(messageEl);

    // Only auto-scroll if not loading history or if already at bottom
    if (!isHistory) {
      chatMessages.scrollTop = chatMessages.scrollHeight;
    }
  }

  addSystemMessage(text) {
    const chatMessages = document.getElementById("chatMessages");
    const messageEl = document.createElement("div");
    messageEl.className = "chat-message system";
    messageEl.textContent = text;

    chatMessages.appendChild(messageEl);
    chatMessages.scrollTop = chatMessages.scrollHeight;
  }

  async startScreenShare() {
    // Check if getDisplayMedia is available (should work even without camera)
    if (!navigator.mediaDevices || !navigator.mediaDevices.getDisplayMedia) {
      alert(
        "Screen sharing is not supported by your browser.\n\nPlease use a modern browser like Chrome, Firefox, or Edge."
      );
      console.warn("[ScreenShare] getDisplayMedia not supported");
      return;
    }

    try {
      // ✅ PERFORMANCE OPTIMIZATION: Limit resolution to 720p max and frame rate to 10 FPS
      // This reduces CPU usage significantly for low-spec/localhost testing
      // CRITICAL FIX: Disable audio capture to prevent feedback loop
      this.screenStream = await navigator.mediaDevices.getDisplayMedia({
        video: {
          mediaSource: "screen",
          width: { ideal: 1280, max: 1280 }, // ✅ Limit to 720p max
          height: { ideal: 720, max: 720 }, // ✅ Limit to 720p max
          frameRate: { ideal: 10, max: 15 }, // ✅ Target 10 FPS (max 15)
        },
        audio: false, // ✅ FIX: Disable audio to prevent feedback loop
      });

      this.isScreenSharing = true;
      document.getElementById("shareScreenBtn").style.display = "none";
      document.getElementById("stopScreenBtn").style.display = "block";

      // ✅ FIX: Reset capture elements for new stream
      this.captureVideoElement = null;
      this.captureCanvas = null;
      this.captureCtx = null;
      this.frameCount = 0;

      // ✅ FIX: Show local preview for teacher (immediate feedback)
      this.showLocalScreenPreview(this.screenStream);

      // Capture and send screen frames
      this.captureScreenFrames();

      // Handle stream end
      this.screenStream.getVideoTracks()[0].onended = () => {
        this.stopScreenShare();
      };
    } catch (error) {
      console.error("[ScreenShare] Error sharing screen:", error);

      // Provide user-friendly error messages
      let errorMessage = "Failed to share screen.\n\n";
      if (
        error.name === "NotAllowedError" ||
        error.name === "PermissionDeniedError"
      ) {
        errorMessage +=
          "Screen sharing permission was denied.\n\nPlease click 'Share' when prompted.";
      } else if (error.name === "NotSupportedError") {
        errorMessage +=
          "Screen sharing is not supported on this browser or system.";
      } else {
        errorMessage += "Error: " + error.message;
      }

      alert(errorMessage);
    }
  }

  captureScreenFrames() {
    if (!this.screenStream || !this.isScreenSharing) return;

    // ✅ FIX: Reuse video element if exists
    if (!this.captureVideoElement) {
      this.captureVideoElement = document.createElement("video");
      this.captureVideoElement.srcObject = this.screenStream;
      this.captureVideoElement.play();
    }

    // ✅ FIX: Reuse canvas if exists
    if (!this.captureCanvas) {
      this.captureCanvas = document.createElement("canvas");
      this.captureCtx = this.captureCanvas.getContext("2d");
    }

    const video = this.captureVideoElement;
    const canvas = this.captureCanvas;
    const ctx = this.captureCtx;

    // Wait for video to be ready
    if (video.readyState < 2) {
      video.onloadedmetadata = () => {
        this.startCaptureLoop(video, canvas, ctx);
      };
    } else {
      this.startCaptureLoop(video, canvas, ctx);
    }
  }

  startCaptureLoop(video, canvas, ctx) {
    if (!this.isScreenSharing) return;

    const sendFrame = () => {
      if (!this.isScreenSharing) {
        this.captureIntervalId = null;
        return;
      }

      // ✅ FIX: Use current quality settings dynamically
      const quality = this.currentQuality;
      const maxWidth = Math.min(quality.width, 1280); // Cap at 720p max
      const maxHeight = Math.min(quality.height, 720); // Cap at 720p max

      let targetWidth = video.videoWidth;
      let targetHeight = video.videoHeight;

      // Scale down if needed while preserving aspect ratio
      if (targetWidth > maxWidth || targetHeight > maxHeight) {
        const scale = Math.min(
          maxWidth / targetWidth,
          maxHeight / targetHeight
        );
        targetWidth = Math.floor(targetWidth * scale);
        targetHeight = Math.floor(targetHeight * scale);
      }

      // Only resize canvas if dimensions changed
      if (canvas.width !== targetWidth || canvas.height !== targetHeight) {
        canvas.width = targetWidth;
        canvas.height = targetHeight;
      }

      ctx.drawImage(video, 0, 0, targetWidth, targetHeight);

      // ✅ FIX: Use quality.jpegQuality dynamically
      canvas.toBlob(
        (blob) => {
          if (blob) {
            if (this.frameCount < 10 || this.frameCount % 100 == 0) {
              const frameSizeKB = (blob.size / 1024).toFixed(1);
              console.log(
                `[ScreenShare] Frame ${
                  this.frameCount
                }: ${frameSizeKB} KB (${targetWidth}x${targetHeight}) @ ${
                  quality.fps
                } FPS, Q:${quality.jpegQuality.toFixed(2)}`
              );
            }
            this.frameCount++;

            blob.arrayBuffer().then((buffer) => {
              const packet = this.createMediaPacket(2, buffer);
              this.sendBinary(packet);
            });
          }
        },
        "image/jpeg",
        quality.jpegQuality // ✅ FIX: Dynamic quality from currentQuality
      );

      // ✅ FIX: Use quality.fps dynamically for frame interval
      const frameDelay = 1000 / quality.fps; // Calculate delay from FPS
      this.captureIntervalId = setTimeout(sendFrame, frameDelay);
    };

    if (typeof this.frameCount === "undefined") {
      this.frameCount = 0;
    }

    sendFrame();
  }

  stopScreenShare() {
    // ✅ FIX: Stop capture loop
    if (this.captureIntervalId) {
      clearTimeout(this.captureIntervalId);
      this.captureIntervalId = null;
    }

    // ✅ FIX: Clean up video/canvas elements
    if (this.captureVideoElement) {
      this.captureVideoElement.srcObject = null;
      this.captureVideoElement = null;
    }
    this.captureCanvas = null;
    this.captureCtx = null;

    if (this.screenStream) {
      this.screenStream.getTracks().forEach((track) => track.stop());
      this.screenStream = null;
    }

    this.isScreenSharing = false;
    document.getElementById("shareScreenBtn").style.display = "block";
    document.getElementById("stopScreenBtn").style.display = "none";

    // ✅ FIX: Hide local preview and show "No screen sharing" message
    this.hideLocalScreenPreview();

    const noScreenEl = document.getElementById("noScreen");
    if (noScreenEl) {
      noScreenEl.style.display = "block";
    }
  }

  /**
   * Show local screen preview (for teacher who is sharing)
   * Displays the screen stream directly in the sharedScreen element
   */
  showLocalScreenPreview(stream) {
    const sharedScreenEl = document.getElementById("sharedScreen");
    const noScreenEl = document.getElementById("noScreen");

    if (!sharedScreenEl) return;

    // Hide "No screen sharing" message
    if (noScreenEl) {
      noScreenEl.style.display = "none";
    }

    // Remove any existing content
    sharedScreenEl.innerHTML = "";

    // Create video element for local preview
    const video = document.createElement("video");
    video.srcObject = stream;
    video.autoplay = true;
    video.playsInline = true;
    video.muted = true; // Mute to prevent feedback
    video.style.width = "100%";
    video.style.height = "100%";
    video.style.objectFit = "contain";

    // Add to shared screen element
    sharedScreenEl.appendChild(video);

    console.log("[ScreenShare] Local preview displayed");
  }

  /**
   * Hide local screen preview
   */
  hideLocalScreenPreview() {
    const sharedScreenEl = document.getElementById("sharedScreen");
    if (sharedScreenEl) {
      // Stop all video elements
      const videos = sharedScreenEl.querySelectorAll("video");
      videos.forEach((video) => {
        if (video.srcObject) {
          video.srcObject.getTracks().forEach((track) => track.stop());
        }
        video.srcObject = null;
      });

      // Clear content
      sharedScreenEl.innerHTML = "";
    }
  }

  async toggleVoice() {
    if (this.isVoiceActive) {
      this.stopVoice();
    } else {
      await this.startVoice();
    }
  }

  async startVoice() {
    // Check if media devices are available (fix for missing hardware)
    if (!this.mediaDevicesAvailable) {
      alert(
        "Media devices not supported by your browser.\n\nVoice chat is disabled."
      );
      console.warn("[Voice] Media devices API not available");
      return;
    }

    if (!this.audioInputAvailable) {
      alert(
        "No microphone detected.\n\nVoice chat requires an audio input device.\n\nIf you recently removed your camera/microphone driver, voice chat will not work until an audio input device is available."
      );
      console.warn("[Voice] No audio input devices available");
      return;
    }

    try {
      this.voiceStream = await navigator.mediaDevices.getUserMedia({
        audio: true,
      });
      this.isVoiceActive = true;

      const btn = document.getElementById("toggleVoiceBtn");
      btn.textContent = "🔴 Disable Voice";
      btn.classList.add("btn-active");

      // Set up audio processing
      this.audioContext = new (window.AudioContext ||
        window.webkitAudioContext)();
      const source = this.audioContext.createMediaStreamSource(
        this.voiceStream
      );

      // Use ScriptProcessor for audio capture
      const processor = this.audioContext.createScriptProcessor(4096, 1, 1);

      processor.onaudioprocess = (e) => {
        if (!this.isVoiceActive) return;

        const audioData = e.inputBuffer.getChannelData(0);

        // Convert Float32Array to Int16Array
        const int16Data = new Int16Array(audioData.length);
        for (let i = 0; i < audioData.length; i++) {
          int16Data[i] = Math.max(
            -32768,
            Math.min(32767, audioData[i] * 32768)
          );
        }

        // Create and send media packet
        const packet = this.createMediaPacket(1, int16Data.buffer); // Type 1 = Voice
        this.sendBinary(packet);

        // Update volume indicator
        this.updateVolumeIndicator(audioData);
      };

      source.connect(processor);
      processor.connect(this.audioContext.destination);
    } catch (error) {
      console.error("[Voice] Error starting voice:", error);

      // Provide user-friendly error messages
      let errorMessage = "Failed to access microphone.\n\n";
      if (
        error.name === "NotFoundError" ||
        error.name === "DevicesNotFoundError"
      ) {
        errorMessage +=
          "No microphone was found on your system.\n\nPlease connect a microphone and try again.";
      } else if (
        error.name === "NotAllowedError" ||
        error.name === "PermissionDeniedError"
      ) {
        errorMessage +=
          "Microphone permission was denied.\n\nPlease allow microphone access in your browser settings.";
      } else if (
        error.name === "NotReadableError" ||
        error.name === "TrackStartError"
      ) {
        errorMessage +=
          "Your microphone is already in use by another application.\n\nPlease close other apps using the microphone.";
      } else {
        errorMessage += "Error: " + error.message;
      }

      alert(errorMessage);
    }
  }

  stopVoice() {
    if (this.voiceStream) {
      this.voiceStream.getTracks().forEach((track) => track.stop());
      this.voiceStream = null;
    }

    // ✅ NEW: Cleanup jitter buffer
    if (this.jitterBuffer) {
      this.jitterBuffer.cleanup();
      this.jitterBuffer = null;
    }

    if (this.audioContext) {
      this.audioContext.close();
      this.audioContext = null;
    }

    this.isVoiceActive = false;

    const btn = document.getElementById("toggleVoiceBtn");
    btn.textContent = "🎤 Enable Voice";
    btn.classList.remove("btn-active");

    document.getElementById("volumeLevel").style.width = "0%";
  }

  createMediaPacket(mediaType, mediaData) {
    // Packet format: [ClientID_Length(4)][ClientID][RoomID_Length(4)][RoomID][MediaType(1)][Data]
    const clientIdBytes = new TextEncoder().encode(this.clientId);
    const roomIdBytes = new TextEncoder().encode(this.currentRoom || "");

    const packetSize =
      4 +
      clientIdBytes.length +
      4 +
      roomIdBytes.length +
      1 +
      mediaData.byteLength;
    const packet = new ArrayBuffer(packetSize);
    const view = new DataView(packet);

    let offset = 0;

    // ClientID length
    view.setUint32(offset, clientIdBytes.length);
    offset += 4;

    // ClientID
    new Uint8Array(packet, offset, clientIdBytes.length).set(clientIdBytes);
    offset += clientIdBytes.length;

    // RoomID length
    view.setUint32(offset, roomIdBytes.length);
    offset += 4;

    // RoomID
    new Uint8Array(packet, offset, roomIdBytes.length).set(roomIdBytes);
    offset += roomIdBytes.length;

    // Media type
    view.setUint8(offset, mediaType);
    offset += 1;

    // Media data
    new Uint8Array(packet, offset).set(new Uint8Array(mediaData));

    return packet;
  }

  updateVolumeIndicator(audioData) {
    // Calculate RMS volume
    let sum = 0;
    for (let i = 0; i < audioData.length; i++) {
      sum += audioData[i] * audioData[i];
    }
    const rms = Math.sqrt(sum / audioData.length);
    const volume = Math.min(100, rms * 500);

    document.getElementById("volumeLevel").style.width = volume + "%";
  }

  async playReceivedAudio(arrayBuffer) {
    try {
      // ✅ FIX: Validate arrayBuffer
      if (!arrayBuffer || arrayBuffer.byteLength === 0) {
        return;
      }

      // ✅ NEW: Use jitter buffer for smooth playback
      if (!this.jitterBuffer) {
        if (typeof JitterBuffer !== "undefined") {
          this.jitterBuffer = new JitterBuffer();
          console.log("[Audio] ✅ Jitter buffer enabled for voice");
        } else {
          console.warn(
            "[Audio] JitterBuffer not loaded, using direct playback"
          );
        }
      }

      if (this.jitterBuffer) {
        // Use jitter buffer (recommended for smooth voice)
        // Note: For now, we don't have sequence numbers from server
        // So we'll use a simple counter
        if (!this.audioSeqCounter) {
          this.audioSeqCounter = 0;
        }

        const seq = this.audioSeqCounter++;
        const timestamp = Date.now();

        this.jitterBuffer.addPacket(seq, timestamp, arrayBuffer);
      } else {
        // Fallback: Direct playback (old method)
        // ✅ FIX: Check audioContext state before creating
        if (!this.audioContext || this.audioContext.state === "closed") {
          this.audioContext = new (window.AudioContext ||
            window.webkitAudioContext)();
        }

        // ✅ FIX: Resume audio context if suspended (browser autoplay policy)
        if (this.audioContext.state === "suspended") {
          try {
            await this.audioContext.resume();
          } catch (e) {
            console.warn("[Audio] Failed to resume audio context:", e);
          }
        }

        // Validate audio data
        if (arrayBuffer.byteLength % 2 !== 0) {
          console.warn(
            "[Audio] Invalid audio data length:",
            arrayBuffer.byteLength
          );
          return;
        }

        // Convert Int16 to Float32
        const int16Array = new Int16Array(arrayBuffer);
        const float32Array = new Float32Array(int16Array.length);

        for (let i = 0; i < int16Array.length; i++) {
          float32Array[i] = int16Array[i] / 32768.0;
        }

        // Create audio buffer
        const audioBuffer = this.audioContext.createBuffer(
          1,
          float32Array.length,
          this.audioContext.sampleRate
        );
        audioBuffer.getChannelData(0).set(float32Array);

        // Play audio
        const source = this.audioContext.createBufferSource();
        source.buffer = audioBuffer;
        source.connect(this.audioContext.destination);
        source.start();
      }
    } catch (error) {
      console.error("[Audio] Error playing audio:", error);
      // ✅ FIX: Reset audioContext on error
      if (this.audioContext && this.audioContext.state === "closed") {
        this.audioContext = null;
      }
    }
  }

  updateStatus(text, connected) {
    const statusEl = document.getElementById("connectionStatus");
    document.getElementById("statusText").textContent = text;

    statusEl.classList.remove("connected", "error");
    if (connected) {
      statusEl.classList.add("connected");
    } else if (text.includes("error") || text.includes("failed")) {
      statusEl.classList.add("error");
    }
  }

  escapeHtml(text) {
    const div = document.createElement("div");
    div.textContent = text;
    return div.innerHTML;
  }

  // ===== ADAPTIVE BITRATE METHODS =====

  async handleQualityChange(qualityMessage) {
    console.log("[ABR] Quality change requested:", qualityMessage);

    const newQuality = {
      level: qualityMessage.level,
      width: qualityMessage.width,
      height: qualityMessage.height,
      fps: qualityMessage.fps,
      jpegQuality: qualityMessage.jpegQuality,
    };

    // Show notification to user
    this.showQualityNotification(newQuality.level);

    // Update quality indicator
    this.updateQualityIndicator(newQuality.level);

    // If currently screen sharing, apply new settings
    if (this.isScreenSharing) {
      await this.adjustScreenShareQuality(newQuality);
    }

    // Store for next time screen sharing starts
    this.currentQuality = newQuality;

    // Send acknowledgement to server
    this.sendMessage({
      type: "quality_ack",
      level: newQuality.level,
    });
  }

  async adjustScreenShareQuality(quality) {
    console.log("[ABR] Adjusting screen share quality to:", quality);

    // ✅ FIX: Update quality without restarting screen sharing
    this.currentQuality = quality;

    // If currently sharing, restart capture loop with new settings
    if (this.isScreenSharing && this.screenStream) {
      // Stop current capture loop
      if (this.captureIntervalId) {
        clearTimeout(this.captureIntervalId);
        this.captureIntervalId = null;
      }

      // Restart capture with new quality settings
      console.log("[ABR] Restarting capture loop with new quality settings");
      this.captureScreenFrames();
    }
  }

  async startScreenShareWithQuality(quality) {
    // Check if getDisplayMedia is available
    if (!navigator.mediaDevices || !navigator.mediaDevices.getDisplayMedia) {
      alert(
        "Screen sharing is not supported by your browser.\n\nPlease use a modern browser like Chrome, Firefox, or Edge."
      );
      console.warn("[ScreenShare] getDisplayMedia not supported");
      return;
    }

    try {
      // ✅ PERFORMANCE OPTIMIZATION: Limit resolution to 720p max and frame rate to 10 FPS
      // This reduces CPU usage significantly for low-spec/localhost testing
      // CRITICAL FIX: Disable audio to prevent feedback loop
      this.screenStream = await navigator.mediaDevices.getDisplayMedia({
        video: {
          mediaSource: "screen",
          width: { ideal: Math.min(quality.width, 1280), max: 1280 }, // ✅ Cap at 720p
          height: { ideal: Math.min(quality.height, 720), max: 720 }, // ✅ Cap at 720p
          frameRate: { ideal: Math.min(quality.fps, 10), max: 15 }, // ✅ Target 10 FPS max
        },
        audio: false, // ✅ FIX: Disable audio to prevent feedback loop
      });

      this.isScreenSharing = true;
      document.getElementById("shareScreenBtn").style.display = "none";
      document.getElementById("stopScreenBtn").style.display = "block";

      // ✅ FIX: Show local preview for teacher (immediate feedback)
      this.showLocalScreenPreview(this.screenStream);

      console.log("[ABR] Screen sharing started with quality:", quality);

      // Capture and send frames with new quality
      this.captureScreenFramesWithQuality(quality);

      // Handle stream end
      this.screenStream.getVideoTracks()[0].onended = () => {
        this.stopScreenShare();
      };
    } catch (error) {
      console.error("[ABR] Error starting screen share:", error);

      // Provide user-friendly error messages
      let errorMessage = "Failed to start screen share.\n\n";
      if (
        error.name === "NotAllowedError" ||
        error.name === "PermissionDeniedError"
      ) {
        errorMessage += "Screen sharing permission was denied.";
      } else {
        errorMessage += "Error: " + error.message;
      }

      alert(errorMessage);
    }
  }

  captureScreenFramesWithQuality(quality) {
    if (!this.screenStream || !this.isScreenSharing) return;

    const video = document.createElement("video");
    video.srcObject = this.screenStream;
    video.play();

    video.onloadedmetadata = () => {
      const canvas = document.createElement("canvas");
      const ctx = canvas.getContext("2d");

      // ✅ PERFORMANCE OPTIMIZATION: Lower quality settings for CPU efficiency
      // Use same settings as captureScreenFrames() for consistency
      const safeWidth = 1280; // Max 720p (performance mode)
      const safeHeight = 720; // Max 720p (performance mode)
      const safeQuality = 0.5; // ✅ Lower quality (0.5) - reduces CPU usage by 40-50%

      canvas.width = safeWidth;
      canvas.height = safeHeight;

      const frameDelay = 1000 / quality.fps;

      // ✅ PERFORMANCE: Initialize frame counter for logging
      if (typeof this.frameCount === "undefined") {
        this.frameCount = 0;
      }

      const sendFrame = () => {
        if (!this.isScreenSharing) return;

        // Draw video to canvas (scales automatically)
        ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

        // Convert to JPEG with SAFE quality
        canvas.toBlob(
          (blob) => {
            if (blob) {
              // ✅ PERFORMANCE: Fragmentation handles large frames automatically
              // No need to warn - Proxy Server will fragment if > 65KB
              // Only log info for debugging (first 5 frames)
              if (this.frameCount < 5) {
                const frameSizeKB = (blob.size / 1024).toFixed(1);
                console.log(
                  `[ScreenShare] Frame ${this.frameCount}: ${frameSizeKB} KB (${safeWidth}x${safeHeight} @ ${safeQuality})`
                );
                if (blob.size > 65000) {
                  console.log(
                    `[ScreenShare] Will be fragmented: ${frameSizeKB} KB > 65 KB`
                  );
                }
              }
              this.frameCount++;

              blob.arrayBuffer().then((buffer) => {
                const packet = this.createMediaPacket(2, buffer);
                this.sendBinary(packet);
              });
            }
          },
          "image/jpeg",
          safeQuality // ✅ PERFORMANCE: Lower quality (0.5) - reduces CPU usage
        );

        setTimeout(sendFrame, frameDelay);
      };

      sendFrame();
    };
  }

  showQualityNotification(level) {
    // Remove existing notification if any
    const existing = document.querySelector(".quality-notification");
    if (existing) {
      existing.remove();
    }

    // Create notification
    const notification = document.createElement("div");
    notification.className = "quality-notification";
    notification.innerHTML = `
      <span class="quality-icon">📊</span>
      Network conditions changed<br>
      <strong>Quality: ${level}</strong>
    `;

    document.body.appendChild(notification);

    // Auto-remove after 5 seconds
    setTimeout(() => {
      notification.remove();
    }, 5000);
  }

  updateQualityIndicator(level) {
    const indicator = document.getElementById("qualityIndicator");
    if (indicator) {
      indicator.textContent = level;
      indicator.className = `quality-indicator quality-${level.toLowerCase()}`;
    }
  }

  // Override existing startScreenShare to use quality settings
  async startScreenShare() {
    await this.startScreenShareWithQuality(this.currentQuality);
  }

  logout() {
    if (this.currentRoom) {
      this.leaveRoom();
    }

    this.stopScreenShare();
    this.stopVoice();

    if (this.ws) {
      this.ws.close();
    }

    sessionStorage.clear();
    window.location.href = "index.html";
  }
}

// Initialize app when page loads
window.addEventListener("load", () => {
  new TutoringApp();
});
