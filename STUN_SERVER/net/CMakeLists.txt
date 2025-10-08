set(BINARY ${CMAKE_PROJECT_NAME}_net)
set(SOURCES adapters.cpp polling.cpp ratelimiter.cpp recvfromex.cpp
            resolvehostname.cpp stunsocket.cpp)
add_library(${BINARY} STATIC ${SOURCES})
target_link_libraries(${BINARY} ${CMAKE_PROJECT_NAME}_common
                      ${CMAKE_PROJECT_NAME}_core)
