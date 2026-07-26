import { zodResolver } from '@hookform/resolvers/zod'
import axios from 'axios'
import { useEffect, useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getPost, updatePost } from '../api/post'
import { createPostSchema, type CreatePostFormData } from '../schemas/post'
import { useAuthStore } from '../store/authStore'

function EditPostPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const user = useAuthStore((state) => state.user)
  const isAuthLoading = useAuthStore((state) => state.isLoading)

  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CreatePostFormData>({
    resolver: zodResolver(createPostSchema),
  })

  const content = useWatch({ control, name: 'content', defaultValue: '' })
  const contentLength = content.length
  const contentMaxLength = 500

  useEffect(() => {
    if (!isAuthLoading && !user) {
      navigate('/login', { replace: true })
    }
  }, [isAuthLoading, user, navigate])

  useEffect(() => {
    if (!id || isAuthLoading || !user) return

    let cancelled = false

    const load = async () => {
      try {
        const post = await getPost(id)
        if (cancelled) return

        if (post.author !== user.username) {
          setError('권한 없음')
          setIsLoading(false)
          return
        }

        reset({ title: post.title, content: post.content })
        setError(null)
      } catch {
        if (!cancelled) {
          setError('글을 불러오지 못했습니다')
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
  }, [id, isAuthLoading, user, reset])

  const onSubmit = async (values: CreatePostFormData) => {
    if (!id) return

    try {
      await updatePost(id, {
        title: values.title,
        content: values.content,
      })
      navigate(`/posts/${id}`)
    } catch (err) {
      if (axios.isAxiosError(err)) {
        alert(err.response?.data?.message ?? '글 수정 실패')
        return
      }
      alert('글 수정 실패')
    }
  }

  if (isAuthLoading || !user) {
    return null
  }

  if (isLoading) {
    return (
      <main className="max-w-2xl mx-auto w-full">
        <p className="text-center text-2xl">글 불러오는 중...</p>
      </main>
    )
  }

  if (error) {
    return (
      <main className="max-w-2xl mx-auto w-full">
        <p className="text-center text-2xl">{error}</p>
      </main>
    )
  }

  return (
    <main className="max-w-2xl mx-auto w-full">
      <h2 className="text-xl mb-4">글 수정</h2>

      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="space-y-1 mb-6">
          <div>
            <h4>제목</h4>
            <input type="text" className="w-full px-4 py-3 border border-gray-300" {...register('title')} />
            <div className="min-h-6">{errors.title && <p className="text-red-500">{errors.title.message}</p>}</div>
          </div>

          <div>
            <div className="flex items-baseline justify-between gap-2">
              <h4>내용</h4>
              <span className="text-gray-500">
                {contentLength} / {contentMaxLength}
              </span>
            </div>
            <textarea rows={10} className="w-full px-4 py-3 border border-gray-300 resize-y" {...register('content')} />
            <div className="min-h-6">{errors.content && <p className="text-red-500">{errors.content.message}</p>}</div>
          </div>
        </div>

        <div className="flex justify-end gap-2">
          <Link to={id ? `/posts/${id}` : '/'} className="cursor-pointer px-3 py-2 hover:underline">
            취소
          </Link>
          <button
            type="submit"
            disabled={isSubmitting}
            className="cursor-pointer px-3 py-2 hover:underline disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isSubmitting ? '저장 중…' : '저장'}
          </button>
        </div>
      </form>
    </main>
  )
}

export default EditPostPage
