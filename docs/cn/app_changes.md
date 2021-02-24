# FakeXposed 更新日志

### v1.1

- 修复低版本创建ContextImpl错误导致后面无法继续
- 修复Android7以下libcutil.so查找属性时导致循环依赖递归调用问题
- 修复Android7以下Java Hook System类问题
- 修复maps规则不生效
- 修复环境变量不生效问题
- 修复重定向路径反向读取问题
- 修复遗漏的syscall open调用号
- 更改新的Runtime.exec匹配规则并生效
- 添加Android 25支持