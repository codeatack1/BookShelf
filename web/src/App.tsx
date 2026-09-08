import { useCallback, useEffect, useLayoutEffect, useRef, useState, type ReactNode } from 'react'
import Library from './Library'
import Schedule from './Schedule'
import Settings from './Settings'
import ThemeSelect, { type Mode } from './ThemeSelect'
import BookDetails from './BookDetails'
import Reader from './Reader'
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
  const candidates = [
    document.querySelector<HTMLElement>('.fade-through-enter .library'),
    document.querySelector<HTMLElement>('.fade-through-enter .schedule'),
    document.querySelector<HTMLElement>('.fade-through-enter .settings'),
    document.querySelector<HTMLElement>('.theme-select'),
    document.querySelector<HTMLElement>('.fade-through-exit .library'),
    document.querySelector<HTMLElement>('.fade-through-exit .schedule'),
    document.querySelector<HTMLElement>('.fade-through-exit .settings'),
    document.getElementById('root'),
    document.body,
    document.documentElement,
  ]
  for (const el of candidates) {
    if (!el) continue
    const v = parse(getComputedStyle(el).backgroundColor)
    if (v !== null) return v
  }
  return null
}

async function reportBackground(bg: number): Promise<void> {
  try {
    await fetch('/api/state/background', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ value: String(bg) }),
    })
  } catch {
    /* сервер может быть недоступен — игнорируем */
  }
}

function syncNativeBackground() {
  const bg = getEffectiveBackground()
  if (bg === null) return
  const bridge = (window as unknown as { AndroidBridge?: { setBackgroundColor?: (c: number) => void } }).AndroidBridge
  bridge?.setBackgroundColor?.(bg)
  reportBackground(bg)
}

declare global {
  interface Window {
    __bookshelfBack__?: () => boolean
  }
}

export type Screen =
  | { name: 'main'; tab: AppTab }
  | { name: 'details'; bookId: string }
  | { name: 'reader'; bookId: string; number: number }
  | { name: 'theme' }

interface StackEntry { key: string; screen: Screen }

function FadeThrough({ tab, onOpenThemeSelect, onOpenBook, active }: { tab: AppTab; onOpenThemeSelect: () => void; onOpenBook: (id: string) => void; active: boolean }) {
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
  if (shownTab === 'library') page = <Library onOpenBook={onOpenBook} active={active} />
  else if (shownTab === 'schedule') page = <Schedule />
  else page = <Settings onOpenThemeSelect={onOpenThemeSelect} />

  return (
    <div key={shownTab} className={leaving ? 'fade-through-exit' : 'fade-through-enter'}>
      {page}
    </div>
  )
}

export default function App() {
  const navWide = useMediaQuery('(min-width: 600px)')
  const [themeId, setThemeId] = useState(() => resolveThemeId(readLS(LS_THEME, 'default')))
  const [mode, setMode] = useState<Mode>(() => {
    const m = readLS(LS_MODE, 'system')
    return m === 'light' || m === 'dark' || m === 'system' ? m : 'system'
  })

  const [stack, setStack] = useState<StackEntry[]>(() => {
    if (readLSBool(LS_PICKED)) {
      return [{ key: 's0', screen: { name: 'main', tab: 'library' } }]
    }
    return [{ key: 's0', screen: { name: 'theme' } }]
  })

  const stackRef = useRef(stack)
  useEffect(() => { stackRef.current = stack }, [stack])

  const keyCounterRef = useRef(0)

  const push = useCallback((screen: Screen) => {
    const key = `s${++keyCounterRef.current}`
    setStack(prev => [...prev, { key, screen }])
  }, [])

  const replaceTop = useCallback((screen: Screen) => {
    setStack(prev => {
      if (prev.length === 0) return prev
      const next = [...prev]
      next[next.length - 1] = { ...next[next.length - 1], screen }
      return next
    })
  }, [])

  const goBackOne = useCallback((): boolean => {
    if (stackRef.current.length <= 1) return false
    setStack(prev => prev.slice(0, -1))
    return true
  }, [])

  const openBook = useCallback((bookId: string) => {
    push({ name: 'details', bookId })
  }, [push])

  const openReader = useCallback((bookId: string, number: number) => {
    if (stackRef.current.length > 0 && stackRef.current[stackRef.current.length - 1].screen.name === 'reader') {
      replaceTop({ name: 'reader', bookId, number })
    } else {
      push({ name: 'reader', bookId, number })
    }
  }, [push, replaceTop])

  const openThemeSelect = useCallback(() => {
    push({ name: 'theme' })
  }, [push])

  const openTab = useCallback((next: AppTab) => {
    const last = stackRef.current[stackRef.current.length - 1]
    if (last && last.screen.name === 'main' && last.screen.tab === next) return
    push({ name: 'main', tab: next })
  }, [push])

  const backFnRef = useRef<() => boolean>(() => false)
  backFnRef.current = goBackOne

  useEffect(() => {
    const fn = () => {
      const result = backFnRef.current()
      console.log('[nav] __bookshelfBack__ ->', result)
      return result
    }
    window.__bookshelfBack__ = fn
    return () => {
      window.__bookshelfBack__ = undefined
    }
  }, [])

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
    try {
      localStorage.setItem(LS_THEME, id)
      localStorage.setItem(LS_MODE, m)
      localStorage.setItem(LS_PICKED, '1')
    } catch {
      // ignore storage errors
    }
    setNativeThemeMode(m)
    if (!goBackOne()) {
      replaceTop({ name: 'main', tab: 'library' })
    }
  }, [goBackOne, replaceTop])

  function renderScreen(screen: Screen, active: boolean): ReactNode {
    switch (screen.name) {
      case 'main':
        return (
          <>
            <FadeThrough tab={screen.tab} onOpenThemeSelect={openThemeSelect} onOpenBook={openBook} active={active} />
            {navWide ? (
              <NavRail active={screen.tab} onChange={openTab} />
            ) : (
              <BottomNav active={screen.tab} onChange={openTab} />
            )}
          </>
        )
      case 'details':
        return <BookDetails bookId={screen.bookId} onOpenReader={openReader} onBack={goBackOne} />
      case 'reader':
        return <Reader bookId={screen.bookId} chapterNumber={screen.number} onNavigateChapter={openReader} onBack={goBackOne} />
      case 'theme':
        return <ThemeSelect themeId={themeId} mode={mode} onLiveChange={handleLiveChange} onPick={handlePick} onBack={goBackOne} />
    }
  }

  return (
    <LanguageProvider>
      <div className="screen">
        {stack.map((entry, idx) => {
          const active = idx === stack.length - 1
          return (
            <div key={entry.key} className={active ? 'screen-entry active' : 'screen-entry'} aria-hidden={!active}>
              {renderScreen(entry.screen, active)}
            </div>
          )
        })}
      </div>
    </LanguageProvider>
  )
}
