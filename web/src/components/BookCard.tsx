import type { CSSProperties, KeyboardEvent } from 'react'
import type { Book } from '../data/books'
import Cover from './Cover'
import CoverBadges from './CoverBadges'
import type { DisplayMode } from '../libraryTypes'

interface BookCardProps {
  book: Book
  mode: DisplayMode
  style?: CSSProperties
  onClick?: () => void
}

function handleKey(e: KeyboardEvent, onClick: () => void) {
  if (e.key === 'Enter' || e.key === ' ') {
    e.preventDefault()
    onClick()
  }
}

export default function BookCard({ book, mode, style, onClick }: BookCardProps) {
  const clickable = onClick != null

  if (mode === 'list') {
    return (
      <article
        className={`library__row${clickable ? ' library__row--clickable' : ''}`}
        style={style}
        role={clickable ? 'button' : undefined}
        tabIndex={clickable ? 0 : undefined}
        onClick={onClick}
        onKeyDown={onClick ? (e) => handleKey(e, onClick) : undefined}
      >
        <div className="library__row__cover">
          <Cover book={book} />
        </div>
        <h3 className="library__row__title">{book.title}</h3>
        <div className="library__row__badges">
          {book.downloaded > 0 && <span className="badge badge--tertiary">{book.downloaded}</span>}
          {book.unreadCount > 0 && <span className="badge badge--secondary">{book.unreadCount}</span>}
        </div>
      </article>
    )
  }

  return (
    <article
      className={`library__card library__card--${mode}${clickable ? ' library__card--clickable' : ''}`}
      style={style}
      role={clickable ? 'button' : undefined}
      tabIndex={clickable ? 0 : undefined}
      onClick={onClick}
      onKeyDown={onClick ? (e) => handleKey(e, onClick) : undefined}
    >
      <div className="library__card__cover">
        <Cover book={book} />
        <CoverBadges book={book} />
        {mode === 'compact' && <h3 className="library__card__title library__card__title--overlay">{book.title}</h3>}
      </div>
      {mode === 'comfortable' && <h3 className="library__card__title">{book.title}</h3>}
    </article>
  )
}
