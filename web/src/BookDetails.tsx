import { useCallback, useEffect, useState } from 'react'
import Cover from './components/Cover'
import { ArrowBackIcon, CheckIcon, StarIcon, StarFilledIcon } from './components/icons'
import { getBookDetail, putProgress } from './api'
import { useT } from './i18n'
import type { BookDetail } from './bookTypes'

interface BookDetailsProps {
  bookId: string
  onOpenReader: (bookId: string, number: number) => void
  onBack: () => boolean
}

export default function BookDetails({ bookId, onOpenReader, onBack }: BookDetailsProps) {
  const { t } = useT()
  const [detail, setDetail] = useState<BookDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState(false)
  const [importing, setImporting] = useState(false)
  const [importError, setImportError] = useState<string | null>(null)

  const canImport = typeof window !== 'undefined' && !!(window as any).AndroidBridge?.importEpub

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setLoadError(false)
    getBookDetail(bookId)
      .then((d) => {
        if (!cancelled) {
          setDetail(d)
          setLoading(false)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setLoadError(true)
          setLoading(false)
        }
      })
    return () => {
      cancelled = true
    }
  }, [bookId])

  const reload = useCallback(() => {
    setLoading(true)
    setLoadError(false)
    getBookDetail(bookId)
      .then((d) => {
        setDetail(d)
        setLoading(false)
      })
      .catch(() => {
        setLoadError(true)
        setLoading(false)
      })
  }, [bookId])

  const retryLoad = reload

  useEffect(() => {
    const prev = (window as any).__onImportResult
    ;(window as any).__onImportResult = (bid: string, status: string, message: string) => {
      if (bid !== bookId) return
      if (status === 'ok') {
        setImporting(false)
        setImportError(null)
        reload()
      } else if (status === 'cancelled') {
        setImporting(false)
      } else {
        setImporting(false)
        setImportError(message || t('details.importError'))
      }
    }
    return () => {
      ;(window as any).__onImportResult = prev
    }
  }, [bookId, reload])

  const handleImport = () => {
    if (!canImport) return
    setImporting(true)
    setImportError(null)
    ;(window as any).AndroidBridge.importEpub(bookId)
  }

  type Patch = Parameters<typeof putProgress>[1]

  const patch = useCallback(
    async (p: Patch) => {
      if (!detail) return
      try {
        await putProgress(bookId, p)
        setDetail((prev) => {
          if (!prev) return prev
          return { ...prev, book: { ...prev.book, ...p } as BookDetail['book'] }
        })
      } catch {
        /* ignore */
      }
    },
    [detail, bookId],
  )

  const toggleBookmark = () => {
    if (!detail) return
    patch({ bookmarked: !detail.book.bookmarked })
  }

  const toggleStarted = () => {
    if (!detail) return
    patch({ started: !detail.book.started })
  }

  const toggleCompleted = () => {
    if (!detail) return
    patch({ completed: !detail.book.completed })
  }

  const startReading = () => {
    if (!detail || detail.chapters.length === 0) return
    let num = 1
    if (detail.book.lastReadChapterId) {
      const found = detail.chapters.find((c) => c.id === detail.book.lastReadChapterId)
      if (found) num = found.number
    }
    onOpenReader(bookId, num)
  }

  const openChapter = (number: number) => {
    onOpenReader(bookId, number)
  }

  if (loading) {
    return (
      <div className="book-detail">
        <header className="book-detail__topbar">
          <button type="button" className="icon-btn" aria-label={t('common.back')} onClick={() => onBack()}>
            <ArrowBackIcon />
          </button>
        </header>
        <div className="loading-state">
          <div className="spinner" />
        </div>
      </div>
    )
  }

  if (loadError || !detail) {
    return (
      <div className="book-detail">
        <header className="book-detail__topbar">
          <button type="button" className="icon-btn" aria-label={t('common.back')} onClick={() => onBack()}>
            <ArrowBackIcon />
          </button>
        </header>
        <div className="error-state">
          <p className="error-state__text">{t('common.errorLoading')}</p>
          <button type="button" className="btn-primary" onClick={retryLoad}>
            {t('common.retry')}
          </button>
        </div>
      </div>
    )
  }

  const { book, chapters } = detail
  const readCount = book.totalChapters - book.unreadCount
  const progressPct = book.totalChapters > 0 ? (readCount / book.totalChapters) * 100 : 0
  const hasChapters = chapters.length > 0
  const readingLabel = book.started && book.lastReadAt ? t('details.continueReading') : t('details.read')

  return (
    <div className="book-detail">
      <header className="book-detail__topbar">
        <button type="button" className="icon-btn" aria-label={t('common.back')} onClick={() => onBack()}>
          <ArrowBackIcon />
        </button>
        <span className="book-detail__topbar-title">{book.title}</span>
      </header>

      <div className="book-detail__body">
        <div className="book-detail__hero">
          <div className="book-detail__cover">
            <Cover book={book} />
          </div>
          <div className="book-detail__info">
            <h2 className="book-detail__title">{book.title}</h2>
            {book.author && <p className="book-detail__author">{book.author}</p>}
            <span className="book-detail__category">{t(`categories.${book.category}`)}</span>
            <div className="book-detail__meta">
              {book.year != null && (
                <span>{t('details.year')}: {book.year}</span>
              )}
              {book.genre && (
                <span>{t('details.genre')}: {book.genre}</span>
              )}
              <span>{t('common.chapter')}: {book.lang.toUpperCase()}</span>
              {book.source && (
                <span>{t('details.source')}: {book.source}</span>
              )}
            </div>
          </div>
        </div>

        {book.description && (
          <div className="book-detail__description">
            <h4 className="book-detail__section-title">{t('details.description')}</h4>
            <p>{book.description}</p>
          </div>
        )}

        <div className="book-detail__progress">
          <p className="book-detail__progress-text">
            {t('details.readProgress', { read: readCount, total: book.totalChapters })}
          </p>
          <div className="book-detail__progress-bar">
            <div className="book-detail__progress-bar-fill" style={{ width: `${progressPct}%` }} />
          </div>
        </div>

        <div className="book-detail__toggles">
          <button
            type="button"
            className={`book-detail__toggle${book.bookmarked ? ' book-detail__toggle--active' : ''}`}
            onClick={toggleBookmark}
          >
            {book.bookmarked ? <StarFilledIcon style={{ width: 18, height: 18 }} /> : <StarIcon style={{ width: 18, height: 18 }} />}
            {book.bookmarked ? t('details.bookmarkRemove') : t('details.bookmarkAdd')}
          </button>
          <button
            type="button"
            className={`book-detail__toggle${book.started ? ' book-detail__toggle--active' : ''}`}
            onClick={toggleStarted}
          >
            {t('details.started')}
          </button>
          <button
            type="button"
            className={`book-detail__toggle${book.completed ? ' book-detail__toggle--active' : ''}`}
            onClick={toggleCompleted}
          >
            {t('details.completed')}
          </button>
        </div>

        <div className="book-detail__actions">
          <button
            type="button"
            className="btn-primary"
            onClick={handleImport}
            disabled={!canImport || importing || loadError}
          >
            {importing ? t('details.importing') : t('details.importFile')}
          </button>
          <button
            type="button"
            className="btn-primary"
            onClick={startReading}
            disabled={!hasChapters}
          >
            {readingLabel}
          </button>
        </div>

        {importError && <p className="book-detail__error">{importError}</p>}

        {hasChapters ? (
          <>
            <h3 className="book-detail__chapters-header">
              {t('common.chapter')} ({chapters.length})
            </h3>
            <ul className="book-detail__chapters">
              {chapters.map((ch) => (
                <li key={ch.id}>
                  <button
                    type="button"
                    className="book-detail__chapter"
                    onClick={() => openChapter(ch.number)}
                  >
                    <span className="book-detail__chapter-num">{ch.number}</span>
                    <span className="book-detail__chapter-name">{ch.name}</span>
                    {ch.read && (
                      <span className="book-detail__chapter-check">
                        <CheckIcon />
                      </span>
                    )}
                  </button>
                </li>
              ))}
            </ul>
          </>
        ) : (
          <div className="book-detail__empty">
            <p>{t('details.noChapters')}</p>
            <p>{t('details.fileNotAdded')}</p>
            {canImport && (
              <button
                type="button"
                className="btn-primary"
                onClick={handleImport}
                disabled={importing || loadError}
              >
                {importing ? t('details.importing') : t('details.importFile')}
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
