const tree = require("./analyze-project")

if (process.argv.length !== 3) {
    console.error("Usage: node tree.js <absolutepath>")
    process.exit(1)
}
tree.analyzeProject(process.argv[2]).then(result => {
    console.log(result)
});