#include <jni.h>
#include <fcntl.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <limits.h>

// Exported by libpawnc; this invokes the same compiler entry point as pawncc.
extern int pc_compile(int argc, char **argv);

static pthread_mutex_t g_compile_mutex = PTHREAD_MUTEX_INITIALIZER;

static char *copy_jstring(JNIEnv *env, jstring value) {
    if (value == NULL) return NULL;
    const char *utf = (*env)->GetStringUTFChars(env, value, NULL);
    if (utf == NULL) return NULL;
    char *copy = strdup(utf);
    (*env)->ReleaseStringUTFChars(env, value, utf);
    return copy;
}

JNIEXPORT jint JNICALL
Java_com_youki_pawnforge_PawnNativeCompiler_nativeCompile(
    JNIEnv *env, jclass clazz, jobjectArray java_args,
    jstring java_log_path, jstring java_temp_dir, jstring java_work_dir) {
    (void)clazz;

    if (java_args == NULL) return -100;
    const jsize argc = (*env)->GetArrayLength(env, java_args);
    if (argc < 2) return -101;

    char **argv = (char **)calloc((size_t)argc + 1U, sizeof(char *));
    if (argv == NULL) return -102;

    for (jsize index = 0; index < argc; ++index) {
        jstring item = (jstring)(*env)->GetObjectArrayElement(env, java_args, index);
        argv[index] = copy_jstring(env, item);
        if (item != NULL) (*env)->DeleteLocalRef(env, item);
        if (argv[index] == NULL) {
            for (jsize cleanup = 0; cleanup < index; ++cleanup) free(argv[cleanup]);
            free(argv);
            return -103;
        }
    }

    char *log_path = copy_jstring(env, java_log_path);
    char *temp_dir = copy_jstring(env, java_temp_dir);
    char *work_dir = copy_jstring(env, java_work_dir);
    if (log_path == NULL || temp_dir == NULL || work_dir == NULL) {
        for (jsize index = 0; index < argc; ++index) free(argv[index]);
        free(argv); free(log_path); free(temp_dir); free(work_dir);
        return -104;
    }

    pthread_mutex_lock(&g_compile_mutex);

    int result = -105;
    int log_fd = -1;
    int saved_stdout = -1;
    int saved_stderr = -1;
    char previous_dir[PATH_MAX];
    const int have_previous_dir = getcwd(previous_dir, sizeof(previous_dir)) != NULL;
    char *previous_tmpdir = getenv("TMPDIR") ? strdup(getenv("TMPDIR")) : NULL;

    fflush(stdout);
    fflush(stderr);
    log_fd = open(log_path, O_CREAT | O_TRUNC | O_WRONLY, 0600);
    if (log_fd >= 0) {
        saved_stdout = dup(STDOUT_FILENO);
        saved_stderr = dup(STDERR_FILENO);
        if (saved_stdout >= 0 && saved_stderr >= 0 &&
            dup2(log_fd, STDOUT_FILENO) >= 0 && dup2(log_fd, STDERR_FILENO) >= 0 &&
            chdir(work_dir) == 0) {
            // Keep compiler output unbuffered so every diagnostic reaches the log file.
            setvbuf(stdout, NULL, _IONBF, 0);
            setvbuf(stderr, NULL, _IONBF, 0);
            setenv("TMPDIR", temp_dir, 1);
            result = pc_compile((int)argc, argv);
            fflush(stdout);
            fflush(stderr);
        }
    }

    if (saved_stdout >= 0) { dup2(saved_stdout, STDOUT_FILENO); close(saved_stdout); }
    if (saved_stderr >= 0) { dup2(saved_stderr, STDERR_FILENO); close(saved_stderr); }
    if (log_fd >= 0) { fsync(log_fd); close(log_fd); }
    if (have_previous_dir) chdir(previous_dir);
    if (previous_tmpdir != NULL) { setenv("TMPDIR", previous_tmpdir, 1); free(previous_tmpdir); }
    else unsetenv("TMPDIR");

    pthread_mutex_unlock(&g_compile_mutex);

    for (jsize index = 0; index < argc; ++index) free(argv[index]);
    free(argv); free(log_path); free(temp_dir); free(work_dir);
    return (jint)result;
}
