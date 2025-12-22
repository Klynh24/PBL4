/**
 * Client-side Frame Reassembler for Reliability Layer
 * Ported from Network/tier1-web-client/js/frame-reassembler.js
 */
import FecXorDecoder from "./fec-decoder.js";

export default class FrameReassembler {
  constructor(websocket) {
    this.ws = websocket;

    // frameId -> { fragments: Map, fecPackets: Map, totalFragments: number, timestamp: number }
    this.frameBuffer = new Map();

    this.fecDecoder = new FecXorDecoder(10, 2);

    this.jitterBuffer = [];
    this.maxJitterBufferSize = 5;

    this.lastRenderedFrameId = -1;

    this.lastRenderTime = 0;
    this.minFrameInterval = 16;
    this.pendingRender = false;
    this.onFrameReady = null;

    this.stats = {
      packetsReceived: 0,
      framesCompleted: 0,
      framesDropped: 0,
      nacksSent: 0,
      packetsLost: 0,
      fecRecovered: 0,
    };

    this.nackTimeoutMs = 200;
    this.maxNackAttempts = 5;
    this.nackTimers = new Map();

    setInterval(() => this.cleanupOldFrames(), 5000);

    setInterval(() => {
      const cleaned = this.cleanupCorruptedFrames();
      if (cleaned > 0) {
        // eslint-disable-next-line no-console
        console.warn("[Reassembler] Cleaned up", cleaned, "corrupted frames");
      }
    }, 2000);
  }

  /** @param {ArrayBuffer} packetData */
  processPacket(packetData) {
    this.stats.packetsReceived++;

    const header = this.parseHeader(packetData);
    if (!header) {
      if (
        this.stats.packetsReceived <= 10 ||
        this.stats.packetsReceived % 100 === 0
      ) {
        // eslint-disable-next-line no-console
        console.error(
          "[Reassembler] Invalid packet header, packet size:",
          packetData.byteLength
        );
      }
      return null;
    }

    if (this.stats.packetsReceived <= 5) {
      // eslint-disable-next-line no-console
      console.log(
        "[Reassembler] Packet #" + this.stats.packetsReceived + ":",
        "frameId=" + header.frameId,
        "fragment=" + header.fragmentIndex + "/" + header.totalFragments,
        "size=" + packetData.byteLength
      );
    }

    if (header.isFecPacket) {
      this.storeFecPacket(header, packetData);
      this.tryFecRecovery(header.frameId);
      return null;
    }

    if (header.totalFragments === 1) {
      const payload = new Uint8Array(packetData, 28);
      this.onFrameComplete(header.frameId, payload, header.isKeyframe);
      return null;
    }

    this.storeFragment(header, packetData);
    this.tryFecRecovery(header.frameId);

    const frame = this.frameBuffer.get(header.frameId);
    if (frame && frame.fragments.size === header.totalFragments) {
      // eslint-disable-next-line no-console
      console.log(
        "[Reassembler] Frame complete:",
        header.frameId,
        "(" + frame.fragments.size + "/" + header.totalFragments + " fragments)"
      );
      this.assembleFrame(header.frameId);
      return null;
    }

    if (!this.nackTimers.has(header.frameId)) {
      const timerId = setTimeout(() => {
        this.checkForMissingFragments(header.frameId);
      }, this.nackTimeoutMs);

      this.nackTimers.set(header.frameId, timerId);
    }

    return null;
  }

