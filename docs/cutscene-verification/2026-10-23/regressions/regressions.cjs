const fs = require('node:fs');
const path = require('node:path');
const { spawn } = require('node:child_process');
const crypto = require('node:crypto');
const mc = require('minecraft-protocol');
const data = require('minecraft-data')('26.1.2');
const root = path.resolve(__dirname, '../..');
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
const assert = (value, message) => { if (!value) throw new Error(message); };
const results = [];
const scenario = `id: regression
start: fly
policy: {start-mode: initial, completion: keep-open}
points:
  start: {world: world, x: 0, y: 100, z: 0, yaw: 179, pitch: 0}
  end: {world: world, x: 40, y: 101, z: 40, yaw: -179, pitch: 30}
steps:
  fly: {type: move, from: start, to: end, duration: 160, path: linear, speed: uniform, next: done}
  done: {type: end}
`;
const menu = (id, scene) => `${id}:
  camera-position: {type: player, distance: 1.5}
  ${scene ? 'cutscene: {id: regression, auto-start: true}' : ''}
  layout:
    button: {name: Center, x: 0, y: 0, z: 3.5, text-size: 1, hitbox: {width: 0.5, height: 0.5, depth: 0.25}, hover-style: {color: '#FF0000'}}
    right: {name: Right, x: 0.7, y: 0, z: 3.5, text-size: 1, hitbox: {width: 0.5, height: 0.5, depth: 0.25}, hover-style: {color: '#FF0000'}}
`;
for (const dir of ['cutscene-server-smoke', 'cutscene-folia-smoke']) {
  const base = path.join(root, 'target', dir, 'plugins/UIcore');
  fs.writeFileSync(path.join(base, 'cutscenes/regression.yml'), scenario);
  fs.writeFileSync(path.join(base, 'menu/regression_moving.yml'), menu('regression_moving', true));
  fs.writeFileSync(path.join(base, 'menu/regression_static.yml'), menu('regression_static', false));
}

