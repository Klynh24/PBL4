#ifndef STUN_SERVER_H
#define STUN_SERVER_H

#include <vector>
#include <array>

#include "socket.h"
#include "thread.h"
#include "messagehandler.h"

struct StunServerConfig
{
    bool hasPP = false;
    bool hasPA = false;
    bool hasAP = false;
    bool hasAA = false;

    bool multiThreadedMode = false;

    bool useTCP = false;
    uint32_t maxConnections = 1000;

    SocketAddress addrPP;
    SocketAddress addrPA;
    SocketAddress addrAP;
    SocketAddress addrAA;

    SocketAddress addrPrimaryAdvertised;
    SocketAddress addrAlternateAdvertised;

    bool enableDosProtection = false;
    bool reuseAddr = false;
};

class StunServer
{
private:
    std::array<StunSocket, 4> _sockets;
    std::vector<std::unique_ptr<StunSocketThread>> _threads;
    HRESULT AddSocket(TransportAddressSet *ptSA, SocketRole role, const SocketAddress &addrListen, const SocketAddress &addrAdvertise, bool SetReuseFlag);

public:
    StunServer();
    ~StunServer();
    HRESULT Initialize(const StunServerConfig &config);
    HRESULT Shutdown();

    HRESULT Start();
    HRESULT Stop();
};

#endif