  /** @param {ArrayBuffer} packetData */
  parseHeader(packetData) {
    if (packetData.byteLength < 28) return null;

    const view = new DataView(packetData);

    const magic = view.getUint32(0, false);
    if (magic !== 0x53435245) {
      if (
        this.stats.packetsReceived <= 10 ||
        this.stats.packetsReceived % 100 === 0
      ) {
        // eslint-disable-next-line no-console
        console.error(
          "[Reassembler] Invalid magic number:",
          magic.toString(16),
          "packet size:",
          packetData.byteLength,
          "first 8 bytes:",
          Array.from(new Uint8Array(packetData.slice(0, 8)))
            .map((b) => "0x" + b.toString(16).padStart(2, "0"))
            .join(" ")
        );
      }
      return null;
    }

    const fragmentIndex = view.getUint16(12, false);
    const totalFragments = view.getUint16(14, false);

    const flags = view.getUint8(27);
    const isFecPacket = (flags & 0x01) === 0x01;
    const frameType = view.getUint8(16);

    if (totalFragments > 10000 || fragmentIndex >= 10000) {
      // eslint-disable-next-line no-console
      console.error("[Reassembler] Invalid fragment values:", {
        fragmentIndex,
        totalFragments,
        "raw bytes 12-15": Array.from(new Uint8Array(packetData.slice(12, 16)))
          .map((b) => "0x" + b.toString(16).padStart(2, "0"))
          .join(" "),
      });
      return null;
    }

    return {
      magic,
      sequenceNumber: view.getUint32(4, false),
      frameId: view.getUint32(8, false),
      fragmentIndex,
      totalFragments,
      frameType,
      isKeyframe: !isFecPacket && (frameType === 1 || (frameType & 0x0f) === 1),
      isFecPacket,
      fecIndex: isFecPacket ? fragmentIndex : null,
      totalFecPackets: isFecPacket ? totalFragments : null,
    };
  }

  storeFragment(header, packetData) {
    if (
      !header ||
      header.totalFragments > 10000 ||
      header.fragmentIndex >= 10000
    ) {
      // eslint-disable-next-line no-console
      console.error(
        "[Reassembler] Invalid header, cannot store fragment:",
        header
      );
      return;
    }

    let frame = this.frameBuffer.get(header.frameId);

    if (!frame) {
      frame = {
        fragments: new Map(),
        fecPackets: new Map(),
        totalFragments: header.totalFragments,
        isKeyframe: header.isKeyframe,
        timestamp: Date.now(),
        nackAttempts: 0,
      };
      this.frameBuffer.set(header.frameId, frame);
    } else {
      if (frame.totalFragments !== header.totalFragments) {
        // eslint-disable-next-line no-console
        console.warn(
          "[Reassembler] totalFragments mismatch for frame",
          header.frameId,
          ": stored=" +
            frame.totalFragments +
            ", header=" +
            header.totalFragments
        );
        frame.totalFragments = Math.max(
          frame.totalFragments,
          header.totalFragments
        );
      }
      frame.isKeyframe = frame.isKeyframe || header.isKeyframe;
    }

    const payload = new Uint8Array(packetData, 28);
    frame.fragments.set(header.fragmentIndex, payload);
  }

  storeFecPacket(header, packetData) {
    let frame = this.frameBuffer.get(header.frameId);

    if (!frame) {
      frame = {
        fragments: new Map(),
        fecPackets: new Map(),
        totalFragments: 0,
        isKeyframe: false,
        timestamp: Date.now(),
        nackAttempts: 0,
        totalFecPackets: header.totalFecPackets,
      };
      this.frameBuffer.set(header.frameId, frame);
    }

    const fecPayload = new Uint8Array(packetData, 28);
    frame.fecPackets.set(header.fecIndex, fecPayload);
  }

  tryFecRecovery(frameId) {
    const frame = this.frameBuffer.get(frameId);
    if (!frame || !frame.fecPackets || frame.fecPackets.size === 0) return;

    if (frame.totalFragments === 0) return;

    const missing = [];
    for (let i = 0; i < frame.totalFragments; i++) {
      if (!frame.fragments.has(i)) missing.push(i);
    }

    if (missing.length === 0) return;

    const maxFecCount = frame.fecPackets.size;
    if (missing.length > maxFecCount) return;

    const receivedPackets = [];
    for (let i = 0; i < frame.totalFragments; i++) {
      receivedPackets.push(frame.fragments.get(i) || null);
    }

    const fecPackets = [];
    const keys = Array.from(frame.fecPackets.keys());
    const maxFecIndex = keys.length ? Math.max(...keys) : -1;
    for (let i = 0; i <= maxFecIndex; i++) {
      fecPackets.push(frame.fecPackets.get(i) || null);
    }

    let recoveredCount = 0;
    for (const lostIndex of missing) {
      const recovered = this.fecDecoder.recoverPacket(
        receivedPackets,
        fecPackets,
        lostIndex
      );
      if (recovered) {
        frame.fragments.set(lostIndex, recovered);
        receivedPackets[lostIndex] = recovered;
        recoveredCount++;
        this.stats.fecRecovered++;

        // eslint-disable-next-line no-console
        console.log(
          "[Reassembler] FEC recovered fragment",
          lostIndex,
          "for frame",
          frameId
        );
      }
    }

    if (recoveredCount > 0) {
      if (frame.fragments.size === frame.totalFragments) {
        // eslint-disable-next-line no-console
        console.log(
          "[Reassembler] Frame complete after FEC recovery:",
          frameId
        );
        this.assembleFrame(frameId);
      }
    }
  }

