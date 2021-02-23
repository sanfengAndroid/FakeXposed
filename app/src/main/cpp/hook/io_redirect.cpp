//
// Created by beich on 2020/11/18.
//

#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/stat.h>
#include <linux/fcntl.h>
#include <syscall.h>
#include <errno.h>
#include <limits.h>
#include <map>
#include <fcntl.h>
#include <libgen.h>
#include "io_redirect.h"
#include "../common/alog.h"
#include "hook_main.h"
#include "canonicalize.h"

#include "hook_syscall.h"
#include "hook_stat.h"

std::map<std::string, std::string> IoRedirect::redirect_files_;
std::map<std::string, std::string> IoRedirect::redirect_file_reverses_;
std::map<std::string, std::string> IoRedirect::redirect_dirs_;
std::map<std::string, std::string> IoRedirect::redirect_dir_reverses_;
std::map<std::string, std::string> IoRedirect::redirect_dir_files_;
std::map<std::string, std::string> IoRedirect::redirect_dir_file_reverses_;
std::map<std::string, FileAccess> IoRedirect::file_access_;
int IoRedirect::pid_;
uid_t IoRedirect::uid_;
const char *IoRedirect::proc_maps_[2];


extern std::map<std::string, int> maps_rules;

/*
 * @param src 保证足够长度
 * */
static bool ReplaceString(char *src, const char *sub, const char *dst) {
    char *p;
    size_t sub_len;
    size_t dst_len;
    size_t src_len;

    p = strstr(src, sub);
    if (p == nullptr) {
        return false;
    }
    src_len = strlen(src);
    sub_len = strlen(sub);
    dst_len = strlen(dst);
    memmove(p + dst_len, p + sub_len, src_len - (p - src) - sub_len);
    memcpy(p, dst, dst_len);
    src[src_len - sub_len + dst_len] = '\0';
    return true;
}

bool IoRedirect::AddRedirectFile(const char *src_path, const char *redirect_path) {
    return AddRedirect(src_path, redirect_path, false);
}

bool IoRedirect::AddRedirectDirectory(const char *src_dir, const char *redirect_dir) {
    return AddRedirect(src_dir, redirect_dir, true);
}

bool IoRedirect::AddRedirect(const char *src_path, const char *redirect_path, bool is_dir) {
    if (src_path == nullptr || redirect_path == nullptr) {
        return false;
    }
    char buf[PATH_MAX];
    if (canonicalize(src_path, buf, PATH_MAX)) {
        LOGE("Get canonicalize path failed, source path: %s, error: %d", src_path, errno);
        return false;
    }
    std::map<std::string, std::string> *map = is_dir ? &redirect_dirs_ : &redirect_files_;
    std::map<std::string, std::string> *reverse_map = is_dir ? &redirect_dir_reverses_ : &redirect_file_reverses_;
    (*map)[buf] = redirect_path;
    (*reverse_map)[redirect_path] = buf;
    return true;
}

void IoRedirect::SetPid(pid_t pid) {
    proc_maps_[0] = "/proc/self/maps";
    if (proc_maps_[1] != nullptr) {
        free((void *) proc_maps_[1]);
    }
    asprintf(const_cast<char **>(&proc_maps_[1]), "/proc/%d/maps", pid);
    uid_ = getuid();
}


const char *IoRedirect::RedirectMaps(const char *path) {
    if (!strcmp(proc_maps_[0], path) || !strcmp(proc_maps_[1], path)) {
        LOGW("Fake: Opening the maps file, ready to redirect.");
        IoRedirect redirect;
        const char *fake_path = redirect.RedirectSelfMaps(cache_dir);
        if (fake_path != nullptr) {
            return fake_path;
        }
    }
    return nullptr;
}


bool IoRedirect::AddFileAccess(const char *path, int uid, int gid, int port) {
    if (path == nullptr) {
        return false;
    }
    char buf[PATH_MAX];
    if (canonicalize(path, buf, PATH_MAX)) {
        LOGE("Get canonicalize path failed, source path: %s, error: %d", path, errno);
        return false;
    }
    FileAccess file_access;
    struct stat stat_buf;

    if (get_orig_stat()(buf, &stat_buf) != 0) {
        return false;
    }
    file_access.uid = uid == -1 ? stat_buf.st_uid : uid;
    file_access.gid = gid == -1 ? stat_buf.st_gid : gid;
    file_access.access = port;
    file_access_[path] = file_access;
    return true;
}

