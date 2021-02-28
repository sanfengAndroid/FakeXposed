//
// Created by beich on 2020/11/18.
//

#pragma once

#include <map>
#include <string>
#include <macros.h>

enum MapsMode {
    // 不操作
    kMapsNone,
    // 增加行
    kMapsAdd,
    // 删除行
    kMapsRemove,
    // 修改行
    kMapsModify
};

enum IoMask {
    /*
     * 未找到
     * */
    kIMNotFound = -2,
    /*
     * 匹配对应权限
     * */
    kIMMatch = 0,
    /*
     * 不匹配对应权限
     * */
    kIMMismatch = -1
};

struct FileAccess {
    uid_t uid;
    gid_t gid;
    int access;
};

class IoRedirect {
public:

    IoRedirect() {}

    static bool AddRedirectFile(const char *src_path, const char *redirect_path);

    static void DeleteRedirectFile(const char *src_file);

    /*
     * 重定向路径只重定向一级路径,二级路径需要再次调用添加,目录结尾不包含 '/'
     * 例:
     *      /data/local/tmp -> /data/local/tmp2
     *      /data/local/tmp/fake -> /data/local/tmp2/fake
     *
     * @param src_dir       原文件目录
     * @param redirect_dir  重定向后的目录
     * */
    static bool AddRedirectDirectory(const char *src_dir, const char *redirect_dir);

    static bool AddRedirect(const char *src_path, const char *redirect_path, bool is_dir);

    static void SetPid(pid_t pid);

    static const char *RedirectMaps(const char *path);

    /*
     * 设置文件/目录的访问权限
     * 如果该文件是重定向文件则应该传入重定向后的路径
     * */
    static bool AddFileAccess(const char *path, int uid, int gid,int port);

    static void RemoveFileAccess(const char *path);

    /*
     * 获取文件/目录的访问权限
     * 如果该文件是重定向文件则应该传入重定向后的路径
     * @param return -1则未找到
     * */
    static const FileAccess *GetFileAccess(const char *path);

    static IoMask GetFileMask(const char *path, int port);

    static void DeleteRedirectDirectory(const char *src_dir);

    static const char *GetRedirectFile(const char *src_path);

    static const char *GetRedirectDirectory(const char *src_dir);

    static const char *GetRedirect(const char *src_path);

    /*
     * 反向查找,通过重定向文件查找原始文件
     * */
    static const char *RedirectToSourceFile(const char *redirect_file_path);

    /*
   * 反向查找,通过重定向目录查找原始目录
   * */
    static const char *RedirectToSourceDirectory(const char *redirect_dir);

    static const char *RedirectToSource(const char *path);

    /*
     * 根据 fd查找重定向路径
     * @param return 已经被重定向则返回重定向路径,否者返回null
     * */
    static const char *GetRedirectFd(int src_fd);

    static bool ReadFilePathByFd(int fd, char *buf);

    static const char *RedirectFdToSourceFile(int redirect_file);

    // 高版本写/data/local/tmp目录失败,需要传入一个可写的缓存目录,通常/data/data/package/name/cache
    const char *RedirectSelfMaps(const char *cache_path);

    static int CreateTempFile(const char *cache_path);

    static int CreateTempFile();

    int CreateTempMapsFile(const char *cache_path);

private:
    void RedirectMapsImpl(const int fd, const int fake_fd);

    char *MatchMapsItem(char *line, MapsMode &mode);

    static const char *GetDirRedirectFile(const char *src_path);

private:
    static std::map<std::string, std::string> redirect_files_;

    static std::map<std::string, std::string> redirect_file_reverses_;

    static std::map<std::string, std::string> redirect_dirs_;
    static std::map<std::string, std::string> redirect_dir_reverses_;

    static std::map<std::string, std::string> redirect_dir_files_;
    static std::map<std::string, std::string> redirect_dir_file_reverses_;

    static std::map<std::string, FileAccess> file_access_;

    static int pid_;
    static uid_t uid_;
    static const char *proc_maps_[2];

    char redirect_maps_path_[1024];
    char replace_string_[PATH_MAX];
    DISALLOW_COPY_AND_ASSIGN(IoRedirect);
};