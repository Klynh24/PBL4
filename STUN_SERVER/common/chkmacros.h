

#ifndef CHKMACROS_H
#define CHKMACROS_H

#include "hresult.h"

template <typename T>
inline HRESULT CheckCore(const T t)
{
    t.What_You_Passed_To_Chk_Is_Not_HRESULT();
    return E_FAIL;
}

template <typename T>
inline bool CheckIfCore(const T t)
{
    t.What_You_Passed_To_ChkIf_Is_Not_bool();
    return false;
}

template <>
inline HRESULT CheckCore<HRESULT>(const HRESULT t)
{
    return t;
}

template <>
inline bool CheckIfCore<bool>(const bool t)
{
    return t;
}

#define Chk(expr)               \
    {                           \
        ((void)hr);             \
        hr = CheckCore((expr)); \
        if (FAILED(hr))         \
        {                       \
            goto Cleanup;       \
        }                       \
    }

#define ChkIf(expr, hrerror)     \
    {                            \
        ((void)hr);              \
        if (CheckIfCore((expr))) \
        {                        \
            hr = (hrerror);      \
            goto Cleanup;        \
        }                        \
    }

#define ChkA(expr)              \
    {                           \
        ((void)hr);             \
        hr = CheckCore((expr)); \
        if (FAILED(hr))         \
        {                       \
            ASSERT(false);      \
            goto Cleanup;       \
        }                       \
    }

#define ChkIfA(expr, hrerror)    \
    {                            \
        ((void)hr);              \
        if (CheckIfCore((expr))) \
        {                        \
            ASSERT(false);       \
            hr = (hrerror);      \
            goto Cleanup;        \
        }                        \
    }

#endif
