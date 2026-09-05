import { useT } from '../i18n'
import { TABS, type AppTab } from './BottomNav'

interface NavRailProps {
  active: AppTab
  onChange: (tab: AppTab) => void
}

export default function NavRail({ active, onChange }: NavRailProps) {
  const { t } = useT()
  const activeIndex = Math.max(
    0,
    TABS.findIndex((tab) => tab.id === active),
  )
  return (
    <nav
      className="nav-rail"
      aria-label={t('common.sections')}
      role="tablist"
      style={{ ['--active' as string]: activeIndex }}
    >
      <div className="nav-rail__indicator" aria-hidden="true" />
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