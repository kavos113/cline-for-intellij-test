const esbuild = require("esbuild")
const fs = require("fs")
const path = require("path")

const copyWasmFiles = {
    name: "copy-wasm-files",
    setup(build) {
        build.onEnd(() => {
            const sourceDir = path.join(__dirname, "cline", "node_modules", "web-tree-sitter")
            const targetDir = path.join(__dirname, "build", "resources", "main", "analyze-project")
            
            // Copy tree-sitter.wasm
            fs.copyFileSync(path.join(sourceDir, "tree-sitter.wasm"), path.join(targetDir, "tree-sitter.wasm"))
            
            // Copy language-specific WASM files
            const languageWasmDir = path.join(__dirname, "cline", "node_modules", "tree-sitter-wasms", "out")
            const languages = [
                "typescript",
                "tsx",
                "python",
                "rust",
                "javascript",
                "go",
                "cpp",
                "c",
                "c_sharp",
                "ruby",
                "java",
                "swift",
            ]
            
            languages.forEach((lang) => {
                const filename = `tree-sitter-${lang}.wasm`
                fs.copyFileSync(path.join(languageWasmDir, filename), path.join(targetDir, filename))
            })
        })
    },
}

const extensionConfig = {
    bundle: true,
    minify: false,
    sourcemap: true,
    logLevel: "silent",
    plugins: [
        copyWasmFiles
    ],
    entryPoints: ["cline/src/analyze-project/index.ts"],
    format: "cjs",
    sourcesContent: false,
    platform: "node",
    outfile: "build/resources/main/analyze-project/index.js",
    external: ["vscode"],
}

async function main() {
    const extensionCtx = await esbuild.context(extensionConfig)
    await extensionCtx.rebuild()
    await extensionCtx.dispose()
}

main().catch((e) => {
    console.error(e)
    process.exit(1)
})
