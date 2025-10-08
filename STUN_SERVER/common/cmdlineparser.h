
#ifndef CMDLINEPARSER_H
#define CMDLINEPARSER_H

#include <getopt.h>

class CCmdLineParser
{
    const option *GenerateOptions();

    std::vector<option> _options;

    std::vector<std::string *> _namelessArgs;

    struct OptionDetail
    {
        std::string strName;
        int has_arg;
        std::string *pStrResult;
    };

    std::vector<OptionDetail> _listOptionDetails;

public:
    HRESULT AddOption(const char *pszName, int has_arg, std::string *pStrResult);
    HRESULT AddNonOption(std::string *pStrResult);
    HRESULT ParseCommandLine(int argc, char **argv, int startindex, bool *fParseError);
};

#endif /* CMDLINEPARSER_H */
