//
// Created by beich on 2020/11/15.
//
#pragma once

#include <jni.h>
#include <link.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

#define HOOK_LIB_INIT_NAME "fake_load_library_init"

/*
 * 关于可变长度结构体规则,Hook模块声明调用以下参数后在fake-linker中被释放,
 * fake-linker模块返回可变长度结构体需要手动在Hook模块下释放
 * */

enum HookJniError {
    kHJErrorNO,
    // 方法索引错误,超出范围或不合法
    kHJErrorOffset,
    // 方法为空指针错误
    kHJErrorMethodNull,
    // 已经被Hook又再次调用相同Hook
    kHJErrorRepeatOperation,
    // 错误的执行
    kHJErrorExec
};

/*
 * @param function_offset 方法对应在JNINativeInterface中的偏移
 * @param hook_method hook替换方法指针
 * @param backup_method 备份原方法指针
 * */
//HookJniError HookJniNativeInterface(size_t function_offset, void *hook_method, void **backup_method);

struct HookJniUnit {
    size_t offset;
    void *hook_method;
    void **backup_method;
};

//int HookJniNativeInterfaces(HookJniUnit *items, size_t len);

enum ErrorCode {
    // 没有错误
    kErrorNo = 0,
    // 命名空间 null错误
    kErrorNpNull = -1,
    // 命名空间名字错误
    kErrorNpName = -1 << 1,
    // 没找到对应的命名空间
    kErrorNpNotFound = -1 << 2,
    // soinfo null错误
    kErrorSoinfoNull = -1 << 3,
    // 没找到对应的soinfo
    kErrorSoinfoNotFound = -1 << 4,
    // 没找到指定符号
    kErrorSymbolNotFoundInSoinfo = -1 << 5,
    // 重定位库失败
    kErrorSoinfoRelink = -1 << 6,
    // 该功能未定义
    kErrorFunctionUndefined = -1 << 10,
    // 该功能未实现
    kErrorFunctionNotImplemented = -1 << 11,
    // 不满足该功能的条件
    kErrorFunctionMeetCondition = -1 << 12,
    // Api等级不满足条件
    kErrorApiLevelNotMatch = -1 << 13,
    // 参数类型不匹配错误
    kErrorParameterType = -1 << 14,
    // 参数为null错误
    kErrorParameterNull = -1 << 15,
    // 内部执行错误
    kErrorExec = -1 << 16
};

enum SoinfoParamType {
    // 原始 soinfo指针,无需转换
    kSPOriginal,
    // 通过Android7.0及以上handle
    kSPHandle,
    // 通过地址查找所在的soinfo数据,如果为null则取caller的地址
    kSPAddress,
    // 通过名称查找
    kSPName,
    // 多个名称
    kSPNames,
    // 符号名称,一次只能一个
    kSPSymbol,
    // 空参数
    kSPNull
};

struct SoinfoAttribute {
    void *soinfo_original;
    const char *so_name;
    const char *real_path;
    ElfW(Addr) base;
    size_t size;
#if __ANDROID_API__ >= __ANDROID_API_N__
    void *handle;
#endif
};

enum SoinfoFunType {
    // 查询soinfo
    kSFInquire,
    // 获取soinfo的有关信息
    kSFInquireAttr,
    // Android 7.0以上返回Handle,否者返回原始soinfo,方便后续使用dlsym等方法
    kSFGetHandle,
    // 获取soinfo的名字
    kSFGetName,
    // 获取soinfo的真实路径
    kSFGetRealPath,
    // 获取linker的soinfo
    kSFGetLinker,
    /*
     * 真实dlopen函数的具体实现地址
     * 对应do_dlopen
     * 高版本函数原型 void *do_dlopen(const char *filename, int flags, const android_dlextinfo *extinfo, void*caller_addr);
     *
    */
    kSFGetDlopen,
    /*
     * 真实dlsym函数的具体实现地址
     * Android 7.0及以上对应 do_dlsym
     * 函数原型 bool do_dlsym(void* handle, const char* sym_name, const char* sym_ver, const void* caller_addr, void**symbol)
     * Android 7.0及以下是具体的dlsym地址
     * */
    kSFGetDlsym,
    // 获取linker中的符号,低版本linker符号表是代码生成的,原始导出表不包含这些符号
    // 且版本不同名称也不同,因此单独提供获取符号
    kSFGetLinkerSymbol,
    // 通过soinfo查询导入符号
    kSFGetImportSymbolAddress,
    // 通过soinfo查询导出符号, 不查询内部符号是因为绝大部分库都删除掉了内部符号库
    kSFGetExportSymbolAddress,
    // 调用dlsym方法,这里会自动添加命名空间限制
    kSFCallDlsym,
    // 调用dlopen方法
    kSFCallDlopen
};

//API_PUBLIC void *call_soinfo_function(SoinfoFunType fun_type, SoinfoParamType find_type, const void *find_param, SoinfoParamType param_type, const void *param, int *error_code);

enum CommonFunType {
    // 添加一个soinfo到全局库
    kCFAddSoinfoToGlobal,
    // 添加重定向过滤符号
    // 后续调用CF_MANUAL_RELINK/CF_MANUAL_RELINKS时这些符号不会被重定向,但是系统重定向不会受影响
    kCFAddRelinkFilterSymbols,
    // 添加单个符号过滤
    kCFAddRelinkFilterSymbol,
    // 删除过滤的指定符号,传入空则是清空
    kCFRemoveRelinkFilterSymbols,
    // 删除单个符号
    kCFRemoveRelinkFilterSymbol,
    // 调用手动重定向符号
    kCFCallManualRelink,
    // 调用手动重定向多个库,参数只能so名字
    kCFCallManualRelinks,
};

