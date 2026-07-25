import { zodResolver } from '@hookform/resolvers/zod'
import axios from 'axios'
import { useEffect } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { createPost } from '../api/post'
import { createPostSchema, type CreatePostFormData } from '../schemas/post'
import { useAuthStore } from '../store/authStore'

function CreatePostPage() {
  const navigate = useNavigate()
  const user = useAuthStore((state) => state.user)
  const isLoading = useAuthStore((state) => state.isLoading)

  const {
    register,
    handleSubmit,
    control,
    formState: { errors, isSubmitting },
  } = useForm<CreatePostFormData>({
    resolver: zodResolver(createPostSchema),
  })

  const content = useWatch({ control, name: 'content', defaultValue: '' })
  const contentLength = content.length
  const contentMaxLength = 500

  useEffect(() => {
    if (!isLoading && !user) {
      navigate('/login', { replace: true })
    }
  }, [isLoading, user, navigate])

  const onSubmit = async (values: CreatePostFormData) => {
    try {
      await createPost({
        title: values.title,
        content: values.content,
      })
      navigate('/')
    } catch (error) {
      if (axios.isAxiosError(error)) {
        alert(error.response?.data?.message ?? '글 작성 실패')
        return
      }
      alert('글 작성 실패')
    }
  }

  if (isLoading || !user) {
    return null
  }

  return (
    <main className="max-w-2xl mx-auto w-full">
      <h2 className="text-xl mb-4">글 작성</h2>

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
          <Link to="/" className="cursor-pointer px-3 py-2 hover:underline">
            취소
          </Link>
          <button
            type="submit"
            disabled={isSubmitting}
            className="cursor-pointer px-3 py-2 hover:underline disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isSubmitting ? '발행 중…' : '발행'}
          </button>
        </div>
      </form>
    </main>
  )
}

export default CreatePostPage