void IoRedirect::RemoveFileAccess(const char *path) {
    if (path == nullptr) {
        return;
    }
    char buf[PATH_MAX];
    if (canonicalize(path, buf, PATH_MAX)) {
        LOGE("Get canonicalize path failed, source path: %s, error: %d", path, errno);
        return;
    }
    file_access_.erase(buf);
}


const FileAccess *IoRedirect::GetFileAccess(const char *path) {
    FileAccess *result = nullptr;
    if (path == nullptr) {
        return result;
    }
    char buf[PATH_MAX];
    if (canonicalize(path, buf, PATH_MAX)) {
        return result;
    }
    auto iter = file_access_.find(path);
    if (iter == file_access_.end()) {
        return result;
    }
    return &iter->second;
}

IoMask IoRedirect::GetFileMask(const char *path, int port) {
    IoMask result = kIMNotFound;
    if (path == nullptr) {
        return result;
    }
    char buf[PATH_MAX];
    if (canonicalize(path, buf, PATH_MAX)) {
        return result;
    }
    auto iter = file_access_.find(path);
    if (iter == file_access_.end()) {
        return result;
    }
    if (uid_ == iter->second.uid) {
        result = ((port << 8) & iter->second.access) != 0 ? kIMMatch : kIMMismatch;
    } else if (uid_ == iter->second.gid) {
        result = ((port << 4) & iter->second.access) != 0 ? kIMMatch : kIMMismatch;
    } else {
        result = (port & iter->second.access) != 0 ? kIMMatch : kIMMismatch;
    }
    LOGD("modify file port, orig: %d, match: %d", port, result);
    return result;
}

void IoRedirect::DeleteRedirectFile(const char *src_file) {
    if (src_file == nullptr) {
        return;
    }
    char buf[PATH_MAX];
    if (canonicalize(src_file, buf, PATH_MAX)) {
        return;
    }
    auto iter = redirect_files_.find(src_file);
    if (iter != redirect_files_.end()) {
        redirect_file_reverses_.erase(iter->second);
        redirect_files_.erase(iter);
    }
}


void IoRedirect::DeleteRedirectDirectory(const char *src_dir) {
    if (src_dir == nullptr) {
        return;
    }
    char buf[PATH_MAX];
    if (canonicalize(src_dir, buf, PATH_MAX)) {
        return;
    }
    auto iter = redirect_dirs_.find(buf);
    if (iter != redirect_dirs_.end()) {
        redirect_dir_reverses_.erase(iter->second);
        redirect_dirs_.erase(iter);
        // 这里不查找已经使用的文件,直接全部清空,下次使用时再生成即可
        redirect_dir_files_.clear();
        redirect_dir_file_reverses_.clear();
    }
}


const char *IoRedirect::GetRedirectFile(const char *src_path) {
    if (src_path == nullptr) {
        return src_path;
    }
    char buf[PATH_MAX];
    if (canonicalize(src_path, buf, PATH_MAX)) {
        LOGE("Get canonicalize path failed, source path: %s, error: %d", src_path, errno);
        return src_path;
    }
    auto iter = redirect_files_.find(buf);
    if (iter != redirect_files_.end()) {
        LOGD("redirect file path orig: %s, new path: %s", src_path, iter->second.c_str());
        return iter->second.c_str();
    }
    // 还要判断是否在重定向目录
    const char *result = GetDirRedirectFile(buf);
    if (result != nullptr) {
        return src_path;
    }
    const char *dir = dirname(buf);
    if (strcmp(dir, ".") == 0 || strcmp(dir, "..") == 0) {
        return src_path;
    }
    dir = GetRedirectDirectory(dir);
    if (dir == nullptr) {
        return src_path;
    }
    const char *file = basename(buf);
    char redirect_path[PATH_MAX];
    strlcpy(redirect_path, dir, PATH_MAX);
    strlcat(redirect_path, "/", PATH_MAX);
    strlcat(redirect_path, file, PATH_MAX);
    redirect_dir_files_[buf] = redirect_path;
    redirect_dir_file_reverses_[redirect_path] = buf;
    LOGD("redirect new dir file path orig: %s, new path: %s", src_path, src_path);
    return redirect_dir_files_[buf].c_str();
}

const char *IoRedirect::GetRedirectDirectory(const char *src_dir) {
    if (src_dir == nullptr) {
        return src_dir;
    }
    char buf[PATH_MAX];
    if (canonicalize(src_dir, buf, PATH_MAX)) {
        LOGE("Get canonicalize path failed, source path: %s, error: %d", src_dir, errno);
        return src_dir;
    }
    auto iter = redirect_dirs_.find(buf);
    if (iter == redirect_dirs_.end()) {
        return src_dir;
    }
    return iter->second.c_str();
}

