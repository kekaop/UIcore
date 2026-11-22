const fs = require('node:fs');
const path = require('node:path');
const { spawn } = require('node:child_process');
const crypto = require('node:crypto');
const root = path.resolve(__dirname, '..', '..');
const java = 'C:/Program Files/Java/jdk-25.0.2/bin/java.exe';
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
const results = [];
function assert(value, message) { if (!value) throw new Error(message); }
async function run(family, folder, port, version) {
  const directory = path.join(root, 'target', folder);
  let output = '';
  const transcript = path.join(directory, 'cutscene-verification.log');
  fs.writeFileSync(transcript, '');
  const server = spawn(java, ['-Xms512M', '-Xmx1536M', '-jar', family === 'Paper' ? 'paper.jar' : 'folia.jar', 'nogui'], { cwd: directory, windowsHide: true });
  const append = data => { output += data; fs.appendFileSync(transcript, data); };
  server.stdout.on('data', append); server.stderr.on('data', append);
  const waitFor = async (pattern, since = 0, timeout = 120000) => {
    const deadline = Date.now() + timeout;
    while (Date.now() < deadline) {
      const match = output.slice(since).match(pattern);
      if (match) return match;
      if (server.exitCode !== null) throw new Error(`${family} exited ${server.exitCode}: ${output.slice(-1000)}`);
      await delay(100);
    }
    throw new Error(`${family}: timed out waiting ${pattern}; tail=${output.slice(-1500)}`);
  };
  const command = text => { fs.appendFileSync(transcript, `[TEST] ${text}\n`); server.stdin.write(text + '\n'); };
  let bot;
  const result = { family, version, port, started: new Date().toISOString(), jarSha256: crypto.createHash('sha256').update(fs.readFileSync(path.join(directory, 'plugins/UIcore.jar'))).digest('hex'), checks: [] };
  try {
    await waitFor(/Done \([\d.]+s\)!/);
    console.log(`${family}: ready`);
    bot = spawn(process.execPath, [path.join(__dirname, 'bot.cjs'), String(port), version, 'CutsceneBot'], { cwd: __dirname, windowsHide: true });
    bot.stdout.on('data', () => {}); bot.stderr.on('data', data => fs.appendFileSync(transcript, `[BOT] ${data}`));
    await waitFor(/CutsceneBot\[.*logged in with entity id/);
    command('uicore cutscene validate'); await waitFor(/Valid cutscene configuration/);
    for (const option of ['left', 'right']) {
      const start = output.length;
      command('uicore run demo_cutscene CutsceneBot');
      await waitFor(/CutsceneStartEvent/, start);
      if (option === 'left') {
        await delay(1300);
        let position = output.length;
        command('uicore cutscene pause CutsceneBot'); command('uicore cutscene status CutsceneBot');
        const first = await waitFor(/demo, PAUSED, step=(\w+), ticks=(\d+), progress=([\d.]+)/, position);
        await delay(1300); position = output.length;
        command('uicore cutscene status CutsceneBot');
        const second = await waitFor(/demo, PAUSED, step=(\w+), ticks=(\d+), progress=([\d.]+)/, position);
        assert(first[0] === second[0], `${family}: pause advanced ${first[0]} -> ${second[0]}`);
        result.checks.push({ name: 'pause-resume', result: 'pass', status: first[0], holdMilliseconds: 1300 });
        command('uicore cutscene resume CutsceneBot');
      }
      await waitFor(/CutsceneStepEnterEvent.*stepId=route/, start);
      const choiceStart = output.length;
      command(`cutsceneprobe choose CutsceneBot ${option}`);
      command(`cutsceneprobe choose CutsceneBot ${option === 'left' ? 'right' : 'left'}`);
      await waitFor(/CutsceneCompleteEvent/, choiceStart);
      assert((output.slice(choiceStart).match(/Accepted option /g) || []).length === 1, `${family}: choice accepted more than once`);
      assert(output.slice(choiceStart).includes(`Accepted option ${option}`), `${family}: wrong branch`);
      const reportStart = output.length; command('cutsceneprobe report CutsceneBot');
      const report = await waitFor(/Probe report ping=(\d+) ms eye=([\d.-]+),([\d.-]+),([\d.-]+).*cursor=([\d.-]+),([\d.-]+),([\d.-]+)/, reportStart);
      const expected = [option === 'left' ? 2 : 10, 101, 12];
      expected.forEach((value, index) => assert(Math.abs(Number(report[index+2]) - value) <= 0.0001, `${family}: endpoint mismatch ${report[0]}`));
      assert(Math.abs(Number(report[5])) < 0.0001 && Math.abs(Number(report[6])) < 0.0001, `${family}: stationary cursor drift ${report[0]}`);
      result.checks.push({ name: `${option}-branch-double-choice`, result: 'pass', report: report[0] });
      const closeStart = output.length; command('cutsceneprobe close CutsceneBot');
      await waitFor(/Probe closed=true cameraAbsent=true cursorAbsent=true/, closeStart);
      result.checks.push({ name: `${option}-close`, result: 'pass' });
      console.log(`${family}: ${option} branch, endpoint, fixed cursor, close passed`);
    }
    const start = output.length; command('uicore run smoke_cross CutsceneBot');
    await waitFor(/CutsceneCompleteEvent.*id=cross/, start);
    const reportStart = output.length; command('cutsceneprobe report CutsceneBot');
    const report = await waitFor(/Probe report ping=(\d+) ms eye=([\d.-]+),([\d.-]+),([\d.-]+).*cursor=([\d.-]+),([\d.-]+),([\d.-]+)/, reportStart);
    [40,100,40].forEach((value,index) => assert(Math.abs(Number(report[index+2])-value) <= 0.0001, `${family}: chunk endpoint mismatch`));
    result.checks.push({ name: 'cross-chunks', result: 'pass', report: report[0], genuineFoliaRegionBoundary: 'not established' });
    const closeStart = output.length; command('cutsceneprobe close CutsceneBot');
    await waitFor(/Probe closed=true cameraAbsent=true cursorAbsent=true/, closeStart);
    command('tps');
    await delay(800);
    assert(!/\[UIcore\].*(Exception|initialization failed|Return teleport failed)|Thread failed main thread check|Cannot read world asynchronously/.test(output), `${family}: plugin error in transcript`);
    result.result = 'pass';
    console.log(`${family}: all protocol checks passed`);
  } catch (error) { result.result = 'fail'; result.error = String(error); console.error(result.error); }
  finally {
    if (bot && bot.exitCode === null) bot.kill('SIGINT');
    if (server.exitCode === null) {
      command('stop');
      const deadline = Date.now() + 40000;
      while (server.exitCode === null && Date.now() < deadline) await delay(200);
      if (server.exitCode === null) { server.kill(); result.forcedStop = true; }
    }
    result.finished = new Date().toISOString(); results.push(result);
    fs.writeFileSync(path.join(__dirname, 'verification-results.json'), JSON.stringify(results, null, 2));
  }
}
const cross = `id: cross\nstart: fly\npolicy: {start-mode: initial, completion: keep-open}\npoints:\n  start: {world: world, x: 0, y: 100, z: 0, yaw: 0, pitch: 0}\n  end: {world: world, x: 40, y: 100, z: 40, yaw: 90, pitch: 0}\nsteps:\n  fly: {type: move, from: start, to: end, duration: 40, path: linear, speed: uniform, next: done}\n  done: {type: end}\n`;
const menu = `smoke_cross:\n  camera-position: {type: player, distance: 1.5}\n  cutscene: {id: cross, auto-start: true}\n  layout:\n    marker: {name: Marker, x: 0, y: 0, z: 3}\n`;
for (const folder of ['cutscene-server-smoke','cutscene-folia-smoke']) {
  fs.writeFileSync(path.join(root,'target',folder,'plugins/UIcore/cutscenes/cross.yml'),cross);
  fs.writeFileSync(path.join(root,'target',folder,'plugins/UIcore/menu/smoke_cross.yml'),menu);
}
Promise.all([run('Paper','cutscene-server-smoke',25586,'1.21.11'),run('Folia','cutscene-folia-smoke',25587,'26.1.2')])
  .then(() => { process.exitCode = results.every(result => result.result === 'pass') ? 0 : 1; });
