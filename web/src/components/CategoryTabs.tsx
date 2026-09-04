import { useT } from '../i18n'

interface CategoryTabsProps {
  categories: string[]
  counts: Record<string, number>
  active: string
  onSelect: (category: string) => void
}

export default function CategoryTabs({ categories, counts, active, onSelect }: CategoryTabsProps) {
  const { t } = useT()
  return (
    <nav className="library__tabs" aria-label={t('common.categories')} role="tablist">
      {categories.map((cat) => {
        const selected = cat === active
        return (
          <button
            key={cat}
            type="button"
            role="tab"
            aria-selected={selected}
            className={`library__tab${selected ? ' is-active' : ''}`}
            onClick={() => onSelect(cat)}
          >
            <span className="library__tab__label">{cat === 'all' ? t('library.all') : t(`categories.${cat}`)}</span>
            <span className="library__tab__pill">{counts[cat] ?? 0}</span>
            <span className="library__tab__indicator" aria-hidden="true" />
          </button>
        )
      })}
    </nav>
  )
}