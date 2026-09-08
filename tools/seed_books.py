#!/usr/bin/env python3
"""Seed script: fetch metadata for Russian textbooks and write books.json.

Sources: Google Books API (primary, no key), OpenLibrary (fallback).
Stdlib only. Writes UTF-8 (no BOM) JSON consumed by Room seeding.
"""

import json
import os
import re
import time
import urllib.parse
import urllib.request
import urllib.error

BOOKS = [
    {"id": "math-5-vilenkin", "title": "Математика. 5 класс", "author": "Виленкин Н.Я.", "category": "math"},
    {"id": "math-algebra-7-makarychev", "title": "Алгебра. 7 класс", "author": "Макарычев Ю.Н.", "category": "math"},
    {"id": "math-geometry-7-9-atanasyan", "title": "Геометрия. 7–9 классы", "author": "Атанасян Л.С.", "category": "math"},
    {"id": "physics-7-peryshkin", "title": "Физика. 7 класс", "author": "Пёрышкин А.В.", "category": "physics"},
    {"id": "physics-9-peryshkin-gutnik", "title": "Физика. 9 класс", "author": "Пёрышкин А.В., Гутник Е.М.", "category": "physics"},
    {"id": "physics-10-myakishev", "title": "Физика. 10 класс", "author": "Мякишев Г.Я.", "category": "physics"},
    {"id": "history-6-arsentyev", "title": "История России. 6 класс", "author": "Арсентьев Н.М.", "category": "history"},
    {"id": "history-9-torkunov", "title": "История России. 9 класс", "author": "Торкунов А.В.", "category": "history"},
    {"id": "history-5-vigasin", "title": "Всеобщая история. 5 класс", "author": "Вигасин А.А.", "category": "history"},
    {"id": "russian-7-ladyzhenskaya", "title": "Русский язык. 7 класс", "author": "Ладыженская Т.А.", "category": "literature"},
    {"id": "russian-5-ladyzhenskaya", "title": "Русский язык. 5 класс", "author": "Ладыженская Т.А.", "category": "literature"},
    {"id": "literature-7-korovina", "title": "Литература. 7 класс", "author": "Коровина В.Я.", "category": "literature"},
    {"id": "biology-5-pasechnik", "title": "Биология. 5 класс", "author": "Пасечник В.В.", "category": "biology"},
    {"id": "biology-6-ponomareva", "title": "Биология. 6 класс", "author": "Пономарёва И.Н.", "category": "biology"},
    {"id": "biology-8-kolesov", "title": "Биология. 8 класс", "author": "Колесов Д.В.", "category": "biology"},
    {"id": "informatics-7-bosova", "title": "Информатика. 7 класс", "author": "Босова Л.Л.", "category": "informatics"},
    {"id": "informatics-8-bosova", "title": "Информатика. 8 класс", "author": "Босова Л.Л.", "category": "informatics"},
    {"id": "informatics-9-bosova", "title": "Информатика. 9 класс", "author": "Босова Л.Л.", "category": "informatics"},
]

