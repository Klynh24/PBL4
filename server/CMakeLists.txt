add_executable(server main.cpp connection.cpp server.cpp thread.cpp tcpserver.cpp)

target_link_libraries(server PRIVATE stunserver_core stunserver_net Boost::filesystem Boost::system OpenSSL::SSL OpenSSL::Crypto Threads::Threads)