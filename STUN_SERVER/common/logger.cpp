

#include "commonincludes.hpp"

#include "logger.h"


namespace Logging
{
    static uint32_t s_loglevel = LL_ALWAYS; // error and usage messages only

    void VPrintMsg(const char* pszFormat, va_list& args)
    {
        ::vprintf(pszFormat, args);
        ::printf("\n");
    }

    uint32_t GetLogLevel()
    {
        return s_loglevel;
    }

    void SetLogLevel(uint32_t level)
    {
        s_loglevel = level;
    }


    void LogMsg(uint32_t level, const char* pszFormat, ...)
    {
        va_list args;
        va_start(args, pszFormat);

        if (level <= s_loglevel)
        {
            VPrintMsg(pszFormat, args);
        }

        va_end(args);
    }
}
