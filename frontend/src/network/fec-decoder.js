/**
 * FEC XOR DECODER - Forward Error Correction Decoder (Client Side)
 * Ported from Network/tier1-web-client/js/fec-decoder.js
 */
export default class FecXorDecoder {
  constructor(groupSize = 10, fecCount = 2) {
    this.groupSize = groupSize;
    this.fecCount = fecCount;

    this.stats = {
      packetsRecovered: 0,
      recoveryAttempts: 0,
      recoveryFailures: 0,
    };
  }

  /**
   * @param {(Uint8Array|null)[]} receivedPackets
   * @param {(Uint8Array|null)[]} fecPackets
   * @param {number} lostIndex
   * @returns {Uint8Array|null}
   */
  recoverPacket(receivedPackets, fecPackets, lostIndex) {
    this.stats.recoveryAttempts++;

    if (
      !receivedPackets ||
      !fecPackets ||
      lostIndex < 0 ||
      lostIndex >= receivedPackets.length
    ) {
      this.stats.recoveryFailures++;
      return null;
    }

    const fecIdx = lostIndex % this.fecCount;

    if (fecIdx >= fecPackets.length || !fecPackets[fecIdx]) {
      this.stats.recoveryFailures++;
      return null;
    }

    const fecPacket = fecPackets[fecIdx];

    let maxSize = fecPacket.length;
    for (let i = 0; i < receivedPackets.length; i++) {
      if (receivedPackets[i] && receivedPackets[i].length > maxSize) {
        maxSize = receivedPackets[i].length;
      }
    }

    const recovered = new Uint8Array(maxSize);
    recovered.set(fecPacket.slice(0, Math.min(fecPacket.length, maxSize)));

    for (let i = 0; i < receivedPackets.length; i++) {
      if (i === lostIndex) continue;

      const packet = receivedPackets[i];
      if (packet && this.shouldInclude(i, fecIdx)) {
        this.xorBytes(recovered, packet);
      }
    }

    this.stats.packetsRecovered++;
    return recovered;
  }

  /**
   * @param {(Uint8Array|null)[]} receivedPackets
   * @param {(Uint8Array|null)[]} fecPackets
   * @param {Set<number>} lostIndices
   * @returns {Map<number, Uint8Array>}
   */
  recoverMultiplePackets(receivedPackets, fecPackets, lostIndices) {
    const recovered = new Map();

    if (
      !lostIndices ||
      lostIndices.size === 0 ||
      lostIndices.size > fecPackets.length
    ) {
      return recovered;
    }

    for (const lostIndex of lostIndices) {
      const recoveredPacket = this.recoverPacket(
        receivedPackets,
        fecPackets,
        lostIndex
      );
      if (recoveredPacket) {
        recovered.set(lostIndex, recoveredPacket);
      }
    }

    return recovered;
  }

  /** @param {number} dataIdx @param {number} fecIdx */
  shouldInclude(dataIdx, fecIdx) {
    return dataIdx % this.fecCount === fecIdx;
  }

  /** @param {Uint8Array} target @param {Uint8Array} source */
  xorBytes(target, source) {
    const minLen = Math.min(target.length, source.length);
    for (let i = 0; i < minLen; i++) {
      target[i] ^= source[i];
    }
  }

  getStats() {
    const recoveryRate =
      this.stats.recoveryAttempts > 0
        ? (
            (this.stats.packetsRecovered / this.stats.recoveryAttempts) *
            100
          ).toFixed(2)
        : 0;

    return {
      ...this.stats,
      recoveryRate: recoveryRate + "%",
    };
  }

  resetStats() {
    this.stats = {
      packetsRecovered: 0,
      recoveryAttempts: 0,
      recoveryFailures: 0,
    };
  }
}
