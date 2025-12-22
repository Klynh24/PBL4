/**
 * H.264 Hardware Decoder using WebCodecs API
 * Ported from Network/tier1-web-client/js/h264-decoder.js
 */
export default class H264Decoder {
  constructor(canvas, onFrameReady) {
    this.canvas = canvas;
    this.ctx = canvas.getContext("2d");
    this.onFrameReady = onFrameReady || (() => {});

    this.decoder = null;
    this.decoderConfig = null;
    this.isSupported = false;
    this.initialized = false;
    this.pendingConfig = null;
    this.frameCount = 0;

    this.isSupported = this.checkSupport();

    if (this.isSupported) {
      // eslint-disable-next-line no-console
      console.log("[H264Decoder] WebCodecs supported - using H.264 decoding");
      this.init();
    } else {
      // eslint-disable-next-line no-console
      console.warn("[H264Decoder] WebCodecs not supported - fallback to JPEG");
    }
  }

  checkSupport() {
    return (
      typeof window !== "undefined" &&
      "VideoDecoder" in window &&
      "VideoFrame" in window
    );
  }

  async init() {
    if (!this.isSupported || this.initialized) return;

    try {
      this.decoderConfig = {
        codec: "avc1.42E01E",
        optimizeForLatency: true,
        hardwareAcceleration: "prefer-hardware",
      };

      this.decoder = new VideoDecoder({
        output: (frame) => {
          this.handleFrame(frame);
        },
        error: (error) => {
          // eslint-disable-next-line no-console
          console.error("[H264Decoder] Decode error:", error);
          this.onFrameReady(null, error);
        },
      });

      this.initialized = true;

      if (this.pendingConfig) {
        this.configure(this.pendingConfig);
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error("[H264Decoder] Failed to initialize:", error);
      this.isSupported = false;
      this.initialized = false;
    }
  }

  configure(config) {
    if (!this.isSupported) return false;

    if (!this.initialized) {
      this.pendingConfig = config;
      return false;
    }

    try {
      const decoderConfig = {
        ...this.decoderConfig,
        description: config.description,
        codedWidth: config.width || this.canvas.width,
        codedHeight: config.height || this.canvas.height,
      };

      this.decoder.configure(decoderConfig);
      return true;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error("[H264Decoder] Configuration error:", error);
      return false;
    }
  }

  decode(data, isKeyframe = false, timestamp = null) {
    if (
      !this.isSupported ||
      !this.decoder ||
      this.decoder.state !== "configured"
    ) {
      return false;
    }

    try {
      const chunk = new EncodedVideoChunk({
        type: isKeyframe ? "key" : "delta",
        timestamp: timestamp || this.frameCount * 33333,
        duration: null,
        data,
      });

      this.decoder.decode(chunk);
      this.frameCount++;
      return true;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error("[H264Decoder] Decode error:", error);
      return false;
    }
  }

  handleFrame(frame) {
    try {
      this.ctx.drawImage(frame, 0, 0, this.canvas.width, this.canvas.height);
      this.onFrameReady(frame, null);
      frame.close();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error("[H264Decoder] Frame rendering error:", error);
      this.onFrameReady(null, error);
    }
  }

  static extractConfig(data) {
    const view = new DataView(data.buffer || data, data.byteOffset || 0);
    const config = { description: null, width: 0, height: 0 };

    let sps = null;
    let pps = null;

    let offset = 0;
    while (offset < data.length - 4) {
      if (
        view.getUint8(offset) === 0x00 &&
        view.getUint8(offset + 1) === 0x00
      ) {
        if (
          view.getUint8(offset + 2) === 0x00 &&
          view.getUint8(offset + 3) === 0x01
        ) {
          offset += 4;
        } else if (view.getUint8(offset + 2) === 0x01) {
          offset += 3;
        } else {
          offset++;
          continue;
        }

        if (offset >= data.length) break;

        const nalType = view.getUint8(offset) & 0x1f;

        let nalEnd = offset + 1;
        while (nalEnd < data.length - 3) {
          if (
            view.getUint8(nalEnd) === 0x00 &&
            view.getUint8(nalEnd + 1) === 0x00 &&
            (view.getUint8(nalEnd + 2) === 0x01 ||
              (view.getUint8(nalEnd + 2) === 0x00 &&
                view.getUint8(nalEnd + 3) === 0x01))
          ) {
            break;
          }
          nalEnd++;
        }

        const nalData = data.slice(offset, nalEnd);

        if (nalType === 7) {
          sps = nalData;
        } else if (nalType === 8) {
          pps = nalData;
        }

        offset = nalEnd;
      } else {
        offset++;
      }
    }

    if (sps && pps) {
      const descLength = 4 + sps.length + 1 + 2 + pps.length;
      const description = new Uint8Array(descLength);
      const descView = new DataView(description.buffer);

      let pos = 0;
      descView.setUint8(pos++, 1);
      descView.setUint8(pos++, sps[1]);
      descView.setUint8(pos++, sps[2]);
      descView.setUint8(pos++, sps[3]);
      descView.setUint8(pos++, 0xff);
      descView.setUint8(pos++, 0xe1);
      descView.setUint16(pos, sps.length, false);
      pos += 2;
      description.set(sps, pos);
      pos += sps.length;
      descView.setUint8(pos++, 1);
      descView.setUint16(pos, pps.length, false);
      pos += 2;
      description.set(pps, pos);

      config.description = description;
    }

    return config;
  }

  static isH264(data) {
    if (!data || data.length < 4) return false;

    const view = new DataView(data.buffer || data, data.byteOffset || 0);

    if (
      view.getUint8(0) === 0x00 &&
      view.getUint8(1) === 0x00 &&
      (view.getUint8(2) === 0x00 || view.getUint8(2) === 0x01) &&
      view.getUint8(2) === 0x01
    ) {
      return true;
    }

    if (data.length >= 5) {
      const nalStart = view.getUint32(0, false);
      if (nalStart > 0 && nalStart < data.length - 4) {
        const nalType = view.getUint8(4) & 0x1f;
        if (nalType >= 1 && nalType <= 23) {
          return true;
        }
      }
    }

    return false;
  }

  close() {
    if (this.decoder) {
      this.decoder.close();
      this.decoder = null;
    }
    this.initialized = false;
  }
}
