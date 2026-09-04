import { useState } from 'react'
import { useT, type TranslationKey } from '../i18n'
import { ArrowDownIcon, ArrowUpIcon, CheckIcon, CloseIcon } from './icons'
import {
  DISPLAY_OPTIONS,
  FILTER_LABELS,
  SORT_OPTIONS,
  type DisplayMode,
  type FiltersState,
  type SortDir,
  type SortKey,
  type TriState,
} from '../libraryTypes'

interface FilterSheetProps {
  filters: FiltersState
  sortKey: SortKey
  sortDir: SortDir
  display: DisplayMode
  onFiltersChange: (filters: FiltersState) => void
  onSortKeyChange: (key: SortKey) => void
  onSortDirChange: (dir: SortDir) => void
  onDisplayChange: (mode: DisplayMode) => void
  onClose: () => void
}

type SheetTab = 'filter' | 'sort' | 'display'

const TABS: { value: SheetTab; labelKey: TranslationKey }[] = [
  { value: 'filter', labelKey: 'filterSheet.tabs.filter' },
  { value: 'sort', labelKey: 'filterSheet.tabs.sort' },
  { value: 'display', labelKey: 'filterSheet.tabs.display' },
]

function nextTri(state: TriState): TriState {
  if (state === 'unset') return 'include'
  if (state === 'include') return 'exclude'
  return 'unset'
}

function defaultDirFor(key: SortKey): SortDir {
  return key === 'lastRead' || key === 'dateAdded' ? 'desc' : 'asc'
}

export default function FilterSheet({
  filters,
  sortKey,
  sortDir,
  display,
  onFiltersChange,
  onSortKeyChange,
  onSortDirChange,
  onDisplayChange,
  onClose,
}: FilterSheetProps) {
  const [tab, setTab] = useState<SheetTab>('filter')
  const { t } = useT()

  return (
    <>
      <div className="library__scrim" onClick={onClose} aria-hidden="true" />
      <section className="library__sheet" role="dialog" aria-label={t('common.filters')} aria-modal="true">
        <header className="library__sheet__header">
          <div className="library__sheet__tabs" role="tablist">
            {TABS.map((tb) => (
              <button
                key={tb.value}
                type="button"
                role="tab"
                aria-selected={tab === tb.value}
                className={`library__sheet__tab${tab === tb.value ? ' is-active' : ''}`}
                onClick={() => setTab(tb.value)}
              >
                <span className="library__sheet__tab-label">{t(tb.labelKey)}</span>
                <span className="library__sheet__tab-indicator" aria-hidden="true" />
              </button>
            ))}
          </div>
          <button type="button" className="icon-btn" aria-label={t('common.close')} onClick={onClose}>
            <CloseIcon />
          </button>
        </header>

        <div className="library__sheet__body">
          {tab === 'filter' && (
            <ul className="sheet-list">
              {FILTER_LABELS.map((f) => {
                const state = filters[f.key]
                return (
                  <li key={f.key}>
                    <button
                      type="button"
                      className="sheet-row"
                      onClick={() => onFiltersChange({ ...filters, [f.key]: nextTri(state) })}
                    >
                      <span
                        className={`tri-box${state !== 'unset' ? ' is-set' : ''} tri-box--${state}`}
                        aria-hidden="true"
                      >
                        {state === 'include' && <CheckIcon />}
                        {state === 'exclude' && <CloseIcon />}
                      </span>
                      <span className="sheet-row__label">{t(f.labelKey)}</span>
                      <span className="sheet-row__hint">
                        {state === 'unset'
                          ? t('filterSheet.triState.unset')
                          : state === 'include'
                            ? t('filterSheet.triState.include')
                            : t('filterSheet.triState.exclude')}
                      </span>
                    </button>
                  </li>
                )
              })}
            </ul>
          )}

          {tab === 'sort' && (
            <ul className="sheet-list">
              {SORT_OPTIONS.map((o) => {
                const active = sortKey === o.key
                const hasDir = o.key !== 'random'
                return (
                  <li key={o.key}>
                    <button
                      type="button"
                      className={`sheet-row${active ? ' is-active' : ''}`}
                      onClick={() => {
                        if (active && hasDir) {
                          onSortDirChange(sortDir === 'asc' ? 'desc' : 'asc')
                        } else {
                          onSortKeyChange(o.key)
                          if (hasDir) onSortDirChange(defaultDirFor(o.key))
                        }
                      }}
                    >
                      <span className="sheet-row__label">{t(o.labelKey)}</span>
                      {hasDir && (
                        <span className="sheet-row__arrow" aria-hidden="true">
                          {sortDir === 'asc' ? <ArrowUpIcon /> : <ArrowDownIcon />}
                        </span>
                      )}
                    </button>
                  </li>
                )
              })}
            </ul>
          )}

          {tab === 'display' && (
            <ul className="sheet-list">
              {DISPLAY_OPTIONS.map((o) => {
                const active = display === o.value
                return (
                  <li key={o.value}>
                    <button
                      type="button"
                      className={`sheet-row${active ? ' is-active' : ''}`}
                      onClick={() => onDisplayChange(o.value)}
                    >
                      <span className={`radio-dot${active ? ' is-active' : ''}`} aria-hidden="true" />
                      <span className="sheet-row__label">{t(o.labelKey)}</span>
                    </button>
                  </li>
                )
              })}
            </ul>
          )}
        </div>
      </section>
    </>
  )
}