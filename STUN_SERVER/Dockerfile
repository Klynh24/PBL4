FROM ubuntu:22.04

# Install build dependencies for the STUN server
RUN apt-get update && apt-get install -y \
    build-essential \
    cmake \
    libboost-all-dev \
    libssl-dev \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy project sources
COPY . .

# Configure and build the server target
RUN cmake -S . -B build && cmake --build build --target server

# Run the STUN server in basic mode, listening on UDP 3478 on all interfaces
CMD ["./build/server/server", "--mode", "basic", "--primaryport", "3478"]
