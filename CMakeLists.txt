cmake_minimum_required(VERSION 3.10)
project(stunserver LANGUAGES CXX)
set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

find_package(Boost 1.55.0 COMPONENTS coroutine filesystem system REQUIRED)
find_package(OpenSSL REQUIRED)
find_package(Threads REQUIRED)

include_directories(common core net resources)

target_compile_definitions(stunserver INTERFACE $<$CONFIG:Debug>:DEBUG>)

add_subdirectory(common)
add_subdirectory(core)
add_subdirectory(net)
add_subdirectory(tests)
add_subdirectory(server)
add_subdirectory(client)