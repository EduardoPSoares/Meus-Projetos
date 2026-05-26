import { useState, useEffect } from 'react'
import { useApi } from '../hooks/useApi'
import {
  Users, Server, Clock, Zap, ScrollText, Shield, Ticket,
  TrendingUp, Activity, Cpu, HardDrive
} from 'lucide-react'
import { HeartIcon, DiamondIcon, SwordIcon, GrassBlockIcon } from '../assets/minecraft-icons'

function StatCard({ icon: Icon, mcIcon: McIcon, label, value, sub, variant = 'gold', delay }) {
  const colors = {
    gold: 'text-gold border-gold/20 bg-gold/8',
    green: 'text-emerald-light border-emerald-light/20 bg-emerald-light/8',
    red: 'text-blood-light border-blood-light/20 bg-blood-light/8',
    blue: 'text-rune-blue-light border-rune-blue-light/20 bg-rune-blue-light/8',
  }

  return (
    <div className="rpg-card p-4 sm:p-5 group hover:scale-[1.02] transition-transform animate-stagger-in" style={{ animationDelay: `${delay || 0}ms` }}>
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-text-muted text-[10px] sm:text-xs font-semibold uppercase tracking-wider mb-1">{label}</p>
          <p className="text-xl sm:text-2xl font-bold text-text-primary">{value}</p>
          {sub && <p className="text-text-muted text-[10px] sm:text-xs mt-1">{sub}</p>}
        </div>
        <div className={`p-2 sm:p-2.5 rounded-lg border ${colors[variant]} flex-shrink-0`}>
          {McIcon ? <McIcon size={20} /> : <Icon size={20} />}
        </div>
      </div>
    </div>
  )
}

function ProgressBar({ label, value, max, color = '#c8a84e' }) {
  const percent = max > 0 ? Math.round((value / max) * 100) : 0
  return (
    <div className="space-y-2">
      <div className="flex justify-between text-xs">
        <span className="text-text-secondary font-medium">{label}</span>
        <span className="text-text-muted">{value} / {max} MB</span>
      </div>
      <div className="mc-progress">
        <div
          className="mc-progress-bar"
          style={{ width: `${percent}%`, background: `linear-gradient(180deg, ${color}, ${color}dd)` }}
        />
      </div>
      <p className="text-right text-[10px] text-text-muted">{percent}%</p>
    </div>
  )
}

