import { useState, type CSSProperties, type KeyboardEvent, type ReactNode } from 'react'
import { createBook, importUrl, postSearch } from './api'
import { SearchIcon } from './components/icons'
import { useT } from './i18n'
import type { SearchResult } from './bookTypes'

interface SearchScreenProps {
  onOpenBook: (bookId: string) => void
}

interface CardState {
  status: 'idle' | 'creating' | 'importing' | 'done' | 'error'
  bookId?: string
  message?: string
}

interface ResultCardProps {
  result: SearchResult
  state: CardState
  style?: CSSProperties
  onAdd: (result: SearchResult) => void
  onRetry: (result: SearchResult) => void
  onOpen: (bookId: string) => void
}

function ResultCard({ result, state, style, onAdd, onRetry, onOpen }: ResultCardProps) {
  const { t } = useT()
  const { status, bookId, message } = state
  const done = status === 'done'
  const busy = status === 'creating' || status === 'importing'
  const sourceLabel =
    result.source === 'google' || result.source === 'openlibrary'
      ? t(`search.source.${result.source}`)
      : result.source ?? null
  const meta = [result.author, result.year != null ? String(result.year) : null]
    .filter(Boolean)
    .join(' · ')
  const label =
    status === 'creating'
      ? t('search.adding')
      : status === 'importing'
        ? t('search.importing')
        : status === 'done'
          ? t('search.added')
          : status === 'error'
            ? t('search.retry')
            : t('search.add')

  return (
    <article
      className={`library__card library__card--comfortable search-card${done ? ' library__card--clickable' : ''}`}
      style={style}
      role={done ? 'button' : undefined}
      tabIndex={done ? 0 : undefined}
      onClick={() => {
        if (done && bookId != null) onOpen(bookId)
      }}
      onKeyDown={(e) => {
        if (done && bookId != null && (e.key === 'Enter' || e.key === ' ')) {
          e.preventDefault()
          onOpen(bookId)
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
        disabled={busy || done}
        onClick={(e) => {
          e.stopPropagation()
          if (status === 'error') onRetry(result)
          else if (status === 'idle') onAdd(result)
        }}
      >
        {label}
      </button>
      {status === 'error' && message && (
        <p className="search-card__error">
          {t('search.importError')}: {message}
        </p>
      )}
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
  const [statuses, setStatuses] = useState<Map<string, CardState>>(new Map())
  const [searched, setSearched] = useState(false)

  const queryTooShort = query.trim().length < 4

  const setCardState = (key: string, patch: Partial<CardState>) => {
    setStatuses((prev) => {
      const next = new Map(prev)
      next.set(key, { ...(next.get(key) ?? { status: 'idle' as const }), ...patch })
      return next
    })
  }

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
    if (q.length < 4 || loading) return
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

  const runImport = async (result: SearchResult, bookId: string) => {
    if (!result.pageUrl) {
      setCardState(result.title, { status: 'error', bookId, message: 'pageUrl missing' })
      return
    }
    setCardState(result.title, { status: 'importing', bookId, message: undefined })
    try {
      const res = await importUrl(bookId, result.pageUrl)
      if (res.ok) {
        setCardState(result.title, { status: 'done', bookId, message: undefined })
      } else {
        setCardState(result.title, { status: 'error', bookId, message: res.error ?? 'unknown' })
      }
    } catch (err) {
      console.warn('[search] import-url failed:', err)
      setCardState(result.title, { status: 'error', bookId, message: 'network' })
    }
  }

  const handleAdd = async (result: SearchResult) => {
    setCardState(result.title, { status: 'creating', message: undefined })
    try {
      const book = await createBook({
        title: result.title,
        author: result.author,
        coverUrl: result.coverUrl,
        year: result.year,
        genre: null,
        source: 'pidruchnyk',
        lang: null,
      })
      await runImport(result, book.id)
    } catch (err) {
      console.warn('[search] failed to create book:', err)
      setCardState(result.title, { status: 'error', message: 'create' })
    }
  }

  const handleRetry = (result: SearchResult) => {
    const current = statuses.get(result.title)
    if (current?.bookId) {
      void runImport(result, current.bookId)
    } else {
      void handleAdd(result)
    }
  }

  let body: ReactNode
  if (!searched) {
    body = (
      <div className="library__empty">
        <p className="library__empty__text">{queryTooShort ? t('search.queryTooShort') : t('search.hint')}</p>
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
              state={statuses.get(r.title) ?? { status: 'idle' }}
              style={{ ['--i' as string]: Math.min(index, 8) }}
              onAdd={handleAdd}
              onRetry={handleRetry}
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
        <button
          type="button"
          className="icon-btn"
          aria-label={t('nav.search')}
          onClick={submit}
          disabled={queryTooShort}
        >
          <SearchIcon />
        </button>
      </header>
      {body}
    </div>
  )
}