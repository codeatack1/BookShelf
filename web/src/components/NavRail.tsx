import { useT } from '../i18n'
import { TABS, type AppTab } from './BottomNav'

interface NavRailProps {
  active: AppTab
  onChange: (tab: AppTab) => void
}

export default function NavRail({ active, onChange }: NavRailProps) {
  const { t } = useT()
  return (
    <nav className="nav-rail" aria-label={t('common.sections')} role="tablist">
      {TABS.map((tab) => {
        const selected = tab.id === active
        const Icon = selected ? tab.iconActive : tab.icon
        return (
          <button
            key={tab.id}
            type="button"
            role="tab"
            aria-selected={selected}
            className={`nav-rail__tab${selected ? ' is-active' : ''}`}
            onClick={() => onChange(tab.id)}
          >
            <span className="nav-rail__pill" aria-hidden="true" />
            <span className="nav-rail__icon">
              <Icon />
            </span>
            <span className="nav-rail__label">{t(tab.labelKey)}</span>
          </button>
        )
      })}
    </nav>
  )
}