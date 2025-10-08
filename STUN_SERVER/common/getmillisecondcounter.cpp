

#include "commonincludes.hpp"
#include "oshelper.h"


static uint32_t GetMillisecondCounterUnix()
{
    uint64_t milliseconds = 0;
    uint32_t retvalue;
    timeval tv = {};
    gettimeofday(&tv, NULL);
    milliseconds = (tv.tv_sec * (unsigned long long)1000) + (tv.tv_usec / 1000);
    retvalue = (uint32_t)(milliseconds & (unsigned long long)0xffffffff);
    return retvalue;
}

uint32_t GetMillisecondCounter()
{
#ifdef _WIN32
    return GetTickCount();
#else
    return GetMillisecondCounterUnix();    
#endif
}
