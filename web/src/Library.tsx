import { useMemo, useState } from 'react'
import BookCard from './components/BookCard'
import CategoryTabs from './components/CategoryTabs'
import EmptyState from './components/EmptyState'
import FilterSheet from './components/FilterSheet'
import LibraryTopBar from './components/LibraryTopBar'
import { books } from './data/books'
import { useT } from './i18n'
import {
  DEFAULT_FILTERS,
  FILTER_LABELS,
  FILTER_PREDICATES,
  type DisplayMode,
  type FiltersState,
  type SortDir,
  type SortKey,
} from './libraryTypes'

function idHash(s: string): number {
  let h = 0
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) | 0
  return h
}

export default function Library() {
  const { t, lang } = useT()
  const [searching, setSearching] = useState(false)
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState('all')
  const [filters, setFilters] = useState<FiltersState>(DEFAULT_FILTERS)
  const [sortKey, setSortKey] = useState<SortKey>('title')
  const [sortDir, setSortDir] = useState<SortDir>('asc')
  const [display, setDisplay] = useState<DisplayMode>('comfortable')
  const [sheetOpen, setSheetOpen] = useState(false)
  const [randomSeed] = useState(() => Math.random())

  const categories = useMemo(() => {
    const set = new Set(books.map((b) => b.category))
    return [
      'all',
      ...[...set].sort((a, b) => t(`categories.${a}`).localeCompare(t(`categories.${b}`), lang)),
    ]
  }, [t, lang])

  const counts = useMemo(() => {
    const map: Record<string, number> = { all: books.length }
    for (const b of books) map[b.category] = (map[b.category] ?? 0) + 1
    return map
  }, [])

  const filterActive = useMemo(
    () => FILTER_LABELS.some((f) => filters[f.key] !== 'unset'),
    [filters],
  )

  const items = useMemo(() => {
    let list = books
    if (category !== 'all') list = list.filter((b) => b.category === category)
    const q = query.trim().toLowerCase()
    if (q) list = list.filter((b) => b.title.toLowerCase().includes(q))
    for (const f of FILTER_LABELS) {
      const state = filters[f.key]
      if (state === 'unset') continue
      const pred = FILTER_PREDICATES[f.key]
      list = list.filter((b) => (state === 'include') === pred(b))
    }
    const dir = sortDir === 'asc' ? 1 : -1
    return [...list].sort((a, b) => {
      switch (sortKey) {
        case 'title':
          return a.title.localeCompare(b.title, 'ru') * dir
        case 'chapters':
          return (a.totalChapters - b.totalChapters) * dir
        case 'lastRead':
          return ((a.lastReadAt ?? 0) - (b.lastReadAt ?? 0)) * dir
        case 'dateAdded':
          return (a.dateAdded - b.dateAdded) * dir
        case 'random':
          return idHash(a.id + String(randomSeed)) - idHash(b.id + String(randomSeed))
        default:
          return 0
      }
    })
  }, [category, query, filters, sortKey, sortDir, randomSeed])

  const emptyMessage =
    query.trim() !== '' || filterActive
      ? t('library.nothingFound')
      : books.length === 0
        ? t('library.empty')
        : t('library.noBooksInCategory')

  return (
    <div className="library">
      <LibraryTopBar
        total={books.length}
        searching={searching}
        query={query}
        filterActive={filterActive}
        onSearchToggle={() => setSearching(true)}
        onQueryChange={setQuery}
        onCloseSearch={() => {
          setQuery('')
          setSearching(false)
        }}
        onFilterClick={() => setSheetOpen(true)}
      />
      <CategoryTabs categories={categories} counts={counts} active={category} onSelect={setCategory} />

      {sheetOpen && (
        <FilterSheet
          filters={filters}
          sortKey={sortKey}
          sortDir={sortDir}
          display={display}
          onFiltersChange={setFilters}
          onSortKeyChange={setSortKey}
          onSortDirChange={setSortDir}
          onDisplayChange={setDisplay}
          onClose={() => setSheetOpen(false)}
        />
      )}

      {items.length === 0 ? (
        <EmptyState message={emptyMessage} />
      ) : display === 'list' ? (
        <div className="library__list">
          {items.map((book) => (
            <BookCard key={book.id} book={book} mode={display} />
          ))}
        </div>
      ) : (
        <div className="library__grid">
          {items.map((book) => (
            <BookCard key={book.id} book={book} mode={display} />
          ))}
        </div>
      )}
    </div>
  )
}