//API_PUBLIC void *call_common_function(CommonFunType fun_type, SoinfoParamType find_type, const void *find_param, SoinfoParamType param_type, const void *param, int *error_code);

#if __ANDROID_API__ >= __ANDROID_API_N__

enum NamespaceParamType {
    // 原始android_namespace_t指针,无需转换
    kNPOriginal,
    // 通过原始soinfo指针获取对应命名空间
    kNPSoinfo,
    // 通过 Android 7.0以上soinfo的Handle获取对应命名空间
    kNPSoinfoHandle,
    // 通过具体地址获取对应命名空间,若地址为null则获取caller的命名空间
    kNPAddress,
    // 通过命名空间的名字查找
    kNPNamespaceName,
    // 通过soinfo 名称查找soinfo,再继续其它
    kNPSoinfoName,
    // 路径参数
    kNPPath,
    // 链接命名空间指针
    kNPLinkedNamespace,
    // 空参数
    kNPNull
};

enum NamespaceFunType {
    // 通过 FindNamespaceType 类型查询对应的命名空间
    kNFInquire,
    // 查询当前存在的所有命名空间,此时忽略 FindNamespaceType
    kNFInquireAll,
    // 查询指定命名空间下所有的soinfo
    kNFInquireSoinfo,
    // 查询链接命名空间,如果命名空间为null,则查询(default)命名空间
    kNFInquireLinked,
    // 查找当前命名空间下所有全局soinfo
    kNFInquireGlobalSoinfos,
    // 添加全局soinfo到指定命名空间,如果Hook时机晚就会存在部分java层使用的命名空间不包含该全局so,因此需要手动调用
    kNFAddGlobalSoinfoToNamespace,
    // 通过 FindNamespaceType 获取对应命名空间,然后再将其添加到指定的命名空间,
    // 如果命名空间为null则添加到(default)命名空间
    kNFAddSoinfoToNamespace,
    // Android10及以上添加单个库名到白名单
    kNFAddSoinfoToWhiteList,
    // Android10及以上添加多个个库名到白名单
    kNFAddSoinfosToWhiteList,
    // 添加路径到预加载库路径
    kNFAddPathToLdLibraryPath,
    kNFAddPathsToLdLibraryPath,
    // 添加路径到默认库加载路径
    kNFAddPathToDefaultLibraryPath,
    kNFAddPathsToDefaultLibraryPath,
    // 添加路径到允许的路径
    kNFAddPathToPermittedPath,
    kNFAddPathsToPermittedPath,
    // 添加链接命名空间,注意传递的是对象而不是指针
    kNFAddLinkedNamespace,
    kNFAddLinkedNamespaces,
    // 向指定soinfo添加第二命名空间
    kNFAddSecondNamespace,
    // 改变soinfo的命名空间
    kNPChangeSoinfoNamespace
};

/*
 * 调用相关命名空间函数
 * @return 若返回值是多个集合,则都包装成VarLengthObject对象
 * */
//API_PUBLIC void *
//call_namespace_function(NamespaceFunType fun_type, NamespaceParamType find_type, const void *find_param, NamespaceParamType param_type, const void *param, int *error_code);

#endif

// 其它的一些接口
struct RemoteInvokeInterface {
    HookJniError (*HookJniNative)(size_t function_offset, void *hook_method, void **backup_method);

    int (*HookJniNatives)(HookJniUnit *items, size_t len);

    void *(*CallSoinfoFunction)(SoinfoFunType fun_type, SoinfoParamType find_type, const void *find_param,
                                SoinfoParamType param_type, const void *param, int *error_code);

    void *(*CallCommonFunction)(CommonFunType fun_type, SoinfoParamType find_type, const void *find_param,
                                SoinfoParamType param_type, const void *param, int *error_code);

#if __ANDROID_API__ >= __ANDROID_API_N__

    void *
    (*CallNamespaceFunction)(NamespaceFunType fun_type, NamespaceParamType find_type, const void *find_param, NamespaceParamType param_type, const void *param, int *error_code);

    /*android_namespace_t * */void *(*CallCreateNamespaceImpl)(const char *name, const char *ld_library_path, const char *default_library_path, uint64_t type,
                                                               const char *permitted_when_isolated_path, /* android_namespace_t * */ void *parent_namespace,
                                                               const void *caller_addr);

    void *(*CallDlopenImpl)(const char *filename, int flags, const /*android_dlextinfo*/void *extinfo, void *caller_addr);

    bool (*CallDlsymImpl)(void *handle, const char *symbol, const char *version, void *caller_addr, void **sym);

#else
    void* (*CallDlopenImpl)(const char* filename, int flag, /* const android_dlextinfo* */const void* extinfo);
    void* (*CallDlsymImpl)(void* __handle, const char* __symbol);
#endif
};

/*
 * 待加载库调用初始方法原型,传入dlopen和dlsym地址避免循环调用
 * @param env   JNI环境,供模块注册Java方法使用
 * @param fake_soinfo 当前模块的soinfo,提供Hook模块快速访问
 * @param interface 供Hook模块调用的接口
 * @param cache_path 目的是对一些重定向文件提供统一的缓存路径,该路径必须在当前App中可读写
 * @param config_path 针对Hook模块配置文件的目录,非必要参数,可以从Java层动态初始化参数
 * @param process_name 当前进程的进程名,避免native麻烦获取
 * */
extern void (*fake_load_library_init_ptr)(JNIEnv *env, void *fake_soinfo, const RemoteInvokeInterface *interface, const char *cache_path, const char *config_path,
                                          const char *process_name);
__END_DECLS