async function run(family, folder, port) {
  const directory = path.join(root, 'target', folder);
  const transcript = path.join(directory, 'cutscene-regressions.log');
  fs.writeFileSync(transcript, '');
  const server = spawn('C:/Program Files/Java/jdk-25.0.2/bin/java.exe', ['-Xms512M', '-Xmx1536M', '-jar', family === 'Paper' ? 'paper.jar' : 'folia.jar', 'nogui'], { cwd: directory, windowsHide: true });
  let output = '';
  const append = buffer => { output += buffer; };
  server.stdout.on('data', append); server.stderr.on('data', append);
  const command = text => { text = text.replaceAll('RegressionBot', username); output += `[TEST] ${text}\n`; server.stdin.write(text + '\n'); };
  async function until(predicate, label, timeout = 120000) {
    const end = Date.now() + timeout;
    while (Date.now() < end) {
      if (predicate()) return;
      if (server.exitCode !== null) throw new Error(`${family} exited ${server.exitCode}`);
      await delay(40);
    }
    throw new Error(`${family} timeout: ${label}; ${output.slice(-1500)}`);
  }
  let client;
  const username = 'RegBot' + Math.floor(Math.random()*100000);
  const result = { family, version: '26.1.2', started: new Date().toISOString(), jarSha256: crypto.createHash('sha256').update(fs.readFileSync(path.join(directory, 'plugins/UIcore.jar'))).digest('hex') };
  const entities = new Map(), metadata = new Map();
  let camera = null, playerId, receiver, gameMode, yaw = 0, pitch = 0, bundle = null, auditing = false;
  const audit = { frames: 0, frameErrors: [], detached: [], spawns: {}, corrections: 0, absoluteCorrections: 0,
    clicksSent: 0, receiverClicks: 0, carrierClicks: 0, movingHoverStyleReceived: false, restoredGameMode: null };
  function checkFrame(packets) {
    if (!auditing || !packets) return;
    const cameraMove = packets.find(p => p.name === 'entity_teleport' && p.data.entityId === camera);
    if (!cameraMove) return;
    const p = cameraMove.data;
    const texts = packets.filter(p => p.name === 'entity_teleport' && entities.get(p.data.entityId)?.type === data.entitiesByName.text_display.id);
    if (!texts.length) return;
    audit.frames++;
    if (texts.length !== 2) audit.frameErrors.push(`frame ${audit.frames}: ${texts.length} text updates`);
    const yr = p.yaw * Math.PI / 180, pr = p.pitch * Math.PI / 180;
    const forward = [-Math.sin(yr)*Math.cos(pr), -Math.sin(pr), Math.cos(yr)*Math.cos(pr)];
    const right = [-Math.cos(yr), 0, -Math.sin(yr)];
    const up = [-Math.sin(yr)*Math.sin(pr), Math.cos(pr), Math.cos(yr)*Math.sin(pr)];
    for (const t of texts) {
      const v = [t.data.x-p.x, t.data.y-p.y, t.data.z-p.z];
      const dot = b => v.reduce((sum, n, i) => sum + n*b[i], 0);
      const local = [dot(right), dot(up), dot(forward)];
      const err = Math.max(Math.min(Math.abs(local[0]), Math.abs(local[0]-.7)), Math.abs(local[1]), Math.abs(local[2]-3.5));
      if (err > .00001) audit.frameErrors.push(`frame ${audit.frames}: local ${local}`);
      assert(metadata.get(t.data.entityId)?.get(10) === 1, 'display interpolation differs from camera');
    }
    assert(metadata.get(camera)?.get(10) === 1, 'camera interpolation not one tick');
  }
  try {
    await until(() => /Done \([\d.]+s\)!/.test(output), 'server ready');
    console.log(`${family}: ready`); command('difficulty peaceful');
    client = mc.createClient({ host: '127.0.0.1', port, username, auth: 'offline', version: '26.1.2' });
    client.on('error', e => audit.frameErrors.push(`client error ${e}`));
    client.on('packet', (d, meta) => {
      const name = meta.name;
      if (name === 'bundle_delimiter') { if (bundle) { checkFrame(bundle); bundle = null; } else bundle = []; return; }
      if (bundle) bundle.push({name, data: d});
      if (name === 'login') { playerId = d.entityId; gameMode = d.gameMode; }
      if (name === 'game_state_change' && (d.reason === 3 || d.reason === 'change_game_mode')) gameMode = d.gameMode;
      if (name === 'camera') camera = d.cameraId;
      if (name === 'spawn_entity') {
        if (auditing) audit.spawns[d.entityId] = (audit.spawns[d.entityId] || 0) + 1;
        entities.set(d.entityId, {...d});
        if (d.type === data.entitiesByName.interaction.id && d.entityId < 0) receiver = {...d};
      }
      if (name === 'entity_metadata') {
        const m = metadata.get(d.entityId) || new Map();
        for (const x of d.metadata) m.set(x.key, x.value);
        metadata.set(d.entityId, m);
        if (auditing && d.metadata.some(x => x.key === 23 && /Right/.test(JSON.stringify(x.value)) && /#ff0000/i.test(JSON.stringify(x.value))))
          audit.movingHoverStyleReceived = true;
      }
      if (name === 'entity_destroy') {
        if (auditing && d.entityIds.includes(camera)) audit.detached.push(camera);
        d.entityIds.forEach(id => { entities.delete(id); metadata.delete(id); });
      }
      if (name === 'entity_teleport' || name === 'sync_entity_position') {
        if (entities.has(d.entityId)) Object.assign(entities.get(d.entityId), d);
      }
      if (name === 'position') {
        // Apply correction angles exactly as a client does, then echo its rotation.
        yaw = d.flags.yaw ? yaw + d.yaw : d.yaw;
        pitch = d.flags.pitch ? pitch + d.pitch : d.pitch;
        if (auditing) { audit.corrections++; if (!d.flags.yaw || !d.flags.pitch || d.yaw !== 0 || d.pitch !== 0) audit.absoluteCorrections++; }
        client.write('teleport_confirm', { teleportId: d.teleportId });
        client.write('look', {yaw, pitch, flags: {onGround: false, hasHorizontalCollision: false}});
      }
    });
    await until(() => /RegBot\d+\[.*logged in/.test(output), 'login');
    command('uicore run regression_moving RegressionBot');
    await until(() => /CutsceneStartEvent/.test(output) && receiver && camera < 0, 'cutscene started');
    await delay(250);
    command('cutsceneprobe report RegressionBot');
    const fixedYaw = yaw, fixedPitch = pitch;
    auditing = true;
    async function clicks(menuId, expectedElement, count) {
      const offset = output.length;
      for (let i=0; i<count; i++) {
        assert(gameMode === 3, `${family}: crosshair would be visible, mode=${gameMode}`);
        assert(entities.has(camera) && camera !== playerId, `${family}: camera detached`);
        assert(receiver && metadata.get(receiver.entityId)?.get(8) === 16 && metadata.get(receiver.entityId)?.get(9) === 2, 'missing click receiver');
        // The vanilla picker may select the invisible physical camera pig instead.
        const carrier = Array.from(entities.values()).find(e => e.type === data.entitiesByName.pig.id);
        const target = i % 2 && carrier ? carrier.entityId : receiver.entityId;
        client.write('spectate_entity', {entityId: target});
        audit.clicksSent++;
        if (target === receiver.entityId) audit.receiverClicks++; else audit.carrierClicks++;
        await delay(160);
      }
      await delay(200);
      const actual = (output.slice(offset).match(new RegExp(`Probe click ${menuId}:${expectedElement}\\b`, 'g')) || []).length;
      assert(actual === count, `${family}: ${menuId}:${expectedElement} ${actual}/${count} clicks; ${output.slice(offset, offset+2000)}`);
      return actual;
    }
    result.movingClicks = await clicks('regression_moving', 'button', 10);
    yaw += 31.5;
    client.write('look', {yaw, pitch, flags: {onGround: false, hasHorizontalCollision: false}});
    await delay(300);
    result.movingRightClicks = await clicks('regression_moving', 'right', 5);
    yaw -= 31.5;
    client.write('look', {yaw, pitch, flags: {onGround: false, hasHorizontalCollision: false}});
    await delay(300);
    result.movingClicks += await clicks('regression_moving', 'button', 5);
    assert(/Probe hover regression_moving:right enter=true .*state=PLAYING/.test(output), `${family}: no hover during playback`);
    assert(/Probe hover regression_moving:button enter=false .*state=PLAYING/.test(output), `${family}: no hover leave during playback`);
    assert(audit.movingHoverStyleReceived, `${family}: hover event fired but the red Right text did not reach the client during playback`);
    result.movingHover = 'pass';
    assert(yaw === fixedYaw && pitch === fixedPitch, `${family}: programmatic camera changed raw mouse ${yaw},${pitch} / ${fixedYaw},${fixedPitch}`);
    command('uicore cutscene pause RegressionBot');
    await delay(200);
    result.pausedClicks = await clicks('regression_moving', 'button', 5);
    yaw += 31.5;
    client.write('look', {yaw, pitch, flags: {onGround: false, hasHorizontalCollision: false}});
    await delay(300);
    result.mouseClicks = await clicks('regression_moving', 'right', 5);
    command('uicore cutscene resume RegressionBot');
    await until(() => /CutsceneCompleteEvent/.test(output), 'complete');
    assert(audit.frames > 100, `${family}: insufficient synchronized frames ${audit.frames}`);
    assert(audit.absoluteCorrections === 0, `${family}: absolute angle corrections ${audit.absoluteCorrections}`);
    assert(audit.detached.length === 0, `${family}: camera removed`);
    assert(audit.frameErrors.length === 0, `${family}: frame errors ${audit.frameErrors.slice(0,3)}`);
    assert(Object.entries(audit.spawns).filter(([id]) => [data.entitiesByName.text_display.id, data.entitiesByName.item_display.id].includes(entities.get(Number(id))?.type)).every(([,count]) => count === 1), `${family}: visuals respawned during playback`);
    auditing = false;
    let offset = output.length;
    command('cutsceneprobe close RegressionBot');
    await until(() => output.slice(offset).includes('Probe closed=true cameraAbsent=true cursorAbsent=true'), 'close');
    await delay(200);
    assert(gameMode === 0 && camera === playerId, `${family}: state not restored mode=${gameMode} camera=${camera}`);
    audit.restoredGameMode = gameMode;
    assert(!Array.from(entities.keys()).some(id => id < 0), `${family}: leaked client entities after close`);
    result.restoration = 'pass';
    command('uicore run regression_static RegressionBot');
    await until(() => camera < 0 && entities.has(camera), 'static menu');
    await delay(350);
    // The legacy static menu starts its cursor at the pig's base, .765 below
    // the viewpoint. Aim the cursor ray at the button plane 3.5 blocks away.
    pitch -= .765 * (1 - 2.5/3.5) * 90/3;
    client.write('look', {yaw, pitch, flags: {onGround: false, hasHorizontalCollision: false}});
    await delay(250);
    result.staticClicks = await clicks('regression_static', 'button', 10);
    offset = output.length; command('cutsceneprobe close RegressionBot');
    await until(() => output.slice(offset).includes('Probe closed=true cameraAbsent=true cursorAbsent=true'), 'static close');
    assert(gameMode === 0 && camera === playerId, `${family}: static state not restored`);
    assert(!/\[UIcore\].*(Exception|initialization failed|failed;)|Thread failed main thread check|Cannot read world asynchronously/.test(output), `${family}: plugin exception`);
    result.result = 'pass'; console.log(`${family}: 40/40 clicks, ${audit.frames} frames, HUD and restoration passed`);
  } catch(e) { result.result = 'fail'; result.error = String(e); console.error(result.error); }
  finally {
    auditing = false;
    if (client) client.end('Regression complete');
    if (server.exitCode === null) {
      command('stop');
      const end = Date.now()+40000;
      while(server.exitCode === null && Date.now()<end) await delay(100);
      if(server.exitCode === null) {server.kill(); result.forcedStop = true;}
    }
    result.audit = audit; result.finished = new Date().toISOString(); results.push(result);
    fs.writeFileSync(transcript, output);
    fs.writeFileSync(path.join(__dirname,'regression-results.json'), JSON.stringify(results,null,2));
  }
}
Promise.all([run('Paper','cutscene-server-smoke',25586),run('Folia','cutscene-folia-smoke',25587)])
  .then(() => {process.exitCode = results.every(x => x.result === 'pass') ? 0 : 1;});
