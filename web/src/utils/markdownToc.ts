import { marked, Renderer, Tokenizer, type Tokens } from 'marked'

export interface MarkdownHeading {
  id: string
  level: number
  text: string
}

export interface MarkdownRenderResult {
  html: string
  headings: MarkdownHeading[]
}

class MarkdownPreviewTokenizer extends Tokenizer {
  override del(src: string, maskedSrc: string, prevChar = '') {
    if (src.startsWith('~') && !src.startsWith('~~')) {
      return undefined
    }

    return super.del(src, maskedSrc, prevChar)
  }
}

function normalizeHeadingText(text: string) {
  return text.replace(/\s+/g, ' ').trim()
}

export function slugHeadingId(text: string) {
  const slug = text
    .normalize('NFKC')
    .trim()
    .toLowerCase()
    .replace(/\s+/g, '-')
    .replace(/[^\p{L}\p{N}_-]+/gu, '')
    .replace(/-+/g, '-')
    .replace(/^-|-$/g, '')

  return `heading-${slug || 'section'}`
}

function uniqueHeadingId(text: string, headingCounts: Map<string, number>) {
  const baseId = slugHeadingId(text)
  const count = headingCounts.get(baseId) || 0
  headingCounts.set(baseId, count + 1)
  return count === 0 ? baseId : `${baseId}-${count + 1}`
}

export function renderMarkdownWithToc(content: string): MarkdownRenderResult {
  const headings: MarkdownHeading[] = []
  const headingCounts = new Map<string, number>()
  const renderer = new Renderer()

  renderer.heading = function ({ tokens, depth }: Tokens.Heading) {
    const text = normalizeHeadingText(this.parser.parseInline(tokens, this.parser.textRenderer))
    const id = uniqueHeadingId(text, headingCounts)
    headings.push({ id, level: depth, text })
    return `<h${depth} id="${id}">${this.parser.parseInline(tokens)}</h${depth}>\n`
  }

  return {
    html: marked.parse(content, { renderer, breaks: true, tokenizer: new MarkdownPreviewTokenizer() }) as string,
    headings,
  }
}
