import { zodResolver } from '@hookform/resolvers/zod'
import axios from 'axios'
import { useEffect, useRef, useState, type ChangeEvent } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { createUploadUrl, deleteImage, putImageToS3 } from '../api/image'
import { createPost } from '../api/post'
import { createPostSchema, type CreatePostFormData } from '../schemas/post'
import { useAuthStore } from '../store/authStore'

const ACCEPTED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp']

type AttachedImage = {
  key: string
  name: string
}

function CreatePostPage() {
  const navigate = useNavigate()
  const user = useAuthStore((state) => state.user)
  const isLoading = useAuthStore((state) => state.isLoading)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [images, setImages] = useState<AttachedImage[]>([])
  const [isUploadingImage, setIsUploadingImage] = useState(false)

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

  const openFilePicker = () => {
    fileInputRef.current?.click()
  }

  const onImageSelected = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return

    if (!ACCEPTED_IMAGE_TYPES.includes(file.type)) {
      alert('jpg, png, webp만 가능')
      return
    }

    setIsUploadingImage(true)
    try {
      const { key, uploadUrl } = await createUploadUrl(file.type)
      await putImageToS3(uploadUrl, file)
      setImages((prev) => [...prev, { key, name: file.name }])
    } catch (error) {
      if (axios.isAxiosError(error)) {
        alert(error.response?.data?.message ?? '이미지 업로드 실패')
        return
      }
      alert('이미지 업로드 실패')
    } finally {
      setIsUploadingImage(false)
    }
  }

  const onRemoveImage = async (key: string) => {
    setIsUploadingImage(true)
    try {
      await deleteImage(key)
      setImages((prev) => prev.filter((image) => image.key !== key))
    } catch (error) {
      if (axios.isAxiosError(error)) {
        alert(error.response?.data?.message ?? '이미지 삭제 실패')
        return
      }
      alert('이미지 삭제 실패')
    } finally {
      setIsUploadingImage(false)
    }
  }

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
              <button
                type="button"
                onClick={openFilePicker}
                disabled={isUploadingImage}
                className="cursor-pointer hover:underline disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isUploadingImage ? '잠시만요!' : '이미지'}
              </button>
              <input
                ref={fileInputRef}
                type="file"
                accept={ACCEPTED_IMAGE_TYPES.join(',')}
                className="hidden"
                onChange={onImageSelected}
              />
            </div>
            <ul className="mt-2 space-y-1">
              {images.map((image) => (
                <li key={image.key} className="flex items-center justify-between gap-2">
                  <span className="truncate">{image.name}</span>
                  <button
                    type="button"
                    onClick={() => onRemoveImage(image.key)}
                    disabled={isUploadingImage}
                    className="shrink-0 cursor-pointer hover:underline disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    제거
                  </button>
                </li>
              ))}
            </ul>
            <textarea rows={10} className="w-full px-4 py-3 border border-gray-300 resize-y" {...register('content')} />
            <div className="flex items-start justify-between gap-2 min-h-6">
              <div>{errors.content && <p className="text-red-500">{errors.content.message}</p>}</div>
              <span className="text-gray-500 shrink-0">
                {contentLength} / {contentMaxLength}
              </span>
            </div>
          </div>
        </div>

        <div className="flex justify-end gap-2">
          <Link to="/" className="cursor-pointer px-3 py-2 hover:underline">
            취소
          </Link>
          <button
            type="submit"
            disabled={isSubmitting || isUploadingImage}
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
