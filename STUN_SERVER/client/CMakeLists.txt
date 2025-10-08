set(SOURCES clientmain.cpp)

add_executable(client ${SOURCES})

target_link_libraries(client
  PRIVATE
    ${CMAKE_PROJECT_NAME}_common
    ${CMAKE_PROJECT_NAME}_core
    ${CMAKE_PROJECT_NAME}_net
    Boost::filesystem
    Boost::system
    OpenSSL::SSL
    OpenSSL::Crypto
    Threads::Threads)
