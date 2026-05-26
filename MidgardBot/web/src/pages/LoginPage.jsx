import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'
import { Sun, Moon } from 'lucide-react'
import { SwordIcon, DiamondIcon, GrassBlockIcon } from '../assets/minecraft-icons'

export default function LoginPage() {
  const { theme, toggleTheme } = useTheme()

  async function handleLogin() {
    try {
      const data = await fetch('/api/auth/login').then(r => r.json())
      window.location.href = data.url
    } catch {
      alert('Erro ao conectar com o servidor. Verifique se o bot está online.')
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-shadow relative overflow-hidden">
      {/* Theme toggle */}
      <button
        onClick={toggleTheme}
        className="absolute top-4 right-4 z-20 p-2.5 rounded-xl bg-parchment/80 backdrop-blur border border-border
                   text-text-secondary hover:text-gold transition-colors"
        title={theme === 'dark' ? 'Tema claro' : 'Tema escuro'}
      >
        {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
      </button>

      {/* Background decorations */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-1/4 left-1/5 w-80 h-80 bg-gold/5 rounded-full blur-[120px]"></div>
        <div className="absolute bottom-1/3 right-1/4 w-60 h-60 bg-rune-blue/5 rounded-full blur-[100px]"></div>
        <div className="absolute top-10 right-20 animate-float opacity-10"><DiamondIcon size={40} /></div>
        <div className="absolute bottom-20 left-16 animate-float opacity-10" style={{ animationDelay: '1s' }}><SwordIcon size={36} /></div>
        <div className="absolute top-1/2 right-10 animate-float opacity-10" style={{ animationDelay: '2s' }}><GrassBlockIcon size={32} /></div>
      </div>

      <div className="rpg-card rpg-glow p-8 sm:p-10 max-w-sm w-full mx-4 text-center relative z-10 animate-fade-in mc-border-top">
        {/* Logo */}
        <div className="mb-6 pt-2">
          <div className="w-18 h-18 mx-auto mb-4 rounded-xl bg-gold/10 border border-gold/25 flex items-center justify-center p-4 animate-pulse-glow">
            <GrassBlockIcon size={40} />
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold text-gold text-shadow-glow">Midgard</h1>
          <p className="text-text-muted text-xs uppercase tracking-widest mt-1">Painel Administrativo</p>
        </div>

        <div className="rpg-divider"></div>

        <p className="text-text-secondary text-sm my-6 leading-relaxed">
          Faça login com sua conta Discord para acessar o painel de administração do servidor.
        </p>

        <button
          onClick={handleLogin}
          className="w-full flex items-center justify-center gap-3 px-6 py-3.5 rounded-lg
                     bg-[#5865F2] hover:bg-[#4752C4] text-white font-semibold text-sm
                     transition-all duration-200 hover:shadow-lg hover:shadow-[#5865F2]/20
                     hover:-translate-y-0.5 active:translate-y-0"
        >
          <svg width="18" height="18" viewBox="0 0 71 55" fill="currentColor">
            <path d="M60.1045 4.8978C55.5792 2.8214 50.7265 1.2916 45.6527 0.41542C45.5603 0.39851 45.468 0.440769 45.4204 0.525289C44.7963 1.6353 44.105 3.0834 43.6209 4.2216C38.1637 3.4046 32.7345 3.4046 27.3892 4.2216C26.905 3.0581 26.1886 1.6353 25.5617 0.525289C25.5141 0.443589 25.4218 0.40133 25.3294 0.41542C20.2584 1.2888 15.4057 2.8186 10.8776 4.8978C10.8384 4.9147 10.8048 4.9429 10.7825 4.9795C1.57795 18.7309 -0.943561 32.1443 0.293408 45.3914C0.299005 45.4562 0.335386 45.5182 0.385761 45.5576C6.45866 50.0174 12.3413 52.7249 18.1147 54.5195C18.2071 54.5477 18.305 54.5139 18.3638 54.4378C19.7295 52.5728 20.9469 50.6063 21.9907 48.5383C22.0523 48.4172 21.9935 48.2735 21.8676 48.2256C19.9366 47.4931 18.0979 46.6 16.3292 45.5858C16.1893 45.5041 16.1781 45.304 16.3eli 45.2082C16.6882 44.9422 17.0623 44.6653 17.4195 44.3854C17.4857 44.3336 17.5753 44.3207 17.6534 44.3512C29.2318 49.6202 41.8127 49.6202 53.2581 44.3512C53.3362 44.3178 53.4258 44.3308 53.4948 44.3826C53.8521 44.6625 54.2262 44.9422 54.5811 45.2082C54.7231 45.304 54.7147 45.5041 54.5748 45.5858C52.8061 46.6197 50.9674 47.4931 49.0336 48.2228C48.9077 48.2707 48.8517 48.4172 48.9133 48.5383C49.9807 50.6034 51.1981 52.5699 52.5378 54.4349C52.5938 54.5139 52.6945 54.5477 52.7869 54.5195C58.5883 52.7249 64.471 50.0174 70.5439 45.5576C70.5971 45.5182 70.6307 45.459 70.6363 45.3942C72.1267 30.0364 68.2175 16.7312 60.1933 4.9823C60.1737 4.9429 60.1401 4.9147 60.1045 4.8978Z"/>
          </svg>
          Entrar com Discord
        </button>

        <p className="text-text-muted text-[10px] mt-6 uppercase tracking-wider">
          Acesso restrito — Equipe Midgard RPG
        </p>
      </div>
    </div>
  )
}
