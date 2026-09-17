/**
 * 墨世界自动化测试驱动器(Mineflayer bot)
 *
 * 用法:node bot.js <scenario.json> [结果文件]
 * 场景为动作数组,依次执行:
 *   {"action":"chat","msg":"/tp MoranTestBot 0 66 0"}   聊天/命令(需 op,由 AI 桥授予)
 *   {"action":"wait","ms":1000}                          等待
 *   {"action":"place","block":"moran_mod:pine_sapling","pos":[20,67,21]}
 *       把背包里指定方块放在 pos(以 pos 下方块为参照面)
 *   {"action":"bonemeal","pos":[20,67,21],"times":20}    对 pos 方块使用骨粉
 *   {"action":"breakBlock","pos":[..]}                   破坏方块
 *   {"action":"find","block":"moran_mod:pine_branch","r":32} 找附近方块,报坐标
 *   {"action":"nearby","block":"moran_mod:pine_branch","r":16} 统计周边数量
 *   {"action":"log","msg":"..."}                         写结果
 */
const mineflayer = require('mineflayer');
const fs = require('fs');

const [,, scenarioPath, outPath] = process.argv;
const scenario = JSON.parse(fs.readFileSync(scenarioPath, 'utf-8'));
const results = [];
const log = (m) => { results.push(String(m)); console.log('[bot]', m); };

const bot = mineflayer.createBot({
  host: 'localhost', port: 25565,
  username: 'MoranTestBot', version: '1.20.1',
  auth: 'offline'
});

bot.once('spawn', () => { log('已进入世界 @ ' + JSON.stringify(bot.entity.position)); run().catch(e => { log('场景失败: ' + e.message); finish(); }); });
bot.on('error', e => { log('连接错误: ' + e.message); });
bot.on('end', () => { console.log('[bot] 连接结束'); process.exit(0); });

const sleep = ms => new Promise(r => setTimeout(r, ms));
async function waitSpawn() { while (!bot.entity) await sleep(100); await sleep(500); }

function giveName(item) { return item ? item.name : null; }

async function doAction(step) {
  switch (step.action) {
    case 'chat': bot.chat(step.msg); await sleep(step.waitMs ?? 300); log('chat: ' + step.msg); break;
    case 'wait': await sleep(step.ms); break;
    case 'log': log(step.msg); break;
    case 'tp': bot.chat(`/tp MoranTestBot ${step.pos.join(' ')}`); await sleep(step.waitMs ?? 500);
               log('tp → ' + step.pos); break;
    case 'give': bot.chat(`/give MoranTestBot ${step.item} ${step.count ?? 1}`); await sleep(step.waitMs ?? 500); break;
    case 'place': {
      await bot.equip(mcLibrary(step.block), 'hand');
      const ref = bot.blockAt(v(step.pos[0], step.pos[1] - 1, step.pos[2]));
      await bot.placeBlock(ref, { x: 0, y: 1, z: 0 });
      log(`放置 ${step.block} @ ${step.pos}`);
      await sleep(step.waitMs ?? 300); break;
    }
    case 'bonemeal': {
      await bot.equip(mcLibrary('minecraft:bone_meal'), 'hand');
      const t = bot.blockAt(v(...step.pos));
      for (let i = 0; i < (step.times ?? 10); i++) {
        if (!t) break;
        await bot.useOn(t);
        await sleep(step.waitMs ?? 150);
      }
      log(`骨粉×${step.times ?? 10} @ ${step.pos}`); break;
    }
    case 'breakBlock': {
      const b = bot.blockAt(v(...step.pos));
      await bot.dig(b); log(`破坏 @ ${step.pos}`); await sleep(200); break;
    }
    case 'find': {
      const id = mcLibrary(step.block);
      const b = bot.findBlock({ matching: id, maxDistance: step.r ?? 32 });
      log(b ? `找到 ${step.block} @ ${b.position}` : `附近无 ${step.block}`); break;
    }
    case 'nearby': {
      const id = mcLibrary(step.block);
      const list = bot.findBlocks({ matching: id, maxDistance: step.r ?? 16, count: 200 });
      log(`nearby ${step.block}: ${list.length} 个 (r=${step.r ?? 16})`); break;
    }
    case 'pos': log('当前位置: ' + JSON.stringify(bot.entity.position)); break;
    case 'placeHere': {
      const f = bot.entity.position;
      const sx = Math.floor(f.x), sy = Math.floor(f.y), sz = Math.floor(f.z);
      await bot.equip(mcLibrary(step.block), 'hand');
      const ref = bot.blockAt({ x: sx, y: sy - 1, z: sz });
      await bot.placeBlock(ref, { x: 0, y: 1, z: 0 });
      log(`原地放置 ${step.block} @ [${sx},${sy},${sz}]`);
      await sleep(step.waitMs ?? 400); break;
    }
    case 'bonemealHere': {
      const f = bot.entity.position;
      const t = bot.blockAt({ x: Math.floor(f.x), y: Math.floor(f.y), z: Math.floor(f.z) });
      await bot.equip(mcLibrary('minecraft:bone_meal'), 'hand');
      for (let i = 0; i < (step.times ?? 10); i++) {
        if (!t) break;
        await bot.useOn(t);
        await sleep(step.waitMs ?? 120);
      }
      log(`原地骨粉×${step.times ?? 10}`); break;
    }
    default: log('未知动作: ' + step.action);
  }
}

function mcLibrary(name) {
  const short = name.split(':').pop();
  const b = bot.registry?.blocksByName?.[short] ?? bot.registry?.itemsByName?.[short];
  if (!b) throw new Error('注册表无此 id: ' + name);
  return b.id;
}
const v = (x, y, z) => ({ x: Math.floor(x), y, z: Math.floor(z) });

async function run() {
  await waitSpawn();
  for (const step of scenario) {
    try { await doAction(step); }
    catch (e) { log('动作异常: ' + JSON.stringify(step) + ' → ' + e.message); }
  }
  fs.writeFileSync(outPath || 'bot-result.json', JSON.stringify(results, null, 2));
  log('场景完成');
  finish();
}
function finish() { try { bot.quit(); } catch (e) {} setTimeout(() => process.exit(0), 500); }
