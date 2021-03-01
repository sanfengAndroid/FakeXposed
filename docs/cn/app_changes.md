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

### v1.3
- 修改数据读取模式，调整targetSdk为23，旧版本需要卸载安装，Android9以下直接使用XsharedPreference
- 更改部分Java Hook为Native Java Hook，更加通用稳定和防检测
- 默认添加Edxposed包名到黑名单，解决自身无法访问时其它应用可以访问
- 部分代码优化

### v1.4
- 修改包名和应用介绍
- 添加测试选项