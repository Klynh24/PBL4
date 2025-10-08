

#include "commonincludes.hpp"

namespace boost
{

    void assertion_failed_msg(char const *expr, char const *msg, char const *function, char const *file, long line)
    {
        printf("ASSERTION FAILED: %s\n"
               "         message: %s\n"
               "        function: %s\n"
               "            file: %s\n"
               "            line: %ld\n",
               expr ? expr : "(null)", msg ? msg : "(null)", function ? function : "(null)", file, line);
    }

    void assertion_failed(char const *expr, char const *function, char const *file, long line)
    {
        assertion_failed_msg(expr, expr, function, file, line);
    }

}
