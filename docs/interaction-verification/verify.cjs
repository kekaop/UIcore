const fs=require('node:fs'),path=require('node:path'),{spawn}=require('node:child_process'),crypto=require('node:crypto');
const root=path.resolve(__dirname,'../..'),mc=require(path.join(root,'target/cutscene-client/node_modules/minecraft-protocol'));
const logDir=process.env.UICORE_INTERACTION_LOG_DIR || __dirname;
fs.mkdirSync(logDir,{recursive:true});
const data=require(path.join(root,'target/cutscene-client/node_modules/minecraft-data'))('26.1.2');
const delay=ms=>new Promise(r=>setTimeout(r,ms)),assert=(x,m)=>{if(!x)throw Error(m)};
const menu=`probe_drag:
  drag-input: block-digging
  camera-position: {type: player, distance: 1.5}
  npc: {enabled: false}
  layout:
    root: {type: group, x: 0, y: 0, z: 3.5}
    handle:
      parent: root
      name: Handle
      hitbox: {width: 0.8, height: 0.8, depth: 0.25}
      drag: {enabled: true, target: root, bounds: {x: [-1, 1], y: [-1, 1]}}
    child: {parent: root, name: Child, x: 0.5, y: 0.3, z: 0, pointer-events: false}
`;
const extras=`probe_slider:
  drag-input: block-digging
  camera-position: {type: player, distance: 1.5}
  templates:
    knob: {type: item, material: LIME_DYE, transform: gui, scale: 0.3}
  slots:
    slider:
      content: knob
      x: 0
      y: 0
      z: 3.5
      hitbox: {width: 0.8, height: 0.8, depth: 0.25}
      slider: {state: amount, min: 0, max: 100, step: 5}
      drag: {axis: x, bounds: {x: [-1, 1]}}
probe_drop:
  drag-input: block-digging
  camera-position: {type: player, distance: 1.5}
  layout:
    gem:
      type: item
      material: DIAMOND
      transform: gui
      scale: 0.3
      z: 3.5
      hitbox: {width: 0.6, height: 0.6, depth: 0.25}
      drag: {enabled: true, drop-targets: [socket], bounds: {x: [-2, 2]}}
      on-drop: ['[state] dropped yes']
  slots:
    socket:
      x: 1
      z: 3.5
      hitbox: {width: 0.6, height: 0.6, depth: 0.25}
      on-drop: ['[action] probe:drop']
probe_click:
  camera-position: {type: player, distance: 1.5}
  layout:
    handle: {name: Toggle, z: 3.5, hitbox: {width: 0.8, height: 0.8, depth: 0.25}, drag: {enabled: true}}
probe_zoom:
  camera-position: {type: player, distance: 1.5}
  zoom: {enabled: true, group: map, min: 0.5, max: 1.5, step: 0.5, smoothing: 0.5, pivot: {x: 0.2, y: 0}}
  layout:
    map: {type: group, x: 0.4, y: 0.2, z: 3.5}
    nested: {type: group, parent: map, x: 0.1}
    gem:
      parent: nested
      type: item
      material: DIAMOND
      transform: gui
      scale: 0.4
      x: 0.1
      hitbox: {width: 0.4, height: 0.4, depth: 0.25}
      drag: {enabled: true, drop-targets: [socket]}
    label: {name: Outside, x: -1, y: -0.6, z: 3.5, text-size: 0.5}
  slots:
    socket:
      parent: map
      x: 0.8
      drop-accepts: [gem]
      on-drop: ['[state] zoom_drop true']
      hitbox: {width: 0.4, height: 0.4, depth: 0.25}
`;
const results=[];
async function run(family,folder,port) {
 const dir=path.join(root,'target',folder),jar=path.join(root,'uicore-plugin/target/UIcore-1.1.0.jar');
 fs.copyFileSync(jar,path.join(dir,'plugins/UIcore.jar'));
 fs.copyFileSync(path.join(root,'target/interaction-probe/InteractionProbe.jar'),path.join(dir,'plugins/InteractionProbe.jar'));
 fs.writeFileSync(path.join(dir,'plugins/UIcore/menu/probe_drag.yml'),menu+'\n'+extras);
 const server=spawn('C:/Program Files/Java/jdk-25.0.2/bin/java.exe',['-Xms512M','-Xmx1536M','-jar',family==='Paper'?'paper.jar':'folia.jar','nogui'],{cwd:dir,windowsHide:true});
 let out='',client,barrier,mode,camera,playerId,infoMode,clientSlot,slotUpdates=0,windows=0,yaw=0,pitch=0,sequence=0,restored=false,playerFlags=0;
 let diggingAbilities;
 server.stdout.on('data',b=>out+=b);server.stderr.on('data',b=>out+=b);
 const result={family,hash:crypto.createHash('sha256').update(fs.readFileSync(jar)).digest('hex'),checks:[]};
 const command=s=>{out+='[TEST] '+s+'\n';server.stdin.write(s+'\n')};
 async function wait(predicate,label,ms=60000){const until=Date.now()+ms;while(Date.now()<until){if(predicate())return;if(server.exitCode!==null)throw Error('Server exited');await delay(50)}throw Error(label+' timeout '+out.slice(-1200))}
 const name='DragBot'+Math.floor(Math.random()*10000);
 async function cmd(s,marker){const start=out.length;command('interactionprobe '+s.replace('{p}',name));await wait(()=>out.slice(start).includes(marker),marker,10000)}
 const dig=status=>client.write('block_dig',{status,location:barrier,face:1,sequence:++sequence});
 try {
  await wait(()=>out.includes('Done ('),'startup');console.log(family+' ready');
  client=mc.createClient({host:'127.0.0.1',port,username:name,auth:'offline',version:'26.1.2'});
  client.on('error',e=>out+='CLIENT '+e+'\n');
  client.on('packet',(d,m)=>{
   if(m.name==='login')playerId=d.entityId;
   if(m.name==='camera')camera=d.cameraId;
   if(m.name==='entity_metadata'&&d.entityId===playerId)playerFlags=d.metadata.find(entry=>entry.key===0)?.value??playerFlags;
   if(m.name==='player_info')for(const entry of d.data) {
    if((entry.uuid===client.uuid||entry.player?.name===name)&&entry.gamemode!=null)infoMode=entry.gamemode;
   }
   if(m.name==='open_window'||m.name==='open_horse_window')windows++;
   if(m.name==='held_item_slot'){clientSlot=d.slot;slotUpdates++}
   if(m.name==='game_state_change'&&(d.reason===3||d.reason==='change_game_mode'))mode=d.gameMode;
   if(m.name==='abilities'&&mode===1)diggingAbilities=d.flags;
   if(m.name==='block_change'){
    const block=data.blocksByStateId[d.type];
    if(block?.name==='barrier')barrier=d.location;
    else if(barrier&&JSON.stringify(d.location)===JSON.stringify(barrier))restored=true;
   }
   if(m.name==='position') {yaw=d.flags.yaw?yaw+d.yaw:d.yaw;pitch=d.flags.pitch?pitch+d.pitch:d.pitch;client.write('teleport_confirm',{teleportId:d.teleportId});client.write('look',{yaw,pitch,flags:{onGround:false,hasHorizontalCollision:false}})}
  });
  await wait(()=>out.includes(name+'['),'login');command('uicore run probe_drag '+name);
  await wait(()=>barrier&&mode===1&&camera<0&&diggingAbilities!==undefined,'digging camera');
  assert((diggingAbilities&0x0f)===0,'experience is hidden with flight and instant mining disabled');
  assert(playerFlags&0x20,'empty hand must be hidden on the client');
  command('effect give '+name+' minecraft:glowing 30 0 true');
  await wait(()=>(playerFlags&0x60)===0x60,'player flag update preserves client invisibility');
  command('effect clear '+name+' minecraft:glowing');
  await wait(()=>(playerFlags&0x60)===0x20,'removing glow preserves client invisibility');
  await cmd('carrier {p}','PROBE carrier-visible=false');
  await cmd('pointer {p} 0 0','PROBE pointer ready');
  let start=out.length;dig(0);await wait(()=>out.slice(start).includes('PROBE drag START'),'capture');
  await cmd('pointer {p} 0.6 0.2','PROBE pointer ready');await delay(300);
  await cmd('report {p}','PROBE report');assert(out.includes('root=0.6000,0.2000 child=1.1000,0.5000 error=0.000000'),'parent/hitbox movement');
  start=out.length;dig(1);await wait(()=>out.slice(start).includes('PROBE committed root receiver=null x=0.6000 actual=0.6000'),'free release');
  result.checks.push('digging START/MOVE and committed free drop; group and child world positions');
  await cmd('cancel {p} true','PROBE cancel=true');await cmd('pointer {p} 0.6 0.2','PROBE pointer ready');
  start=out.length;dig(0);await wait(()=>out.slice(start).includes('PROBE drag START'),'second capture');
  await cmd('pointer {p} 0.9 0.3','PROBE pointer ready');await delay(250);start=out.length;dig(1);await wait(()=>out.slice(start).includes('PROBE drag CANCEL'),'cancel drop');
  const check=out.length;await cmd('report {p}','PROBE report');assert(out.slice(check).includes('root=0.6000,0.2000 child=1.1000,0.5000 error=0.000000'),'cancel rollback');result.checks.push('cancelled DROP rolls back group and hitbox positions');
  await cmd('cancel {p} false','PROBE cancel=false');await cmd('pointer {p} 0.6 0.2','PROBE pointer ready');
  start=out.length;dig(0);await wait(()=>out.slice(start).includes('PROBE drag START'),'close capture');
  restored=false;await cmd('close {p}','PROBE closed');await wait(()=>restored&&mode===0&&camera===playerId&&(playerFlags&0x20)===0,'restore block, camera and player flags');result.checks.push('close cancels active gesture and restores client block, camera and player flags');
  async function open(id) {
    barrier=null; command('uicore run '+id+' '+name);
    await wait(()=>barrier&&mode===1&&camera<0,'open '+id); await delay(150);
  }
  async function capture(x,y=0) {
    await cmd('pointer {p} '+x+' '+y,'PROBE pointer ready'); const offset=out.length;dig(0);
    await wait(()=>out.slice(offset).includes('PROBE drag START'),'capture '+x);
  }
  async function release(phase='DROP') {
    const offset=out.length;dig(1);await wait(()=>out.slice(offset).includes('PROBE drag '+phase),'release '+phase);await delay(150);
  }
  async function inspect(id,expected) {
    const offset=out.length;await cmd('inspect {p} '+id,'PROBE inspect');
    assert(out.slice(offset).includes(expected),'inspect '+id+' expected '+expected+' got '+out.slice(offset));
  }
  await open('probe_slider'); await capture(0);
  await cmd('pointer {p} 0.63 0.5','PROBE pointer ready'); await delay(250);
  await inspect('slider','position=0.6300,0.0000 active=true state={amount=80.0}');
  await cmd('cancel {p} true','PROBE cancel=true');await release('CANCEL');
  await inspect('slider','position=0.0000,0.0000 active=false state={}');
  await cmd('cancel {p} false','PROBE cancel=false');
  await capture(0);await cmd('pointer {p} 2 0','PROBE pointer ready');await delay(200);await release();
  await inspect('slider','position=1.0000,0.0000 active=false state={amount=100.0}');
  result.checks.push('populated slot slider: axis lock, clamp, quantization, rollback of absent state');
  await open('probe_drop');await capture(0);
  await cmd('pointer {p} 1.1 0','PROBE pointer ready');await delay(200);
  const accepted=out.length;await release();
  await inspect('gem','position=1.0000,0.0000 active=false');
  const committed=out.slice(accepted);
  const eventAt=committed.indexOf('PROBE committed gem receiver=socket x=1.0000 actual=1.0000 state=null');
  const actionAt=committed.indexOf('PROBE action owner=socket handle=gem moved=gem receiver=socket state=yes');
  assert(eventAt>=0 && actionAt>eventAt,'snap committed before Java event, followed by handle and receiver actions');
  assert(committed.includes('drop_count=1'),'receiver action ran once');
  const rejected=out.length;
  await capture(1);await cmd('pointer {p} 1.8 0','PROBE pointer ready');await delay(200);await release('CANCEL');
  await inspect('gem','position=1.0000,0.0000 active=false');
  await cmd('cancel {p} true','PROBE cancel=true');await capture(1);
  await cmd('pointer {p} 1.1 0','PROBE pointer ready');await delay(200);await release('CANCEL');
  await inspect('gem','position=1.0000,0.0000 active=false');
  assert(!/PROBE (committed gem|action owner=socket)/.test(out.slice(rejected)),'no event or actions for rejected and vetoed drops');
  await cmd('cancel {p} false','PROBE cancel=false');
  result.checks.push('empty receiving slot: snap then Java event and YAML state/Java action; rejected and vetoed drops publish no result');
  await open('probe_drag');
  await cmd('mutate {p} START','PROBE mutation=START');await capture(0);await delay(200);
  await inspect('root','position=0.4000,0.0000 active=false');dig(1);
  await cmd('mutate {p} MOVE','PROBE mutation=MOVE');await capture(.4);
  const moved=out.length;await cmd('pointer {p} 0.7 0','PROBE pointer ready');await wait(()=>out.slice(moved).includes('PROBE drag CANCEL'),'move mutation cancellation');
  await inspect('root','position=0.4000,0.0000 active=false');dig(1);
  result.checks.push('START and MOVE listener mutations cannot leave a stale capture');
  await capture(.4);const revoked=out.length;await cmd('edit {p} handle permission probe.denied','PROBE edited');
  await wait(()=>out.slice(revoked).includes('PROBE drag CANCEL'),'permission revoke');await inspect('root','position=0.4000,0.0000 active=false');dig(1);
  await cmd('edit {p} handle permission null','PROBE edited');
  await capture(.4);const switched=out.length;command('uicore run probe_click '+name);
  await wait(()=>mode===3&&out.slice(switched).includes('PROBE drag CANCEL'),'switch cancels drag and restores spectator');
  assert((playerFlags&0x20)===0,'switching out of block digging restores player flags');
  result.checks.push('permission revocation and same-camera menu switch cancel capture and restore player flags');
  await open('probe_drag');await cmd('remove {p} root','PROBE removed');await delay(300);
  await inspect('child','position=hidden active=false');await cmd('add {p}','PROBE added');
  result.checks.push('removing a parent hides descendants without blocking unrelated API additions');
  await cmd('close {p}','PROBE closed');await wait(()=>camera===playerId,'close before opening zoom');
  await cmd('hotbar {p}','PROBE held slot=0');
  const beforeWindows=windows;
  command('uicore run probe_zoom '+name);
  await wait(()=>mode===3&&camera<0&&infoMode===2,'zoom spectator HUD and Adventure player info');
  const held=slotId=>client.write('held_item_slot',{slotId});
  const click=()=>client.write('arm_animation',{hand:'main_hand'});
  await delay(200);held(8);await delay(760);
  await cmd('render {p} gem','PROBE render gem x=0.600 y=0.200 scale=1.500 item=0.600 error=0.000000');
  await cmd('render {p} socket','PROBE render socket x=1.500 y=0.200 scale=1.500 item=0.000 error=0.000000');
  await cmd('render {p} label','PROBE render label x=-1.000 y=-0.600 scale=0.500 item=0.000 error=0.000000');
  held(4);await delay(200);
  await cmd('render {p} socket','PROBE render socket x=1.500 y=0.200 scale=1.500');
  await cmd('pointer {p} 0.94 0.2','PROBE pointer ready');
  start=out.length;click();await wait(()=>out.slice(start).includes('PROBE drag START target=gem'),'zoomed hitbox capture');
  await cmd('pointer {p} 1.53 0.2','PROBE pointer ready');await delay(170);
  start=out.length;click();await wait(()=>out.slice(start).includes('PROBE committed gem receiver=socket'),'zoomed drop');
  await inspect('gem','position=1.5000,0.2000 active=false state={zoom_drop=true}');
  held(5);await delay(760);
  await cmd('render {p} gem','PROBE render gem x=1.200 y=0.200 scale=1.000 item=0.400 error=0.000000');
  await cmd('hotbar {p}','PROBE held slot=0');
  assert(mode===3&&infoMode===2&&windows===beforeWindows,'zoom retains spectator HUD mode and does not open a screen');
  const beforeRestore=slotUpdates;
  await cmd('close {p}','PROBE closed');
  await wait(()=>slotUpdates>beforeRestore&&clientSlot===0&&camera===playerId,'restore authoritative client slot');
  held(2);await delay(150);await cmd('hotbar {p}','PROBE held slot=2');
  result.checks.push('zoom via held-item packets: wrap, group pivot, scaled item and hitbox, drag/drop, ignored jump, spectator HUD mode and slot restore');
  assert(!/PROBE error|Could not pass event MenuDropEvent|Thread failed main thread check|Cannot read world asynchronously|initialization failed/.test(out),'runtime error');
  result.status='pass';console.log(family+' interaction checks passed');
 }catch(e){result.status='fail';result.error=String(e);console.error(family+' '+e)}
 finally{
  client?.end('done');if(server.exitCode===null){command('stop');await wait(()=>server.exitCode!==null,'stop',45000).catch(()=>server.kill())}
  fs.writeFileSync(path.join(logDir,family.toLowerCase()+'.log'),out);results.push(result);fs.writeFileSync(path.join(logDir,'results.json'),JSON.stringify(results,null,2));
 }
}
(async()=>{await run('Paper','cutscene-server-smoke',25586);await run('Folia','cutscene-folia-smoke',25587);process.exitCode=results.every(r=>r.status==='pass')?0:1})()