MANUAL_COVERS = {
    "math-5-vilenkin": "https://d1v124mdoasvln.cloudfront.net/pictures/books_photos/93/933059/9785091025316_d.jpg",
    "math-algebra-7-makarychev": "https://d1v124mdoasvln.cloudfront.net/pictures/books_photos/93/930789/9785091210293_d.jpg",
    "math-geometry-7-9-atanasyan": "https://d1v124mdoasvln.cloudfront.net/pictures/books_photos/91/915305/9785091111675_d.jpg",
    "physics-7-peryshkin": "https://d1v124mdoasvln.cloudfront.net/pictures/books_photos/94/944707/9785091278934_d.jpg",
    "physics-9-peryshkin-gutnik": "https://d1v124mdoasvln.cloudfront.net/pictures/books_photos/91/915573/9785091025569_d.jpg",
    "physics-10-myakishev": "https://d1v124mdoasvln.cloudfront.net/pictures/books_photos/69/691318/9785090716031_d.jpg",
    "history-6-arsentyev": "https://d1v124mdoasvln.cloudfront.net/pictures/books_photos/82/824736/9785090379397_d.jpg",
    "history-9-torkunov": "https://d1v124mdoasvln.cloudfront.net/pictures/books_photos/90/901884/9785091134636_d.jpg",
    "history-5-vigasin": "https://d1v124mdoasvln.cloudfront.net/pictures/books_photos/49/493515/9785090572361_d.jpg",
    "russian-7-ladyzhenskaya": "https://d1v124mdoasvln.cloudfront.net/pictures/books_photos/69/696161/9785090739450_d.jpg",
    "russian-5-ladyzhenskaya": "https://d1v124mdoasvln.cloudfront.net/pictures/books_photos/72/723851/9785091121087_d.jpg",
    "literature-7-korovina": "https://d1v124mdoasvln.cloudfront.net/pictures/books_photos/87/877874/9785091108033_d.jpg",
    "biology-5-pasechnik": "https://d1v124mdoasvln.cloudfront.net/pictures/books_photos/82/828465/9785091201819_d.jpg",
    "biology-6-ponomareva": "https://d1v124mdoasvln.cloudfront.net/pictures/books_photos/82/828487/9785090879439_d.jpg",
    "biology-8-kolesov": "https://d1v124mdoasvln.cloudfront.net/pictures/books_photos/20/200588/9785090590945_d.jpg",
    "informatics-7-bosova": "https://d1v124mdoasvln.cloudfront.net/pictures/books_photos/32/324274/9785996363520_d.jpg",
    "informatics-8-bosova": "https://d1v124mdoasvln.cloudfront.net/pictures/books_photos/95/958021/9785091278910_d.jpg",
    "informatics-9-bosova": "https://d1v124mdoasvln.cloudfront.net/pictures/books_photos/29/294798/9785996316090_d.jpg",
}

BROWSER_UA = (
    "Mozilla/5.0 (Linux; Android 10; K) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.0 Mobile Safari/537.36"
)

MAX_DESC = 500
GOOGLE_ATTEMPTS_PER_BOOK = 2
GOOGLE_CONSECUTIVE_429_LIMIT = 3
SLEEP_MIN, SLEEP_MAX = 8.0, 10.0


def _sleep_between():
    time.sleep(SLEEP_MIN + (SLEEP_MAX - SLEEP_MIN) * 0.5)


class _GoogleBlocked(Exception):
    pass


def _fetch_json(url, timeout=12):
    req = urllib.request.Request(url, headers={"User-Agent": BROWSER_UA})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode("utf-8"))


def _cover_https(url):
    if url and url.startswith("http://"):
        return "https://" + url[len("http://"):]
    return url


def _year_from_published(published_date):
    if not published_date:
        return None
    m = re.search(r"\d{4}", str(published_date))
    return int(m.group(0)) if m else None


def _subject_keyword(title):
    for kw in ("математика", "алгебра", "геометрия", "физика", "история",
               "русский", "литература", "биология", "информатика",
               "английский", "химия", "география", "обществознание", "информация"):
        if kw in title.lower():
            return kw
    return None


def _grade_of(title):
    """Extract grade number(s) from our Russian title, e.g. '5 класс' -> 5, '7–9' -> (7,9)."""
    low = title.lower()
    m = re.search(r"(\d+)\s*[–—-]\s*(\d+)", low)
    if m:
        return (int(m.group(1)), int(m.group(2)))
    m = re.search(r"(\d+)\s*класс", low)
    if m:
        return int(m.group(1))
    return None


