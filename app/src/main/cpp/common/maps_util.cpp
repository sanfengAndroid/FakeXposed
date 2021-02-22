//
// Created by beich on 2020/11/12.
//

#include <cstring>
#include <cstdlib>
#include <sys/mman.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <cinttypes>
#include <ctime>
#include "maps_util.h"
#include "alog.h"
#include "macros.h"

#define MAPS_PATH "/proc/self/maps"


/*
 * maps文件无法通过 ftell, stat方法获取大小,因此无法使用mmap方式
 * */

inline bool MapsUtil::OpenMaps() {
    if (maps_fd_ != nullptr) {
        fseeko(maps_fd_, 0, SEEK_SET);
        return true;
    }
    maps_fd_ = fopen(MAPS_PATH, "re");
    return (maps_fd_ != nullptr);
}

void MapsUtil::MakeLibraryName(const char *library_name) {
    CHECK(library_name != nullptr);
    if (library_name[0] == '/') {
        strcpy(library_name_, library_name);
    } else {
        library_name_[0] = '/';
        strcpy(&library_name_[1], library_name);
    }
}

bool MapsUtil::MatchPath() {
    char *p = strstr(path_, library_name_);
    return p != nullptr && strlen(p) == strlen(library_name_);
}

void MapsUtil::PageProtectFree() {
    PageProtectFree(page);
    page = nullptr;
}

void MapsUtil::PageProtectFree(PageProtect *protect) {
    if (protect == nullptr || protect == (void *) 1) {
        return;
    }
    PageProtect *next = protect->next;
    delete protect;
    protect = next;
    PageProtectFree(protect);
}

bool MapsUtil::UnlockPageProtect() const {
    if (page == nullptr) {
        return false;
    }
    if (page == (void *) 1) {
        return true;
    }
    PageProtect *page_ = page;
    while (page_ != nullptr && page_->start != 0) {
        if ((page_->old_protect & PROT_WRITE) == 0) {
            page_->new_protect = page_->old_protect | PROT_WRITE | PROT_READ;
            if (mprotect(reinterpret_cast<void *>(page_->start), page_->end - page_->start, page_->new_protect) < 0) {
                return false;
            }
        } else {
            page_->new_protect = page_->old_protect;
        }
        page_ = page_->next;
    }
    return true;
}

bool MapsUtil::RecoveryPageProtect() const {
    return RecoveryPageProtect(page);
}

bool MapsUtil::RecoveryPageProtect(PageProtect *protect) {
    if (protect == nullptr) {
        return false;
    }
    if (protect == (void *) 1) {
        return true;
    }
    PageProtect *page_ = protect;
    while (page_ != nullptr && page_->start != 0) {
        if (page_->old_protect != page_->new_protect) {
            if (mprotect(reinterpret_cast<void *>(page_->start), page_->end - page_->start, page_->old_protect) < 0) {
                return false;
            }
        }
        page_ = page_->next;
    }
    return true;
}

PageProtect *MapsUtil::CopyProtect() {
    copy_ = true;
    return page;
}

/*
 * 查找模块基准忽略内存权限匹配,有些情况并不是r-xp在最前面
 * */
gaddress MapsUtil::FindLibraryBase(const char *library_name, const char *mode) {
    gaddress result = 0;
    if (!OpenMaps()) {
        return result;
    }
    MakeLibraryName(library_name);
    while (GetMapsLine()) {
        if (!FormatLine()) {
            continue;
        }
        if (!strstr(protect_, mode)) {
            continue;
        }
        if (path_[0] == '[' || !MatchPath()) {
            continue;
        }
        result = start_address_;
        break;
    }
    return result;
}

bool MapsUtil::UnlockAddressProtect(void *address) {
    if (!GetMemoryProtect(address)) {
        return false;
    }
    if ((page->old_protect & PROT_WRITE) != 0) {
        page->new_protect = page->old_protect;
        return true;
    }
    page->new_protect = page->old_protect | PROT_WRITE;
    bool success = mprotect(GSIZE_TO_POINTER(page->start), page->end - page->start, page->new_protect) == 0;
    if (!success) {
        PageProtectFree();
        return false;
    }
    // 查找内存页
    return true;
}

char *MapsUtil::GetLibraryRealPath(const char *library_name) {
    char *result = nullptr;
    if (!OpenMaps()) {
        return result;
    }
    MakeLibraryName(library_name);
    while (GetMapsLine()) {
        if (!FormatLine()) {
            continue;
        }
        if (path_[0] == '[' || !MatchPath()) {
            continue;
        }
        result = path_;
        break;
    }
    if (result) {
        return strdup(path_);
    }
    return result;
}

MapsUtil::MapsUtil() {
}

MapsUtil::MapsUtil(const char *library_name) {
    GetLibraryProtect(library_name);
}

MapsUtil::~MapsUtil() {
    CloseMaps();
    if (!copy_) {
        PageProtectFree();
    }
}

bool MapsUtil::GetMemoryProtect(void *address) {
    if (!OpenMaps()) {
        return false;
    }
    page = new PageProtect();
    auto target = reinterpret_cast<gaddress>(address);
    while (GetMapsLine()) {
        if (!FormatLine()) {
            continue;
        }
        if (target >= start_address_ && target <= end_address_) {
            page->start = start_address_;
            page->end = end_address_;
            page->old_protect = FormatProtect();
            break;
        }
    }
    return CheckPage();
}

bool MapsUtil::GetLibraryProtect(const char *library_name) {
    if (!OpenMaps()) {
        return false;
    }
    MakeLibraryName(library_name);
    auto *page_start = new PageProtect();
    page = page_start;
    PageProtect *page_pos = page_start;
    while (GetMapsLine()) {
        if (!FormatLine() || !MatchPath()) {
            continue;
        }
        page_pos->start = start_address_;
        page_pos->end = end_address_;
        page_pos->old_protect = FormatProtect();
        page_pos->next = new PageProtect();
        page_pos = page_pos->next;
    }
    if (!CheckPage()) {
        return false;
    }
    delete page_pos->next;
    page_pos->next = nullptr;
    return true;
}

bool MapsUtil::GetMapsLine() {
    return fgets(line_, MAPS_LINE_LEG, maps_fd_) != nullptr;
}

bool MapsUtil::FormatLine() {
    return sscanf(line_, "%" SCNx64 "-%" SCNx64 " %s %*x %*s %*s %s", &start_address_, &end_address_, protect_, path_) == 4;
}

int MapsUtil::FormatProtect() {
    int port = 0;
    for (int i = 0; i < 7 && protect_[i] != '\0'; ++i) {
        switch (protect_[i]) {
            case 'r':
                port |= PROT_READ;
                break;
            case 'w':
                port |= PROT_WRITE;
                break;
            case 'x':
                port |= PROT_EXEC;
                break;
        }
    }
    return port;
};

void MapsUtil::CloseMaps() {
    if (maps_fd_ != nullptr) {
        fclose(maps_fd_);
        maps_fd_ = nullptr;
    }
}

bool MapsUtil::CheckPage() {
    if (page->start == 0 || page->end == 0) {
        LOGE("read memory protect failed, start: 0x%" PRIx64 ", end: 0x%" PRIx64, page->start, page->end);
        PageProtectFree();
        return false;
    }
    return true;
}