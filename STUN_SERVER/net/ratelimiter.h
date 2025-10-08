#ifndef RATE_TRACKER_H
#define RATE_TRACKER_H

#include <chrono>
#include <mutex>

#include "socketaddress.h"
#include "fasthash.h"

struct RateTracker
{
    uint64_t count;
    std::chrono::steady_clock::time_point firstEntryTime;
    std::chrono::steady_clock::time_point lastEntryTime;
    std::chrono::steady_clock::time_point penaltyTime;
};

struct RateTrackerAddress
{
    uint64_t addrbytes[2];
    bool operator==(const RateTrackerAddress &other)
    {
        return ((other.addrbytes[0] == addrbytes[0]) && (other.addrbytes[1] == addrbytes[1]));
    }

    RateTrackerAddress() : addrbytes() {}
};

inline size_t FastHash_Hash(const RateTrackerAddress &addr)
{
    size_t result;

    if (sizeof(size_t) >= 8)
    {
        result = addr.addrbytes[0] ^ addr.addrbytes[1];
    }
    else
    {
        uint32_t *pTmp = (uint32_t *)(addr.addrbytes);
        result = pTmp[0] ^ pTmp[1] ^ pTmp[2] ^ pTmp[3];
    }
    return result;
}

class RateLimiter
{
protected:
    virtual std::chrono::steady_clock::time_point get_time();
    uint64_t get_rate(const RateTracker *pRT);

    FastHashDynamic<RateTrackerAddress, RateTracker> _table;

    bool _isUsingLock;

    std::mutex _mutex;

    bool RateCheckImpl(const CSocketAddress &addr);

public:
    static constexpr uint64_t MAX_RATE = 3600;
    static constexpr uint64_t MIN_COUNT_FOR_CONSIDERATION = 60;
    static constexpr uint64_t RESET_INTERVAL_SECONDS = 120;
    static constexpr uint64_t PENALTY_TIME_SECONDS = 3600;

    bool RateCheck(const CSocketAddress &addr);

    RateLimiter(size_t tablesize, bool isUsingLock);
    virtual ~RateLimiter();
};

#endif