def _score_result(api_title, our_title):
    """Heuristic match score. Higher is better. 0 = no match."""
    if not api_title:
        return 0
    score = 0
    al = api_title.lower().strip()
    ol = our_title.lower().strip()

    kw = _subject_keyword(our_title)
    if kw and kw in al:
        score += 5

    want_grade = _grade_of(our_title)
    got_grade = _grade_of(api_title)
    if want_grade is not None and got_grade is not None:
        if isinstance(want_grade, tuple) and isinstance(got_grade, tuple):
            score += max(0, 6 - abs(want_grade[0] - got_grade[0]) - abs(want_grade[1] - got_grade[1]))
        elif isinstance(want_grade, tuple):
            score += max(0, 3 - abs(want_grade[0] - got_grade) - abs(want_grade[1] - got_grade))
        else:
            score += max(0, 4 - abs(want_grade - got_grade))
    else:
        # no grade info -> neutral
        score += 1

    # shared significant tokens
    wtokens = [t for t in re.split(r"[^\wё]+", ol) if len(t) > 1]
    gtokens = [t for t in re.split(r"[^\wё]+", al) if len(t) > 1]
    hits = sum(1 for t in wtokens if t in gtokens)
    score += hits * 2
    return score


def _google_book(book, google_state):
    """Query Google Books. Returns entry dict or None. May raise _GoogleBlocked."""
    q = "intitle:{0}+inauthor:{1}".format(book["title"], book["author"])
    url = "https://www.googleapis.com/books/v1/volumes?q={0}&maxResults=1&country=US".format(
        urllib.parse.quote(q)
    )
    last_429 = 0
    for attempt in range(GOOGLE_ATTEMPTS_PER_BOOK):
        try:
            data = _fetch_json(url, timeout=8)
        except urllib.error.HTTPError as e:
            if e.code == 429:
                last_429 += 1
                google_state["consecutive_429"] += 1
                if google_state["consecutive_429"] >= GOOGLE_CONSECUTIVE_429_LIMIT:
                    google_state["blocked"] = True
                    raise _GoogleBlocked()
                # Keep trying this book without a long pause so blocking is
                # detected fast; politeness pause happens once per book below.
                continue
            if e.code in (500, 502, 503, 504) and attempt < GOOGLE_ATTEMPTS_PER_BOOK - 1:
                _sleep_between()
                continue
            return None
        except (urllib.error.URLError, TimeoutError, ConnectionError, OSError):
            if attempt < GOOGLE_ATTEMPTS_PER_BOOK - 1:
                _sleep_between()
                continue
            return None
        google_state["consecutive_429"] = 0
        break

    items = data.get("items") or []
    if not items:
        return None
    vi = items[0].get("volumeInfo") or {}
    entry = {}

    api_title = (vi.get("title") or "").strip()
    entry["title"] = api_title if api_title else book["title"]

    authors = [a for a in (vi.get("authors") or []) if a]
    entry["author"] = ", ".join(authors) if authors else book["author"]

    desc = (vi.get("description") or "").strip()
    if desc:
        desc = re.sub(r"\s+", " ", desc)
        if len(desc) > MAX_DESC:
            desc = desc[:MAX_DESC].rsplit(" ", 1)[0] + "…"
    entry["description"] = desc or ""

    entry["year"] = _year_from_published(vi.get("publishedDate"))

    cover = _cover_https(vi.get("imageLinks", {}).get("thumbnail"))
    entry["coverUrl"] = cover

    entry["source"] = "google_books"
    return entry