  assembleFrame(frameId) {
    const frame = this.frameBuffer.get(frameId);
    if (!frame) return null;

    if (this.nackTimers.has(frameId)) {
      clearTimeout(this.nackTimers.get(frameId));
      this.nackTimers.delete(frameId);
    }

    let totalSize = 0;
    for (const fragment of frame.fragments.values())
      totalSize += fragment.length;

    const assembled = new Uint8Array(totalSize);
    let offset = 0;

    for (let i = 0; i < frame.totalFragments; i++) {
      const fragment = frame.fragments.get(i);
      if (!fragment) {
        // eslint-disable-next-line no-console
        console.error(
          "[Reassembler] Missing fragment",
          i,
          "for frame",
          frameId
        );
        this.frameBuffer.delete(frameId);
        this.stats.framesDropped++;
        return null;
      }

      assembled.set(fragment, offset);
      offset += fragment.length;
    }

    this.frameBuffer.delete(frameId);
    this.stats.framesCompleted++;

    this.onFrameComplete(frameId, assembled, frame.isKeyframe);
    return null;
  }

  checkForMissingFragments(frameId) {
    const frame = this.frameBuffer.get(frameId);
    if (!frame) return;

    if (frame.fragments.size === frame.totalFragments) {
      this.assembleFrame(frameId);
      return;
    }

    if (frame.nackAttempts >= this.maxNackAttempts) {
      // eslint-disable-next-line no-console
      console.warn(
        "[Reassembler] Too many NACKs for frame",
        frameId,
        ", dropping"
      );
      this.frameBuffer.delete(frameId);
      this.nackTimers.delete(frameId);
      this.stats.framesDropped++;
      return;
    }

    this.tryFecRecovery(frameId);

    if (frame.fragments.size === frame.totalFragments) {
      this.assembleFrame(frameId);
      return;
    }

    const missing = [];
    if (frame.totalFragments > 10000) {
      // eslint-disable-next-line no-console
      console.error(
        "[Reassembler] Invalid totalFragments:",
        frame.totalFragments,
        "for frame",
        frameId
      );
      this.frameBuffer.delete(frameId);
      this.nackTimers.delete(frameId);
      this.stats.framesDropped++;
      return;
    }

    for (let i = 0; i < frame.totalFragments; i++) {
      if (!frame.fragments.has(i)) missing.push(i);
    }

    if (missing.length > 0) {
      this.sendNACK(frameId, missing);
      frame.nackAttempts++;
      this.stats.nacksSent++;
      this.stats.packetsLost += missing.length;

      const timerId = setTimeout(() => {
        this.checkForMissingFragments(frameId);
      }, this.nackTimeoutMs * 2);

      this.nackTimers.set(frameId, timerId);
    }
  }

