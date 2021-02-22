# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
string(TOLOWER ${CMAKE_BUILD_TYPE} THIS_BUILD_TYPE)

set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -Os -Oz -ffunction-sections -fdata-sections  -fvisibility=hidden -fvisibility-inlines-hidden -Werror")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -Os -Oz -fno-rtti -fno-exceptions -ffunction-sections -fdata-sections -fvisibility=hidden -fvisibility-inlines-hidden -Werror")
set(CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG}  -Werror")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG}  -Werror")


configure_file("${CMAKE_CURRENT_SOURCE_DIR}/module_config.h.in" "${CMAKE_CURRENT_SOURCE_DIR}/common/module_config.h")

include_directories(common)
include_directories(interface)

set(COPY_DIR ${CMAKE_ANDROID_ARCH_ABI})
if (${MERGE_BUILD} AND ${CMAKE_ANDROID_ARCH_ABI} STREQUAL "arm64-v8a")
	set(COPY_DIR "armeabi-v7a")
	set(HOOK_MODULE_NAME ${HOOK_MODULE_NAME}64)
elseif (${MERGE_BUILD} AND ${CMAKE_ANDROID_ARCH_ABI} STREQUAL "x86_64")
	set(COPY_DIR "x86")
	set(HOOK_MODULE_NAME ${HOOK_MODULE_NAME}64)
endif ()

message(STATUS "api:${ANDROID_PLATFORM}, build type:${THIS_BUILD_TYPE}, hook module:${HOOK_MODULE_NAME}, copy dir:${COPY_DIR}")

add_library(
        ${HOOK_MODULE_NAME}
        SHARED

		hook/hook_common.cpp
		hook/hook_main.cpp
		hook/hook_dlfcn.cpp
		hook/hook_exec.cpp
		hook/hook_io.cpp
		hook/hook_stat.cpp
		hook/hook_unistd.cpp
		hook/hook_time.cpp
		hook/hook_syscall.cpp
		hook/hook_string.cpp
		hook/io_redirect.cpp
		hook/hook_vfs.cpp
		hook/hook_jni_native_interface.cpp
		hook/hook_env.cpp
		hook/hook_properties.cpp
		hook/canonicalize_md.c

        common/maps_util.cpp

		# JNI Common
		common/jni_helper.cpp
)



# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)


# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries(
        ${HOOK_MODULE_NAME}
        ${log-lib})
set_target_properties(${HOOK_MODULE_NAME} PROPERTIES
        RUNTIME_OUTPUT_DIRECTORY
        ${CMAKE_CURRENT_SOURCE_DIR}/../libs/${THIS_BUILD_TYPE}/${COPY_DIR})

set_target_properties(${HOOK_MODULE_NAME} PROPERTIES
		LIBRARY_OUTPUT_DIRECTORY
		${CMAKE_CURRENT_SOURCE_DIR}/../libs/${THIS_BUILD_TYPE}/${COPY_DIR})

set_target_properties(${HOOK_MODULE_NAME} PROPERTIES
		LINK_FLAGS_RELEASE
		"-Wl,--version-script=\"${CMAKE_CURRENT_SOURCE_DIR}/hook/hook.map.txt\"")