const char *IoRedirect::GetRedirect(const char *src_path) {
    if (src_path == nullptr) {
        return nullptr;
    }
    const char *result = GetRedirectFile(src_path);
    if (result != src_path) {
        return result;
    }
    result = GetRedirectDirectory(src_path);
    return result != src_path ? result : src_path;
}

const char *IoRedirect::GetDirRedirectFile(const char *src_path) {
    auto iter = redirect_dir_files_.find(src_path);
    if (iter != redirect_dir_files_.end()) {
        LOGD("redirect dir file path orig: %s, new path: %s", src_path, src_path);
        return iter->second.c_str();
    }
    return src_path;
}

const char *IoRedirect::RedirectToSourceFile(const char *redirect_file_path) {
    if (redirect_file_path == nullptr) {
        return redirect_file_path;
    }
    auto iter = redirect_file_reverses_.find(redirect_file_path);
    if (iter != redirect_file_reverses_.end()) {
        return iter->second.c_str();
    }
    auto iter2 = redirect_dir_file_reverses_.find(redirect_file_path);
    if (iter2 != redirect_dir_file_reverses_.end()) {
        return iter2->second.c_str();
    }
    // 当文件没有通过GetRedirectFile方式获取时,还需要再判断路径
    const char *src_dir = dirname(redirect_file_path);
    const char *redirect_dir = RedirectToSourceDirectory(src_dir);
    if (src_dir == redirect_dir || redirect_dir == nullptr) {
        return redirect_file_path;
    }
    char buf[PATH_MAX];
    const char *name = basename(redirect_file_path);
    strlcpy(buf, redirect_dir, PATH_MAX);
    strlcat(buf, "/", PATH_MAX);
    strlcat(buf, name, PATH_MAX);
    redirect_dir_files_[buf] = redirect_file_path;
    redirect_dir_file_reverses_[redirect_file_path] = buf;
    return redirect_dir_file_reverses_[redirect_file_path].c_str();
}

const char *IoRedirect::GetRedirectFd(int src_fd) {
    if (src_fd < 0) {
        return nullptr;
    }
    char path[PATH_MAX];
    if (!ReadFilePathByFd(src_fd, path)) {
        return nullptr;
    }
    if (path[0] != '/') {
        // 非标准文件
        return nullptr;
    }
    const char *result = GetRedirectFile(path);
    return result != path ? result : nullptr;
}

bool IoRedirect::ReadFilePathByFd(int fd, char *buf) {
    char path[35];
    sprintf(path, "/proc/self/fd/%d", fd);
    ssize_t size = readlink(path, buf, PATH_MAX);
    if (size < 1) {
        return false;
    }
    buf[size] = '\0';
    return true;
}

const char *IoRedirect::RedirectFdToSourceFile(int redirect_file) {
    if (redirect_file < 0) {
        return nullptr;
    }
    char buf[35];
    snprintf(buf, 35, "/proc/self/fd/%d", redirect_file);
    char path[PATH_MAX];
    size_t len = readlink(buf, path, PATH_MAX);
    if (len < 1) {
        return nullptr;
    }
    path[len] = '\0';
    if (path[0] != '/') {
        // 非标准文件
        return nullptr;
    }
    const char *result = RedirectToSourceFile(path);
    return result != path ? result : nullptr;
}


const char *IoRedirect::RedirectToSourceDirectory(const char *redirect_dir) {
    if (redirect_dir == nullptr) {
        return redirect_dir;
    }
    auto iter = redirect_dir_reverses_.find(redirect_dir);
    if (iter == redirect_file_reverses_.end()) {
        return redirect_dir;
    }
    return iter->second.c_str();
}

const char *IoRedirect::RedirectToSource(const char *path) {
    if (path == nullptr) {
        return nullptr;
    }
    const char *result = RedirectToSourceFile(path);
    if (result != path) {
        return result;
    }
    result = RedirectToSourceDirectory(path);
    return result != path ? result : path;
}


int IoRedirect::CreateTempFile(const char *cache_path) {
    int fd = -1;
    char *path = nullptr;
    if (asprintf(&path, "%s/tmp.XXXXXXXXXX", cache_path) == -1) {
        return -1;
    }
    fd = mkstemp(path);

    if (fd == -1) {
        fd = open(path, O_RDWR | O_CLOEXEC | O_TMPFILE | O_EXCL, nullptr);
        if (fd != -1) {
            goto __end;
        }
        fd = open(path, O_CREAT | O_RDWR | O_TRUNC | O_CLOEXEC, nullptr);
        if (fd == -1) {
            LOGE("create temp file error, path: %s", path);
        }
    }
    __end:
    unlink(path);
    free(path);
    return fd;
}

