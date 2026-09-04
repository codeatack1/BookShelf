import type { ComponentType, SVGProps } from 'react'
import { useT, type TranslationKey } from '../i18n'
import {
  BookFilledIcon,
  BookIcon,
  CalendarFilledIcon,
  CalendarIcon,
  SettingsFilledIcon,
  SettingsIcon,
} from './icons'

export type AppTab = 'schedule' | 'library' | 'settings'

interface BottomNavProps {
  active: AppTab
  onChange: (tab: AppTab) => void
}

type TabIcon = ComponentType<SVGProps<SVGSVGElement>>

interface TabDef {
  id: AppTab
  labelKey: TranslationKey
  icon: TabIcon
  iconActive: TabIcon
}

const TABS: ReadonlyArray<TabDef> = [
  { id: 'schedule', labelKey: 'nav.schedule', icon: CalendarIcon, iconActive: CalendarFilledIcon },
  { id: 'library', labelKey: 'nav.library', icon: BookIcon, iconActive: BookFilledIcon },
  { id: 'settings', labelKey: 'nav.settings', icon: SettingsIcon, iconActive: SettingsFilledIcon },
]

export default function BottomNav({ active, onChange }: BottomNavProps) {
  const { t } = useT()
  return (
    <nav className="bottom-nav" aria-label={t('common.sections')} role="tablist">
      {TABS.map((tab) => {
        const selected = tab.id === active
        const Icon = selected ? tab.iconActive : tab.icon
        return (
          <button
            key={tab.id}
            type="button"
            role="tab"
            aria-selected={selected}
            className={`bottom-nav__tab${selected ? ' is-active' : ''}`}
            onClick={() => onChange(tab.id)}
          >
            <span className="bottom-nav__pill" aria-hidden="true" />
            <span className="bottom-nav__icon">
              <Icon />
            </span>
            <span className="bottom-nav__label">{t(tab.labelKey)}</span>
          </button>
        )
      })}
    </nav>
  )
}