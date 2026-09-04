import type { Book } from './data/books'
import type { TranslationKey } from './i18n'

export type TriState = 'unset' | 'include' | 'exclude'

export type FilterKey = 'downloaded' | 'unread' | 'started' | 'bookmarked' | 'completed'

export type FiltersState = Record<FilterKey, TriState>

export const DEFAULT_FILTERS: FiltersState = {
  downloaded: 'unset',
  unread: 'unset',
  started: 'unset',
  bookmarked: 'unset',
  completed: 'unset',
}

export const FILTER_LABELS: { key: FilterKey; labelKey: TranslationKey }[] = [
  { key: 'downloaded', labelKey: 'filterSheet.filters.downloaded' },
  { key: 'unread', labelKey: 'filterSheet.filters.unread' },
  { key: 'started', labelKey: 'filterSheet.filters.started' },
  { key: 'bookmarked', labelKey: 'filterSheet.filters.bookmarked' },
  { key: 'completed', labelKey: 'filterSheet.filters.completed' },
]

export const FILTER_PREDICATES: Record<FilterKey, (book: Book) => boolean> = {
  downloaded: (b) => b.downloaded > 0,
  unread: (b) => b.unreadCount > 0,
  started: (b) => b.started,
  bookmarked: (b) => b.bookmarked,
  completed: (b) => b.completed,
}

export type SortKey = 'title' | 'chapters' | 'lastRead' | 'dateAdded' | 'random'

export type SortDir = 'asc' | 'desc'

export type DisplayMode = 'compact' | 'comfortable' | 'coverOnly' | 'list'

export const SORT_OPTIONS: { key: SortKey; labelKey: TranslationKey }[] = [
  { key: 'title', labelKey: 'filterSheet.sort.title' },
  { key: 'chapters', labelKey: 'filterSheet.sort.chapters' },
  { key: 'lastRead', labelKey: 'filterSheet.sort.lastRead' },
  { key: 'dateAdded', labelKey: 'filterSheet.sort.dateAdded' },
  { key: 'random', labelKey: 'filterSheet.sort.random' },
]

export const DISPLAY_OPTIONS: { value: DisplayMode; labelKey: TranslationKey }[] = [
  { value: 'compact', labelKey: 'filterSheet.display.compact' },
  { value: 'comfortable', labelKey: 'filterSheet.display.comfortable' },
  { value: 'coverOnly', labelKey: 'filterSheet.display.coverOnly' },
  { value: 'list', labelKey: 'filterSheet.display.list' },
]