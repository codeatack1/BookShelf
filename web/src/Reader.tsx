import { useCallback, useEffect, useRef, useState } from 'react'
import { ArrowBackIcon, CheckIcon } from './components/icons'
import { getBookDetail, getChapterContent, putProgress } from './api'
import { useT } from './i18n'
import type { ChapterContent } from './bookTypes'

interface ReaderProps {
  bookId: string
  chapterNumber: number
  onNavigateChapter: (bookId: string, number: number) => void
}

export default function Reader({ bookId, chapterNumber, onNavigateChapter }: ReaderProps) {
  const { t } = useT()
  const [chapter, setChapter] = useState<ChapterContent | null>(null)
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState(false)
  const progressSentRef = useRef<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setLoadError(false)
    setChapter(null)
    getChapterContent(bookId, chapterNumber)
      .then((ch) => {
        if (!cancelled) {
          setChapter(ch)
          setLoading(false)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setLoadError(true)
          setLoading(false)
        }
      })
    getBookDetail(bookId)
      .then((d) => {
        if (!cancelled) setTotal(d.book.totalChapters)
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [bookId, chapterNumber])

  useEffect(() => {
    if (!chapter) return
    if (progressSentRef.current === chapter.id) return
    progressSentRef.current = chapter.id
    putProgress(bookId, {
      started: true,
      lastReadChapterId: chapter.id,
      lastReadAt: Date.now(),
    }).catch(() => {})
  }, [chapter, bookId])

  const toggleRead = useCallback(() => {
    if (!chapter) return
    const newRead = !chapter.read
    setChapter((prev) => (prev ? { ...prev, read: newRead } : prev))
    putProgress(bookId, { chapterId: chapter.id, read: newRead }).catch(() => {})
  }, [chapter, bookId])

  if (loading) {
    return (
      <div className="reader">
        <header className="reader__topbar">
          <button type="button" className="icon-btn" aria-label={t('common.back')} onClick={() => history.back()}>
            <ArrowBackIcon />
          </button>
        </header>
        <div className="loading-state">
          <div className="spinner" />
        </div>
      </div>
    )
  }

  if (loadError || !chapter) {
    return (
      <div className="reader">
        <header className="reader__topbar">
          <button type="button" className="icon-btn" aria-label={t('common.back')} onClick={() => history.back()}>
            <ArrowBackIcon />
          </button>
        </header>
        <div className="error-state">
          <p className="error-state__text">{t('common.errorLoading')}</p>
        </div>
      </div>
    )
  }

  const counterText =
    total > 0
      ? t('details.chapterOf', { current: chapterNumber, total })
      : String(chapterNumber)

  const content = (() => {
    switch (chapter.contentType) {
      case 'html':
        return (
          <iframe
            srcDoc={chapter.content}
            sandbox=""
            title={chapter.name}
            className="reader__iframe"
          />
        )
      case 'image':
        return (
          <img
            src={chapter.content}
            alt={chapter.name}
            className="reader__img"
          />
        )
      default:
        return <p className="reader__placeholder">{t('details.noChapters')}</p>
    }
  })()

  return (
    <div className="reader">
      <header className="reader__topbar">
        <button type="button" className="icon-btn" aria-label={t('common.back')} onClick={() => history.back()}>
          <ArrowBackIcon />
        </button>
        <span className="reader__title">{chapter.name}</span>
        <span className="reader__counter">{counterText}</span>
      </header>

      <div className="reader__content">{content}</div>

      <div className="reader__nav">
        <button
          type="button"
          className="reader__nav-btn"
          disabled={chapterNumber <= 1}
          onClick={() => {
            progressSentRef.current = null
            onNavigateChapter(bookId, chapterNumber - 1)
          }}
        >
          ◀ {t('details.prevChapter')}
        </button>
        <button
          type="button"
          className={`reader__read-toggle${chapter.read ? ' reader__read-toggle--active' : ''}`}
          onClick={toggleRead}
        >
          <CheckIcon style={{ width: 18, height: 18 }} />
          {chapter.read ? t('details.markAsUnread') : t('details.markAsRead')}
        </button>
        <button
          type="button"
          className="reader__nav-btn"
          disabled={chapterNumber >= total}
          onClick={() => {
            progressSentRef.current = null
            onNavigateChapter(bookId, chapterNumber + 1)
          }}
        >
          {t('details.nextChapter')} ▶
        </button>
      </div>
    </div>
  )
}
