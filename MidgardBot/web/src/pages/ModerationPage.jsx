import { useState, useEffect, useRef } from 'react'
import { useApi } from '../hooks/useApi'
import { Shield, Ban, AlertTriangle, Clock, User, Plus, X, Upload, Send, Search, Download } from 'lucide-react'

function TypeBadge({ type }) {
  const map = {
    ban: { label: 'Ban', cls: 'rpg-badge-red' },
    tempban: { label: 'Tempban', cls: 'rpg-badge-red' },
    warn: { label: 'Warn', cls: 'rpg-badge-gold' },
    mute: { label: 'Mute', cls: 'rpg-badge-blue' },
    kick: { label: 'Kick', cls: 'rpg-badge-gold' },
  }
  const s = map[type] || { label: type, cls: 'rpg-badge-blue' }
  return <span className={`rpg-badge ${s.cls}`}>{s.label}</span>
}

function SeverityBadge({ severity }) {
  const map = {
    leve: { label: 'Leve', cls: 'rpg-badge-gold' },
    'média': { label: 'Média', cls: 'rpg-badge-blue' },
    pesada: { label: 'Pesada', cls: 'rpg-badge-red' },
  }
  const s = map[severity] || { label: severity || 'N/A', cls: 'rpg-badge-blue' }
  return <span className={`rpg-badge ${s.cls}`}>{s.label}</span>
}

function parseSeverity(reason) {
  if (!reason) return { severity: '', cleanReason: reason || '' }
  const m = reason.match(/^\[(leve|média|pesada)\]\s*/i)
  if (m) return { severity: m[1].toLowerCase(), cleanReason: reason.slice(m[0].length) }
  return { severity: '', cleanReason: reason }
}

