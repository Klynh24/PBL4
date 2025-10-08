
#ifndef STRINGHELPER_H
#define STRINGHELPER_H

#include <string>

namespace StringHelper
{

    bool IsNullOrEmpty(const char *psz);

    void ToLower(std::string &str);
    void Trim(std::string &str);

    int ValidateNumberString(const char *psz, int nMinValue, int nMaxValue, int *pnResult);
}

#endif /* STRINGHELPER_H */
