//
// Created by beich on 2020/11/17.
//

#pragma once



//
////C_API API_PUBLIC const char *strcasestr(const char *haystack, const char *needle)__RENAME(strcasestr) __attribute_pure__;
//int access(const char *pathname, int mode);
//
//int faccessat(int dirfd, const char *pathname, int mode, int flags);
//
//int stat(const char *path, struct stat *sb);
//
////strong_alias(stat, stat64)
//int fstat(int __fd, struct stat *__buf);
//
////strong_alias(fstat, fstat64)
//int fstatat(int __dir_fd, const char *__path, struct stat *__buf, int __flags);
//
//int fstatat64(int __dir_fd, const char *__path, struct stat64 *__buf, int __flags);
//
//int __fstatfs64(int, size_t, struct statfs *);
//
//int statfs(const char *path, struct statfs *result);
//
////strong_alias(statfs, statfs64)
//int lstat(const char *path, struct stat *sb);
//
////strong_alias(lstat, lstat64)
//int fstatfs(int __fd, struct statfs *__buf);
//
////strong_alias(fstatfs, fstatfs64)
//int statvfs(const char *__path, struct statvfs *__result);
//
////strong_alias(statvfs, statvfs64)
//int utimes(const char *path, const timeval tv[2]);
//
//int lutimes(const char *path, const timeval tv[2]);
//
//int futimes(int fd, const timeval tv[2]);
//
//int futimesat(int fd, const char *path, const timeval tv[2]);
//
//int utimensat(int __dir_fd, const char *__path, const struct timespec __times[2], int __flags);
//
//int futimens(int __dir_fd, const struct timespec __times[2]) __INTRODUCED_IN(19);
//
//int link(const char *old_path, const char *new_path);
//
//int linkat(int __old_dir_fd, const char *__old_path, int __new_dir_fd, const char *__new_path, int __flags);
//
//ssize_t readlink(const char *path, char *buf, size_t size);
//
//ssize_t __readlinkat_chk(int dirfd, const char *path, char *buf, size_t size, size_t buf_size);
//
//ssize_t readlinkat(int __dir_fd, const char *__path, char *__buf, size_t __buf_size);
//
//ssize_t __readlink_chk(const char *path, char *buf, size_t size, size_t buf_size);
//
//int symlink(const char *old_path, const char *new_path);
//
//int symlinkat(const char *__old_path, int __new_dir_fd, const char *__new_path);
//
//int unlink(const char *path);
//
//int unlinkat(int __dirfd, const char *__path, int __flags);
//
//#if defined(__USE_FILE_OFFSET64)
//int truncate(const char* __path, off_t __length) __RENAME(truncate64) __INTRODUCED_IN(21);
//int ftruncate(int __fd, off_t __length) __RENAME(ftruncate64);
//#else
//
//int truncate(const char *__path, off_t __length);
//
//int ftruncate(int __fd, off_t __length);
//
//#endif
//
//int truncate64(const char *__path, off64_t __length) __INTRODUCED_IN(21);
//
//int ftruncate64(int __fd, off64_t __length);
//
//char *getcwd(char *buf, size_t size);
//
//int __getcwd(char *buf, size_t size);
//
//char *__getcwd_chk(char *buf, size_t len, size_t actual_size);
//
//int execve(const char *__file, char *const *__argv, char *const *__envp);
//
//int fexecve(int __fd, char *const *__argv, char *const *__envp) __INTRODUCED_IN(28);

