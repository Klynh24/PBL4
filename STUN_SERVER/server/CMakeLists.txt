set(SOURCES main.cpp sampleauthprovider.cpp server.cpp
            stunsocketthread.cpp)

add_executable(server ${SOURCES})
target_link_libraries(
  server
  PRIVATE
    ${CMAKE_PROJECT_NAME}_common
    ${CMAKE_PROJECT_NAME}_core
    ${CMAKE_PROJECT_NAME}_net)
