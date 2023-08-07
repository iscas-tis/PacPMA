cmake_minimum_required(VERSION 3.8)

# set the project name and version
project(storm-c-wrapper VERSION 1.8.1)
set(CMAKE_BUILD_TYPE "release")

# specify the C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED True)

# search for Storm library
find_package(storm REQUIRED)

# specify source files
set(SOURCE_FILES src/main.cpp)

# set executable
add_executable(${PROJECT_NAME} ${SOURCE_FILES})
target_include_directories(${PROJECT_NAME} PUBLIC ${SOURCE_FILES} ${storm_INCLUDE_DIR} ${storm-parsers_INCLUDE_DIR} ${storm-pars_INCLUDE_DIR})
target_link_libraries(${PROJECT_NAME} PRIVATE storm storm-parsers storm-pars)

