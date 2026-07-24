const readline = require('readline');
const { tagSentence } = require('ilo-nasin');

// look for --help command line parameter
for (let arg of process.argv) {
  if (arg == "--help") { // print help and exit
    console.error("Command line interface to Toki Pona POS tagger 'ilo-nasin'.");
    console.error("Each line supplied to stdin is parsed by ilo-nasin");
    console.error("resulting in one JSON object (per line) output to stdout.");
    console.error("e.g.:");
    console.error("echo \"mi wile e tenpo tan wile mi pona\" | node index.js");
    process.exit();
  }
} // next
const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout,
  terminal: false
});

let lineCount = 0;

rl.on('line', (line) => {
  lineCount++;
  const parse = tagSentence(line);
  console.log(JSON.stringify(parse));
});

rl.once('close', () => {
});
