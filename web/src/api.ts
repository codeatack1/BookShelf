import type { Book, BookDetail, ChapterContent, CreateBookPayload, SearchResponse } from './bookTypes'

const BASE = '/api'

async function fetchJSON<T>(url: string): Promise<T> {
  const res = await fetch(url)
  if (!res.ok) throw new Error(`API ${res.status}: ${url}`)
  return res.json() as Promise<T>
}

export async function getLibrary(query?: string, category?: string): Promise<Book[]> {
  const params = new URLSearchParams()
  if (query) params.set('query', query)
  if (category && category !== 'all') params.set('category', category)
  const qs = params.toString()
  const books = await fetchJSON<Book[]>(`${BASE}/library${qs ? '?' + qs : ''}`)
  console.log(`[api] library: ${books.length} books`)
  return books
}

export async function getBookDetail(id: string): Promise<BookDetail> {
  const detail = await fetchJSON<BookDetail>(`${BASE}/books/${encodeURIComponent(id)}`)
  console.log(`[api] detail: ${id} chapters=${detail.chapters.length}`)
  return detail
}

export async function getChapterContent(bookId: string, number: number): Promise<ChapterContent> {
  const ch = await fetchJSON<ChapterContent>(
    `${BASE}/books/${encodeURIComponent(bookId)}/chapters/${number}`,
  )
  console.log(`[api] reader: ${bookId} ch<${number}>`)
  return ch
}

export async function putProgress(
  id: string,
  patch: {
    started?: boolean
    completed?: boolean
    bookmarked?: boolean
    lastReadAt?: number
    lastReadChapterId?: string
    chapterId?: string
    read?: boolean
  },
): Promise<void> {
  await fetch(`${BASE}/books/${encodeURIComponent(id)}/progress`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(patch),
  })
  console.log(`[api] progress: ${id}`)
}

export async function postSearch(query: string, page = 1): Promise<SearchResponse> {
  const res = await fetch('/api/search', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ query, page }),
  })
  if (!res.ok) throw new Error(`API ${res.status}: /api/search`)
  const data = (await res.json()) as SearchResponse
  console.log('[api] search: page', page, 'results', data.results.length)
  return data
}

export async function createBook(payload: CreateBookPayload): Promise<Book> {
  const res = await fetch('/api/books', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  if (!res.ok) throw new Error(`API ${res.status}: /api/books`)
  const data = (await res.json()) as Book
  console.log('[api] book created:', data.id)
  return data
}
