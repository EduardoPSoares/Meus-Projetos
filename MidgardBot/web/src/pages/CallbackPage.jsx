import { useEffect, useState, useRef } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function CallbackPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const { login } = useAuth()
  const [error, setError] = useState(null)
  const processed = useRef(false)

  useEffect(() => {
    if (processed.current) return
    const code = searchParams.get('code')
    const state = searchParams.get('state')
    if (!code || !state) {
      navigate('/login')
      return
    }
    processed.current = true

    fetch('/api/auth/callback', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code, state })
    })
      .then(async r => {
        if (r.status === 403) {
          const body = await r.json().catch(() => ({}))
          setError(body.error || 'Acesso restrito à equipe do servidor')
          return null
        }
        if (!r.ok) throw new Error('Falha na autenticação')
        return r.json()
      })
      .then(data => {
        if (!data) return
        login(data.token, {
          userId: data.userId,
          username: data.username,
          avatarUrl: data.avatarUrl,
          roleKeys: data.roleKeys || []
        })
        navigate('/')
      })
      .catch(() => {
        processed.current = false
        navigate('/login')
      })
  }, [searchParams, navigate, login])

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-shadow">
        <div className="rpg-card p-8 max-w-sm w-full mx-4 text-center animate-fade-in">
          <div className="w-16 h-16 mx-auto mb-4 rounded-xl bg-blood/20 border border-blood/30 flex items-center justify-center">
            <span className="text-3xl">🛡️</span>
          </div>
          <h2 className="text-xl font-bold text-blood-light mb-2">Acesso Negado</h2>
          <p className="text-text-secondary text-sm mb-6">{error}</p>
          <p className="text-text-muted text-xs mb-4">
            Apenas membros da equipe com cargo autorizado podem acessar o painel.
          </p>
          <button
            onClick={() => navigate('/login')}
            className="rpg-button w-full"
          >
            Voltar ao Login
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-shadow">
      <div className="text-center animate-fade-in">
        <div className="w-16 h-16 mx-auto mb-4 rounded-xl bg-gold/20 border border-gold/30 flex items-center justify-center animate-pulse-glow">
          <span className="text-3xl">⚔️</span>
        </div>
        <p className="text-text-secondary">Autenticando...</p>
      </div>
    </div>
  )
}
