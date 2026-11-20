cmake_minimum_required(VERSION 3.22.1)
project(origin_harness LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(origin_harness SHARED
        origin_harness.cpp
        origin_jni.cpp)

find_library(log-lib log)
target_link_libraries(origin_harness ${log-lib})

# No third-party sources are vendored here. Engines are optional and loaded separately.
