import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'

export type Lang = 'ru' | 'en'
export type Params = Record<string, string | number>

const LS_LANG = 'bs-lang'

const ru = {
  common: {
    close: 'Закрыть',
    search: 'Поиск',
    clearSearch: 'Сбросить поиск',
    filters: 'Фильтры',
    categories: 'Категории',
    sections: 'Разделы',
    localSource: 'Локальный источник',
    russian: 'Русский',
    english: 'English',
  },
  themeSelect: {
    subtitle: 'Выбери тему оформления',
    modeAriaLabel: 'Режим оформления',
    modeLight: 'Светлая',
    modeDark: 'Тёмная',
    modeSystem: 'Системная',
    continue: 'Продолжить',
  },
  nav: {
    schedule: 'Расписание',
    library: 'Учебники',
    settings: 'Настройки',
  },
  library: {
    title: 'Библиотека',
    searchPlaceholder: 'Поиск…',
    all: 'Все',
    totalBooks: 'Всего книг: {total}',
    empty: 'Библиотека пуста',
    nothingFound: 'Ничего не найдено',
    noBooksInCategory: 'Нет книг в этой категории',
  },
  filterSheet: {
    tabs: { filter: 'Фильтр', sort: 'Сортировка', display: 'Отображение' },
    filters: {
      downloaded: 'Скачано',
      unread: 'Непрочитано',
      started: 'Начато',
      bookmarked: 'В закладках',
      completed: 'Завершено',
    },
    sort: {
      title: 'По алфавиту',
      chapters: 'Всего глав',
      lastRead: 'Последнее чтение',
      dateAdded: 'Дата добавления',
      random: 'Случайно',
    },
    display: {
      compact: 'Компактная сетка',
      comfortable: 'Удобная сетка',
      coverOnly: 'Только обложки',
      list: 'Список',
    },
    triState: { unset: 'Не задано', include: 'Включить', exclude: 'Исключить' },
  },
  schedule: {
    title: 'Расписание',
    placeholder: 'Здесь будет расписание уроков',
  },
  settings: {
    title: 'Настройки',
    theme: 'Тема оформления',
    about: 'О приложении',
    version: 'Версия 0.1.0',
    soon: 'Скоро',
    account: 'Аккаунт',
    schedule: 'Расписание уроков',
    aboutText: 'BookShelf — библиотека учебников',
    language: 'Язык',
  },
  categories: {
    math: 'Математика',
    physics: 'Физика',
    history: 'История',
    literature: 'Литература',
    biology: 'Биология',
    informatics: 'Информатика',
  },
} as const

type DeepString<T> = { readonly [K in keyof T]: T[K] extends string ? string : DeepString<T[K]> }

const en: DeepString<typeof ru> = {
  common: {
    close: 'Close',
    search: 'Search',
    clearSearch: 'Clear search',
    filters: 'Filters',
    categories: 'Categories',
    sections: 'Sections',
    localSource: 'Local source',
    russian: 'Русский',
    english: 'English',
  },
  themeSelect: {
    subtitle: 'Choose theme',
    modeAriaLabel: 'Appearance mode',
    modeLight: 'Light',
    modeDark: 'Dark',
    modeSystem: 'System',
    continue: 'Continue',
  },
  nav: {
    schedule: 'Schedule',
    library: 'Textbooks',
    settings: 'Settings',
  },
  library: {
    title: 'Library',
    searchPlaceholder: 'Search…',
    all: 'All',
    totalBooks: 'Total books: {total}',
    empty: 'Library is empty',
    nothingFound: 'Nothing found',
    noBooksInCategory: 'No books in this category',
  },
  filterSheet: {
    tabs: { filter: 'Filter', sort: 'Sort', display: 'Display' },
    filters: {
      downloaded: 'Downloaded',
      unread: 'Unread',
      started: 'Started',
      bookmarked: 'Bookmarked',
      completed: 'Completed',
    },
    sort: {
      title: 'Alphabetically',
      chapters: 'Total chapters',
      lastRead: 'Last read',
      dateAdded: 'Date added',
      random: 'Random',
    },
    display: {
      compact: 'Compact grid',
      comfortable: 'Comfortable grid',
      coverOnly: 'Cover-only grid',
      list: 'List',
    },
    triState: { unset: 'Not set', include: 'Include', exclude: 'Exclude' },
  },
  schedule: {
    title: 'Schedule',
    placeholder: 'Class schedule will be here',
  },
  settings: {
    title: 'Settings',
    theme: 'Theme',
    about: 'About',
    version: 'Version 0.1.0',
    soon: 'Soon',
    account: 'Account',
    schedule: 'Class schedule',
    aboutText: 'BookShelf — textbook library',
    language: 'Language',
  },
  categories: {
    math: 'Mathematics',
    physics: 'Physics',
    history: 'History',
    literature: 'Literature',
    biology: 'Biology',
    informatics: 'Computer science',
  },
}

const translations = { ru, en } as const

type Keys<T> = {
  [K in keyof T & string]: T[K] extends Record<string, unknown> ? `${K}.${Keys<T[K]>}` : K
}[keyof T & string]

export type TranslationKey = Keys<typeof ru>

interface LanguageContextValue {
  lang: Lang
  setLang: (lang: Lang) => void
  t: (key: TranslationKey | (string & {}), params?: Params) => string
}

const LanguageContext = createContext<LanguageContextValue | null>(null)

function getInitialLang(): Lang {
  try {
    const stored = localStorage.getItem(LS_LANG)
    if (stored === 'ru' || stored === 'en') return stored
  } catch {
    // ignore storage errors
  }
  try {
    return navigator.language.toLowerCase().startsWith('en') ? 'en' : 'ru'
  } catch {
    return 'ru'
  }
}

function lookup(dict: unknown, path: string): string | undefined {
  let node: unknown = dict
  for (const part of path.split('.')) {
    if (typeof node !== 'object' || node === null) return undefined
    node = (node as Record<string, unknown>)[part]
  }
  return typeof node === 'string' ? node : undefined
}

export function LanguageProvider({ children }: { children: ReactNode }) {
  const [lang, setLang] = useState<Lang>(getInitialLang)

  useEffect(() => {
    document.documentElement.lang = lang
    try {
      localStorage.setItem(LS_LANG, lang)
    } catch {
      // ignore storage errors
    }
  }, [lang])

  const t = useCallback(
    (key: string, params?: Params) => {
      const raw = lookup(translations[lang], key) ?? lookup(translations.ru, key) ?? key
      if (params == null) return raw
      return raw.replace(/\{(\w+)\}/g, (match, name: string) => String(params[name] ?? match))
    },
    [lang],
  )

  const value = useMemo(() => ({ lang, setLang, t }), [lang, t])

  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>
}

export function useT(): LanguageContextValue {
  const ctx = useContext(LanguageContext)
  if (!ctx) throw new Error('useT must be used within LanguageProvider')
  return ctx
}