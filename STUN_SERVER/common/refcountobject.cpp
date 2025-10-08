

#include "commonincludes.hpp"
#include "refcountobject.h"


#include "atomichelpers.h"


CBasicRefCount::CBasicRefCount()
{
    m_nRefs = 0;
}

CBasicRefCount::~CBasicRefCount()
{
    ;
}

int CBasicRefCount::InternalAddRef()
{
    return AtomicIncrement(&m_nRefs);
}

int CBasicRefCount::InternalRelease()
{
    int refcount = AtomicDecrement(&m_nRefs);
    if (refcount == 0)
    {
        OnFinalRelease();
    }
    return refcount;
}

void CBasicRefCount::OnFinalRelease()
{
    delete this;
}



