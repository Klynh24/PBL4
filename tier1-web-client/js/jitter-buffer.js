/**
 * ✅ CRITICAL FIX: Client-Side Jitter Buffer for Voice
 *
 * Problem: Voice packets arrive out of order, causing audio glitches/gaps
 * Solution: Buffer audio packets and play them in order with adaptive delay
 *
 * Benefits:
 * - Smooth voice playback
 * - Handles network jitter
 * - Prevents audio stuttering
 * - Adaptive buffer size
 */
class JitterBuffer {
  constructor() {
    this.buffer = new Map(); // seq -> {timestamp, data, arrivalTime}
    this.nextExpectedSeq = 0;
    this.bufferDelayMs = 60; // Target 60ms buffer
    this.minBufferMs = 20;
    this.maxBufferMs = 200;
    this.lastPlayoutTime = 0;

    // Audio context for playback
    this.audioContext = null;
    this.nextPlayTime = 0;

    // Metrics
    this.packetsReceived = 0;
    this.packetsPlayed = 0;
    this.packetsLate = 0;
    this.packetsDropped = 0;
    this.totalJitter = 0;
    this.lastArrivalTime = 0;
    this.lastPacketTimestamp = 0;

    console.log("[JitterBuffer] ✅ Initialized (target: 60ms)");
  }

  /**
   * Initialize audio context
   */
  initAudioContext() {
    if (!this.audioContext) {
      this.audioContext = new (window.AudioContext ||
        window.webkitAudioContext)();
      this.nextPlayTime = this.audioContext.currentTime;
      console.log("[JitterBuffer] Audio context initialized");
    }
  }

  /**
   * Add audio packet to buffer
   * @param {number} seq - Sequence number
   * @param {number} timestamp - Packet timestamp (ms)
   * @param {ArrayBuffer} data - Audio data (Int16 PCM)
   */
  addPacket(seq, timestamp, data) {
    this.packetsReceived++;

    const currentTime = Date.now();

    // Calculate jitter
    if (this.lastArrivalTime > 0 && this.lastPacketTimestamp > 0) {
      const arrivalDelta = currentTime - this.lastArrivalTime;
      const timestampDelta = timestamp - this.lastPacketTimestamp;
      const jitter = Math.abs(arrivalDelta - timestampDelta);
      this.totalJitter += jitter;

      // Adapt buffer size based on jitter
      this.adaptBufferSize(jitter);
    }

    this.lastArrivalTime = currentTime;
    this.lastPacketTimestamp = timestamp;

    // Add to buffer
    this.buffer.set(seq, {
      timestamp: timestamp,
      data: data,
      arrivalTime: currentTime,
    });

    // Cleanup old packets (keep last 50 max)
    if (this.buffer.size > 50) {
      const oldestSeq = Math.min(...this.buffer.keys());
      this.buffer.delete(oldestSeq);
      this.packetsDropped++;
    }

    // Try to play buffered packets
    this.playBufferedPackets();
  }

  /**
   * Adapt buffer size based on observed jitter
   */
  adaptBufferSize(jitter) {
    if (jitter > this.bufferDelayMs * 0.8) {
      // High jitter, increase buffer
      this.bufferDelayMs = Math.min(this.maxBufferMs, this.bufferDelayMs + 10);
    } else if (jitter < this.bufferDelayMs * 0.3) {
      // Low jitter, decrease buffer for lower latency
      this.bufferDelayMs = Math.max(this.minBufferMs, this.bufferDelayMs - 5);
    }
  }

  /**
   * Play buffered packets that are ready
   */
  playBufferedPackets() {
    if (this.buffer.size === 0) {
      return;
    }

    this.initAudioContext();

    const currentTime = Date.now();

    // Find packets ready to play
    const readyPackets = [];

    for (const [seq, packet] of this.buffer.entries()) {
      const timeInBuffer = currentTime - packet.arrivalTime;

      if (timeInBuffer >= this.bufferDelayMs) {
        readyPackets.push({ seq, packet });
      }
    }

    // Sort by sequence number
    readyPackets.sort((a, b) => a.seq - b.seq);

    // Play ready packets
    for (const { seq, packet } of readyPackets) {
      this.playPacket(packet.data);
      this.buffer.delete(seq);
      this.packetsPlayed++;
      this.nextExpectedSeq = seq + 1;
    }
  }

  /**
   * Play audio packet
   * @param {ArrayBuffer} data - Audio data (Int16 PCM)
   */
  playPacket(data) {
    try {
      // Convert Int16 to Float32
      const int16Array = new Int16Array(data);
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

      // Schedule playback
      const source = this.audioContext.createBufferSource();
      source.buffer = audioBuffer;
      source.connect(this.audioContext.destination);

      // Play at next scheduled time
      const playTime = Math.max(
        this.nextPlayTime,
        this.audioContext.currentTime
      );
      source.start(playTime);

      // Update next play time
      const duration = audioBuffer.duration;
      this.nextPlayTime = playTime + duration;
    } catch (error) {
      console.error("[JitterBuffer] Error playing packet:", error);
    }
  }

  /**
   * Get buffer status
   */
  getStatus() {
    return {
      bufferSize: this.buffer.size,
      bufferDelayMs: this.bufferDelayMs,
      packetsReceived: this.packetsReceived,
      packetsPlayed: this.packetsPlayed,
      packetsLate: this.packetsLate,
      packetsDropped: this.packetsDropped,
      averageJitter:
        this.packetsReceived > 1
          ? this.totalJitter / (this.packetsReceived - 1)
          : 0,
    };
  }

  /**
   * Print statistics
   */
  printStats() {
    const status = this.getStatus();
    console.log("\n=== Jitter Buffer Statistics ===");
    console.log(`Buffer Size: ${status.bufferSize} packets`);
    console.log(`Buffer Delay: ${status.bufferDelayMs} ms`);
    console.log(`Packets Received: ${status.packetsReceived}`);
    console.log(`Packets Played: ${status.packetsPlayed}`);
    console.log(`Packets Late: ${status.packetsLate}`);
    console.log(`Packets Dropped: ${status.packetsDropped}`);
    console.log(`Average Jitter: ${status.averageJitter.toFixed(2)} ms`);
    console.log("================================\n");
  }

  /**
   * Clear buffer
   */
  clear() {
    this.buffer.clear();
    this.nextExpectedSeq = 0;
    this.lastPlayoutTime = 0;

    if (this.audioContext) {
      this.nextPlayTime = this.audioContext.currentTime;
    }
  }

  /**
   * Cleanup
   */
  cleanup() {
    this.clear();

    if (this.audioContext && this.audioContext.state !== "closed") {
      this.audioContext.close();
      this.audioContext = null;
    }
  }
}