def _openlibrary(book):
    """Progressive OpenLibrary queries with fuzzy scoring. Returns entry or None."""
    candidates = []
    try:
        # 1. structured title+author
        url1 = "https://openlibrary.org/search.json?title={0}&author={1}&limit=5&fields=title,author_name,cover_i,first_publish_year,key".format(
            urllib.parse.quote(book["title"]), urllib.parse.quote(book["author"])
        )
        candidates += _fetch_json(url1, timeout=8).get("docs") or []
    except Exception:
        candidates += []

    if not candidates:
        try:
            # 2. full short title
            short = book["title"].replace(".", " ").strip()
            url2 = "https://openlibrary.org/search.json?q={0}&limit=5&fields=title,author_name,cover_i,first_publish_year,key".format(
                urllib.parse.quote(short)
            )
            candidates += _fetch_json(url2, timeout=8).get("docs") or []
        except Exception:
            candidates += []

    if not candidates:
        try:
            # 3. substring before "класс": e.g. "Математика 5"
            sub = re.split(r"класс", book["title"], flags=re.IGNORECASE)[0]
            sub = sub.replace(".", " ").replace("–", " ").replace("—", "-").strip()
            if sub:
                url3 = "https://openlibrary.org/search.json?q={0}&limit=5&fields=title,author_name,cover_i,first_publish_year,key".format(
                    urllib.parse.quote(sub)
                )
                candidates += _fetch_json(url3, timeout=8).get("docs") or []
        except Exception:
            candidates += []

    if not candidates:
        return None

    best = None
    best_score = 0
    for d in candidates:
        s = _score_result(d.get("title"), book["title"])
        if s > best_score:
            best_score = s
            best = d

    if best is None or best_score < 4:
        return None

    entry = {}
    cover_i = best.get("cover_i")
    entry["coverUrl"] = (
        "https://covers.openlibrary.org/b/id/{0}-L.jpg".format(cover_i)
        if cover_i else ""
    )
    entry["description"] = ""
    entry["year"] = best.get("first_publish_year")
    if entry["year"]:
        entry["year"] = int(entry["year"])

    auth = [a for a in (best.get("author_name") or []) if a]
    entry["author"] = ", ".join(auth) if auth else book["author"]

    api_title = (best.get("title") or "").strip()
    entry["title"] = api_title if api_title else book["title"]

    entry["source"] = "openlibrary"
    if not cover_i:
        return None
    return entry


def seed():
    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    out_path = os.path.join(repo_root, "app", "src", "main", "assets", "seed", "books.json")
    os.makedirs(os.path.dirname(out_path), exist_ok=True)

    google_state = {"consecutive_429": 0, "blocked": False}
    results = []
    ok_count = {"google_books": 0, "openlibrary": 0, "manual_direct": 0, "none": 0}

    for idx, book in enumerate(BOOKS):
        entry = {"id": book["id"], "title": book["title"], "author": book["author"],
                 "category": book["category"], "lang": "ru", "genre": "Учебник",
                 "coverUrl": None, "description": "", "year": None, "source": "none"}
        did_network = False

        g = None
        if not google_state["blocked"]:
            did_network = True
            try:
                g = _google_book(book, google_state)
            except _GoogleBlocked:
                g = None
                print("[GOOGLE-BLOCKED] устойчивая блокировка, больше не пробуем Google")
            except Exception:
                g = None

        if g and g.get("coverUrl"):
            entry.update(g)
        else:
            # OpenLibrary only if Google gave no usable cover (or Google blocked).
            try:
                ol = _openlibrary(book)
                did_network = True
            except Exception:
                ol = None
            if ol and ol.get("coverUrl"):
                entry.update(ol)

        # Manual fallback: if still no cover and we have a verified direct URL.
        if not entry.get("coverUrl") and book["id"] in MANUAL_COVERS:
            entry["coverUrl"] = MANUAL_COVERS[book["id"]]
            entry["source"] = "manual_direct"

        source = entry.get("source") or "none"
        if source not in ok_count:
            ok_count[source] = 0
        ok_count[source] += 1

        results.append(entry)
        cover_short = (entry.get("coverUrl") or "")[:30]
        print("[{0}] {1} | cover={2} | year={3} | src={4}".format(
            "FOUND" if entry.get("coverUrl") else "MISS",
            book["id"], cover_short, entry.get("year"), source
        ))

        # Pause only between real network requests (never after a pure manual fill).
        if did_network and idx < len(BOOKS) - 1:
            _sleep_between()

    payload = {
        "version": 1,
        "generatedAt": int(time.time() * 1000),
        "books": results,
    }

    with open(out_path, "w", encoding="utf-8", newline="") as f:
        json.dump(payload, f, ensure_ascii=False, indent=2)
        f.write("\n")

    print("TOTAL sources: {0}".format(
        ", ".join("{0}={1}".format(k, v) for k, v in sorted(ok_count.items()))
    ))
    print("BOOKS WITHOUT COVER: {0}".format(
        sum(1 for e in results if not e.get("coverUrl"))
    ))
    print(out_path)


if __name__ == "__main__":
    seed()
