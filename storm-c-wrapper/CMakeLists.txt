cmake_minimum_required(VERSION 3.22)

# set the project name and version
project(storm-c-wrapper VERSION 1.9.1)
set(CMAKE_BUILD_TYPE "release")

# specify the C++ standard
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED True)

# search for Storm library
find_package(storm REQUIRED)

# specify source files
set(SOURCE_DIR ${CMAKE_SOURCE_DIR}/src)

# set executable
add_executable(${PROJECT_NAME} ${SOURCE_DIR}/main.cpp)
target_include_directories(${PROJECT_NAME} PUBLIC ${SOURCE_DIR} ${storm_INCLUDE_DIR} ${storm-parsers_INCLUDE_DIR} ${storm-pars_INCLUDE_DIR})
target_link_libraries(${PROJECT_NAME} PRIVATE storm storm-parsers storm-pars)

