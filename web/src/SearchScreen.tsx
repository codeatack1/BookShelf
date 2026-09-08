import { useState, type CSSProperties, type KeyboardEvent, type ReactNode } from 'react'
import { createBook, postSearch } from './api'
import { SearchIcon } from './components/icons'
import { useT } from './i18n'
import type { SearchResult } from './bookTypes'

interface SearchScreenProps {
  onOpenBook: (bookId: string) => void
}

interface ResultCardProps {
  result: SearchResult
  addedId?: string
  style?: CSSProperties
  onAdd: (result: SearchResult) => void
  onOpen: (bookId: string) => void
}

function ResultCard({ result, addedId, style, onAdd, onOpen }: ResultCardProps) {
  const { t } = useT()
  const added = addedId != null
  const sourceLabel =
    result.source === 'google' || result.source === 'openlibrary'
      ? t(`search.source.${result.source}`)
      : result.source ?? null
  const meta = [result.author, result.year != null ? String(result.year) : null]
    .filter(Boolean)
    .join(' · ')

  return (
    <article
      className={`library__card library__card--comfortable search-card${added ? ' library__card--clickable' : ''}`}
      style={style}
      role={added ? 'button' : undefined}
      tabIndex={added ? 0 : undefined}
      onClick={() => {
        if (addedId != null) onOpen(addedId)
      }}
      onKeyDown={(e) => {
        if (addedId != null && (e.key === 'Enter' || e.key === ' ')) {
          e.preventDefault()
          onOpen(addedId)
        }
      }}
    >
      <div className="library__card__cover">
        {result.coverUrl ? (
          <img className="cover-img" src={result.coverUrl} alt="" draggable={false} />
        ) : (
          <div className="cover-placeholder" aria-hidden="true">
            <span className="cover-placeholder__letter">
              {(result.title.trim()[0] ?? '?').toUpperCase()}
            </span>
          </div>
        )}
      </div>
      <h3 className="library__card__title">{result.title}</h3>
      {meta && <p className="search-card__meta">{meta}</p>}
      {sourceLabel && <span className="search-card__source">{sourceLabel}</span>}
      <button
        type="button"
        className="btn-primary search-card__btn"
        disabled={added}
        onClick={(e) => {
          e.stopPropagation()
          if (!added) onAdd(result)
        }}
      >
        {added ? t('search.added') : t('search.add')}
      </button>
    </article>
  )
}

export default function SearchScreen({ onOpenBook }: SearchScreenProps) {
  const { t } = useT()
  const [query, setQuery] = useState('')
  const [submittedQuery, setSubmittedQuery] = useState('')
  const [results, setResults] = useState<SearchResult[]>([])
  const [page, setPage] = useState(1)
  const [hasNextPage, setHasNextPage] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [added, setAdded] = useState<Map<string, string>>(new Map())
  const [searched, setSearched] = useState(false)

  const run = async (q: string, p: number) => {
    setLoading(true)
    setError(null)
    try {
      const data = await postSearch(q, p)
      setResults((prev) => (p === 1 ? data.results : [...prev, ...data.results]))
      setPage(p)
      setHasNextPage(data.hasNextPage)
      setSearched(true)
    } catch {
      setError(t('search.error'))
    } finally {
      setLoading(false)
    }
  }

  const submit = () => {
    const q = query.trim()
    if (!q || loading) return
    setSubmittedQuery(q)
    void run(q, 1)
  }

  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') submit()
  }

  const loadMore = () => {
    const q = submittedQuery || query.trim()
    if (!q || loading || !hasNextPage) return
    void run(q, page + 1)
  }

  const retry = () => {
    const q = submittedQuery || query.trim()
    if (!q || loading) return
    void run(q, 1)
  }

  const addBook = async (result: SearchResult) => {
    try {
      const book = await createBook({
        title: result.title,
        author: result.author ?? null,
        coverUrl: result.coverUrl ?? null,
        description: result.description ?? null,
        year: result.year ?? null,
        source: result.source,
      })
      setAdded((prev) => new Map(prev).set(result.title, book.id))
    } catch (err) {
      console.warn('[search] failed to add book:', err)
    }
  }

  let body: ReactNode
  if (!searched) {
    body = (
      <div className="library__empty">
        <p className="library__empty__text">{t('search.hint')}</p>
      </div>
    )
  } else if (loading && results.length === 0) {
    body = (
      <div className="loading-state">
        <div className="spinner" />
      </div>
    )
  } else if (error) {
    body = (
      <div className="error-state">
        <p className="error-state__text">{error}</p>
        <button type="button" className="btn-primary" onClick={retry}>
          {t('search.retry')}
        </button>
      </div>
    )
  } else if (results.length === 0) {
    body = (
      <div className="library__empty">
        <p className="library__empty__text">{t('search.noResults')}</p>
      </div>
    )
  } else {
    body = (
      <>
        <div className="library__grid">
          {results.map((r, index) => (
            <ResultCard
              key={`${r.source}:${r.title}`}
              result={r}
              addedId={added.get(r.title)}
              style={{ ['--i' as string]: Math.min(index, 8) }}
              onAdd={addBook}
              onOpen={onOpenBook}
            />
          ))}
        </div>
        {hasNextPage && (
          <button
            type="button"
            className="search__load-more"
            onClick={loadMore}
            disabled={loading}
          >
            {loading ? t('common.loading') : t('search.loadMore')}
          </button>
        )}
      </>
    )
  }

  return (
    <div className="search">
      <header className="library__topbar">
        <input
          className="library__search-input"
          type="text"
          placeholder={t('search.placeholder')}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={handleKeyDown}
          autoFocus
        />
        <button type="button" className="icon-btn" aria-label={t('nav.search')} onClick={submit}>
          <SearchIcon />
        </button>
      </header>
      {body}
    </div>
  )
}