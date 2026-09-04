import EmptyState from './components/EmptyState'
import { useT } from './i18n'

export default function Schedule() {
  const { t } = useT()
  return (
    <div className="schedule">
      <header className="schedule__topbar">
        <h1 className="schedule__title">{t('schedule.title')}</h1>
      </header>
      <EmptyState message={t('schedule.placeholder')} />
    </div>
  )
}