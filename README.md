# Cline for IntelliJ Test
unofficial reimplementation of the [cline](https://github.com/cline/cline) for IntelliJ IDEA

## setup
```bash
git submodule init
cd cline
git checkout -b version-1.0.4 v1.0.4
npm run install:all

cd ../
npm install
node esbuild.js
```

## known issues
- no support for markdown
- cannot "start new task"
- not change project path when changing project