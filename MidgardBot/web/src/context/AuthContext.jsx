import { createContext, useContext, useState, useCallback, useEffect, useRef } from 'react'

const AuthContext = createContext(null)

const OWNER_ID = '1261729412710137876'
const ALL_ROLES = ['FUNDADOR', 'CEOO', 'ADMIN', 'DEV', 'DEV_JR', 'MODERADOR', 'LOREMAKER', 'AJUDANTE', 'BUILDER', 'CINEGRAFISTA', 'INTERPRETE', 'STAFF']

export function AuthProvider({ children }) {
  const [token, setToken] = useState(localStorage.getItem('midgard_token'))
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('midgard_user')
    return saved ? JSON.parse(saved) : null
  })
  const [roleOverride, setRoleOverride] = useState(null)

  const login = useCallback((tokenValue, userData) => {
    localStorage.setItem('midgard_token', tokenValue)
    localStorage.setItem('midgard_user', JSON.stringify(userData))
    setToken(tokenValue)
    setUser(userData)
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('midgard_token')
    localStorage.removeItem('midgard_user')
    setToken(null)
    setUser(null)
  }, [])

  // Ao iniciar, buscar dados atualizados do usuário (inclui roleKeys)
  const validatingRef = useRef(false)
  useEffect(() => {
    if (!token || validatingRef.current) return
    validatingRef.current = true
    fetch('/api/auth/me', { headers: { Authorization: `Bearer ${token}` } })
      .then(r => {
        if (!r.ok) throw new Error('Token inválido')
        return r.json()
      })
      .then(data => {
        const updated = {
          userId: data.userId,
          username: data.username,
          avatarUrl: data.avatarUrl,
          roleKeys: data.roleKeys || []
        }
        localStorage.setItem('midgard_user', JSON.stringify(updated))
        setUser(updated)
      })
      .catch(() => {
        // Token expirado ou inválido
        localStorage.removeItem('midgard_token')
        localStorage.removeItem('midgard_user')
        setToken(null)
        setUser(null)
      })
      .finally(() => {
        validatingRef.current = false
      })
  }, [token])

  /**
   * Verifica se o usuário possui algum dos roleKeys informados.
   * Ex: hasRole('MODERADOR', 'DEV', 'CEOO', 'FUNDADOR')
   */
  const hasRole = useCallback((...keys) => {
    if (!user?.roleKeys) return false
    const activeRoles = roleOverride || user.roleKeys
    return keys.some(k => activeRoles.includes(k))
  }, [user, roleOverride])

  const isOwner = user?.userId === OWNER_ID

  return (
    <AuthContext.Provider value={{ token, user, login, logout, hasRole, isOwner, roleOverride, setRoleOverride, ALL_ROLES }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
