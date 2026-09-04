import { useState } from 'react'
import { useT } from './i18n'
import {
  ChevronRightIcon,
  CloseIcon,
  EventNoteIcon,
  InfoIcon,
  PaletteIcon,
  PersonIcon,
  TranslateIcon,
} from './components/icons'

interface SettingsProps {
  onOpenThemeSelect: () => void
}

export default function Settings({ onOpenThemeSelect }: SettingsProps) {
  const { t, lang, setLang } = useT()
  const [aboutOpen, setAboutOpen] = useState(false)
  const [langOpen, setLangOpen] = useState(false)

  return (
    <div className="settings">
      <header className="settings__topbar">
        <h1 className="settings__title">{t('settings.title')}</h1>
      </header>

      <div className="settings__list">
        <button type="button" className="settings-row" onClick={onOpenThemeSelect}>
          <span className="settings-row__icon">
            <PaletteIcon />
          </span>
          <span className="settings-row__label">{t('settings.theme')}</span>
          <span className="settings-row__arrow" aria-hidden="true">
            <ChevronRightIcon />
          </span>
        </button>

        <button type="button" className="settings-row" onClick={() => setLangOpen(true)}>
          <span className="settings-row__icon">
            <TranslateIcon />
          </span>
          <span className="settings-row__label">{t('settings.language')}</span>
          <span className="settings-row__value">
            {lang === 'ru' ? t('common.russian') : t('common.english')}
          </span>
          <span className="settings-row__arrow" aria-hidden="true">
            <ChevronRightIcon />
          </span>
        </button>

        <button type="button" className="settings-row" onClick={() => setAboutOpen(true)}>
          <span className="settings-row__icon">
            <InfoIcon />
          </span>
          <span className="settings-row__label">{t('settings.about')}</span>
          <span className="settings-row__arrow" aria-hidden="true">
            <ChevronRightIcon />
          </span>
        </button>

        <button type="button" className="settings-row" disabled>
          <span className="settings-row__icon">
            <EventNoteIcon />
          </span>
          <span className="settings-row__label">{t('settings.schedule')}</span>
          <span className="settings-row__soon">{t('settings.soon')}</span>
        </button>

        <button type="button" className="settings-row" disabled>
          <span className="settings-row__icon">
            <PersonIcon />
          </span>
          <span className="settings-row__label">{t('settings.account')}</span>
          <span className="settings-row__soon">{t('settings.soon')}</span>
        </button>
      </div>

      {langOpen && (
        <>
          <div className="library__scrim" onClick={() => setLangOpen(false)} aria-hidden="true" />
          <div
            className="settings__dialog"
            role="dialog"
            aria-modal="true"
            aria-label={t('settings.language')}
          >
            <div className="settings__dialog__header">
              <h2 className="settings__dialog__title">{t('settings.language')}</h2>
              <button
                type="button"
                className="icon-btn"
                aria-label={t('common.close')}
                onClick={() => setLangOpen(false)}
              >
                <CloseIcon />
              </button>
            </div>
            <div className="settings__lang-list">
              <button
                type="button"
                className={`sheet-row${lang === 'ru' ? ' is-active' : ''}`}
                onClick={() => {
                  setLang('ru')
                  setLangOpen(false)
                }}
              >
                <span className={`radio-dot${lang === 'ru' ? ' is-active' : ''}`} aria-hidden="true" />
                <span className="sheet-row__label">{t('common.russian')}</span>
              </button>
              <button
                type="button"
                className={`sheet-row${lang === 'en' ? ' is-active' : ''}`}
                onClick={() => {
                  setLang('en')
                  setLangOpen(false)
                }}
              >
                <span className={`radio-dot${lang === 'en' ? ' is-active' : ''}`} aria-hidden="true" />
                <span className="sheet-row__label">{t('common.english')}</span>
              </button>
            </div>
          </div>
        </>
      )}

      {aboutOpen && (
        <>
          <div className="library__scrim" onClick={() => setAboutOpen(false)} aria-hidden="true" />
          <div
            className="settings__dialog"
            role="dialog"
            aria-modal="true"
            aria-label={t('settings.about')}
          >
            <div className="settings__dialog__header">
              <h2 className="settings__dialog__title">{t('settings.about')}</h2>
              <button
                type="button"
                className="icon-btn"
                aria-label={t('common.close')}
                onClick={() => setAboutOpen(false)}
              >
                <CloseIcon />
              </button>
            </div>
            <p className="settings__dialog__text">{t('settings.aboutText')}</p>
            <p className="settings__dialog__version">{t('settings.version')}</p>
            <button type="button" className="btn-primary" onClick={() => setAboutOpen(false)}>
              {t('common.close')}
            </button>
          </div>
        </>
      )}
    </div>
  )
}