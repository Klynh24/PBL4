#include "commonincludes.hpp"
#include "socketaddress.h"
#include "fasthash.h"
#include "ratelimiter.h"

constexpr uint64_t RateLimiter::MAX_RATE;
constexpr uint64_t RateLimiter::MIN_COUNT_FOR_CONSIDERATION;
constexpr uint64_t RateLimiter::RESET_INTERVAL_SECONDS;
constexpr uint64_t RateLimiter::PENALTY_TIME_SECONDS;

RateLimiter::RateLimiter(size_t tablesize, bool isUsingLock)
{
    _table.InitTable(tablesize, tablesize / 2);
    this->_isUsingLock = isUsingLock;
}

RateLimiter::~RateLimiter() {}

std::chrono::steady_clock::time_point RateLimiter::get_time()
{
    return std::chrono::steady_clock::now();
}

uint64_t RateLimiter::get_rate(const RateTracker *pRT)
{
    if (pRT->count < MIN_COUNT_FOR_CONSIDERATION)
    {
        return 0;
    }

    auto duration = pRT->lastEntryTime - pRT->firstEntryTime;
    auto seconds = std::chrono::duration_cast<std::chrono::milliseconds>(duration).count() / 1000;

    if (seconds < 1.0)
        seconds = 1.0;

    uint64_t rate = static_cast<uint64_t>((pRT->count * 3600) / seconds);

    return rate;
}

bool RateLimiter::RateCheck(const CSocketAddress &addr)
{
    if (_isUsingLock)
    {
        std::lock_guard<std::mutex> lock(_mutex);
        return RateCheckImpl(addr);
    }

    return RateCheckImpl(addr);
}

bool RateLimiter::RateCheckImpl(const CSocketAddress &addr)
{
    RateTrackerAddress rtaddr;
    addr.GetIP(rtaddr.addrbytes, sizeof(rtaddr.addrbytes));

    auto currentTime = get_time();

    RateTracker *pRT = this->_table.Lookup(rtaddr);
    uint64_t rate = 0;

    if (pRT == nullptr)
    {
        RateTracker rt;
        rt.count = 1;
        rt.firstEntryTime = currentTime;
        rt.lastEntryTime = currentTime;
        rt.penaltyTime = currentTime;

        int result = _table.Insert(rtaddr, rt);
        /*Tối ưu bằng LRU*/
        if (result == -1)
        {
            RateTrackerAddress oldestAddr;
            auto oldestTime = std::chrono::steady_clock::time_point::max();
            bool foundOldest = false;

            for (auto it = this->_table.begin(); it != this->_table.end(); ++it)
            {
                if (it->value.lastEntryTime < oldestTime)
                {
                    oldestTime = it->value.lastEntryTime;
                    oldestAddr = it->key;
                    foundOldest = true;
                }
            }
            if (foundOldest)
            {
                _table.Remove(oldestAddr);
                _table.Insert(rtaddr, rt);
            }
        }
        return true;
    }
    pRT->count++;
    pRT->lastEntryTime = currentTime;

    if (pRT->penaltyTime > currentTime)
        return false;

    rate = get_rate(pRT);
    if (pRT->penaltyTime <= currentTime && (pRT->penaltyTime != pRT->firstEntryTime))
    {
        if (rate < MAX_RATE)
            pRT->penaltyTime = pRT->firstEntryTime;
        else
        {
            pRT->penaltyTime = currentTime + std::chrono::seconds(PENALTY_TIME_SECONDS);
            return false;
        }
    }

    if (rate >= MAX_RATE)
    {
        pRT->penaltyTime = currentTime + std::chrono::seconds(PENALTY_TIME_SECONDS);
        return false;
    }

    auto age = pRT->lastEntryTime - pRT->firstEntryTime;
    if (age > std::chrono::seconds(RESET_INTERVAL_SECONDS))
    {
        _table.Remove(rtaddr);
    }

    return true;
}
