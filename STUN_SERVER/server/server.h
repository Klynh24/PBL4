#ifndef STUN_SERVER_H
#define STUN_SERVER_H

#include "stunsocket.h"
#include "stunsocketthread.h"
#include "stunauth.h"
#include "messagehandler.h"

class CStunServerConfig
{
public:
    bool fHasPP;
    bool fHasPA;
    bool fHasAP;
    bool fHasAA;

    bool fMultiThreadedMode;

    bool fTCP;
    uint32_t nMaxConnections;

    CSocketAddress addrPP;
    CSocketAddress addrPA;
    CSocketAddress addrAP;
    CSocketAddress addrAA;

    CSocketAddress addrPrimaryAdvertised;
    CSocketAddress addrAlternateAdvertised;

    bool fEnableDosProtection;

    bool fReuseAddr;

    CStunServerConfig();
};

class CStunServer : public CBasicRefCount, public CObjectFactory<CStunServer>, public IRefCounted
{
private:
    CStunSocket _arrSockets[4];
    std::vector<CStunSocketThread *> _threads;

    CStunServer();
    ~CStunServer();

    friend class CObjectFactory<CStunServer>;

    CRefCountedPtr<IStunAuth> _spAuth;

    HRESULT AddSocket(TransportAddressSet *pTSA, SocketRole role, const CSocketAddress &addrListen, const CSocketAddress &addrAdvertise, bool fSetReuseFlag);

public:
    HRESULT Initialize(const CStunServerConfig &config);
    HRESULT Shutdown();

    HRESULT Start();
    HRESULT Stop();

    ADDREF_AND_RELEASE_IMPL();
};

#endif
