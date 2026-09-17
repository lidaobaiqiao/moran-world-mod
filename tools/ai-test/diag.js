const mineflayer = require('mineflayer');
const bot = mineflayer.createBot({ host: 'localhost', port: 25565, username: 'MoranDiag', version: '1.20.1', auth: 'offline' });
bot.once('spawn', () => {
  setTimeout(() => {
    const reg = bot.registry;
    console.log('[diag] registry 存在:', !!reg);
    if (reg) {
      const blocks = Object.keys(reg.blocksByName || {});
      console.log('[diag] blocksByName 数量:', blocks.length);
      console.log('[diag] peach 相关:', blocks.filter(k => k.includes('peach')).slice(0, 8));
      console.log('[diag] willow 相关:', blocks.filter(k => k.includes('willow')).slice(0, 8));
    }
    console.log('[diag] 位置:', JSON.stringify(bot.entity.position));
    bot.chat('/tp MoranDiag 45 85 45');
    setTimeout(() => {
      console.log('[diag] tp后位置:', JSON.stringify(bot.entity.position));
      bot.quit(); process.exit(0);
    }, 1500);
  }, 2000);
});
bot.on('error', e => console.log('[diag] error:', e.message));
