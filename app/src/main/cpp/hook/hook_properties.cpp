//
// Created by beich on 2021/2/19.
//

#include "hook_properties.h"
#include <sys/system_properties.h>
#include <map>
/*
 * Android 5.0 ~ 8.1 property_get   --> __system_property_get
 * Android 9.0 ~ 10.0 android::base::GetProperty    --> __system_property_find ,__system_property_read_callback
 * */

// 返回属性值长度,不存在则返回0
FUN_INTERCEPT HOOK_DEF(int, __system_property_get, const char *name, char *value) {
    int ret = get_orig___system_property_get()(name, value);
    LOGMV("name: %s, value: %s", name, value);
    if (ret < 1) {
        return ret;
    }
    if (PropertiesIsBlacklisted(name)) {
        value[0] = '\0';
        return 0;
    }
    const char *new_value = PropertiesReplace(name, value);
    if (new_value != nullptr) {
        LOGD("__system_property_get property place old value: %s, new value: %s", value, new_value);
        strcpy(value, new_value);
        ret = strlen(value);
    }
    return ret;
}

FUN_INTERCEPT HOOK_DEF(const prop_info *, __system_property_find, const char *name) {
    const prop_info *ret = get_orig___system_property_find()(name);
    // 修复Android 7及以下 libcutil 出现的循环依赖问题，会递归查找
#if __ANDROID_API__ > __ANDROID_API_O__
    LOGMV("name: %s, result: %p", name, ret);
#endif
    return ret != nullptr && PropertiesIsBlacklisted(name) ? nullptr : ret;
}

std::map<void *, void (*)(void *, const char *, const char *, uint32_t)> callbacks;

static void handle_system_property(void *cookie, const char *name, const char *value, uint32_t serial) {
    void (*callback)(void *, const char *, const char *, uint32_t) = callbacks[cookie];
    const char *new_value = PropertiesReplace(name, value);
    callback(cookie, name, new_value == nullptr ? value : new_value, serial);
}

FUN_INTERCEPT HOOK_DEF(void, __system_property_read_callback,
                       const prop_info *pi,
                       void (*callback)(void *__cookie, const char *__name, const char *__value, uint32_t __serial),
                       void *cookie) __INTRODUCED_IN(26) {
    LOGMV("prop_info: %p, cookie: %p", pi, cookie);
    if (cookie == nullptr) {
        get_orig___system_property_read_callback()(pi, callback, cookie);
        return;
    }
    callbacks[cookie] = callback;
    get_orig___system_property_read_callback()(pi, handle_system_property, cookie);
}
