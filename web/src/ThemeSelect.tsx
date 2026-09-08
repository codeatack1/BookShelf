import { themes } from './themes'
import { useT, type TranslationKey } from './i18n'
import { CheckIcon, CloseIcon } from './components/icons'

export type Mode = 'light' | 'dark' | 'system'

const MODES: { value: Mode; labelKey: TranslationKey }[] = [
  { value: 'light', labelKey: 'themeSelect.modeLight' },
  { value: 'dark', labelKey: 'themeSelect.modeDark' },
  { value: 'system', labelKey: 'themeSelect.modeSystem' },
]

interface ThemeSelectProps {
  themeId: string
  mode: Mode
  onLiveChange: (themeId: string, mode: Mode) => void
  onPick: (themeId: string, mode: Mode) => void
  onBack?: () => void
}

export default function ThemeSelect({ themeId, mode, onLiveChange, onPick, onBack }: ThemeSelectProps) {
  const { t } = useT()

  const previewMode: 'light' | 'dark' =
    mode === 'system'
      ? window.matchMedia('(prefers-color-scheme: dark)').matches
        ? 'dark'
        : 'light'
      : mode

  const handleMode = (m: Mode) => onLiveChange(themeId, m)
  const handleTheme = (id: string) => onLiveChange(id, mode)

  return (
    <div className="theme-select">
      <header className="theme-select__header">
        <div>
          <h1 className="theme-select__title">BookShelf</h1>
          <p className="theme-select__subtitle">{t('themeSelect.subtitle')}</p>
        </div>
        {onBack && (
          <button type="button" className="icon-btn" aria-label={t('common.close')} onClick={onBack}>
            <CloseIcon />
          </button>
        )}
      </header>

      <div className="theme-select__body">
        <div className="segmented" role="radiogroup" aria-label={t('themeSelect.modeAriaLabel')}>
          {MODES.map((m) => (
            <button
              key={m.value}
              type="button"
              role="radio"
              aria-checked={mode === m.value}
              className={`segmented__btn${mode === m.value ? ' is-active' : ''}`}
              onClick={() => handleMode(m.value)}
            >
              {t(m.labelKey)}
            </button>
          ))}
        </div>

        <div className="theme-grid">
          {themes.map((t) => {
            const pal = t.palettes[previewMode]
            const selected = t.id === themeId
            return (
              <button
                key={t.id}
                type="button"
                className={`theme-card${selected ? ' theme-card--selected' : ''}`}
                aria-pressed={selected}
                onClick={() => handleTheme(t.id)}
              >
                <span
                  className="theme-card__swatch"
                  style={{ backgroundColor: pal.surfaceContainerHigh }}
                >
                  <span className="theme-card__blob" style={{ backgroundColor: pal.primary }}>
                    <span
                      className="theme-card__blob-dot"
                      style={{ backgroundColor: pal.onPrimary }}
                    />
                  </span>
                  <span className="theme-card__chip" style={{ backgroundColor: pal.surface }}>
                    <span className="theme-card__chip-dot" style={{ backgroundColor: pal.error }} />
                    <span
                      className="theme-card__chip-bar"
                      style={{ backgroundColor: pal.onSurfaceVariant }}
                    />
                  </span>
                </span>
                <span className="theme-card__name">{t.name}</span>
                {selected && (
                  <span className="theme-card__check" aria-hidden="true">
                    <CheckIcon />
                  </span>
                )}
              </button>
            )
          })}
        </div>
      </div>

      <footer className="theme-select__footer">
        <button type="button" className="btn-primary" onClick={() => onPick(themeId, mode)}>
          {t('themeSelect.continue')}
        </button>
      </footer>
    </div>
  )
}