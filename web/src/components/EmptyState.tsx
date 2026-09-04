import { SentimentIcon } from './icons'

export default function EmptyState({ message }: { message: string }) {
  return (
    <div className="library__empty">
      <SentimentIcon className="library__empty__icon" />
      <p className="library__empty__text">{message}</p>
    </div>
  )
}