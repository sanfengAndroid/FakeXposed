# FakeXposed Changelog

### v1.1

- Fix the error of creating ContextImpl in low version, which caused the subsequent failure to continue
- Fix the problem of circular dependency recursive calls when libcutil.so below Android7 looks for attributes
- Fix Java Hook System class issues below Android7
- Fix maps rules not taking effect
- Fix environment variables not taking effect
- Fix the redirection path reverse reading problem
- Fix missing syscall open call number
- Change the new Runtime.exec matching rules and take effect
- Add Android 25 support