export default function DashboardPage() {
  const api = useApi()
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadDashboard()
    const interval = setInterval(loadDashboard, 15000)
    return () => clearInterval(interval)
  }, [])

  async function loadDashboard() {
    try {
      const result = await api.get('/dashboard')
      setData(result)
    } catch (e) {
      console.error('Erro ao carregar dashboard:', e)
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-center">
          <div className="animate-pulse-glow w-14 h-14 rounded-xl bg-gold/15 border border-gold/25 flex items-center justify-center mx-auto mb-3">
            <GrassBlockIcon size={24} />
          </div>
          <p className="text-text-muted text-sm">Carregando dados...</p>
        </div>
      </div>
    )
  }

  if (!data) return <p className="text-text-muted">Erro ao carregar dados.</p>

  const uptime = formatUptime(data.bot?.uptime || 0)

  return (
    <div className="space-y-5 sm:space-y-6">
      {/* Bot info banner */}
      <div className="rpg-card rpg-glow p-5 sm:p-6 mc-border-top animate-slide-up">
        <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4">
          {data.bot?.avatar && (
            <img src={data.bot.avatar} alt="" className="w-14 h-14 rounded-xl border-2 border-gold/30 shadow-lg" />
          )}
          <div className="flex-1 min-w-0">
            <h3 className="text-lg sm:text-xl font-bold text-text-primary">{data.bot?.name}</h3>
            <div className="flex flex-wrap items-center gap-2 sm:gap-3 mt-1.5">
              <span className="rpg-badge rpg-badge-green">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-light animate-pulse mr-1.5"></span>
                {data.bot?.status}
              </span>
              <span className="text-text-muted text-xs flex items-center gap-1"><Zap size={10} />{data.bot?.ping}ms</span>
              <span className="text-text-muted text-xs">•</span>
              <span className="text-text-muted text-xs flex items-center gap-1"><Clock size={10} />{uptime}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Stats grid */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
        <StatCard icon={Users} label="Membros" value={data.bot?.users ?? 0} variant="blue" delay={0} />
        <StatCard icon={Server} label="Servidores" value={data.bot?.guilds ?? 0} variant="gold" delay={80} />
        <StatCard icon={ScrollText} label="WL Pendentes" value={data.whitelist?.pending ?? 0} variant="gold"
          sub={`${data.whitelist?.approved ?? 0} aprovadas`} delay={160} />
        <StatCard icon={Zap} label="Ping" value={`${data.bot?.ping ?? 0}ms`} variant="green" delay={240} />
      </div>

      {/* Second row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-3 sm:gap-4">
        {/* Whitelist summary */}
        <div className="rpg-card p-4 sm:p-5 animate-stagger-in" style={{ animationDelay: '200ms' }}>
          <h4 className="text-gold text-xs sm:text-sm font-semibold mb-4 flex items-center gap-2">
            <ScrollText size={16} />
            Resumo Whitelist
          </h4>
          <div className="space-y-3">
            <div className="flex justify-between items-center">
              <span className="text-text-secondary text-sm">Pendentes</span>
              <span className="rpg-badge rpg-badge-gold">{data.whitelist?.pending ?? 0}</span>
            </div>
            <div className="rpg-divider"></div>
            <div className="flex justify-between items-center">
              <span className="text-text-secondary text-sm">Aprovadas</span>
              <span className="rpg-badge rpg-badge-green">{data.whitelist?.approved ?? 0}</span>
            </div>
            <div className="rpg-divider"></div>
            <div className="flex justify-between items-center">
              <span className="text-text-secondary text-sm">Rejeitadas</span>
              <span className="rpg-badge rpg-badge-red">{data.whitelist?.rejected ?? 0}</span>
            </div>
          </div>
        </div>

        {/* Server info */}
        <div className="rpg-card p-4 sm:p-5 animate-stagger-in" style={{ animationDelay: '300ms' }}>
          <h4 className="text-gold text-xs sm:text-sm font-semibold mb-4 flex items-center gap-2">
            <Server size={16} />
            Servidor Minecraft
          </h4>
          <div className="space-y-3">
            <div className="flex justify-between items-center">
              <span className="text-text-secondary text-sm">IP</span>
              <span className="text-text-primary text-sm font-mono bg-shadow/50 px-2 py-0.5 rounded">{data.server?.ip ?? 'N/A'}</span>
            </div>
            <div className="rpg-divider"></div>
            <div className="flex justify-between items-center">
              <span className="text-text-secondary text-sm">Porta</span>
              <span className="text-text-primary text-sm font-mono bg-shadow/50 px-2 py-0.5 rounded">{data.server?.port ?? 'N/A'}</span>
            </div>
          </div>
        </div>

        {/* Memory */}
        <div className="rpg-card p-4 sm:p-5 animate-stagger-in" style={{ animationDelay: '400ms' }}>
          <h4 className="text-gold text-xs sm:text-sm font-semibold mb-4 flex items-center gap-2">
            <Cpu size={16} />
            Memória JVM
          </h4>
          <div className="space-y-4">
            <ProgressBar
              label="Heap Utilizada"
              value={data.memory?.used ?? 0}
              max={data.memory?.max ?? 1}
              color="#c8a84e"
            />
            <ProgressBar
              label="Heap Alocada"
              value={data.memory?.total ?? 0}
              max={data.memory?.max ?? 1}
              color="#4a7fb5"
            />
          </div>
        </div>
      </div>
    </div>
  )
}

function formatUptime(ms) {
  const s = Math.floor(ms / 1000)
  const d = Math.floor(s / 86400)
  const h = Math.floor((s % 86400) / 3600)
  const m = Math.floor((s % 3600) / 60)
  if (d > 0) return `${d}d ${h}h ${m}m`
  if (h > 0) return `${h}h ${m}m`
  return `${m}m`
}
