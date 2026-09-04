import { useT } from '../i18n'
import { CloseIcon, FilterListIcon, SearchIcon } from './icons'

interface LibraryTopBarProps {
  total: number
  searching: boolean
  query: string
  filterActive: boolean
  onSearchToggle: () => void
  onQueryChange: (value: string) => void
  onCloseSearch: () => void
  onFilterClick: () => void
}

export default function LibraryTopBar({
  total,
  searching,
  query,
  filterActive,
  onSearchToggle,
  onQueryChange,
  onCloseSearch,
  onFilterClick,
}: LibraryTopBarProps) {
  const { t } = useT()
  return (
    <header className="library__topbar">
      {searching ? (
        <>
          <input
            className="library__search-input"
            type="text"
            placeholder={t('library.searchPlaceholder')}
            value={query}
            onChange={(e) => onQueryChange(e.target.value)}
            autoFocus
          />
          <button type="button" className="icon-btn" aria-label={t('common.clearSearch')} onClick={onCloseSearch}>
            <CloseIcon />
          </button>
        </>
      ) : (
        <>
          <h1 className="library__title">{t('library.title')}</h1>
          <span className="library__count" aria-label={t('library.totalBooks', { total })}>
            {total}
          </span>
          <span className="library__topbar__spacer" />
          <button type="button" className="icon-btn" aria-label={t('common.search')} onClick={onSearchToggle}>
            <SearchIcon />
          </button>
        </>
      )}
      <button
        type="button"
        className={`icon-btn library__topbar__filter${filterActive ? ' is-active' : ''}`}
        aria-label={t('common.filters')}
        aria-pressed={filterActive}
        onClick={onFilterClick}
      >
        <FilterListIcon />
      </button>
    </header>
  )
}