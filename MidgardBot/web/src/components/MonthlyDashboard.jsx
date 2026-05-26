import { useState, useEffect, useMemo } from 'react'
import { useApi } from '../hooks/useApi'
import {
  X, Calendar, Users, FileText, Award, TrendingUp,
  ChevronLeft, ChevronRight, Activity, Crown, Star,
  BarChart3, User, Flame
} from 'lucide-react'
import { ShieldPixelIcon } from '../assets/minecraft-icons'

export default function MonthlyDashboard({ onClose }) {
  const api = useApi()
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // Mês selecionado
  const [year, setYear] = useState(() => new Date().getFullYear())
  const [month, setMonth] = useState(() => new Date().getMonth() + 1)

  const monthStr = `${year}-${String(month).padStart(2, '0')}`

  const MONTH_NAMES = [
    'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
    'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
  ]

  useEffect(() => {
    loadDashboard()
    const interval = setInterval(loadDashboard, 30000)
    return () => clearInterval(interval)
  }, [monthStr])

  async function loadDashboard() {
    setLoading(true)
    setError(null)
    try {
      const result = await api.get(`/reports/monthly-dashboard?month=${monthStr}`)
      setData(result)
    } catch (e) {
      setError(e.message || 'Erro ao carregar dashboard')
    } finally {
      setLoading(false)
    }
  }

  function prevMonth() {
    if (month === 1) { setMonth(12); setYear(y => y - 1) }
    else setMonth(m => m - 1)
  }

  function nextMonth() {
    const now = new Date()
    const nextM = month === 12 ? 1 : month + 1
    const nextY = month === 12 ? year + 1 : year
    if (nextY > now.getFullYear() || (nextY === now.getFullYear() && nextM > now.getMonth() + 1)) return
    if (month === 12) { setMonth(1); setYear(y => y + 1) }
    else setMonth(m => m + 1)
  }

  const isCurrentMonth = year === new Date().getFullYear() && month === new Date().getMonth() + 1

  // Calcular max contagem para barras proporcionais
  const maxRoleCount = useMemo(() => {
    if (!data?.byRole) return 1
    return Math.max(1, ...data.byRole.map(r => r.count))
  }, [data])

  const maxAuthorCount = useMemo(() => {
    if (!data?.byAuthor) return 1
    return Math.max(1, ...data.byAuthor.map(a => a.count))
  }, [data])

  const maxDailyCount = useMemo(() => {
    if (!data?.dailyActivity) return 1
    return Math.max(1, ...data.dailyActivity.map(d => d.count))
  }, [data])

  // Gerar todos os dias do mês para o heatmap
  const calendarDays = useMemo(() => {
    if (!data) return []
    const daysInMonth = new Date(year, month, 0).getDate()
    const dailyMap = {}
    if (data.dailyActivity) {
      for (const d of data.dailyActivity) {
        dailyMap[d.date] = d.count
      }
    }
    const days = []
    for (let d = 1; d <= daysInMonth; d++) {
      const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(d).padStart(2, '0')}`
      days.push({ day: d, date: dateStr, count: dailyMap[dateStr] || 0 })
    }
    return days
  }, [data, year, month])

  // Intensidade do heatmap
  function heatColor(count) {
    if (count === 0) return 'bg-parchment-lighter/50'
    const ratio = count / maxDailyCount
    if (ratio <= 0.25) return 'bg-emerald-light/20 border-emerald-light/30'
    if (ratio <= 0.5) return 'bg-emerald-light/40 border-emerald-light/40'
    if (ratio <= 0.75) return 'bg-emerald-light/60 border-emerald-light/50'
    return 'bg-emerald-light/80 border-emerald-light/60'
  }

  function formatDay(dateStr) {
    if (!dateStr) return ''
    const [, , d] = dateStr.split('-')
    return parseInt(d, 10)
  }

  // Ranking medals
  function getMedal(index) {
    if (index === 0) return <Crown size={16} className="text-yellow-400" />
    if (index === 1) return <Crown size={16} className="text-gray-400" />
    if (index === 2) return <Crown size={16} className="text-amber-600" />
    return <span className="text-text-muted text-xs font-bold w-4 text-center">{index + 1}º</span>
  }

  return (
    <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-start justify-center p-4 pt-8 overflow-y-auto animate-backdrop" onClick={onClose}>
      <div
        className="w-full max-w-5xl bg-parchment border border-border rounded-2xl shadow-2xl animate-scale-in overflow-hidden"
        onClick={e => e.stopPropagation()}
      >
        {/* Header */}
        <div className="relative bg-gradient-to-r from-parchment-dark via-parchment to-parchment-dark border-b border-border px-6 py-5">
          <div className="absolute inset-0 bg-gradient-to-r from-gold/5 via-transparent to-gold/5" />
          <div className="relative flex items-center justify-between">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 rounded-xl bg-gold/15 border border-gold/30 flex items-center justify-center">
                <BarChart3 size={24} className="text-gold" />
              </div>
              <div>
                <h2 className="text-xl font-bold text-text-primary">Dashboard Mensal</h2>
                <p className="text-text-muted text-xs mt-0.5">Visão geral das atividades da equipe</p>
              </div>
            </div>
            <button
              onClick={onClose}
              className="p-2 rounded-lg text-text-muted hover:text-text-primary hover:bg-parchment-lighter/50 transition-colors"
            >
              <X size={20} />
            </button>
          </div>

          {/* Seletor de mês */}
          <div className="relative flex items-center justify-center gap-4 mt-4">
            <button
              onClick={prevMonth}
              className="p-2 rounded-lg text-text-muted hover:text-gold hover:bg-gold/10 transition-all"
            >
              <ChevronLeft size={20} />
            </button>
            <div className="flex items-center gap-2 px-5 py-2 rounded-xl bg-parchment-lighter/50 border border-border min-w-[220px] justify-center">
              <Calendar size={16} className="text-gold" />
              <span className="text-text-primary font-bold text-sm">
                {MONTH_NAMES[month - 1]} {year}
              </span>
            </div>
            <button
              onClick={nextMonth}
              disabled={isCurrentMonth}
              className="p-2 rounded-lg text-text-muted hover:text-gold hover:bg-gold/10 transition-all disabled:opacity-30 disabled:cursor-not-allowed"
            >
              <ChevronRight size={20} />
            </button>
          </div>
        </div>

        {/* Content */}
        <div className="p-6">
          {loading && (
            <div className="flex items-center justify-center py-20">
              <div className="flex flex-col items-center gap-3">
                <div className="w-10 h-10 border-2 border-gold/30 border-t-gold rounded-full animate-spin" />
                <span className="text-text-muted text-sm">Carregando dados...</span>
              </div>
            </div>
          )}

          {error && (
            <div className="text-center py-20">
              <FileText size={40} className="mx-auto mb-3 opacity-20 text-text-muted" />
              <p className="text-text-muted">{error}</p>
            </div>
          )}

          {!loading && !error && data && (
            <div className="space-y-6 animate-fade-in">
              {/* Stats Cards */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <StatCard
                  icon={<FileText size={20} />}
                  label="Total de Relatórios"
                  value={data.totalReports}
                  color="gold"
                />
                <StatCard
                  icon={<Users size={20} />}
                  label="Contribuidores"
                  value={data.totalContributors}
                  color="rune-blue"
                />
                <StatCard
                  icon={<Activity size={20} />}
                  label="Dias Ativos"
                  value={`${data.activeDays}/${data.totalDaysInMonth}`}
                  color="emerald"
                />
                <StatCard
                  icon={<TrendingUp size={20} />}
                  label="Média / Dia Ativo"
                  value={data.activeDays > 0 ? (data.totalReports / data.activeDays).toFixed(1) : '0'}
                  color="diamond"
                />
              </div>

              {/* Empty state */}
              {data.totalReports === 0 && (
                <div className="rpg-card p-16 text-center">
                  <FileText size={48} className="mx-auto mb-4 opacity-15 text-text-muted" />
                  <p className="text-text-muted text-lg font-medium">Nenhum relatório neste mês</p>
                  <p className="text-text-muted/70 text-sm mt-1">Selecione outro mês ou registre novas atividades</p>
                </div>
              )}

              {data.totalReports > 0 && (
                <>
                  {/* Grid 2 colunas: Cargos + Ranking */}
                  <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                    {/* Relatórios por Cargo */}
                    <div className="rpg-card overflow-hidden">
                      <div className="px-5 py-4 border-b border-border flex items-center gap-2">
                        <ShieldPixelIcon size={18} className="text-gold" />
                        <h3 className="font-bold text-sm text-text-primary">Por Cargo</h3>
                      </div>
                      <div className="p-5 space-y-3">
                        {data.byRole.map((role, i) => (
                          <div key={role.roleId} className="animate-stagger-in" style={{ animationDelay: `${i * 60}ms` }}>
                            <div className="flex items-center justify-between mb-1.5">
                              <div className="flex items-center gap-2">
                                <div
                                  className="w-3 h-3 rounded-full shrink-0"
                                  style={{ backgroundColor: role.color }}
                                />
                                <span className="text-sm font-semibold" style={{ color: role.color }}>
                                  {role.roleName}
                                </span>
                              </div>
                              <span className="text-text-secondary text-xs font-bold">
                                {role.count} <span className="text-text-muted font-normal">relat.</span>
                              </span>
                            </div>
                            <div className="w-full h-2.5 bg-parchment-lighter/80 rounded-full overflow-hidden">
                              <div
                                className="h-full rounded-full transition-all duration-700 ease-out"
                                style={{
                                  width: `${(role.count / maxRoleCount) * 100}%`,
                                  background: `linear-gradient(90deg, ${role.color}99, ${role.color})`
                                }}
                              />
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>

                    {/* Ranking de Contribuidores */}
                    <div className="rpg-card overflow-hidden">
                      <div className="px-5 py-4 border-b border-border flex items-center gap-2">
                        <Award size={18} className="text-gold" />
                        <h3 className="font-bold text-sm text-text-primary">Ranking de Contribuidores</h3>
                      </div>
                      <div className="p-5 space-y-2 max-h-[360px] overflow-y-auto scrollbar-thin">
                        {data.byAuthor.map((author, i) => (
                          <div
                            key={author.authorId}
                            className={`flex items-center gap-3 p-3 rounded-xl transition-all animate-stagger-in ${
                              i === 0 ? 'bg-yellow-400/8 border border-yellow-400/20' :
                              i === 1 ? 'bg-gray-400/8 border border-gray-400/15' :
                              i === 2 ? 'bg-amber-600/8 border border-amber-600/15' :
                              'bg-parchment-lighter/30 border border-transparent'
                            }`}
                            style={{ animationDelay: `${i * 50}ms` }}
                          >
                            <div className="w-6 flex items-center justify-center shrink-0">
                              {getMedal(i)}
                            </div>
                            {author.authorAvatar ? (
                              <img
                                src={author.authorAvatar}
                                alt=""
                                className="w-9 h-9 rounded-lg border border-border shrink-0"
                              />
                            ) : (
                              <div className="w-9 h-9 rounded-lg bg-parchment-lighter border border-border flex items-center justify-center shrink-0">
                                <User size={14} className="text-text-muted" />
                              </div>
                            )}
                            <div className="flex-1 min-w-0">
                              <p className="text-text-primary text-sm font-semibold truncate">{author.authorName}</p>
                              <p className="text-xs" style={{ color: author.roleColor }}>{author.roleName}</p>
                            </div>
                            <div className="text-right shrink-0">
                              <span className="text-text-primary font-bold text-base">{author.count}</span>
                              <p className="text-text-muted text-[10px]">relat.</p>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>

                  {/* Atividade Diária — Gráfico de barras */}
                  <div className="rpg-card overflow-hidden">
                    <div className="px-5 py-4 border-b border-border flex items-center gap-2">
                      <TrendingUp size={18} className="text-gold" />
                      <h3 className="font-bold text-sm text-text-primary">Atividade Diária</h3>
                    </div>
                    <div className="p-5">
                      <div className="flex items-end gap-[3px] h-36">
                        {calendarDays.map((d, i) => {
                          const height = d.count > 0 ? Math.max(12, (d.count / maxDailyCount) * 100) : 4
                          return (
                            <div
                              key={d.date}
                              className="flex-1 flex flex-col items-center justify-end group relative"
                              style={{ animationDelay: `${i * 15}ms` }}
                            >
                              {/* Tooltip */}
                              <div className="absolute -top-10 left-1/2 -translate-x-1/2 bg-shadow border border-border rounded-lg px-2.5 py-1 text-[10px] whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity z-10 pointer-events-none">
                                <span className="text-text-primary font-semibold">{d.count}</span>
                                <span className="text-text-muted"> relat. em {formatDay(d.date)}/{String(month).padStart(2, '0')}</span>
                              </div>
                              <div
                                className={`w-full rounded-t-sm transition-all duration-500 ease-out cursor-default ${
                                  d.count > 0
                                    ? 'bg-gradient-to-t from-gold/60 to-gold hover:from-gold/80 hover:to-gold-light'
                                    : 'bg-parchment-lighter/60'
                                }`}
                                style={{ height: `${height}%`, animationDelay: `${i * 15}ms` }}
                              />
                            </div>
                          )
                        })}
                      </div>
                      {/* Labels dos dias */}
                      <div className="flex gap-[3px] mt-1.5">
                        {calendarDays.map(d => (
                          <div key={d.date} className="flex-1 text-center">
                            <span className={`text-[8px] ${d.day % 5 === 1 || d.day === calendarDays.length ? 'text-text-muted' : 'text-transparent'}`}>
                              {d.day}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>

                  {/* Calendário Heatmap */}
                  <div className="rpg-card overflow-hidden">
                    <div className="px-5 py-4 border-b border-border flex items-center gap-2">
                      <Flame size={18} className="text-gold" />
                      <h3 className="font-bold text-sm text-text-primary">Mapa de Calor</h3>
                      <div className="flex items-center gap-1.5 ml-auto">
                        <span className="text-text-muted text-[10px]">Menos</span>
                        <div className="w-3 h-3 rounded-sm bg-parchment-lighter/50 border border-border/50" />
                        <div className="w-3 h-3 rounded-sm bg-emerald-light/20 border border-emerald-light/30" />
                        <div className="w-3 h-3 rounded-sm bg-emerald-light/40 border border-emerald-light/40" />
                        <div className="w-3 h-3 rounded-sm bg-emerald-light/60 border border-emerald-light/50" />
                        <div className="w-3 h-3 rounded-sm bg-emerald-light/80 border border-emerald-light/60" />
                        <span className="text-text-muted text-[10px]">Mais</span>
                      </div>
                    </div>
                    <div className="p-5">
                      <div className="grid grid-cols-7 gap-1.5">
                        {/* Cabeçalho dos dias da semana */}
                        {['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'].map(d => (
                          <div key={d} className="text-center text-[10px] text-text-muted font-medium pb-1">{d}</div>
                        ))}
                        {/* Espaços vazios para alinhar o primeiro dia */}
                        {Array.from({ length: new Date(year, month - 1, 1).getDay() }).map((_, i) => (
                          <div key={`empty-${i}`} />
                        ))}
                        {/* Dias do mês */}
                        {calendarDays.map(d => (
                          <div
                            key={d.date}
                            className={`aspect-square rounded-md border flex flex-col items-center justify-center transition-all hover:scale-110 cursor-default group relative ${
                              d.count > 0 ? heatColor(d.count) : 'bg-parchment-lighter/30 border-border/30'
                            }`}
                          >
                            <span className={`text-xs font-medium ${d.count > 0 ? 'text-text-primary' : 'text-text-muted/60'}`}>
                              {d.day}
                            </span>
                            {d.count > 0 && (
                              <span className="text-[8px] text-emerald-light font-bold">{d.count}</span>
                            )}
                            {/* Tooltip */}
                            <div className="absolute -top-8 left-1/2 -translate-x-1/2 bg-shadow border border-border rounded-lg px-2 py-0.5 text-[10px] whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity z-10 pointer-events-none">
                              <span className="text-text-primary font-semibold">{d.count}</span>
                              <span className="text-text-muted"> atividade{d.count !== 1 ? 's' : ''}</span>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>

                  {/* Top 3 Destaque */}
                  {data.byAuthor.length >= 3 && (
                    <div className="rpg-card overflow-hidden">
                      <div className="px-5 py-4 border-b border-border flex items-center gap-2">
                        <Star size={18} className="text-gold" />
                        <h3 className="font-bold text-sm text-text-primary">Destaques do Mês</h3>
                      </div>
                      <div className="p-5">
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                          {data.byAuthor.slice(0, 3).map((author, i) => {
                            const medals = ['🥇', '🥈', '🥉']
                            const bgColors = [
                              'from-yellow-400/10 to-yellow-400/5 border-yellow-400/25',
                              'from-gray-400/10 to-gray-400/5 border-gray-400/20',
                              'from-amber-600/10 to-amber-600/5 border-amber-600/20'
                            ]
                            return (
                              <div
                                key={author.authorId}
                                className={`bg-gradient-to-br ${bgColors[i]} border rounded-xl p-5 text-center animate-stagger-in`}
                                style={{ animationDelay: `${i * 100}ms` }}
                              >
                                <div className="text-3xl mb-3">{medals[i]}</div>
                                {author.authorAvatar ? (
                                  <img
                                    src={author.authorAvatar}
                                    alt=""
                                    className="w-16 h-16 rounded-xl border-2 border-border mx-auto mb-3"
                                  />
                                ) : (
                                  <div className="w-16 h-16 rounded-xl bg-parchment-lighter border-2 border-border flex items-center justify-center mx-auto mb-3">
                                    <User size={24} className="text-text-muted" />
                                  </div>
                                )}
                                <p className="text-text-primary font-bold text-sm">{author.authorName}</p>
                                <p className="text-xs mt-0.5" style={{ color: author.roleColor }}>{author.roleName}</p>
                                <div className="mt-3 flex items-center justify-center gap-1">
                                  <span className="text-2xl font-black text-text-primary">{author.count}</span>
                                  <span className="text-text-muted text-xs">relatórios</span>
                                </div>
                              </div>
                            )
                          })}
                        </div>
                      </div>
                    </div>
                  )}
                </>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

/* ========== Stat Card ========== */
function StatCard({ icon, label, value, color }) {
  const colorMap = {
    gold: { bg: 'bg-gold/10', border: 'border-gold/25', icon: 'text-gold', value: 'text-gold' },
    'rune-blue': { bg: 'bg-rune-blue/10', border: 'border-rune-blue/25', icon: 'text-rune-blue', value: 'text-rune-blue' },
    emerald: { bg: 'bg-emerald-light/10', border: 'border-emerald-light/25', icon: 'text-emerald-light', value: 'text-emerald-light' },
    diamond: { bg: 'bg-diamond/10', border: 'border-diamond/25', icon: 'text-diamond', value: 'text-diamond' },
  }
  const c = colorMap[color] || colorMap.gold

  return (
    <div className={`${c.bg} border ${c.border} rounded-xl p-4 text-center transition-all hover:scale-[1.02]`}>
      <div className={`${c.icon} flex justify-center mb-2 opacity-80`}>{icon}</div>
      <p className={`text-2xl font-black ${c.value}`}>{value}</p>
      <p className="text-text-muted text-[10px] mt-1 uppercase tracking-wider font-medium">{label}</p>
    </div>
  )
}