  sendNACK(frameId, missingIndices) {
    const message = `NACK:${frameId}:${missingIndices.join(",")}`;

    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(message);
    } else {
      // eslint-disable-next-line no-console
      console.error("[Reassembler] Cannot send NACK, WebSocket not open");
    }
  }

  onFrameComplete(frameId, frameData, isKeyframe) {
    this.jitterBuffer.push({
      frameId,
      data: frameData,
      isKeyframe,
      timestamp: Date.now(),
    });

    this.jitterBuffer.sort((a, b) => a.frameId - b.frameId);

    if (this.jitterBuffer.length > this.maxJitterBufferSize) {
      const oldest = this.jitterBuffer.shift();
      if (oldest.frameId <= this.lastRenderedFrameId) {
        // ok
      } else {
        this.jitterBuffer.unshift(oldest);
        this.jitterBuffer.pop();
      }
    }

    this.scheduleRender();
    return null;
  }

  scheduleRender() {
    if (this.pendingRender) return;

    this.pendingRender = true;

    if (typeof requestAnimationFrame !== "undefined") {
      requestAnimationFrame(() => {
        this.processRender();
      });
    } else {
      const now = Date.now();
      const timeSinceLastRender = now - this.lastRenderTime;
      const delay = Math.max(0, this.minFrameInterval - timeSinceLastRender);
      setTimeout(() => {
        this.processRender();
      }, delay);
    }
  }

  processRender() {
    this.pendingRender = false;

    const nextFrame = this.getNextFrame();
    if (nextFrame) {
      this.lastRenderTime = Date.now();
      if (this.onFrameReady) {
        this.onFrameReady(nextFrame);
      }

      if (this.jitterBuffer.length > 0) {
        const nextInBuffer = this.jitterBuffer[0];
        if (
          nextInBuffer.frameId === this.lastRenderedFrameId + 1 ||
          nextInBuffer.isKeyframe
        ) {
          this.scheduleRender();
        }
      }
    }
  }

  getNextFrame() {
    if (this.jitterBuffer.length === 0) return null;

    const nextFrame = this.jitterBuffer[0];

    if (this.lastRenderedFrameId === -1) {
      this.jitterBuffer.shift();
      this.lastRenderedFrameId = nextFrame.frameId;
      // eslint-disable-next-line no-console
      console.log("[Reassembler] Rendering first frame:", nextFrame.frameId);
      return nextFrame;
    }

    if (nextFrame.frameId === this.lastRenderedFrameId + 1) {
      this.jitterBuffer.shift();
      this.lastRenderedFrameId = nextFrame.frameId;
      return nextFrame;
    }

    if (nextFrame.isKeyframe && nextFrame.frameId > this.lastRenderedFrameId) {
      this.jitterBuffer.shift();
      this.lastRenderedFrameId = nextFrame.frameId;
      // eslint-disable-next-line no-console
      console.log("[Reassembler] Rendering keyframe", nextFrame.frameId);
      return nextFrame;
    }

    const age = Date.now() - nextFrame.timestamp;
    if (age > 200) {
      this.jitterBuffer.shift();
      this.lastRenderedFrameId = nextFrame.frameId;
      return this.getNextFrame();
    }

    return null;
  }

  setOnFrameReady(callback) {
    this.onFrameReady = callback;
  }

  cleanupOldFrames() {
    const now = Date.now();
    const maxAge = 5000;

    for (const [frameId, frame] of this.frameBuffer.entries()) {
      if (now - frame.timestamp > maxAge) {
        // eslint-disable-next-line no-console
        console.warn("[Reassembler] Dropping old incomplete frame:", frameId);
        this.frameBuffer.delete(frameId);

        if (this.nackTimers.has(frameId)) {
          clearTimeout(this.nackTimers.get(frameId));
          this.nackTimers.delete(frameId);
        }

        this.stats.framesDropped++;
      }
    }
  }

  getStats() {
    const lossRate =
      this.stats.packetsReceived > 0
        ? ((this.stats.packetsLost / this.stats.packetsReceived) * 100).toFixed(
            2
          )
        : 0;

    const fecStats = this.fecDecoder.getStats();

    return {
      ...this.stats,
      lossRate: lossRate + "%",
      bufferSize: this.frameBuffer.size,
      jitterBufferSize: this.jitterBuffer.length,
      fecStats,
    };
  }

  reset() {
    this.frameBuffer.clear();
    this.jitterBuffer = [];
    this.lastRenderedFrameId = -1;

    for (const timerId of this.nackTimers.values()) {
      clearTimeout(timerId);
    }
    this.nackTimers.clear();

    this.stats = {
      packetsReceived: 0,
      framesCompleted: 0,
      framesDropped: 0,
      nacksSent: 0,
      packetsLost: 0,
      fecRecovered: 0,
    };
  }

  cleanupCorruptedFrames() {
    const corruptedFrames = [];
    for (const [frameId, frame] of this.frameBuffer.entries()) {
      if (frame.totalFragments > 10000) corruptedFrames.push(frameId);
    }

    for (const frameId of corruptedFrames) {
      // eslint-disable-next-line no-console
      console.warn("[Reassembler] Cleaning up corrupted frame:", frameId);
      this.frameBuffer.delete(frameId);
      if (this.nackTimers.has(frameId)) {
        clearTimeout(this.nackTimers.get(frameId));
        this.nackTimers.delete(frameId);
      }
      this.stats.framesDropped++;
    }

    return corruptedFrames.length;
  }
}
