<h1 align="center">
  Nadia
</h1>

<p align="center">
  <img src="https://images.unsplash.com/photo-1532170579297-281918c8ae72?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=884&q=80"/>
</p>

<p align="center">
  <a href="https://www.npmjs.com/package/nadia"><img alt="Version" src="https://img.shields.io/npm/v/nadia"></a>
  <a href="https://www.npmjs.com/package/nadia"><img alt="Version" src="https://img.shields.io/npm/dt/nadia?color=orange"></a>
  <a href="LICENSE"><img alt="License" src="https://img.shields.io/badge/License-ISC-blue.svg"></a>
  <a href="https://twitter.com/utsmannn"><img alt="Twitter" src="https://img.shields.io/twitter/follow/utsmannn"></a>
  <a href="https://github.com/utsmannn"><img alt="Github" src="https://img.shields.io/github/followers/utsmannn?label=follow&style=social"></a>
  <h3 align="center">Android App Bundle Installer</h3>
</p>

---

# Install
### Npm required
https://nodejs.org/en/download/

### Install with npm command
Install globally
```
npm install -g nadia
```

Verify installation with
```
nadia -h
```

Result:
```
Usage: application [OPTIONS]

  Nadia
  Android App Bundle Installer (aab installer)
  v1.0.18

Options:
  -a, --aab PATH       Bundle aab file
  -k, --keystore PATH  Keystore of aab file
  -r, --replace        Enable replace, the application will be uninstall first
                       before install new version of aab
  -h, --help           Show this message and exit
```

# Usage
You need:
1. Connected android devices with debug mode on
2. *.aab file,
3. Keystore with *.keystore extensions
4. Keystore password, alias and password alias

For installation
```
nadia -a {aab} -k {keystore}
```

Example
```
nadia -a utsmanganteng.aab -k rahasia.keystore
```

---

# Technical information
Build with:
- Kotlin
- JDeploy, publishing java app to npm package (https://www.jdeploy.com/)
- Clikt, command line build library for kotlin (https://ajalt.github.io/clikt/)
- OkHttp, handle download required files (https://square.github.io/okhttp/)
- Zip4j, handle extracting downloaded zip (https://github.com/srikanth-lingala/zip4j)
- Gson, parse and create configuration file (https://github.com/google/gson)

---