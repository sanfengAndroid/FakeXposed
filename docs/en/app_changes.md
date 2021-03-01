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

### v1.3
- Modify the data reading mode, adjust the targetSdk to 23, the old version needs to be uninstalled and installed, and use XsharedPreference directly below Android9
- Change some Java Hooks to Native Java Hooks, which are more general and stable and anti-detection
- Add the Edxposed package name to the blacklist by default, so that other applications can access it when you can’t access it
- Partial code optimization

### v1.4
- Modify package name and application introduction
- Add test options