export default function ModerationPage() {
  const api = useApi()
  const [punishments, setPunishments] = useState([])
  const [stats, setStats] = useState(null)
  const [filter, setFilter] = useState('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)
  const [loading, setLoading] = useState(true)
  const [showModal, setShowModal] = useState(false)
  const [tab, setTab] = useState('panel') // 'panel' ou 'plugin'
  const [importing, setImporting] = useState(false)
  const [importResult, setImportResult] = useState(null)
  const [selectedPunishment, setSelectedPunishment] = useState(null)

  useEffect(() => {
    load()
    const interval = setInterval(load, 30000)
    return () => clearInterval(interval)
  }, [filter, page, tab, search])
  useEffect(() => {
    loadStats()
    const interval = setInterval(loadStats, 30000)
    return () => clearInterval(interval)
  }, [tab])

  async function load() {
    setLoading(true)
    try {
      const params = new URLSearchParams({ page, limit: 15, source: tab })
      if (filter) params.set('type', filter)
      if (search.trim()) params.set('search', search.trim())
      const data = await api.get(`/punishments?${params}`)
      setPunishments(data.punishments || [])
      setTotalPages(data.pages || 1)
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  async function loadStats() {
    try {
      const data = await api.get(`/punishments/stats?source=${tab}`)
      setStats(data)
    } catch (e) {
      console.error(e)
    }
  }

  function switchTab(newTab) {
    setTab(newTab)
    setFilter('')
    setSearch('')
    setPage(1)
  }

  return (
    <div className="space-y-4 sm:space-y-6">
      {/* Tabs */}
      <div className="flex gap-2 border-b border-border-subtle pb-2 animate-slide-down">
        <button
          onClick={() => switchTab('panel')}
          className={`px-4 py-2 text-sm font-medium rounded-t transition-colors ${
            tab === 'panel'
              ? 'bg-bg-card text-gold-primary border-b-2 border-gold-primary'
              : 'text-text-muted hover:text-text-secondary'
          }`}
        >
          <AlertTriangle size={14} className="inline mr-1.5 -mt-0.5" />
          Nossas punições
        </button>
        <button
          onClick={() => switchTab('plugin')}
          className={`px-4 py-2 text-sm font-medium rounded-t transition-colors ${
            tab === 'plugin'
              ? 'bg-bg-card text-gold-primary border-b-2 border-gold-primary'
              : 'text-text-muted hover:text-text-secondary'
          }`}
        >
          <Shield size={14} className="inline mr-1.5 -mt-0.5" />
          Punições padrão do Discord
        </button>
      </div>
      {/* Stats */}
      {stats && (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
          {stats.byType && Object.entries(stats.byType).map(([type, count], i) => (
            <div key={type} className="rpg-card p-3 sm:p-4 text-center group hover:scale-[1.02] transition-transform animate-stagger-in" style={{ animationDelay: `${i * 60}ms` }}>
              <p className="text-xl sm:text-2xl font-bold text-text-primary">{count}</p>
              <p className="text-text-muted text-[10px] sm:text-xs uppercase mt-1">{type}s</p>
            </div>
          ))}
          <div className="rpg-card p-3 sm:p-4 text-center animate-stagger-in" style={{ animationDelay: '300ms' }}>
            <p className="text-xl sm:text-2xl font-bold text-blood-light">{stats.active || 0}</p>
            <p className="text-text-muted text-[10px] sm:text-xs uppercase mt-1">Ativos</p>
          </div>
        </div>
      )}

      {/* Filters + New Punishment Button */}
      <div className="flex flex-wrap items-center gap-2 animate-slide-down">
        {tab === 'plugin' && ['', 'ban', 'tempban', 'warn', 'kick', 'mute'].map(f => (
          <button
            key={f}
            onClick={() => { setFilter(f); setPage(1) }}
            className={`rpg-button-secondary text-xs sm:text-sm
              ${filter === f ? 'active' : ''}`}
          >
            {f || 'Todos'}
          </button>
        ))}
        <div className="relative w-full sm:w-80">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" />
          <input
            type="text"
            value={search}
            onChange={e => {
              setSearch(e.target.value)
              setPage(1)
            }}
            placeholder={tab === 'panel'
              ? 'Buscar por jogador, moderador, ID ou motivo...'
              : 'Buscar por jogador, moderador, ID ou motivo...'}
            className="rpg-input w-full pl-9 py-2 text-xs sm:text-sm"
          />
        </div>
        <div className="ml-auto flex items-center gap-2">
          {tab === 'panel' && (
            <>
              <button
                onClick={async () => {
                  if (!confirm('Importar punições existentes do fórum do Discord para o banco de dados?')) return
                  setImporting(true)
                  setImportResult(null)
                  try {
                    const res = await api.post('/punishments/import-forum')
                    setImportResult(res)
                    load()
                    loadStats()
                  } catch (err) {
                    setImportResult({ error: err.message || 'Erro na importação' })
                  } finally {
                    setImporting(false)
                  }
                }}
                disabled={importing}
                className="rpg-button-secondary text-xs sm:text-sm flex items-center gap-1.5 disabled:opacity-50"
              >
                <Download size={14} /> {importing ? 'Importando...' : 'Importar do Fórum'}
              </button>
              <button
                onClick={() => setShowModal(true)}
                className="rpg-button text-xs sm:text-sm flex items-center gap-1.5"
              >
                <Plus size={16} /> Nova Punição
              </button>
            </>
          )}
        </div>
      </div>

      {/* Import Result */}
      {importResult && (
        <div className={`rpg-card p-3 text-sm animate-fade-in ${importResult.error ? 'border-blood/50' : 'border-emerald-500/50'} border`}>
          {importResult.error ? (
            <p className="text-blood-light">{importResult.error}</p>
          ) : (
            <p className="text-emerald-400">
              {importResult.message}
            </p>
          )}
          <button onClick={() => setImportResult(null)} className="text-text-muted text-xs mt-1 hover:text-text-secondary">Fechar</button>
        </div>
      )}

      {/* Table */}
      {loading ? (
        <div className="text-center py-12 text-text-muted">Carregando...</div>
      ) : punishments.length === 0 ? (
        <div className="rpg-card p-12 text-center">
          <Shield size={40} className="mx-auto text-text-muted mb-3 opacity-50" />
          <p className="text-text-muted">Nenhuma punição encontrada.</p>
        </div>
      ) : (
        <div className="rpg-card overflow-hidden animate-fade-in">
          <div className="table-responsive">
            <table className="rpg-table">
              <thead>
                <tr>
                  {tab === 'panel' ? <th>Severidade</th> : <th>Tipo</th>}
                  <th>Jogador</th>
                  <th className="hide-mobile">Moderador</th>
                  <th className="hide-mobile">Motivo</th>
                  <th>Data</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {punishments.map((p) => {
                  const { severity, cleanReason } = parseSeverity(p.reason)
                  return (
                  <tr key={p.id} onClick={() => setSelectedPunishment(p)} className="cursor-pointer hover:bg-bg-hover transition-colors">
                    {tab === 'panel' ? (
                      <td><SeverityBadge severity={severity} /></td>
                    ) : (
                      <td><TypeBadge type={p.type} /></td>
                    )}
                    <td>
                      <div className="flex items-center gap-2">
                        <img
                          src={p.targetAvatar || `https://cdn.discordapp.com/embed/avatars/${(parseInt(p.targetDiscordId) || 0) % 5}.png`}
                          alt="" className="w-6 h-6 rounded-full flex-shrink-0"
                        />
                        <span className="text-text-primary text-sm">{p.targetDiscordName || p.targetName || p.targetDiscordId}</span>
                      </div>
                    </td>
                    <td className="hide-mobile">
                      <div className="flex items-center gap-2">
                        <img
                          src={p.moderatorAvatar || `https://cdn.discordapp.com/embed/avatars/${(parseInt(p.moderatorId) || 0) % 5}.png`}
                          alt="" className="w-6 h-6 rounded-full flex-shrink-0"
                        />
                        <span className="text-text-secondary text-sm">{p.moderatorName || p.moderatorId}</span>
                      </div>
                    </td>
                    <td className="text-text-secondary text-sm max-w-xs truncate hide-mobile">{tab === 'panel' ? cleanReason || '—' : p.reason || '—'}</td>
                    <td className="text-text-muted text-xs">{formatDate(p.startTime)}</td>
                    <td>
                      {p.active === false || p.removedBy ? (
                        <span className="rpg-badge rpg-badge-green">Removida</span>
                      ) : p.endTime && p.endTime > 0 && p.endTime <= Date.now() ? (
                        <span className="rpg-badge rpg-badge-blue">Expirada</span>
                      ) : (
                        <span className="rpg-badge rpg-badge-red">Ativa</span>
                      )}
                    </td>
                  </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex justify-center gap-2">
          <button
            onClick={() => setPage(p => Math.max(1, p - 1))}
            disabled={page <= 1}
            className="rpg-button text-sm disabled:opacity-40"
          >
            ← Anterior
          </button>
          <span className="px-4 py-2 text-text-muted text-sm">
            Página {page} de {totalPages}
          </span>
          <button
            onClick={() => setPage(p => Math.min(totalPages, p + 1))}
            disabled={page >= totalPages}
            className="rpg-button text-sm disabled:opacity-40"
          >
            Próxima →
          </button>
        </div>
      )}

      {/* Modal Nova Punição */}
      {showModal && (
        <NewPunishmentModal
          api={api}
          onClose={() => setShowModal(false)}
          onSuccess={() => { setShowModal(false); load(); loadStats() }}
        />
      )}

      {/* Modal Detalhes da Punição */}
      {selectedPunishment && (
        <PunishmentDetailModal
          punishment={selectedPunishment}
          tab={tab}
          api={api}
          onClose={() => setSelectedPunishment(null)}
          onRevoked={() => { setSelectedPunishment(null); load(); loadStats() }}
        />
      )}
    </div>
  )
}

function PunishmentDetailModal({ punishment: p, tab, api, onClose, onRevoked }) {
  const { severity, cleanReason } = parseSeverity(p.reason)
  const isPanel = tab === 'panel'
  const [confirmRevoke, setConfirmRevoke] = useState(false)
  const [revokeReason, setRevokeReason] = useState('')
  const [revoking, setRevoking] = useState(false)
  const [revokeError, setRevokeError] = useState('')

  const isActive = p.active !== false && !p.removedBy &&
    !(p.endTime && p.endTime > 0 && p.endTime <= Date.now())

  const status = p.active === false || p.removedBy
    ? { label: 'Removida', cls: 'rpg-badge-green' }
    : p.endTime && p.endTime > 0 && p.endTime <= Date.now()
      ? { label: 'Expirada', cls: 'rpg-badge-blue' }
      : { label: 'Ativa', cls: 'rpg-badge-red' }

  async function handleRevoke() {
    setRevoking(true)
    setRevokeError('')
    try {
      await api.put(`/punishments/${p.id}/revoke`, { reason: revokeReason.trim() })
      onRevoked()
    } catch (err) {
      setRevokeError(err.message || 'Erro ao revogar punição')
    } finally {
      setRevoking(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/70 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="rpg-card w-full max-w-md animate-scale-in" onClick={e => e.stopPropagation()}>
        <div className="flex items-center justify-between p-4 border-b border-border-subtle">
          <h2 className="text-lg font-bold text-text-primary flex items-center gap-2">
            <Shield size={20} className="text-blood-light" /> Detalhes da Punição
          </h2>
          <button onClick={onClose} className="text-text-muted hover:text-text-primary transition-colors">
            <X size={20} />
          </button>
        </div>

        <div className="p-4 space-y-4">
          {/* Jogador */}
          <div className="flex items-center gap-3">
            <img
              src={p.targetAvatar || `https://cdn.discordapp.com/embed/avatars/${(parseInt(p.targetDiscordId) || 0) % 5}.png`}
              alt="" className="w-12 h-12 rounded-full border-2 border-border-subtle"
            />
            <div>
              <p className="text-text-primary font-semibold">{p.targetDiscordName || p.targetName || p.targetDiscordId}</p>
              <p className="text-text-muted text-xs">ID: {p.targetDiscordId}</p>
            </div>
            <div className="ml-auto">
              <span className={`rpg-badge ${status.cls}`}>{status.label}</span>
            </div>
          </div>

          {/* Info grid */}
          <div className="grid grid-cols-2 gap-3">
            <div className="rpg-card p-3 bg-bg-dark/30">
              <p className="text-text-muted text-[10px] uppercase mb-1">{isPanel ? 'Severidade' : 'Tipo'}</p>
              {isPanel ? <SeverityBadge severity={severity} /> : <TypeBadge type={p.type} />}
            </div>
            <div className="rpg-card p-3 bg-bg-dark/30">
              <p className="text-text-muted text-[10px] uppercase mb-1">Data</p>
              <p className="text-text-primary text-sm">{formatDate(p.startTime)}</p>
            </div>
          </div>

          {/* Moderador */}
          <div className="rpg-card p-3 bg-bg-dark/30">
            <p className="text-text-muted text-[10px] uppercase mb-2">Moderador</p>
            <div className="flex items-center gap-2">
              <img
                src={p.moderatorAvatar || `https://cdn.discordapp.com/embed/avatars/${(parseInt(p.moderatorId) || 0) % 5}.png`}
                alt="" className="w-8 h-8 rounded-full"
              />
              <span className="text-text-primary text-sm font-medium">{p.moderatorName || p.moderatorId}</span>
            </div>
          </div>

          {/* Motivo */}
          <div className="rpg-card p-3 bg-bg-dark/30">
            <p className="text-text-muted text-[10px] uppercase mb-1">Motivo</p>
            <p className="text-text-secondary text-sm whitespace-pre-wrap">{isPanel ? cleanReason || '—' : p.reason || '—'}</p>
          </div>

          {/* Remoção (se aplicável) */}
          {p.removedBy && (
            <div className="rpg-card p-3 bg-bg-dark/30 border border-emerald-500/20">
              <p className="text-text-muted text-[10px] uppercase mb-1">Removida por</p>
              <p className="text-emerald-400 text-sm">{p.removedBy}</p>
              {p.removedReason && <p className="text-text-secondary text-sm mt-1">{p.removedReason}</p>}
            </div>
          )}

          {/* Expiração (se aplicável) */}
          {p.endTime && p.endTime > 0 && (
            <div className="rpg-card p-3 bg-bg-dark/30">
              <p className="text-text-muted text-[10px] uppercase mb-1">Expira em</p>
              <p className="text-text-secondary text-sm">{formatDate(p.endTime)}</p>
            </div>
          )}

          {/* Revogar punição */}
          {isActive && !confirmRevoke && (
            <button
              onClick={() => setConfirmRevoke(true)}
              className="w-full py-2.5 rounded-lg border border-blood/30 text-blood-light hover:bg-blood/10 transition-colors text-sm font-medium flex items-center justify-center gap-2"
            >
              <Ban size={14} /> Revogar Punição
            </button>
          )}

          {isActive && confirmRevoke && (
            <div className="rpg-card p-3 border border-blood/30 space-y-3 animate-fade-in">
              <p className="text-blood-light text-sm font-semibold">Confirmar revogação</p>
              <p className="text-text-muted text-xs">Esta ação não pode ser desfeita. A punição será marcada como removida.</p>
              <textarea
                value={revokeReason}
                onChange={e => setRevokeReason(e.target.value)}
                placeholder="Motivo da revogação (opcional)..."
                className="rpg-input w-full h-16 resize-none text-sm"
                maxLength={500}
              />
              {revokeError && <p className="text-blood-light text-xs">{revokeError}</p>}
              <div className="flex gap-2">
                <button
                  onClick={handleRevoke}
                  disabled={revoking}
                  className="flex-1 py-2 rounded-lg bg-blood/20 border border-blood/40 text-blood-light hover:bg-blood/30 transition-colors text-sm font-medium disabled:opacity-50"
                >
                  {revoking ? 'Revogando...' : 'Confirmar Revogação'}
                </button>
                <button
                  onClick={() => { setConfirmRevoke(false); setRevokeReason(''); setRevokeError('') }}
                  className="px-4 py-2 rounded-lg border border-border-subtle text-text-muted hover:text-text-secondary transition-colors text-sm"
                >
                  Cancelar
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function NewPunishmentModal({ api, onClose, onSuccess }) {
  const [targetId, setTargetId] = useState('')
  const [severity, setSeverity] = useState('leve')
  const [reason, setReason] = useState('')
  const [image, setImage] = useState(null)
  const [imagePreview, setImagePreview] = useState(null)
  const [sending, setSending] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const fileRef = useRef()

  // Busca de membros
  const [memberSearch, setMemberSearch] = useState('')
  const [memberResults, setMemberResults] = useState([])
  const [searchLoading, setSearchLoading] = useState(false)
  const [selectedMember, setSelectedMember] = useState(null)
  const searchTimeout = useRef(null)

  function handleMemberSearch(query) {
    setMemberSearch(query)
    setSelectedMember(null)
    setTargetId('')
    if (searchTimeout.current) clearTimeout(searchTimeout.current)
    if (!query || query.length < 2) {
      setMemberResults([])
      return
    }
    searchTimeout.current = setTimeout(async () => {
      setSearchLoading(true)
      try {
        const data = await api.get('/staff')
        const members = (data.staff || [])
          .filter(m => (m.displayName || m.name || '').toLowerCase().includes(query.toLowerCase()))
          .slice(0, 10)
          .map(m => ({ id: m.discordId, name: m.displayName || m.name, avatar: m.avatar }))
        setMemberResults(members)
      } catch {
        setMemberResults([])
      } finally {
        setSearchLoading(false)
      }
    }, 400)
  }

  function selectMember(member) {
    setSelectedMember(member)
    setTargetId(member.id)
    setMemberSearch('')
    setMemberResults([])
  }

  function handleImageChange(e) {
    const file = e.target.files[0]
    if (!file) return
    if (!file.type.startsWith('image/')) {
      setError('Somente imagens são permitidas')
      return
    }
    if (file.size > 10 * 1024 * 1024) {
      setError('Imagem excede 10MB')
      return
    }
    setImage(file)
    setImagePreview(URL.createObjectURL(file))
    setError('')
  }

  function clearImage() {
    setImage(null)
    setImagePreview(null)
    if (fileRef.current) fileRef.current.value = ''
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setSuccess('')

    if (!targetId) {
      setError('Selecione um usuário alvo')
      return
    }
    if (!reason.trim()) {
      setError('Informe o motivo')
      return
    }

    setSending(true)
    try {
      const form = new FormData()
      form.append('targetId', targetId)
      form.append('type', 'warn')
      form.append('severity', severity)
      form.append('reason', reason.trim())
      if (image) form.append('image', image)

      const res = await api.postForm('/punishments/forum', form)
      setSuccess(res.message || 'Punição publicada com sucesso!')
      setTimeout(onSuccess, 1200)
    } catch (err) {
      setError(err.message || 'Erro ao publicar punição')
    } finally {
      setSending(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/70 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="rpg-card w-full max-w-lg max-h-[90vh] overflow-y-auto animate-scale-in" onClick={e => e.stopPropagation()}>
        <div className="flex items-center justify-between p-4 border-b border-border-subtle">
          <h2 className="text-lg font-bold text-text-primary flex items-center gap-2">
            <Shield size={20} className="text-blood-light" /> Nova Advertência
          </h2>
          <button onClick={onClose} className="text-text-muted hover:text-text-primary transition-colors">
            <X size={20} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-4 space-y-4">
          {/* Busca de usuário */}
          <div>
            <label className="block text-sm text-text-secondary mb-1">Usuário Alvo</label>
            {selectedMember ? (
              <div className="flex items-center gap-2 rpg-card p-2">
                {selectedMember.avatar && (
                  <img src={selectedMember.avatar} alt="" className="w-8 h-8 rounded-full" />
                )}
                <span className="text-text-primary text-sm font-medium">{selectedMember.name}</span>
                <span className="text-text-muted text-xs">({selectedMember.id})</span>
                <button type="button" onClick={() => { setSelectedMember(null); setTargetId('') }}
                  className="ml-auto text-text-muted hover:text-blood-light">
                  <X size={14} />
                </button>
              </div>
            ) : (
              <div className="relative">
                <div className="flex items-center gap-2">
                  <div className="relative flex-1">
                    <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" />
                    <input
                      type="text"
                      value={memberSearch}
                      onChange={e => handleMemberSearch(e.target.value)}
                      placeholder="Buscar membro por nome..."
                      className="rpg-input pl-9 w-full"
                    />
                  </div>
                  <span className="text-text-muted text-xs">ou</span>
                  <input
                    type="text"
                    value={targetId}
                    onChange={e => { setTargetId(e.target.value); setSelectedMember(null) }}
                    placeholder="ID do Discord"
                    className="rpg-input w-40"
                  />
                </div>
                {memberResults.length > 0 && (
                  <div className="absolute z-10 mt-1 w-full rpg-card max-h-48 overflow-y-auto shadow-lg">
                    {memberResults.map(m => (
                      <button key={m.id} type="button" onClick={() => selectMember(m)}
                        className="w-full flex items-center gap-2 p-2 hover:bg-bg-hover text-left transition-colors">
                        {m.avatar && <img src={m.avatar} alt="" className="w-6 h-6 rounded-full" />}
                        <span className="text-text-primary text-sm">{m.name}</span>
                      </button>
                    ))}
                  </div>
                )}
                {searchLoading && (
                  <div className="absolute z-10 mt-1 w-full rpg-card p-3 text-center text-text-muted text-sm">
                    Buscando...
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Severidade */}
          <div>
            <label className="block text-sm text-text-secondary mb-1">Severidade</label>
            <div className="flex gap-2">
              {['leve', 'média', 'pesada'].map(s => (
                <button key={s} type="button" onClick={() => setSeverity(s)}
                  className={`rpg-button-secondary text-sm flex-1 capitalize ${severity === s ? 'active' : ''}`}>
                  {s}
                </button>
              ))}
            </div>
          </div>

          {/* Motivo */}
          <div>
            <label className="block text-sm text-text-secondary mb-1">Motivo</label>
            <textarea
              value={reason}
              onChange={e => setReason(e.target.value)}
              placeholder="Descreva o motivo da punição..."
              className="rpg-input w-full h-24 resize-none"
              maxLength={1500}
            />
            <p className="text-text-muted text-xs text-right mt-0.5">{reason.length}/1500</p>
          </div>

          {/* Upload de imagem */}
          <div>
            <label className="block text-sm text-text-secondary mb-1">Imagem (opcional)</label>
            {imagePreview ? (
              <div className="relative">
                <img src={imagePreview} alt="Preview" className="w-full max-h-40 object-contain rounded border border-border-subtle" />
                <button type="button" onClick={clearImage}
                  className="absolute top-1 right-1 bg-bg-dark/80 p-1 rounded-full text-text-muted hover:text-blood-light">
                  <X size={14} />
                </button>
              </div>
            ) : (
              <button type="button" onClick={() => fileRef.current?.click()}
                className="rpg-card w-full p-4 border-2 border-dashed border-border-subtle hover:border-gold-primary text-center transition-colors cursor-pointer">
                <Upload size={24} className="mx-auto text-text-muted mb-1" />
                <p className="text-text-muted text-sm">Clique para adicionar uma imagem</p>
              </button>
            )}
            <input ref={fileRef} type="file" accept="image/*" onChange={handleImageChange} className="hidden" />
          </div>

          {/* Preview da mensagem */}
          <div className="rpg-card p-3 bg-bg-dark/30">
            <p className="text-text-muted text-xs mb-1">Preview da mensagem:</p>
            <p className="text-text-secondary text-sm">
              🛑 O Usuário <span className="text-gold-primary">@{selectedMember?.name || targetId || '???'}</span> está recebendo um(a){' '}
              <strong>Advertência {severity}</strong>{' '}
              após uma infração de Regras in-game.
            </p>
            {reason && <p className="text-text-secondary text-sm mt-1"><strong>Motivo:</strong> {reason}</p>}
          </div>

          {error && <p className="text-blood-light text-sm">{error}</p>}
          {success && <p className="text-emerald-400 text-sm">{success}</p>}

          <button type="submit" disabled={sending}
            className="rpg-button w-full flex items-center justify-center gap-2 disabled:opacity-50">
            {sending ? 'Publicando...' : <><Send size={16} /> Publicar no Fórum</>}
          </button>
        </form>
      </div>
    </div>
  )
}

function formatDate(ts) {
  if (!ts) return '—'
  return new Date(ts).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}
