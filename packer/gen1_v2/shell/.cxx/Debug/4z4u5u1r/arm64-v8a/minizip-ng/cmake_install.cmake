# Install script for directory: C:/Users/tinyx/Downloads/gen1_v1/shell/src/main/cpp/minizip-ng

# Set the install prefix
if(NOT DEFINED CMAKE_INSTALL_PREFIX)
  set(CMAKE_INSTALL_PREFIX "C:/Program Files (x86)/dpt")
endif()
string(REGEX REPLACE "/$" "" CMAKE_INSTALL_PREFIX "${CMAKE_INSTALL_PREFIX}")

# Set the install configuration name.
if(NOT DEFINED CMAKE_INSTALL_CONFIG_NAME)
  if(BUILD_TYPE)
    string(REGEX REPLACE "^[^A-Za-z0-9_]+" ""
           CMAKE_INSTALL_CONFIG_NAME "${BUILD_TYPE}")
  else()
    set(CMAKE_INSTALL_CONFIG_NAME "Debug")
  endif()
  message(STATUS "Install configuration: \"${CMAKE_INSTALL_CONFIG_NAME}\"")
endif()

# Set the component getting installed.
if(NOT CMAKE_INSTALL_COMPONENT)
  if(COMPONENT)
    message(STATUS "Install component: \"${COMPONENT}\"")
    set(CMAKE_INSTALL_COMPONENT "${COMPONENT}")
  else()
    set(CMAKE_INSTALL_COMPONENT)
  endif()
endif()

# Install shared libraries without execute permission?
if(NOT DEFINED CMAKE_INSTALL_SO_NO_EXE)
  set(CMAKE_INSTALL_SO_NO_EXE "0")
endif()

# Is this installation the result of a crosscompile?
if(NOT DEFINED CMAKE_CROSSCOMPILING)
  set(CMAKE_CROSSCOMPILING "TRUE")
endif()

# Set default install directory permissions.
if(NOT DEFINED CMAKE_OBJDUMP)
  set(CMAKE_OBJDUMP "C:/Users/tinyx/AppData/Local/Android/Sdk/ndk/21.4.7075529/toolchains/llvm/prebuilt/windows-x86_64/bin/aarch64-linux-android-objdump.exe")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib" TYPE STATIC_LIBRARY FILES "C:/Users/tinyx/Downloads/gen1_v1/shell/.cxx/Debug/4z4u5u1r/arm64-v8a/minizip-ng/libminizip.a")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  if(EXISTS "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/cmake/minizip/minizip.cmake")
    file(DIFFERENT EXPORT_FILE_CHANGED FILES
         "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/cmake/minizip/minizip.cmake"
         "C:/Users/tinyx/Downloads/gen1_v1/shell/.cxx/Debug/4z4u5u1r/arm64-v8a/minizip-ng/CMakeFiles/Export/lib/cmake/minizip/minizip.cmake")
    if(EXPORT_FILE_CHANGED)
      file(GLOB OLD_CONFIG_FILES "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/cmake/minizip/minizip-*.cmake")
      if(OLD_CONFIG_FILES)
        message(STATUS "Old export file \"$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/cmake/minizip/minizip.cmake\" will be replaced.  Removing files [${OLD_CONFIG_FILES}].")
        file(REMOVE ${OLD_CONFIG_FILES})
      endif()
    endif()
  endif()
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib/cmake/minizip" TYPE FILE FILES "C:/Users/tinyx/Downloads/gen1_v1/shell/.cxx/Debug/4z4u5u1r/arm64-v8a/minizip-ng/CMakeFiles/Export/lib/cmake/minizip/minizip.cmake")
  if("${CMAKE_INSTALL_CONFIG_NAME}" MATCHES "^([Dd][Ee][Bb][Uu][Gg])$")
    file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib/cmake/minizip" TYPE FILE FILES "C:/Users/tinyx/Downloads/gen1_v1/shell/.cxx/Debug/4z4u5u1r/arm64-v8a/minizip-ng/CMakeFiles/Export/lib/cmake/minizip/minizip-debug.cmake")
  endif()
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib/cmake/minizip" TYPE FILE FILES
    "C:/Users/tinyx/Downloads/gen1_v1/shell/.cxx/Debug/4z4u5u1r/arm64-v8a/minizip-ng/minizip-config-version.cmake"
    "C:/Users/tinyx/Downloads/gen1_v1/shell/.cxx/Debug/4z4u5u1r/arm64-v8a/minizip-ng/minizip-config.cmake"
    )
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include" TYPE FILE FILES
    "C:/Users/tinyx/Downloads/gen1_v1/shell/src/main/cpp/minizip-ng/mz.h"
    "C:/Users/tinyx/Downloads/gen1_v1/shell/src/main/cpp/minizip-ng/mz_os.h"
    "C:/Users/tinyx/Downloads/gen1_v1/shell/src/main/cpp/minizip-ng/mz_crypt.h"
    "C:/Users/tinyx/Downloads/gen1_v1/shell/src/main/cpp/minizip-ng/mz_strm.h"
    "C:/Users/tinyx/Downloads/gen1_v1/shell/src/main/cpp/minizip-ng/mz_strm_buf.h"
    "C:/Users/tinyx/Downloads/gen1_v1/shell/src/main/cpp/minizip-ng/mz_strm_mem.h"
    "C:/Users/tinyx/Downloads/gen1_v1/shell/src/main/cpp/minizip-ng/mz_strm_split.h"
    "C:/Users/tinyx/Downloads/gen1_v1/shell/src/main/cpp/minizip-ng/mz_strm_os.h"
    "C:/Users/tinyx/Downloads/gen1_v1/shell/src/main/cpp/minizip-ng/mz_zip.h"
    "C:/Users/tinyx/Downloads/gen1_v1/shell/src/main/cpp/minizip-ng/mz_zip_rw.h"
    "C:/Users/tinyx/Downloads/gen1_v1/shell/src/main/cpp/minizip-ng/mz_strm_zlib.h"
    "C:/Users/tinyx/Downloads/gen1_v1/shell/src/main/cpp/minizip-ng/mz_strm_pkcrypt.h"
    "C:/Users/tinyx/Downloads/gen1_v1/shell/src/main/cpp/minizip-ng/mz_compat.h"
    "C:/Users/tinyx/Downloads/gen1_v1/shell/.cxx/Debug/4z4u5u1r/arm64-v8a/minizip-ng/zip.h"
    "C:/Users/tinyx/Downloads/gen1_v1/shell/.cxx/Debug/4z4u5u1r/arm64-v8a/minizip-ng/unzip.h"
    )
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib/pkgconfig" TYPE FILE FILES "C:/Users/tinyx/Downloads/gen1_v1/shell/.cxx/Debug/4z4u5u1r/arm64-v8a/minizip-ng/minizip.pc")
endif()

