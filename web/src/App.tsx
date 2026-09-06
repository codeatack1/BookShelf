import { useCallback, useEffect, useLayoutEffect, useState, type ReactNode } from 'react'
import Library from './Library'
import Schedule from './Schedule'
import Settings from './Settings'
import ThemeSelect, { type Mode } from './ThemeSelect'
import BottomNav, { type AppTab } from './components/BottomNav'
import NavRail from './components/NavRail'
import { LanguageProvider } from './i18n'
import { themes } from './themes'
import { useMediaQuery } from './useMediaQuery'

const LS_THEME = 'bs-theme'
const LS_MODE = 'bs-mode'
const LS_PICKED = 'bs-theme-picked'

function readLS(key: string, fallback: string): string {
  try {
    return localStorage.getItem(key) ?? fallback
  } catch {
    return fallback
  }
}

function readLSBool(key: string): boolean {
  try {
    return localStorage.getItem(key) === '1'
  } catch {
    return false
  }
}

function resolveThemeId(id: string): string {
  if (themes.some((t) => t.id === id)) return id
  return themes[0]?.id ?? 'default'
}

function resolveMode(mode: Mode): 'light' | 'dark' {
  if (mode !== 'system') return mode
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function setNativeThemeMode(mode: string) {
  const bridge = (window as unknown as { AndroidBridge?: { setThemeMode?: (m: string) => void } }).AndroidBridge
  bridge?.setThemeMode?.(mode)
}

function getEffectiveBackground(): number | null {
  const parse = (s: string): number | null => {
    const m = s.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*([\d.]+))?\)/)
    if (!m) return null
    const a = m[4] === undefined ? 1 : parseFloat(m[4])
    if (a <= 0.99) return null
    const [r, g, b] = [parseInt(m[1]), parseInt(m[2]), parseInt(m[3])]
    return ((0xff << 24) | (r << 16) | (g << 8) | b) >>> 0
  }
  const candidates = [document.body, document.documentElement, document.getElementById('root')]
  for (const el of candidates) {
    if (!el) continue
    const v = parse(getComputedStyle(el).backgroundColor)
    if (v !== null) return v
  }
  return null
}

function syncNativeBackground() {
  const bg = getEffectiveBackground()
  if (bg === null) return
  const bridge = (window as unknown as { AndroidBridge?: { setBackgroundColor?: (c: number) => void } }).AndroidBridge
  bridge?.setBackgroundColor?.(bg)
}

function FadeThrough({ tab, onOpenThemeSelect }: { tab: AppTab; onOpenThemeSelect: () => void }) {
  const [shownTab, setShownTab] = useState<AppTab>(tab)
  const [leaving, setLeaving] = useState(false)

  useEffect(() => {
    if (shownTab === tab) {
      setLeaving(false)
      return
    }
    setLeaving(true)
    const t = setTimeout(() => {
      setShownTab(tab)
      setLeaving(false)
    }, 70)
    return () => clearTimeout(t)
  }, [tab, shownTab])

  let page: ReactNode
  if (shownTab === 'library') page = <Library />
  else if (shownTab === 'schedule') page = <Schedule />
  else page = <Settings onOpenThemeSelect={onOpenThemeSelect} />

  return (
    <div key={shownTab} className={leaving ? 'fade-through-exit' : 'fade-through-enter'}>
      {page}
    </div>
  )
}

export default function App() {
  const [everPicked, setEverPicked] = useState(() => readLSBool(LS_PICKED))
  const [showMain, setShowMain] = useState(() => readLSBool(LS_PICKED))
  const [tab, setTab] = useState<AppTab>('library')
  const navWide = useMediaQuery('(min-width: 600px)')
  const [themeId, setThemeId] = useState(() => resolveThemeId(readLS(LS_THEME, 'default')))
  const [mode, setMode] = useState<Mode>(() => {
    const m = readLS(LS_MODE, 'system')
    return m === 'light' || m === 'dark' || m === 'system' ? m : 'system'
  })

  useLayoutEffect(() => {
    document.documentElement.dataset.theme = resolveThemeId(themeId)
    document.documentElement.dataset.mode = resolveMode(mode)
    setNativeThemeMode(mode)
    syncNativeBackground()
  }, [themeId, mode])

  useEffect(() => {
    if (mode !== 'system') return
    const mq = window.matchMedia('(prefers-color-scheme: dark)')
    const onChange = () => {
      document.documentElement.dataset.mode = mq.matches ? 'dark' : 'light'
      syncNativeBackground()
    }
    mq.addEventListener('change', onChange)
    return () => mq.removeEventListener('change', onChange)
  }, [mode])

  const handleLiveChange = useCallback((id: string, m: Mode) => {
    setThemeId(id)
    setMode(m)
  }, [])

  const handlePick = useCallback((id: string, m: Mode) => {
    setThemeId(id)
    setMode(m)
    setEverPicked(true)
    setShowMain(true)
    try {
      localStorage.setItem(LS_THEME, id)
      localStorage.setItem(LS_MODE, m)
      localStorage.setItem(LS_PICKED, '1')
    } catch {
      // ignore storage errors
    }
    setNativeThemeMode(m)
    syncNativeBackground()
  }, [])

  const openThemeSelect = useCallback(() => setShowMain(false), [])
  const closeThemeSelect = useCallback(() => setShowMain(true), [])

  return (
    <LanguageProvider>
      <div className="screen">
        {showMain ? (
          <>
            <FadeThrough tab={tab} onOpenThemeSelect={openThemeSelect} />
            {navWide ? (
              <NavRail active={tab} onChange={setTab} />
            ) : (
              <BottomNav active={tab} onChange={setTab} />
            )}
          </>
        ) : (
          <ThemeSelect
            themeId={themeId}
            mode={mode}
            onLiveChange={handleLiveChange}
            onPick={handlePick}
            onClose={everPicked ? closeThemeSelect : undefined}
          />
        )}
      </div>
    </LanguageProvider>
  )
}