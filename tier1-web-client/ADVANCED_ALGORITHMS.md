# Thuật Toán & Công Nghệ Tối Ưu Hóa Hiệu Năng

## 1. Hardware Acceleration

### 1.1. Hardware Encoding (Server)

- **NVENC (NVIDIA)**, **QuickSync (Intel)**, **VCE (AMD)** qua FFmpeg
- H.264 encoding với preset `ultrafast`, GOP size 60, VBR
- Giảm CPU 60-80%, tăng FPS, giảm latency

### 1.2. Hardware Decoding (Client)

- **WebCodecs API** cho H.264 decoding
- Hardware-accelerated, low-latency
- Fallback về JPEG nếu không hỗ trợ

## 2. Forward Error Correction (FEC)

### 2.1. XOR-based FEC

- Nhóm 10 packets → tạo 2 FEC packets (20% overhead)
- FEC packets = XOR của data packets theo pattern
- Recover packets bị mất mà không cần retransmission
- Giảm latency (không cần NACK cho 1-2 packet loss)

## 3. Packet Prioritization

### 3.1. Priority Levels

- **CRITICAL (0)**: Voice packets
- **HIGH (1)**: Keyframes
- **MEDIUM (2)**: Delta frames
- **LOW (3)**: FEC packets

### 3.2. Implementation

- **PriorityBlockingQueue** ở server và client
- Sử dụng ở UDPSenderThread, PrioritySender (Proxy), và client-side
- Đảm bảo critical data được gửi trước

## 4. Worker Pattern Architecture

### 4.1. Worker Threads

- **VideoWorker**: Xử lý video frames (60 FPS, <50ms latency)
- **AudioMixerWorker**: Mix audio từ tất cả clients
- **ClientAudioWorker**: Xử lý audio cho từng client riêng (<150ms latency)
- **UDPSenderThread**: Dedicated thread cho UDP sending (lock-free)

### 4.2. Benefits

- Parallel processing, giảm blocking
- Tối ưu throughput cho nhiều clients
- Isolation giữa các tasks

## 5. Zero-Copy Optimizations

### 5.1. Memory-Mapped Files

- RetransmissionBuffer sử dụng `MappedByteBuffer`
- Off-heap storage, không chiếm Java heap
- Tránh copy dữ liệu (kernel ↔ user space)

### 5.2. DirectByteBuffer Pool

- DirectBufferPool cho UDP packets
- Tránh heap allocation và GC pressure
- Tái sử dụng buffers

### 5.3. Buffer/Object Pooling

- BufferPool: 1400B, 64KB, 256KB, 1MB
- ObjectPool cho objects phức tạp
- Giảm allocation overhead

## 6. Parallel Broadcasting

### 6.1. Dedicated UDP Sender Thread

- Single thread với PriorityBlockingQueue
- Lock-free packet submission
- Packet pacing (100μs delay giữa packets)

### 6.2. Fire-and-Forget Broadcasting

- Submit tasks, không chờ completion
- Thread pool cho parallel sending
- Maximum throughput cho 100+ clients

## 7. Packet Fragmentation & Reassembly

### 7.1. UDP Fragmentation

- Chia frame lớn thành packets ≤ 1372 bytes (MTU-safe)
- Header 28 bytes: Magic, SeqNum, FrameID, FragmentIndex, etc.
- Hỗ trợ frame lớn không bị giới hạn 1500 bytes

### 7.2. Frame Reassembly (Client)

- Jitter buffer cho ordered playback
- Out-of-order handling
- NACK-based retransmission cho missing fragments
- FEC recovery integration

### 7.3. Rate-Limited Rendering

- `requestAnimationFrame` cho smooth playback
- 16ms interval (~60 FPS)
- Sequential frame ordering

## 8. Adaptive Quality Management

### 8.1. Per-Client Adaptive Quality

- Mỗi client có quality riêng dựa trên network conditions
- Không còn "Worst Client Wins"
- Quality levels: HIGH, MEDIUM, LOW, MINIMAL

### 8.2. Network Quality Monitoring

- Sliding window packet loss calculation
- Exponential Moving Average (EMA) cho bandwidth estimation
- Assessment: GOOD (<5%), POOR (5-15%), CRITICAL (>15%)

### 8.3. Quality Evaluation

- Background task đánh giá mỗi 5 giây
- Tự động điều chỉnh quality
- Per-client optimization

## 9. NACK-based Retransmission

### 9.1. Retransmission Buffer

- Memory-mapped file buffer (100MB)
- Circular buffer, lưu trữ 2000 packets
- Off-heap storage

### 9.2. NACK Mechanism

- Client phát hiện missing packets (gap trong sequence)
- Gửi NACK với danh sách sequence numbers
- Server retransmit từ buffer
- Timeout 200ms, max 5 attempts

## 10. Jitter Buffer

### 10.1. Client-Side Buffer

- Lưu completed frames để ordered playback
- Max size: 5 frames
- Frame age threshold: 200ms (skip frames quá cũ)

### 10.2. Ordered Rendering

- Sequential frame display
- Keyframe có thể break sequence (recovery)
- Rate-limited với requestAnimationFrame

## Luồng Hoạt Động

### Video Streaming Flow

1. **Client Capture**: Screen capture → JPEG/H.264 encoding
2. **Client → Proxy**: WebSocket binary (fragmented nếu cần)
3. **Proxy → Core Server**: UDP packets với fragmentation
4. **Core Server Processing**:
   - UDPMediaHandler nhận packets
   - Route đến VideoWorker
   - VideoWorker: Store vào retransmission buffer → Broadcast
5. **Broadcasting**:
   - BroadcastWorker xác định priority
   - UDPSenderThread gửi với priority queue
   - Parallel sending đến tất cả clients
6. **Core → Proxy**: UDP packets với priority
7. **Proxy → Client**: WebSocket binary với PrioritySender
8. **Client Processing**:
   - FrameReassembler reassemble fragments
   - FEC recovery nếu cần
   - NACK nếu còn missing
   - Jitter buffer cho ordering
   - H.264 decoder hoặc JPEG display

### Audio Streaming Flow

1. **Client Capture**: Microphone → PCM audio
2. **Client → Proxy → Core Server**: UDP packets
3. **AudioMixerWorker**: Mix audio từ tất cả clients
4. **ClientAudioWorker**: Process audio cho từng client (volume, echo cancellation)
5. **Broadcast**: Gửi mixed audio về clients
6. **Client Playback**: Jitter buffer → AudioContext

### Packet Loss Recovery Flow

1. **FEC First**: Client thử recover bằng FEC packets
2. **NACK Request**: Nếu FEC không đủ, gửi NACK
3. **Retransmission**: Server retransmit từ RetransmissionBuffer
4. **Timeout Handling**: Skip frames quá cũ (200ms threshold)

## Tổng Kết Hiệu Quả

### Băng Thông

- FEC: +20% overhead (nhưng giảm retransmission)
- Fragmentation: Hỗ trợ frame lớn
- Adaptive Quality: Giảm 50-80% khi cần

### CPU

- Hardware Encoding: Giảm 60-80%
- Zero-Copy: Giảm memory copy
- Pooling: Giảm allocation overhead

### Latency

- Hardware Acceleration: <50ms encoding
- Priority Queue: Critical data gửi trước
- Worker Pattern: Parallel processing
- **Target**: <100ms end-to-end

### Memory

- Memory-Mapped Files: Off-heap storage
- Buffer Pooling: Reuse buffers
- Giảm heap usage 60-80%
