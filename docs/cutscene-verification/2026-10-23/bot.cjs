const fs = require('node:fs');
const path = require('node:path');
const mc = require('minecraft-protocol');
const port = Number(process.argv[2] || 25586);
const version = process.argv[3] || '1.21.11';
const username = process.argv[4] || 'CutsceneBot';
const output = path.join(__dirname, `packets-${port}.json`);
const record = { started: new Date().toISOString(), port, version, username, camera: null,
  counts: {}, cameraPackets: [], playerTeleports: [], chat: [], errors: [], samples: [] };
const entities = new Map();
const clean = value => value === undefined ? null : JSON.parse(JSON.stringify(value, (_, item) => typeof item === 'bigint' ? item.toString() : item));
const client = mc.createClient({ host: '127.0.0.1', port, username, auth: 'offline', version });
client.on('login', () => { console.log('LOGIN', version, username); record.loggedIn = new Date().toISOString(); });
client.on('packet', (data, meta) => {
  record.counts[meta.name] = (record.counts[meta.name] || 0) + 1;
  const packet = { at: new Date().toISOString(), type: meta.name, ...clean(data) };
  if (meta.name === 'camera') { record.camera = data.cameraId; record.cameraPackets.push(packet); console.log('CAMERA', data.cameraId); }
  if (meta.name === 'spawn_entity') entities.set(data.entityId, { x: data.x, y: data.y, z: data.z, yaw: data.yaw, pitch: data.pitch });
  if (meta.name === 'entity_teleport' || meta.name === 'sync_entity_position') {
    entities.set(data.entityId, { x: data.x, y: data.y, z: data.z, yaw: data.yaw, pitch: data.pitch });
    if (data.entityId === record.camera) record.cameraPackets.push(packet);
  }
  if (meta.name === 'entity_move_look' || meta.name === 'rel_entity_move') {
    const previous = entities.get(data.entityId);
    if (previous) entities.set(data.entityId, { x: previous.x + data.dX / 4096, y: previous.y + data.dY / 4096,
      z: previous.z + data.dZ / 4096, yaw: data.yaw ?? previous.yaw, pitch: data.pitch ?? previous.pitch });
    if (data.entityId === record.camera) record.cameraPackets.push(packet);
  }
  if (meta.name === 'position') {
    record.playerTeleports.push(packet);
    client.write('teleport_confirm', { teleportId: data.teleportId });
  }
  if (meta.name === 'system_chat' || meta.name === 'kick_disconnect' || meta.name === 'disconnect') {
    record.chat.push(packet);
    console.log(meta.name, JSON.stringify(clean(data)));
  }
});
const timer = setInterval(() => {
  if (record.camera !== null) record.samples.push({ at: new Date().toISOString(), cameraId: record.camera, pose: entities.get(record.camera) });
  fs.writeFileSync(output, JSON.stringify(record, null, 2));
}, 1000);
client.on('error', error => { record.errors.push(String(error)); console.log('ERROR', error.message); });
client.on('end', reason => { record.ended = new Date().toISOString(); record.endReason = String(reason); finish(); });
function finish() { clearInterval(timer); fs.writeFileSync(output, JSON.stringify(record, null, 2)); process.exit(0); }
process.on('SIGINT', () => { client.end('Smoke complete'); setTimeout(finish, 250); });
setTimeout(() => { client.end('Smoke timeout'); setTimeout(finish, 250); }, 180000);
