import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { getPost, type PostResponse } from '../api/post'

function PostDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [post, setPost] = useState<PostResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return

    let cancelled = false

    const load = async () => {
      try {
        const data = await getPost(id)
        if (!cancelled) {
          setPost(data)
          setError(null)
        }
      } catch {
        if (!cancelled) {
          setError('글을 불러오지 못했습니다')
          setPost(null)
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    load()
    return () => {
      cancelled = true
    }
  }, [id])

  return (
    <main className="max-w-2xl mx-auto w-full">
      {isLoading && <p className="text-center text-2xl">글 불러오는 중...</p>}
      {error && <p className="text-center text-2xl">{error}</p>}

      {!isLoading && !error && post && (
        <article>
          <h2 className="text-3xl mb-2">{post.title}</h2>
          <p className="text-sm text-gray-500 mb-6">{`${post.author} · ${formatDate(post.createdAt)}`}</p>
          <div className="whitespace-pre-wrap break-words text-xl">{post.content}</div>
        </article>
      )}
    </main>
  )
}

function formatDate(iso: string) {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  return date.toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export default PostDetailPage
