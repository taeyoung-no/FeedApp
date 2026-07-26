import axios from 'axios'
import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { deletePost, getPost, type PostResponse } from '../api/post'
import { useAuthStore } from '../store/authStore'

function PostDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const user = useAuthStore((state) => state.user)
  const [post, setPost] = useState<PostResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)

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

  const handleDelete = async () => {
    if (!id || isDeleting) return

    setIsDeleting(true)
    try {
      await deletePost(id)
      navigate('/')
    } catch (err) {
      if (axios.isAxiosError(err)) {
        alert(err.response?.data?.message ?? '글 삭제 실패')
      } else {
        alert('글 삭제 실패')
      }
      setIsDeleting(false)
    }
  }

  return (
    <main className="max-w-2xl mx-auto w-full">
      {isLoading && <p className="text-center text-2xl">글 불러오는 중...</p>}
      {error && <p className="text-center text-2xl">{error}</p>}

      {!isLoading && !error && post && (
        <article>
          <h2 className="text-3xl mb-2">{post.title}</h2>
          <div className="flex items-center gap-3 text-gray-500 mb-6">
            <p>{`${post.author} · ${formatDate(post.createdAt)}`}</p>
            {user && user.username === post.author && (
              <>
                <Link to={`/posts/${post.id}/edit`} className="text-black cursor-pointer hover:underline">
                  수정
                </Link>
                <button
                  type="button"
                  onClick={handleDelete}
                  disabled={isDeleting}
                  className="text-black cursor-pointer hover:underline disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {isDeleting ? '삭제 중…' : '삭제'}
                </button>
              </>
            )}
          </div>
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
