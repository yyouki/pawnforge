cmake_minimum_required(VERSION 3.22.1)
project(PawnForgeNative C)

set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# The compiler is pinned as a Git submodule so Android Studio and CI build the same source.
set(PAWN_VENDOR_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../third_party/pawn-compiler-android")
if(NOT EXISTS "${PAWN_VENDOR_DIR}/source/compiler/CMakeLists.txt")
    message(FATAL_ERROR "Pawn compiler submodule missing. Run: git submodule update --init --recursive")
endif()

# Copy compiler source into the build tree so the Android TMPDIR patch never edits the submodule.
set(PAWN_VENDOR_COPY "${CMAKE_CURRENT_BINARY_DIR}/vendor-pawn")
file(REMOVE_RECURSE "${PAWN_VENDOR_COPY}")
file(MAKE_DIRECTORY "${PAWN_VENDOR_COPY}")
file(COPY "${PAWN_VENDOR_DIR}/source" "${PAWN_VENDOR_DIR}/cmake" DESTINATION "${PAWN_VENDOR_COPY}")
configure_file("${PAWN_VENDOR_DIR}/license.txt" "${PAWN_VENDOR_COPY}/license.txt" COPYONLY)

set(PAWN_COMPILER_DIR "${PAWN_VENDOR_COPY}/source/compiler")
set(PAWNC_GLUE "${PAWN_COMPILER_DIR}/libpawnc.c")
file(READ "${PAWNC_GLUE}" PAWNC_GLUE_SOURCE)

# Android app sandboxes do not provide a writable global /tmp. Respect TMPDIR instead.
set(PAWN_TMP_OLD [=[
    static const char template[]="/tmp/pawnXXXXXX";
    if ((tname=malloc(sizeof(template)))!=NULL) {
      int fdtmp;
      strncpy(tname,template,sizeof(template));
]=])
set(PAWN_TMP_NEW [=[
    const char *tmpdir = getenv("TMPDIR");
    size_t template_size;
    if (tmpdir == NULL || tmpdir[0] == '\0') tmpdir = "/tmp";
    template_size = strlen(tmpdir) + sizeof("/pawnXXXXXX");
    if ((tname=malloc(template_size))!=NULL) {
      int fdtmp;
      snprintf(tname, template_size, "%s/pawnXXXXXX", tmpdir);
]=])
string(REPLACE "${PAWN_TMP_OLD}" "${PAWN_TMP_NEW}" PAWNC_GLUE_PATCHED "${PAWNC_GLUE_SOURCE}")
if(PAWNC_GLUE_PATCHED STREQUAL PAWNC_GLUE_SOURCE)
    message(FATAL_ERROR "Could not apply Pawn compiler TMPDIR patch; inspect libpawnc.c in the pinned submodule.")
endif()
file(WRITE "${PAWNC_GLUE}" "${PAWNC_GLUE_PATCHED}")

# Build the upstream compiler shared library; it exports pc_compile().
add_subdirectory("${PAWN_COMPILER_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/pawn-compiler-build")

# libpawnc is loaded beside this JNI bridge from the APK native library directory.
set_target_properties(pawnc PROPERTIES BUILD_RPATH "" INSTALL_RPATH "")

add_library(pawnforge_jni SHARED pawn_compiler_jni.c)
target_include_directories(pawnforge_jni PRIVATE "${PAWN_COMPILER_DIR}")
target_link_libraries(pawnforge_jni PRIVATE pawnc)
