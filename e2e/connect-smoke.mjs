import mineflayer from 'mineflayer'

const host = process.env.MC_HOST ?? '127.0.0.1'
const port = Number(process.env.MC_PORT ?? 25566)
const username = process.env.MC_BOT_NAME ?? `MechBot${Math.floor(Math.random() * 10000)}`
const version = process.env.MC_VERSION

const bot = mineflayer.createBot({
  host,
  port,
  username,
  auth: 'offline',
  ...(version ? { version } : {})
})

const timeout = setTimeout(() => {
  console.error(`[connect] timeout for ${host}:${port}`)
  try {
    bot.end('timeout')
  } finally {
    process.exit(2)
  }
}, 30000)

bot.once('spawn', async () => {
  clearTimeout(timeout)
  console.log(`[connect] spawned as ${bot.username}`)
  console.log(`[connect] version=${bot.version}`)
  console.log(`[connect] position=${bot.entity.position}`)
  bot.chat('/mech help')
  await wait(1500)
  bot.end('connect smoke complete')
})

bot.on('message', message => {
  console.log(`[chat] ${message.toString()}`)
})

bot.on('kicked', reason => {
  clearTimeout(timeout)
  console.error(`[connect] kicked: ${JSON.stringify(reason)}`)
  process.exitCode = 3
})

bot.on('error', error => {
  clearTimeout(timeout)
  console.error(`[connect] error: ${error.stack ?? error.message}`)
  process.exitCode = 4
})

bot.on('end', reason => {
  clearTimeout(timeout)
  console.log(`[connect] ended: ${reason ?? 'no reason'}`)
})

function wait(ms) {
  return new Promise(resolve => setTimeout(resolve, ms))
}
