import { useEffect, useMemo, useRef, useState } from 'react'
import BookCard from './components/BookCard'
import CategoryTabs from './components/CategoryTabs'
import EmptyState from './components/EmptyState'
import FilterSheet from './components/FilterSheet'
import LibraryTopBar from './components/LibraryTopBar'
import { books as mockBooks } from './data/books'
import { getLibrary } from './api'
import { useT } from './i18n'
import type { Book } from './bookTypes'
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

interface LibraryProps {
  onOpenBook: (bookId: string) => void
  active: boolean
}

export default function Library({ onOpenBook, active }: LibraryProps) {
  const { t, lang } = useT()
  const [books, setBooks] = useState<Book[]>([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState(false)
  const [searching, setSearching] = useState(false)
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState('all')
  const [filters, setFilters] = useState<FiltersState>(DEFAULT_FILTERS)
  const [sortKey, setSortKey] = useState<SortKey>('title')
  const [sortDir, setSortDir] = useState<SortDir>('asc')
  const [display, setDisplay] = useState<DisplayMode>('comfortable')
  const [sheetOpen, setSheetOpen] = useState(false)
  const [randomSeed] = useState(() => Math.random())

  const scrollRef = useRef<number>(0)

  useEffect(() => {
    if (active) {
      requestAnimationFrame(() => { window.scrollTo(0, scrollRef.current) })
    } else {
      scrollRef.current = window.scrollY
    }
  }, [active])

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setLoadError(false)
    getLibrary()
      .then((data) => {
        if (!cancelled) {
          setBooks(data)
          setLoading(false)
        }
      })
      .catch(() => {
        console.warn('[api] fallback to mock')
        if (!cancelled) {
          setBooks(mockBooks)
          setLoading(false)
        }
      })
    return () => {
      cancelled = true
    }
  }, [])

  const retryLoad = () => {
    setLoading(true)
    setLoadError(false)
    getLibrary()
      .then((data) => {
        setBooks(data)
        setLoading(false)
      })
      .catch(() => {
        console.warn('[api] fallback to mock')
        setBooks(mockBooks)
        setLoading(false)
      })
  }

  const categories = useMemo(() => {
    const set = new Set(books.map((b) => b.category))
    return [
      'all',
      ...[...set].sort((a, b) => t(`categories.${a}`).localeCompare(t(`categories.${b}`), lang)),
    ]
  }, [t, lang, books])

  const counts = useMemo(() => {
    const map: Record<string, number> = { all: books.length }
    for (const b of books) map[b.category] = (map[b.category] ?? 0) + 1
    return map
  }, [books])

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
  }, [category, query, filters, sortKey, sortDir, randomSeed, books])

  const emptyMessage =
    query.trim() !== '' || filterActive
      ? t('library.nothingFound')
      : books.length === 0
        ? t('library.empty')
        : t('library.noBooksInCategory')

  if (loading) {
    return (
      <div className="library">
        <LibraryTopBar
          total={0}
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
        <div className="loading-state">
          <div className="spinner" />
        </div>
      </div>
    )
  }

  if (loadError) {
    return (
      <div className="library">
        <LibraryTopBar
          total={0}
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
        <div className="error-state">
          <p className="error-state__text">{t('common.errorLoading')}</p>
          <button type="button" className="btn-primary" onClick={retryLoad}>
            {t('common.retry')}
          </button>
        </div>
      </div>
    )
  }

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
          {items.map((book, index) => (
            <BookCard
              key={book.id}
              book={book}
              mode={display}
              style={{ ['--i' as string]: Math.min(index, 8) }}
              onClick={() => onOpenBook(book.id)}
            />
          ))}
        </div>
      ) : (
        <div className="library__grid">
          {items.map((book, index) => (
            <BookCard
              key={book.id}
              book={book}
              mode={display}
              style={{ ['--i' as string]: Math.min(index, 8) }}
              onClick={() => onOpenBook(book.id)}
            />
          ))}
        </div>
      )}
    </div>
  )
}
