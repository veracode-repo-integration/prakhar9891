const http = require('http');

const TARGET_HOST = process.env.TARGET_HOST || 'localhost';
const TARGET_PORT = process.env.TARGET_PORT || 8080;
const CONCURRENCY = parseInt(process.env.CONCURRENCY || '10', 10);
const TOTAL_REQUESTS = parseInt(process.env.TOTAL_REQUESTS || '100', 10);

console.log(`=============================================`);
console.log(`⚡ POS API Load Testing Benchmark Tool`);
console.log(`Target: http://${TARGET_HOST}:${TARGET_PORT}/actuator/health`);
console.log(`Concurrency: ${CONCURRENCY} | Total Requests: ${TOTAL_REQUESTS}`);
console.log(`=============================================`);

let completed = 0;
let successful = 0;
let failed = 0;
let active = 0;
const start = Date.now();
const latencies = [];

function runRequest() {
  if (completed >= TOTAL_REQUESTS) {
    if (active === 0) {
      report();
    }
    return;
  }

  completed++;
  active++;
  const reqStart = Date.now();

  const req = http.get({
    host: TARGET_HOST,
    port: TARGET_PORT,
    path: '/actuator/health',
    timeout: 5000
  }, (res) => {
    latencies.push(Date.now() - reqStart);
    if (res.statusCode === 200) {
      successful++;
    } else {
      failed++;
    }
    active--;
    runRequest();
  });

  req.on('error', (err) => {
    latencies.push(Date.now() - reqStart);
    failed++;
    active--;
    runRequest();
  });

  req.end();
}

function report() {
  const duration = (Date.now() - start) / 1000;
  const avgLatency = latencies.reduce((a, b) => a + b, 0) / latencies.length;
  const reqPerSec = (successful + failed) / duration;

  console.log(`\n=============================================`);
  console.log(`📊 Load Test Report`);
  console.log(`=============================================`);
  console.log(`Duration:       ${duration.toFixed(2)} seconds`);
  console.log(`Successful:     ${successful}`);
  console.log(`Failed:         ${failed}`);
  console.log(`Avg Latency:    ${avgLatency.toFixed(2)} ms`);
  console.log(`Throughput:     ${reqPerSec.toFixed(2)} req/sec`);
  console.log(`=============================================`);
  process.exit(failed > 0 ? 1 : 0);
}

// Start concurrency loop
for (let i = 0; i < CONCURRENCY; i++) {
  runRequest();
}
