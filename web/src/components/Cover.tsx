import type { Book } from '../data/books'

function hashText(s: string): number {
  let h = 0
  for (let i = 0; i < s.length; i++) {
    h = (h << 5) - h + s.charCodeAt(i)
    h |= 0
  }
  return Math.abs(h)
}

function hslToTextColor(h: number, sat: number, light: number): string {
  const s = sat / 100
  const l = light / 100
  const c = (1 - Math.abs(2 * l - 1)) * s
  const x = c * (1 - Math.abs(((h / 60) % 2) - 1))
  const m = l - c / 2
  let r = 0
  let g = 0
  let b = 0
  if (h < 60) [r, g, b] = [c, x, 0]
  else if (h < 120) [r, g, b] = [x, c, 0]
  else if (h < 180) [r, g, b] = [0, c, x]
  else if (h < 240) [r, g, b] = [0, x, c]
  else if (h < 300) [r, g, b] = [x, 0, c]
  else [r, g, b] = [c, 0, x]
  const linear = (v: number) => {
    const u = v + m
    return u <= 0.03928 ? u / 12.92 : Math.pow((u + 0.055) / 1.055, 2.4)
  }
  const lum = 0.2126 * linear(r) + 0.7152 * linear(g) + 0.0722 * linear(b)
  return lum > 0.35 ? '#1B1B1F' : '#FFFFFF'
}

interface CoverProps {
  book: Book
}

export default function Cover({ book }: CoverProps) {
  if (book.cover && /^(https?:|data:)/.test(book.cover)) {
    return <img className="cover-img" src={book.cover} alt="" draggable={false} />
  }

  const h = hashText(book.id + book.title) % 360
  const sat = 38 + (hashText(book.title) % 18)
  const light = 42 + (hashText(book.author) % 13)
  const background = book.cover ?? `hsl(${h}, ${sat}%, ${light}%)`
  const textColor = hslToTextColor(h, sat, light)
  const letter = (book.title.trim()[0] ?? '?').toUpperCase()

  return (
    <div className="cover-placeholder" style={{ backgroundColor: background }} aria-hidden="true">
      <span className="cover-placeholder__letter" style={{ color: textColor }}>
        {letter}
      </span>
    </div>
  )
}