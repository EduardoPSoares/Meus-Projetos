import { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { Eye, EyeOff, X, RotateCcw } from 'lucide-react'

export default function RoleOverrideWidget() {
  const { isOwner, user, roleOverride, setRoleOverride, ALL_ROLES } = useAuth()
  const [open, setOpen] = useState(false)
  const [selectedRoles, setSelectedRoles] = useState([])

  if (!isOwner) return null

  const toggleRole = (role) => {
    setSelectedRoles(prev =>
      prev.includes(role) ? prev.filter(r => r !== role) : [...prev, role]
    )
  }

  const apply = () => {
    if (selectedRoles.length === 0) {
      setRoleOverride(null)
    } else {
      setRoleOverride([...selectedRoles])
    }
  }

  const reset = () => {
    setSelectedRoles([])
    setRoleOverride(null)
  }

  if (!open) {
    return (
      <button
        onClick={() => setOpen(true)}
        className={`fixed bottom-20 right-6 z-[60] p-3 rounded-full shadow-lg border transition-all duration-200 ${
          roleOverride
            ? 'bg-amber-600 border-amber-400 text-white animate-pulse'
            : 'bg-parchment border-border text-text-secondary hover:text-gold hover:border-gold/30'
        }`}
        title="Simular cargo"
      >
        {roleOverride ? <EyeOff size={20} /> : <Eye size={20} />}
      </button>
    )
  }

  return (
    <div className="fixed bottom-20 right-6 z-[60] w-72 bg-parchment border border-border rounded-xl shadow-2xl overflow-hidden">
      <div className="flex items-center justify-between px-4 py-3 border-b border-border bg-parchment-light/50">
        <div className="flex items-center gap-2">
          <Eye size={16} className="text-gold" />
          <span className="text-sm font-semibold text-text-primary">Simular Visão</span>
        </div>
        <button onClick={() => setOpen(false)} className="text-text-muted hover:text-text-primary">
          <X size={16} />
        </button>
      </div>

      <div className="p-3 max-h-64 overflow-y-auto space-y-1">
        {roleOverride && (
          <div className="mb-2 px-2 py-1.5 rounded-lg bg-amber-500/10 border border-amber-500/20 text-xs text-amber-400 text-center">
            Modo simulação ativo
          </div>
        )}
        <p className="text-[10px] text-text-muted uppercase tracking-widest px-1 mb-1">Selecione os cargos</p>
        {ALL_ROLES.map(role => {
          const isReal = user?.roleKeys?.includes(role)
          const isSelected = selectedRoles.includes(role)
          return (
            <button
              key={role}
              onClick={() => toggleRole(role)}
              className={`w-full flex items-center justify-between px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
                isSelected
                  ? 'bg-gold/15 text-gold border border-gold/30'
                  : 'text-text-secondary hover:bg-parchment-light border border-transparent'
              }`}
            >
              <span>{role}</span>
              {isReal && <span className="text-[9px] text-emerald-400 opacity-60">seu cargo</span>}
            </button>
          )
        })}
      </div>

      <div className="p-3 border-t border-border flex gap-2">
        <button
          onClick={reset}
          className="flex-1 flex items-center justify-center gap-1.5 px-3 py-2 rounded-lg text-xs font-medium text-text-secondary hover:bg-parchment-light border border-border transition-colors"
        >
          <RotateCcw size={12} />
          Resetar
        </button>
        <button
          onClick={apply}
          className="flex-1 px-3 py-2 rounded-lg text-xs font-medium bg-gold/15 text-gold border border-gold/30 hover:bg-gold/25 transition-colors"
        >
          Aplicar
        </button>
      </div>
    </div>
  )
}
