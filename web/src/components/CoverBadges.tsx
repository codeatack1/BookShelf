import type { Book } from '../data/books'
import { useT } from '../i18n'
import { FolderIcon } from './icons'

export default function CoverBadges({ book }: { book: Book }) {
  const { t } = useT()
  return (
    <div className="library__badges">
      <div className="library__badges__start">
        {book.downloaded > 0 && <span className="badge badge--tertiary">{book.downloaded}</span>}
        {book.unreadCount > 0 && <span className="badge badge--secondary">{book.unreadCount}</span>}
      </div>
      <div className="library__badges__end">
        {book.isLocal ? (
          <span className="badge badge--tertiary badge--icon" aria-label={t('common.localSource')}>
            <FolderIcon />
          </span>
        ) : (
          <span className="badge badge--tertiary">{book.lang.toUpperCase()}</span>
        )}
      </div>
    </div>
  )
}