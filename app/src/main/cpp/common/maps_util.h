//
// Created by beich on 2020/11/12.
//
#pragma once

#include "gtype.h"
#include "macros.h"

struct PageProtect {
    gaddress start;
    gaddress end;
    uint32_t old_protect;
    uint32_t new_protect;
    PageProtect *next;
};

class MapsUtil {
public:
    MapsUtil();

    ~MapsUtil();

    explicit MapsUtil(const char *library_name);

    bool GetMemoryProtect(void *address);

    bool GetLibraryProtect(const char *library_name);

    bool UnlockAddressProtect(void *address);

    gaddress FindLibraryBase(const char *library_name, const char *mode);

    char *GetLibraryRealPath(const char *library_name);

    bool UnlockPageProtect() const;

    bool RecoveryPageProtect() const;

    bool Found() const {
        return page != nullptr;
    }

    gaddress GetStartAddress() const{
        return start_address_;
    }

    PageProtect *CopyProtect();

    static bool RecoveryPageProtect(PageProtect *protect);

    static void PageProtectFree(PageProtect *protect);

private:
    inline bool GetMapsLine();

    inline bool FormatLine();

    inline int FormatProtect();

    inline bool MatchPath();

    inline bool OpenMaps();

    inline void CloseMaps();

    void MakeLibraryName(const char *library_name);

    bool CheckPage();

    inline void PageProtectFree();

private:
#define MAPS_LINE_LEG 1024
    char line_[MAPS_LINE_LEG]{};
    char path_[MAPS_LINE_LEG]{};
    FILE *maps_fd_ = nullptr;
    char library_name_[256]{};
    gaddress start_address_ = 0;
    gaddress end_address_ = 0;
    char protect_[7]{};
    bool copy_ = false;
public:
    PageProtect *page = nullptr;

    DISALLOW_COPY_AND_ASSIGN(MapsUtil);
};