export interface Book {
  id: string
  title: string
  author: string | null
  category: string
  cover?: string | null
  description?: string | null
  genre?: string | null
  year?: number | null
  source?: string | null
  totalChapters: number
  unreadCount: number
  downloaded: number
  isLocal: boolean
  lang: string
  bookmarked: boolean
  started: boolean
  completed: boolean
  lastReadAt?: number | null
  lastReadChapterId?: string | null
  dateAdded: number
}

export interface Chapter {
  id: string
  number: number
  name: string
  contentType: 'none' | 'html' | 'image'
  read: boolean
}

export interface ChapterContent extends Chapter {
  content: string
}

export interface BookDetail {
  book: Book
  chapters: Chapter[]
}

export interface SearchResult {
  title: string
  author?: string | null
  coverUrl?: string | null
  description?: string | null
  year?: number | null
  source: string
}

export interface SearchResponse {
  results: SearchResult[]
  hasNextPage: boolean
  error?: string | null
}

export interface CreateBookPayload {
  title: string
  author?: string | null
  coverUrl?: string | null
  description?: string | null
  year?: number | null
  genre?: string | null
  source?: string | null
  lang?: string | null
}
