/**
 * ✅ FEC XOR DECODER - Forward Error Correction Decoder (Client Side)
 * 
 * Recover lost packets using FEC packets mà không cần NACK
 * 
 * Algorithm:
 * - Nhận data packets + FEC packets
 * - Khi detect packet mất → sử dụng FEC để recover
 * - Pattern: FEC1 = XOR(odd indices), FEC2 = XOR(even indices)
 */
class FecXorDecoder {
    constructor(groupSize = 10, fecCount = 2) {
        this.groupSize = groupSize;
        this.fecCount = fecCount;
        
        // Stats
        this.stats = {
            packetsRecovered: 0,
            recoveryAttempts: 0,
            recoveryFailures: 0
        };
    }
    
    /**
     * Recover một packet bị mất
     * 
     * @param {Array<Uint8Array|null>} receivedPackets - List packets đã nhận (null = mất)
     * @param {Array<Uint8Array>} fecPackets - List FEC packets
     * @param {number} lostIndex - Index của packet bị mất
     * @returns {Uint8Array|null} Recovered packet hoặc null nếu không thể recover
     */
    recoverPacket(receivedPackets, fecPackets, lostIndex) {
        this.stats.recoveryAttempts++;
        
        if (!receivedPackets || !fecPackets || lostIndex < 0 || lostIndex >= receivedPackets.length) {
            this.stats.recoveryFailures++;
            return null;
        }
        
        // Tìm FEC packet phù hợp (cùng pattern với lost packet)
        const fecIdx = lostIndex % this.fecCount;
        
        if (fecIdx >= fecPackets.length || !fecPackets[fecIdx]) {
            this.stats.recoveryFailures++;
            return null; // Không có FEC packet tương ứng
        }
        
        const fecPacket = fecPackets[fecIdx];
        
        // Tìm max size
        let maxSize = fecPacket.length;
        for (let i = 0; i < receivedPackets.length; i++) {
            if (receivedPackets[i] && receivedPackets[i].length > maxSize) {
                maxSize = receivedPackets[i].length;
            }
        }
        
        // Bắt đầu với FEC packet (copy)
        const recovered = new Uint8Array(maxSize);
        recovered.set(fecPacket.slice(0, Math.min(fecPacket.length, maxSize)));
        
        // XOR với tất cả received packets (trừ lost packet)
        for (let i = 0; i < receivedPackets.length; i++) {
            if (i === lostIndex) {
                continue; // Skip lost packet
            }
            
            const packet = receivedPackets[i];
            if (packet && this.shouldInclude(i, fecIdx)) {
                this.xorBytes(recovered, packet);
            }
        }
        
        this.stats.packetsRecovered++;
        return recovered;
    }
    
    /**
     * Recover nhiều packets bị mất (nếu có đủ FEC)
     * 
     * @param {Array<Uint8Array|null>} receivedPackets - List packets đã nhận
     * @param {Array<Uint8Array>} fecPackets - List FEC packets
     * @param {Set<number>} lostIndices - Set các indices bị mất
     * @returns {Map<number, Uint8Array>} Map của recovered packets (index -> packet)
     */
    recoverMultiplePackets(receivedPackets, fecPackets, lostIndices) {
        const recovered = new Map();
        
        if (!lostIndices || lostIndices.size === 0 || lostIndices.size > fecPackets.length) {
            return recovered; // Không đủ FEC để recover
        }
        
        // Recover từng packet một
        for (const lostIndex of lostIndices) {
            const recoveredPacket = this.recoverPacket(receivedPackets, fecPackets, lostIndex);
            if (recoveredPacket) {
                recovered.set(lostIndex, recoveredPacket);
            }
        }
        
        return recovered;
    }
    
    /**
     * Kiểm tra xem data packet index có nên được XOR vào FEC packet này không
     */
    shouldInclude(dataIdx, fecIdx) {
        return (dataIdx % this.fecCount) === fecIdx;
    }
    
    /**
     * XOR byte array vào target (in-place)
     */
    xorBytes(target, source) {
        const minLen = Math.min(target.length, source.length);
        for (let i = 0; i < minLen; i++) {
            target[i] ^= source[i];
        }
    }
    
    /**
     * Get statistics
     */
    getStats() {
        const recoveryRate = this.stats.recoveryAttempts > 0
            ? (this.stats.packetsRecovered / this.stats.recoveryAttempts * 100).toFixed(2)
            : 0;
        
        return {
            ...this.stats,
            recoveryRate: recoveryRate + '%'
        };
    }
    
    /**
     * Reset statistics
     */
    resetStats() {
        this.stats = {
            packetsRecovered: 0,
            recoveryAttempts: 0,
            recoveryFailures: 0
        };
    }
}

// Export for use in other files
if (typeof module !== 'undefined' && module.exports) {
    module.exports = FecXorDecoder;
}

