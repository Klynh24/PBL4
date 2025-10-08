

#ifndef LOGGING_H
#define LOGGING_H



const uint32_t LL_ALWAYS = 0;      // only help messages, output the user expects to see, and critical error messages
const uint32_t LL_DEBUG = 1;    // messages helpful for debugging
const uint32_t LL_VERBOSE = 2;     // every packet
const uint32_t LL_VERBOSE_EXTREME = 3; // every packet and all the details

namespace Logging
{
    uint32_t GetLogLevel();
    void SetLogLevel(uint32_t level);
    void LogMsg(uint32_t level, const char* pszFormat, ...);
}

#endif
