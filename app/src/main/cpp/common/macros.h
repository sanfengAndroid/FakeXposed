//
// Created by beich on 2020/11/6.
//

#pragma once

#include <stdint.h>
#include <stdlib.h>
#include <sys/user.h>
#include "alog.h"


#define async_safe_fatal(...) \
  do { \
    LOGE(__VA_ARGS__);\
    abort(); \
  } while (0) \

#define CHECK(predicate) \
  do { \
    if (!(predicate)) { \
      async_safe_fatal("%s:%d: %s CHECK '" #predicate "' failed", \
          __FILE__, __LINE__, __FUNCTION__); \
    } \
  } while(0)\

#define CHECK_OUTPUT(predicate, ...)\
 do { \
    if (!(predicate)) { \
      LOGE(__VA_ARGS__);\
      async_safe_fatal("%s:%d: %s CHECK '" #predicate "' failed", \
          __FILE__, __LINE__, __FUNCTION__); \
    } \
  } while(0)\


#define strong_alias(name, aliasname) __strong_alias(name, aliasname)

#define weak_alias(name, aliasname) _weak_alias (name, aliasname)
#define _weak_alias(name, aliasname) \
extern __typeof (name) aliasname __attribute__ ((weak, alias(#name)));

// Returns the address of the page containing address 'x'.
#define PAGE_START(x) ((x) & PAGE_MASK)

// Returns the offset of address 'x' in its page.
#define PAGE_OFFSET(x) ((x) & ~PAGE_MASK)

// Returns the address of the next page after address 'x', unless 'x' is
// itself at the start of a page.
#define PAGE_END(x) PAGE_START((x) + (PAGE_SIZE-1))

#define DISALLOW_COPY_AND_ASSIGN(TypeName) \
  TypeName(const TypeName&) = delete;             \
  void operator=(const TypeName&) = delete

#define DISALLOW_IMPLICIT_CONSTRUCTORS(TypeName) \
  TypeName() = delete;                                  \
  DISALLOW_COPY_AND_ASSIGN(TypeName)

#define ROUND_UP_POWER_OF_2(value) \
  ((sizeof(value) == 8) \
    ? (1UL << (64 - __builtin_clzl(static_cast<unsigned long>(value)))) \
    : (1UL << (32 - __builtin_clz(static_cast<unsigned int>(value)))))

#if defined(__cplusplus)
#define __BEGIN_DECLS extern "C" {
#define __END_DECLS }
#else
#define __BEGIN_DECLS
#define __END_DECLS
#endif

#define ALIGN_CHECK(size, len) (((len) % (size)) == 0)

#define API_LOCAL __attribute__((visibility("hidden")))
#define API_PUBLIC __attribute__((visibility ("default")))

#define C_API extern "C"

static constexpr uintptr_t align_down(uintptr_t p, size_t align) {
    return p & ~(align - 1);
}

static constexpr uintptr_t align_up(uintptr_t p, size_t align) {
    return (p + align - 1) & ~(align - 1);
}

template<typename T>
static inline T *align_down(T *p, size_t align) {
    return reinterpret_cast<T *>(align_down(reinterpret_cast<uintptr_t>(p), align));
}

template<typename T>
static inline T *align_up(T *p, size_t align) {
    return reinterpret_cast<T *>(align_up(reinterpret_cast<uintptr_t>(p), align));
}

template<typename T>
static inline T *untag_address(T *p) {
#if defined(__aarch64__)
    return reinterpret_cast<T*>(reinterpret_cast<uintptr_t>(p) & ((1ULL << 56) - 1));
#else
    return p;
#endif
}