int IoRedirect::CreateTempMapsFile(const char *cache_path) {
    if (sprintf(redirect_maps_path_, "%s/maps_%d", cache_path, getpid()) == -1) {
        return -1;
    }
    int fd = get_orig_syscall()(__NR_openat, AT_FDCWD, redirect_maps_path_, force_O_LARGEFILE(O_CREAT | O_RDWR | O_TRUNC | O_CLOEXEC), S_IRUSR | S_IWUSR);
    if (fd == -1) {
        LOGE("create temp file error, path: %s", redirect_maps_path_);
    }
    return fd;
}

char *IoRedirect::MatchMapsItem(char *line, MapsMode &mode) {
    char *key;
    const char *word;
    bool copy = false;
    char names[1024];
    mode = kMapsNone;
    if (maps_rules.empty() || strlen(line) < 1) {
        return nullptr;
    }
    for (auto &maps_rule : maps_rules) {
        word = maps_rule.first.c_str();
        switch (maps_rule.second) {
            case kMapsNone:
                break;
            case kMapsAdd:
                LOGD("[maps] Add line not in use.");
                break;
            case kMapsRemove:
                if (strstr(line, word) != nullptr) {
                    mode = kMapsRemove;
                    LOGD("[maps] delete line: %s", line);
                    return nullptr;
                }
                break;
            case kMapsModify:
                if (!copy) {
                    strcpy(replace_string_, line);
                    copy = true;
                }
                // 将其更改为2个字符串
                strcpy(names, word);
                key = strchr(names, '?');
                if (key == nullptr) {
                    break;
                }
                key[0] = '\0';
                key++;
                if (ReplaceString(replace_string_, names, key)) {
                    LOGD("[maps] replace str:  %s,replaced: %s", word, key);
                    mode = kMapsModify;
                }
                break;
            default:
                break;
        }
    }
    if (mode == kMapsModify) {
        return replace_string_;
    }
    return nullptr;
}

void IoRedirect::RedirectMapsImpl(const int fd, const int fake_fd) {
    char data[PATH_MAX];
    char *p = data, *e;
    size_t n = PATH_MAX - 1;
    ssize_t r;
    MapsMode mode = kMapsNone;
    size_t len;

    while ((r = TEMP_FAILURE_RETRY(read(fd, p, n))) > 0) {
        p[r] = '\0';
        p = data;

        while ((e = strchr(p, '\n')) != nullptr) {
            e[0] = '\0';

            char *path = MatchMapsItem(p, mode);
            switch (mode) {
                case kMapsAdd:
                    len = strlen(path);
                    write(fake_fd, path, len);
                    write(fake_fd, "\n", 1);
                    e[0] = '\n';
                    write(fake_fd, p, e - p + 1);
                    break;
                case kMapsRemove:
                    break;
                case kMapsModify:
                    len = strlen(path);
                    write(fake_fd, path, len);
                    write(fake_fd, "\n", 1);
                    break;
                case kMapsNone:
                    e[0] = '\n';
                    write(fake_fd, p, e - p + 1);
                    break;
            }
            p = e + 1;
        }
        if (p == data) { // !any_entry
            LOGE("fake_maps: cannot process line larger than %u bytes!", PATH_MAX);
            goto __break;
        } //if

        const size_t remain = strlen(p);
        if (remain <= (PATH_MAX / 2)) {
            memcpy(data, p, remain * sizeof(p[0]));
        } else {
            memmove(data, p, remain * sizeof(p[0]));
        } //if

        p = data + remain;
        n = PATH_MAX - 1 - remain;
    }

    __break:
    return;
}


const char *IoRedirect::RedirectSelfMaps(const char *cache_path) {
    // 调用系统call,避免触发死循环
    int maps_fd = get_orig_syscall()(__NR_openat, AT_FDCWD, "/proc/self/maps", force_O_LARGEFILE(O_RDONLY), 0);
    if (maps_fd == -1) {
        errno = EACCES;
        return nullptr;
    }
    int fake_fd = CreateTempMapsFile(cache_path);
    if (fake_fd == -1) {
        return nullptr;
    }
    RedirectMapsImpl(maps_fd, fake_fd);
    close(fake_fd);
    return redirect_maps_path_;
}