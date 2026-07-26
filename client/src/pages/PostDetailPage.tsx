import { zodResolver } from '@hookform/resolvers/zod'
import axios from 'axios'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { createComment, getComments, type CommentResponse } from '../api/comment'
import { deletePost, getPost, type PostResponse } from '../api/post'
import { createCommentSchema, type CreateCommentFormData } from '../schemas/comment'
import { useAuthStore } from '../store/authStore'

function PostDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const user = useAuthStore((state) => state.user)
  const [post, setPost] = useState<PostResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)

  const [comments, setComments] = useState<CommentResponse[]>([])
  const [commentsError, setCommentsError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CreateCommentFormData>({
    resolver: zodResolver(createCommentSchema),
  })

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

  useEffect(() => {
    if (!id) return

    let cancelled = false

    const loadComments = async () => {
      try {
        const data = await getComments(id)
        if (!cancelled) {
          setComments(data)
          setCommentsError(null)
        }
      } catch {
        if (!cancelled) {
          setCommentsError('댓글을 불러오지 못했습니다')
          setComments([])
        }
      }
    }

    loadComments()
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

  const onSubmitComment = async (values: CreateCommentFormData) => {
    if (!id) return

    if (!user) {
      navigate('/login')
      return
    }

    try {
      const created = await createComment(id, { content: values.content })
      setComments((prev) => [created, ...prev])
      reset()
    } catch (err) {
      if (axios.isAxiosError(err)) {
        alert(err.response?.data?.message ?? '댓글 작성 실패')
      } else {
        alert('댓글 작성 실패')
      }
    }
  }

  return (
    <main className="max-w-2xl mx-auto w-full">
      {isLoading && <p className="text-center text-2xl">글 불러오는 중...</p>}
      {error && <p className="text-center text-2xl">{error}</p>}

      {!isLoading && !error && post && (
        <>
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

          <section className="mt-10">
            <h3 className="text-xl mb-4">댓글</h3>

            {commentsError && <p className="text-gray-500 mb-4">{commentsError}</p>}

            <ul className="space-y-4 mb-6">
              {comments.length === 0 && !commentsError && <li className="text-gray-500">댓글이 없습니다</li>}
              {comments.map((comment) => (
                <li key={comment.id}>
                  <p className="text-sm text-gray-500 mb-1">{`${comment.author} · ${formatDate(comment.createdAt)}`}</p>
                  <p className="whitespace-pre-wrap break-words">{comment.content}</p>
                </li>
              ))}
            </ul>

            <form onSubmit={handleSubmit(onSubmitComment)} noValidate>
              <input
                type="text"
                placeholder="댓글을 입력하세요"
                className="w-full px-4 py-3 border border-gray-300"
                disabled={isSubmitting}
                {...register('content')}
              />
              <div className="min-h-6">
                {errors.content && <p className="text-red-500">{errors.content.message}</p>}
              </div>
              <div className="flex justify-end">
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="cursor-pointer px-3 py-2 hover:underline disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {isSubmitting ? '작성 중…' : '작성'}
                </button>
              </div>
            </form>
          </section>
        </>
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
