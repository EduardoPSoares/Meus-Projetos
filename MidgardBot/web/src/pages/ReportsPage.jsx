import { useState, useEffect, useRef } from 'react'
import { useApi } from '../hooks/useApi'
import { useAuth } from '../context/AuthContext'
import {
  FileText, Plus, Calendar, User, ChevronDown, ChevronUp,
  Trash2, Clock, Filter, Send, X, CheckCircle, AlertTriangle,
  Paperclip, Image as ImageIcon, Video, Link2, Eye,
  ZoomIn, ZoomOut, RotateCw, Maximize2, BarChart3, Mic
} from 'lucide-react'
import { ShieldPixelIcon } from '../assets/minecraft-icons'
import MonthlyDashboard from '../components/MonthlyDashboard'
import MeetingsTab from '../components/MeetingsTab'

export default function ReportsPage() {
  const api = useApi()
  const { user } = useAuth()
  const [reports, setReports] = useState([])
  const [roles, setRoles] = useState([])
  const [highestRole, setHighestRole] = useState(null)
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [filterRole, setFilterRole] = useState('')
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)
  const [submitting, setSubmitting] = useState(false)

  // Form state
  const [form, setForm] = useState({ title: '', description: '', roleId: '' })
  const [files, setFiles] = useState([])
  const [links, setLinks] = useState([])
  const [linkInput, setLinkInput] = useState('')
  const fileInputRef = useRef(null)

  // Modal state
  const [modal, setModal] = useState(null)
  const [deleting, setDeleting] = useState(false)
  const [showDashboard, setShowDashboard] = useState(false)
  const [previewImg, setPreviewImg] = useState(null)
  const [activeTab, setActiveTab] = useState('reports') // 'reports' | 'meetings'

  useEffect(() => {
    loadRoles()
  }, [])

  useEffect(() => {
    loadReports()
    const interval = setInterval(loadReports, 30000)
    return () => clearInterval(interval)
  }, [page, filterRole])

  async function loadRoles() {
    try {
      const data = await api.get('/reports/roles')
      setRoles(data.roles || [])
      if (data.highestRole) {
        setHighestRole(data.highestRole)
        setForm(f => ({ ...f, roleId: data.highestRole.id }))
      }
    } catch (e) {
      console.error(e)
    }
  }

  async function loadReports() {
    setLoading(true)
    try {
      let url = `/reports?page=${page}&limit=20`
      if (filterRole) url += `&roleId=${filterRole}`
      const data = await api.get(url)
      setReports(data.reports || [])
      setTotalPages(data.pages || 1)
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  async function handleSubmit(e) {
    e.preventDefault()
    if (!form.title.trim() || !form.description.trim() || !highestRole) return
    setSubmitting(true)
    try {
      if (files.length > 0 || links.length > 0) {
        const fd = new FormData()
        fd.append('title', form.title)
        fd.append('description', form.description)
        fd.append('roleId', highestRole.id)
        if (links.length > 0) fd.append('links', JSON.stringify(links))
        for (const f of files) fd.append('files', f)
        await api.postForm('/reports', fd)
      } else {
        await api.post('/reports', { title: form.title, description: form.description, roleId: highestRole.id })
      }
      setForm({ title: '', description: '', roleId: highestRole.id })
      setFiles([])
      setLinks([])
      setLinkInput('')
      setShowForm(false)
      setPage(1)
      await loadReports()
      setModal({ type: 'success', message: 'Relatório enviado com sucesso!' })
    } catch (err) {
      setModal({ type: 'error', message: err.message || 'Erro ao enviar relatório' })
    } finally {
      setSubmitting(false)
    }
  }

  function handleDelete(id) {
    setModal({ type: 'confirm-delete', message: 'Tem certeza que deseja excluir este relatório?', id })
  }

  async function confirmDelete() {
    if (!modal?.id) return
    setDeleting(true)
    try {
      await api.del(`/reports/${modal.id}`)
      setModal({ type: 'success', message: 'Relatório excluído com sucesso!' })
      await loadReports()
    } catch (err) {
      setModal({ type: 'error', message: err.message || 'Erro ao excluir relatório' })
    } finally {
      setDeleting(false)
    }
  }

  function handleFileSelect(e) {
    const newFiles = Array.from(e.target.files)
    if (files.length + newFiles.length > 5) {
      setModal({ type: 'error', message: 'Máximo de 5 arquivos por relatório' })
      return
    }
    for (const f of newFiles) {
      if (f.size > 10 * 1024 * 1024) {
        setModal({ type: 'error', message: `"${f.name}" excede 10MB` })
        return
      }
    }
    setFiles(prev => [...prev, ...newFiles])
    if (fileInputRef.current) fileInputRef.current.value = ''
  }

  function removeFile(idx) {
    setFiles(prev => prev.filter((_, i) => i !== idx))
  }

  function addLink() {
    const url = linkInput.trim()
    if (!url) return
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
      setModal({ type: 'error', message: 'Link deve começar com http:// ou https://' })
      return
    }
    if (links.length >= 5) {
      setModal({ type: 'error', message: 'Máximo de 5 links por relatório' })
      return
    }
    setLinks(prev => [...prev, url])
    setLinkInput('')
  }

  function removeLink(idx) {
    setLinks(prev => prev.filter((_, i) => i !== idx))
  }



  function getFileIcon(file) {
    if (file.type.startsWith('image/')) return <ImageIcon size={14} />
    if (file.type.startsWith('video/')) return <Video size={14} />
    return <Paperclip size={14} />
  }

  // Agrupar relatórios por cargo
  const grouped = {}
  for (const r of reports) {
    const key = r.roleId || 'sem-cargo'
    if (!grouped[key]) grouped[key] = { roleName: r.roleName || 'Sem Cargo', roleId: r.roleId, reports: [] }
    grouped[key].reports.push(r)
  }

  // Agrupar por data dentro de cada cargo (para a timeline)
  function groupByDate(list) {
    const dateMap = {}
    for (const r of list) {
      const d = dateOnly(r.activityDate)
      if (!dateMap[d]) dateMap[d] = []
      dateMap[d].push(r)
    }
    return Object.entries(dateMap).sort(([a], [b]) => b.localeCompare(a))
  }

  function formatDate(dateStr) {
    if (!dateStr) return '—'
    try {
      // Suporta "YYYY-MM-DD" e "YYYY-MM-DD HH:mm"
      const datePart = dateStr.substring(0, 10)
      const timePart = dateStr.length > 10 ? dateStr.substring(11) : null
      const [y, m, d] = datePart.split('-')
      return timePart ? `${d}/${m}/${y} ${timePart}` : `${d}/${m}/${y}`
    } catch {
      return dateStr
    }
  }

  // Retorna somente a parte da data para agrupar na timeline
  function dateOnly(dateStr) {
    if (!dateStr) return 'Sem data'
    return dateStr.substring(0, 10)
  }

  function formatTimestamp(ts) {
    if (!ts) return ''
    try {
      const d = new Date(ts.replace(' ', 'T'))
      return d.toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
    } catch {
      return ts
    }
  }

  function getRoleColor(roleId) {
    const role = roles.find(r => r.id === roleId)
    return role?.color || '#c8a84e'
  }

  return (
    <div className="space-y-5 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
        <div>
          <h2 className="text-xl font-bold text-text-primary flex items-center gap-2">
            <FileText size={22} className="text-gold" />
            Relatórios da Staff
          </h2>
          <p className="text-text-muted text-xs mt-1">Registre e acompanhe as atividades da equipe</p>
        </div>
        <div className="flex items-center gap-2">
          {activeTab === 'reports' && (
            <>
              <button
                onClick={() => setShowDashboard(true)}
                className="flex items-center gap-2 text-sm px-4 py-2 rounded-lg border border-border
                           text-text-secondary hover:text-gold hover:border-gold/30 transition-all"
              >
                <BarChart3 size={15} />
                Dashboard Mensal
              </button>
              <button
                onClick={() => setShowForm(!showForm)}
                className="rpg-button flex items-center gap-2 text-sm"
              >
                {showForm ? <X size={16} /> : <Plus size={16} />}
                {showForm ? 'Cancelar' : 'Novo Relatório'}
              </button>
            </>
          )}
        </div>
      </div>

      {/* Abas */}
      <div className="rpg-card p-1 flex gap-1">
        <button
          onClick={() => setActiveTab('reports')}
          className={`flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg text-sm font-medium transition-all
            ${activeTab === 'reports'
              ? 'bg-gold/15 text-gold border border-gold/30'
              : 'text-text-muted hover:text-text-secondary hover:bg-parchment-light/50 border border-transparent'
            }`}
        >
          <FileText size={15} />
          Relatórios
        </button>
        <button
          onClick={() => setActiveTab('meetings')}
          className={`flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg text-sm font-medium transition-all
            ${activeTab === 'meetings'
              ? 'bg-gold/15 text-gold border border-gold/30'
              : 'text-text-muted hover:text-text-secondary hover:bg-parchment-light/50 border border-transparent'
            }`}
        >
          <Mic size={15} />
          Reuniões
        </button>
      </div>

      {/* Conteúdo da aba Reuniões */}
      {activeTab === 'meetings' && <MeetingsTab />}

      {/* Conteúdo da aba Relatórios */}
      {activeTab === 'reports' && (<>


      {/* Formulário */}
      {showForm && (
        <form onSubmit={handleSubmit} className="rpg-card p-5 space-y-4 animate-scale-in mc-border-top">
          <h3 className="text-gold font-semibold text-sm uppercase tracking-wider flex items-center gap-2">
            <Send size={14} />
            Registrar Atividade
          </h3>
          <div className="rpg-divider" />

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Cargo (fixo — maior cargo do usuário) */}
            <div>
              <label className="text-text-secondary text-xs font-semibold uppercase tracking-wider mb-1.5 block">Cargo</label>
              {highestRole ? (
                <div
                  className="w-full border rounded-lg px-3 py-2.5 text-sm font-semibold"
                  style={{ background: highestRole.color + '15', borderColor: highestRole.color + '40', color: highestRole.color }}
                >
                  {highestRole.name}
                </div>
              ) : (
                <div className="w-full bg-parchment border border-border rounded-lg px-3 py-2.5 text-sm text-text-muted">
                  Nenhum cargo encontrado
                </div>
              )}
            </div>

            {/* Data — preenchida automaticamente pelo servidor */}
            <div>
              <label className="text-text-secondary text-xs font-semibold uppercase tracking-wider mb-1.5 block">Data & Hora</label>
              <div className="w-full bg-parchment border border-border rounded-lg px-3 py-2.5 text-sm text-text-muted flex items-center gap-2">
                <Clock size={14} />
                Preenchido automaticamente ao enviar
              </div>
            </div>
          </div>

          {/* Título */}
          <div>
            <label className="text-text-secondary text-xs font-semibold uppercase tracking-wider mb-1.5 block">Título</label>
            <input
              type="text"
              value={form.title}
              onChange={e => setForm({ ...form, title: e.target.value })}
              placeholder="Ex: Revisão de 5 whitelists pendentes"
              maxLength={200}
              spellCheck
              lang="pt-BR"
              className="w-full bg-parchment border border-border rounded-lg px-3 py-2.5 text-sm text-text-primary
                         placeholder:text-text-muted/50 focus:border-gold/50 focus:outline-none transition-colors"
              required
            />
          </div>

          {/* Descrição */}
          <div>
            <label className="text-text-secondary text-xs font-semibold uppercase tracking-wider mb-1.5 block">Descrição</label>
            <textarea
              value={form.description}
              onChange={e => setForm({ ...form, description: e.target.value })}
              placeholder="Descreva detalhes da atividade realizada..."
              rows={4}
              maxLength={2000}
              spellCheck
              lang="pt-BR"
              className="w-full bg-parchment border border-border rounded-lg px-3 py-2.5 text-sm text-text-primary
                         placeholder:text-text-muted/50 focus:border-gold/50 focus:outline-none transition-colors resize-none"
              required
            />
            <p className="text-text-muted text-[10px] mt-1 text-right">{form.description.length}/2000</p>
          </div>

          {/* Anexos: Arquivos */}
          <div>
            <label className="text-text-secondary text-xs font-semibold uppercase tracking-wider mb-1.5 block">
              Anexos <span className="text-text-muted font-normal normal-case">(opcional — máx. 5 arquivos, 10MB cada)</span>
            </label>
            <div className="space-y-2">
              {files.length > 0 && (
                <div className="flex flex-wrap gap-2">
                  {files.map((f, i) => (
                    <div key={i} className="flex items-center gap-2 bg-parchment border border-border rounded-lg px-3 py-1.5 text-xs">
                      {getFileIcon(f)}
                      <span className="text-text-secondary max-w-[150px] truncate">{f.name}</span>
                      <span className="text-text-muted">({(f.size / 1024 / 1024).toFixed(1)}MB)</span>
                      <button type="button" onClick={() => removeFile(i)} className="text-text-muted hover:text-blood-light transition-colors">
                        <X size={12} />
                      </button>
                    </div>
                  ))}
                </div>
              )}
              {files.length < 5 && (
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  className="flex items-center gap-2 px-3 py-2 rounded-lg border border-dashed border-border
                             text-text-muted text-xs hover:border-gold/40 hover:text-gold/80 transition-colors"
                >
                  <Paperclip size={14} />
                  Adicionar imagem ou vídeo
                </button>
              )}
              <input
                ref={fileInputRef}
                type="file"
                accept="image/jpeg,image/png,image/gif,image/webp,video/mp4,video/webm"
                multiple
                onChange={handleFileSelect}
                className="hidden"
              />
            </div>
          </div>

          {/* Anexos: Links */}
          <div>
            <label className="text-text-secondary text-xs font-semibold uppercase tracking-wider mb-1.5 block">
              Links <span className="text-text-muted font-normal normal-case">(opcional — máx. 5)</span>
            </label>
            <div className="space-y-2">
              {links.length > 0 && (
                <div className="space-y-1">
                  {links.map((link, i) => (
                    <div key={i} className="flex items-center gap-2 bg-parchment border border-border rounded-lg px-3 py-1.5 text-xs">
                      <Link2 size={12} className="text-blue-400 shrink-0" />
                      <span className="text-blue-400 truncate max-w-[300px]">{link}</span>
                      <button type="button" onClick={() => removeLink(i)} className="text-text-muted hover:text-blood-light transition-colors ml-auto shrink-0">
                        <X size={12} />
                      </button>
                    </div>
                  ))}
                </div>
              )}
              {links.length < 5 && (
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={linkInput}
                    onChange={e => setLinkInput(e.target.value)}
                    onKeyDown={e => e.key === 'Enter' && (e.preventDefault(), addLink())}
                    placeholder="https://exemplo.com"
                    className="flex-1 bg-parchment border border-border rounded-lg px-3 py-2 text-xs text-text-primary
                               placeholder:text-text-muted/50 focus:border-gold/50 focus:outline-none transition-colors"
                  />
                  <button
                    type="button"
                    onClick={addLink}
                    className="px-3 py-2 rounded-lg border border-border text-text-muted text-xs hover:border-gold/40 hover:text-gold/80 transition-colors"
                  >
                    <Plus size={14} />
                  </button>
                </div>
              )}
            </div>
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="rpg-button w-full sm:w-auto flex items-center justify-center gap-2 text-sm disabled:opacity-50"
          >
            <Send size={14} />
            {submitting ? 'Enviando...' : 'Enviar Relatório'}
          </button>
        </form>
      )}

      {/* Filtro por cargo */}
      <div className="rpg-card p-3 flex flex-wrap items-center gap-2 animate-slide-down">
        <Filter size={14} className="text-text-muted" />
        <button
          onClick={() => { setFilterRole(''); setPage(1) }}
          className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all border
            ${!filterRole
              ? 'bg-gold/15 text-gold border-gold/30'
              : 'bg-parchment text-text-muted border-border hover:text-text-secondary hover:border-border-light'
            }`}
        >
          Todos
        </button>
        {roles.map(r => (
          <button
            key={r.id}
            onClick={() => { setFilterRole(r.id); setPage(1) }}
            className="px-3 py-1.5 rounded-lg text-xs font-medium transition-all border"
            style={{
              background: filterRole === r.id ? r.color + '20' : undefined,
              color: filterRole === r.id ? r.color : undefined,
              borderColor: filterRole === r.id ? r.color + '50' : undefined
            }}
          >
            {r.name}
          </button>
        ))}
      </div>

      {/* Loading */}
      {loading && (
        <div className="text-center py-12 text-text-muted">Carregando relatórios...</div>
      )}

      {/* Sem relatórios */}
      {!loading && reports.length === 0 && (
        <div className="rpg-card p-12 text-center">
          <FileText size={40} className="mx-auto mb-3 opacity-20 text-text-muted" />
          <p className="text-text-muted">Nenhum relatório encontrado.</p>
          <p className="text-text-muted text-xs mt-1">Clique em "Novo Relatório" para registrar sua primeira atividade.</p>
        </div>
      )}

      {/* Timeline por cargo */}
      {!loading && Object.entries(grouped).map(([roleId, group]) => (
        <RoleSection
          key={roleId}
          group={group}
          groupByDate={groupByDate}
          formatDate={formatDate}
          formatTimestamp={formatTimestamp}
          getRoleColor={getRoleColor}
          onDelete={handleDelete}
          currentUserId={user?.userId}
          onPreview={setPreviewImg}
        />
      ))}

      {/* Paginação */}
      {!loading && totalPages > 1 && (
        <div className="flex justify-center items-center gap-3">
          <button
            onClick={() => setPage(p => Math.max(1, p - 1))}
            disabled={page <= 1}
            className="rpg-button text-xs disabled:opacity-30"
          >
            Anterior
          </button>
          <span className="text-text-muted text-xs">
            Página {page} de {totalPages}
          </span>
          <button
            onClick={() => setPage(p => Math.min(totalPages, p + 1))}
            disabled={page >= totalPages}
            className="rpg-button text-xs disabled:opacity-30"
          >
            Próxima
          </button>
        </div>
      )}
      {/* Modal */}
      {modal && (
        <div className="fixed inset-0 bg-black/70 z-50 flex items-center justify-center p-4 animate-backdrop" onClick={() => !deleting && setModal(null)}>
          <div className="rpg-card p-6 max-w-sm w-full animate-scale-in mc-border-top text-center" onClick={e => e.stopPropagation()}>
            {modal.type === 'success' && (
              <>
                <div className="w-14 h-14 rounded-full bg-emerald-500/15 border border-emerald-500/30 flex items-center justify-center mx-auto mb-4">
                  <CheckCircle size={28} className="text-emerald-400" />
                </div>
                <h3 className="text-text-primary font-bold text-base mb-1">Sucesso</h3>
                <p className="text-text-secondary text-sm mb-5">{modal.message}</p>
                <button onClick={() => setModal(null)} className="rpg-button text-sm px-6">OK</button>
              </>
            )}
            {modal.type === 'error' && (
              <>
                <div className="w-14 h-14 rounded-full bg-blood/15 border border-blood/30 flex items-center justify-center mx-auto mb-4">
                  <AlertTriangle size={28} className="text-blood-light" />
                </div>
                <h3 className="text-text-primary font-bold text-base mb-1">Erro</h3>
                <p className="text-text-secondary text-sm mb-5">{modal.message}</p>
                <button onClick={() => setModal(null)} className="rpg-button text-sm px-6">OK</button>
              </>
            )}
            {modal.type === 'confirm-delete' && (
              <>
                <div className="w-14 h-14 rounded-full bg-blood/15 border border-blood/30 flex items-center justify-center mx-auto mb-4">
                  <Trash2 size={28} className="text-blood-light" />
                </div>
                <h3 className="text-text-primary font-bold text-base mb-1">Excluir Relatório</h3>
                <p className="text-text-secondary text-sm mb-5">{modal.message}</p>
                <div className="flex items-center justify-center gap-3">
                  <button
                    onClick={() => setModal(null)}
                    disabled={deleting}
                    className="px-5 py-2 rounded-lg text-sm font-medium bg-parchment border border-border text-text-secondary hover:bg-parchment-light transition-colors disabled:opacity-50"
                  >
                    Cancelar
                  </button>
                  <button
                    onClick={confirmDelete}
                    disabled={deleting}
                    className="px-5 py-2 rounded-lg text-sm font-medium bg-blood/80 hover:bg-blood text-white border border-blood transition-colors disabled:opacity-50"
                  >
                    {deleting ? 'Excluindo...' : 'Excluir'}
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      )}

      {/* Preview de imagem com zoom */}
      {previewImg && <ImagePreviewModal src={previewImg} onClose={() => setPreviewImg(null)} />}
      </>)}

      {/* Dashboard Mensal */}
      {showDashboard && <MonthlyDashboard onClose={() => setShowDashboard(false)} />}
    </div>
  )
}

/* ========== Modal de Preview com Zoom ========== */
function ImagePreviewModal({ src, onClose }) {
  const [scale, setScale] = useState(1)
  const [rotate, setRotate] = useState(0)
  const [position, setPosition] = useState({ x: 0, y: 0 })
  const [dragging, setDragging] = useState(false)
  const dragStart = useRef({ x: 0, y: 0 })
  const posStart = useRef({ x: 0, y: 0 })

  const zoomIn = () => setScale(s => Math.min(s + 0.5, 5))
  const zoomOut = () => { setScale(s => { const next = Math.max(s - 0.5, 0.5); if (next <= 1) setPosition({ x: 0, y: 0 }); return next })}
  const resetZoom = () => { setScale(1); setPosition({ x: 0, y: 0 }); setRotate(0) }
  const rotateCw = () => setRotate(r => (r + 90) % 360)

  const handleWheel = (e) => {
    e.preventDefault()
    if (e.deltaY < 0) zoomIn()
    else zoomOut()
  }

  const handleMouseDown = (e) => {
    if (scale <= 1) return
    e.preventDefault()
    setDragging(true)
    dragStart.current = { x: e.clientX, y: e.clientY }
    posStart.current = { ...position }
  }
  const handleMouseMove = (e) => {
    if (!dragging) return
    setPosition({
      x: posStart.current.x + (e.clientX - dragStart.current.x),
      y: posStart.current.y + (e.clientY - dragStart.current.y)
    })
  }
  const handleMouseUp = () => setDragging(false)

  const handleBackdrop = (e) => {
    if (e.target === e.currentTarget) onClose()
  }

  useEffect(() => {
    const handleKey = (e) => {
      if (e.key === 'Escape') onClose()
      if (e.key === '+' || e.key === '=') zoomIn()
      if (e.key === '-') zoomOut()
      if (e.key === '0') resetZoom()
      if (e.key === 'r') rotateCw()
    }
    window.addEventListener('keydown', handleKey)
    return () => window.removeEventListener('keydown', handleKey)
  }, [])

  return (
    <div
      className="fixed inset-0 bg-black/85 z-50 flex items-center justify-center animate-backdrop select-none"
      onClick={handleBackdrop}
      onMouseMove={handleMouseMove}
      onMouseUp={handleMouseUp}
      onMouseLeave={handleMouseUp}
    >
      {/* Toolbar */}
      <div className="absolute top-4 left-1/2 -translate-x-1/2 flex items-center gap-1 bg-shadow/90 border border-border rounded-xl px-2 py-1.5 z-10 backdrop-blur-sm">
        <button onClick={zoomOut} className="p-1.5 rounded-lg text-text-muted hover:text-text-primary hover:bg-parchment-light/20 transition-colors" title="Diminuir (-)">
          <ZoomOut size={18} />
        </button>
        <span className="text-xs text-text-secondary font-medium min-w-[3.5rem] text-center">{Math.round(scale * 100)}%</span>
        <button onClick={zoomIn} className="p-1.5 rounded-lg text-text-muted hover:text-text-primary hover:bg-parchment-light/20 transition-colors" title="Ampliar (+)">
          <ZoomIn size={18} />
        </button>
        <div className="w-px h-5 bg-border mx-1" />
        <button onClick={resetZoom} className="p-1.5 rounded-lg text-text-muted hover:text-text-primary hover:bg-parchment-light/20 transition-colors" title="Resetar (0)">
          <Maximize2 size={18} />
        </button>
        <button onClick={rotateCw} className="p-1.5 rounded-lg text-text-muted hover:text-text-primary hover:bg-parchment-light/20 transition-colors" title="Girar (R)">
          <RotateCw size={18} />
        </button>
        <div className="w-px h-5 bg-border mx-1" />
        <button onClick={onClose} className="p-1.5 rounded-lg text-text-muted hover:text-text-primary hover:bg-parchment-light/20 transition-colors" title="Fechar (Esc)">
          <X size={18} />
        </button>
      </div>

      {/* Imagem */}
      <div
        className="overflow-hidden flex items-center justify-center"
        style={{ width: '100vw', height: '100vh' }}
        onWheel={handleWheel}
      >
        <img
          src={src}
          alt=""
          className="rounded-lg transition-transform duration-150 ease-out"
          style={{
            transform: `translate(${position.x}px, ${position.y}px) scale(${scale}) rotate(${rotate}deg)`,
            maxWidth: '90vw',
            maxHeight: '85vh',
            objectFit: 'contain',
            cursor: scale > 1 ? (dragging ? 'grabbing' : 'grab') : 'default',
          }}
          onMouseDown={handleMouseDown}
          draggable={false}
        />
      </div>
    </div>
  )
}

/* ========== Seção por Cargo ========== */
function RoleSection({ group, groupByDate, formatDate, formatTimestamp, getRoleColor, onDelete, currentUserId, onPreview }) {
  const [expanded, setExpanded] = useState(true)
  const color = getRoleColor(group.roleId)
  const dated = groupByDate(group.reports)

  return (
    <div className="rpg-card overflow-hidden animate-stagger-in" style={{ animationDelay: '100ms' }}>
      {/* Header do cargo */}
      <button
        onClick={() => setExpanded(!expanded)}
        className="w-full flex items-center justify-between p-4 hover:bg-parchment-light/50 transition-colors"
      >
        <div className="flex items-center gap-3">
          <div
            className="w-9 h-9 rounded-lg flex items-center justify-center border"
            style={{ background: color + '18', borderColor: color + '35' }}
          >
            <ShieldPixelIcon size={18} style={{ color }} />
          </div>
          <div className="text-left">
            <h3 className="font-bold text-sm" style={{ color }}>{group.roleName}</h3>
            <p className="text-text-muted text-[10px]">
              {group.reports.length} relatório{group.reports.length !== 1 ? 's' : ''}
            </p>
          </div>
        </div>
        {expanded ? <ChevronUp size={16} className="text-text-muted" /> : <ChevronDown size={16} className="text-text-muted" />}
      </button>

      {/* Timeline */}
      {expanded && (
        <div className="px-4 pb-4 animate-slide-down">
          <div className="rpg-divider" />
          <div className="relative mt-4">
            {/* Linha vertical da timeline */}
            <div
              className="absolute left-[18px] top-0 bottom-0 w-[2px] rounded-full"
              style={{ background: `linear-gradient(to bottom, ${color}40, ${color}10)` }}
            />

            {dated.map(([date, items]) => (
              <div key={date} className="relative mb-6 last:mb-0">
                {/* Marcador de data na timeline */}
                <div className="flex items-center gap-3 mb-3">
                  <div
                    className="relative z-10 w-[38px] h-[38px] rounded-full flex items-center justify-center border-2 shrink-0"
                    style={{ background: color + '20', borderColor: color + '50' }}
                  >
                    <Calendar size={15} style={{ color }} />
                  </div>
                  <div
                    className="px-3 py-1 rounded-lg text-xs font-bold border"
                    style={{ background: color + '10', borderColor: color + '25', color }}
                  >
                    {formatDate(date)}
                  </div>
                </div>

                {/* Cards de relatórios nessa data */}
                <div className="ml-[48px] space-y-3">
                  {items.map((report, ri) => (
                    <div
                      key={report.id}
                      className="bg-parchment rounded-lg p-4 border border-border hover:border-border-light transition-all group animate-stagger-in"
                      style={{ animationDelay: `${ri * 60}ms` }}
                    >
                      <div className="flex items-start justify-between gap-3">
                        <div className="flex items-start gap-3 min-w-0">
                          {report.authorAvatar ? (
                            <img
                              src={report.authorAvatar}
                              alt=""
                              className="w-8 h-8 rounded-lg border border-border shrink-0 mt-0.5"
                            />
                          ) : (
                            <div className="w-8 h-8 rounded-lg bg-parchment-lighter border border-border flex items-center justify-center shrink-0 mt-0.5">
                              <User size={14} className="text-text-muted" />
                            </div>
                          )}
                          <div className="min-w-0">
                            <h4 className="text-text-primary font-semibold text-sm leading-tight">
                              {report.title}
                            </h4>
                            <p className="text-text-secondary text-xs mt-0.5">
                              {report.authorDisplayName || report.authorName}
                            </p>
                          </div>
                        </div>
                        <div className="flex items-center gap-2 shrink-0">
                          <span className="text-text-muted text-[10px] flex items-center gap-1 whitespace-nowrap">
                            <Clock size={10} />
                            {formatTimestamp(report.createdAt)}
                          </span>
                          {currentUserId === report.authorId && (
                            <button
                              onClick={() => onDelete(report.id)}
                              className="opacity-0 group-hover:opacity-100 p-1 rounded hover:bg-blood/20 text-text-muted hover:text-blood-light transition-all"
                              title="Excluir relatório"
                            >
                              <Trash2 size={13} />
                            </button>
                          )}
                        </div>
                      </div>
                      <p className="text-text-secondary text-xs mt-3 leading-relaxed whitespace-pre-wrap break-words">
                        {report.description}
                      </p>

                      {/* Anexos */}
                      {report.attachments && report.attachments.length > 0 && (
                        <div className="mt-3 pt-3 border-t border-border/50 space-y-2">
                          {/* Imagens */}
                          {report.attachments.filter(a => a.type === 'image').length > 0 && (
                            <div className="flex flex-wrap gap-2">
                              {report.attachments.filter(a => a.type === 'image').map(att => (
                                <button
                                  key={att.id}
                                  onClick={() => onPreview(`/api/reports/uploads/${att.filename}`)}
                                  className="relative group/img rounded-lg overflow-hidden border border-border hover:border-gold/30 transition-all"
                                >
                                  <img
                                    src={`/api/reports/uploads/${att.filename}`}
                                    alt={att.originalName}
                                    className="w-20 h-20 object-cover"
                                    loading="lazy"
                                  />
                                  <div className="absolute inset-0 bg-black/0 group-hover/img:bg-black/40 transition-colors flex items-center justify-center">
                                    <Eye size={16} className="text-white opacity-0 group-hover/img:opacity-100 transition-opacity" />
                                  </div>
                                </button>
                              ))}
                            </div>
                          )}

                          {/* Vídeos */}
                          {report.attachments.filter(a => a.type === 'video').map(att => (
                            <div key={att.id} className="rounded-lg overflow-hidden border border-border">
                              <video
                                src={`/api/reports/uploads/${att.filename}`}
                                controls
                                preload="metadata"
                                className="max-w-full max-h-48 rounded-lg"
                              />
                            </div>
                          ))}

                          {/* Links */}
                          {report.attachments.filter(a => a.type === 'link').map(att => (
                            <a
                              key={att.id}
                              href={att.url}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="flex items-center gap-2 text-xs text-blue-400 hover:text-blue-300 transition-colors"
                            >
                              <Link2 size={12} className="shrink-0" />
                              <span className="truncate">{att.url}</span>
                            </a>
                          ))}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
