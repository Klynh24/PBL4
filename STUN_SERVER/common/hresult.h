
#ifndef HRESULT_H_
#define HRESULT_H_


// HRESULT
typedef int32_t HRESULT;

#define SEVERITY_SUCCESS    0
#define SEVERITY_ERROR      1


#define SUCCEEDED(hr) (((HRESULT)(hr)) >= 0)
#define FAILED(hr) (((HRESULT)(hr)) < 0)

#define SYSCALL_SUCCEEDED(x) (x!=-1)
#define SYSCALL_FAILED(x)  (x == -1)

#define HRESULT_CODE(hr)    ((hr) & 0xFFFF)
#define HRESULT_FACILITY(hr)  (((hr) >> 16) & 0x1fff)
#define HRESULT_SEVERITY(hr)  (((hr) >> 31) & 0x1)
#define MAKE_HRESULT(sev,fac,code) \
    ((HRESULT) (((unsigned long)(sev)<<31) | ((unsigned long)(fac)<<16) | ((unsigned long)(code))) )

#define FACILITY_ERRNO 0x800
#define ERRNO_TO_HRESULT(err) MAKE_HRESULT(SEVERITY_ERROR, FACILITY_ERRNO, err)
#define ERRNOHR ERRNO_TO_HRESULT(ERRNO_TO_HRESULT(errno))



#define S_OK                             ((HRESULT)0)
#define S_FALSE                          ((HRESULT)1L)
#define E_UNEXPECTED                     ((HRESULT)(0x8000FFFFL))
#define E_NOTIMPL                        ((HRESULT)(0x80004001L))
#define E_OUTOFMEMORY                    ((HRESULT)(0x8007000EL))
#define E_INVALIDARG                     ((HRESULT)(0x80070057L))
#define E_NOINTERFACE                    ((HRESULT)(0x80004002L))
#define E_POINTER                        ((HRESULT)(0x80004003L))
#define E_HANDLE                         ((HRESULT)(0x80070006L))
#define E_ABORT                          ((HRESULT)(0x80004004L))
#define E_FAIL                           ((HRESULT)(0x80004005L))
#define E_ACCESSDENIED                   ((HRESULT)(0x80070005L))
#define E_PENDING                        ((HRESULT)(0x8000000AL))




#endif /* HRESULT_H_ */
