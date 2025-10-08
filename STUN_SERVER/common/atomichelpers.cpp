
#include "commonincludes.hpp"
#include "atomichelpers.h"

#if defined(i386) || defined(__i386) || defined(__i386__)
#define ATOMICS_USE_XADD
#endif

#ifdef ATOMICS_USE_XADD

unsigned int xadd_4(volatile void *pVal, unsigned int inc)
{
    unsigned int result;
    unsigned int *pValInt = (unsigned int *)pVal;

    asm volatile(
        "lock; xaddl %%eax, %2;"
        : "=a"(result)
        : "a"(inc), "m"(*pValInt)
        : "memory");

    return (result);
}

int AtomicIncrement(int *pInt)
{
    COMPILE_TIME_ASSERT(sizeof(int) == 4);
    // InterlockedIncrement
    unsigned int result = xadd_4(pInt, 1) + 1;
    return (int)result;
}

int AtomicDecrement(int *pInt)
{
    // InterlockedDecrement
    unsigned int result = xadd_4(pInt, -1) - 1;
    return (int)result;
}

#else

int AtomicIncrement(int *pInt)
{
    COMPILE_TIME_ASSERT(sizeof(int) == 4);
    // InterlockedIncrement
    return __sync_add_and_fetch(pInt, 1);
}

int AtomicDecrement(int *pInt)
{
    // InterlockedDecrement
    return __sync_sub_and_fetch(pInt, 1);
}

#endif
