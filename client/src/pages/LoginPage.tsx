import { zodResolver } from '@hookform/resolvers/zod'
import axios from 'axios'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { login } from '../api/member'
import { loginSchema, type LoginFormData } from '../schemas/login'
import { useAuthStore } from '../store/authStore'

function LoginPage() {
  const navigate = useNavigate()
  const setUser = useAuthStore((state) => state.setUser)
  const [serverError, setServerError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  })

  const onSubmit = async (values: LoginFormData) => {
    setServerError(null)

    try {
      const user = await login({
        username: values.username,
        password: values.password,
      })
      setUser(user)
      navigate('/')
    } catch (error) {
      if (axios.isAxiosError(error)) {
        setServerError(error.response?.data?.message ?? '로그인 실패')
        return
      }
      setServerError('로그인 실패')
    }
  }

  return (
    <main className="max-w-2xl mx-auto w-full">
      <h2 className="text-xl mb-4">로그인하세요</h2>

      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="space-y-1 mb-6">
          <div>
            <h4>아이디</h4>
            <input
              type="text"
              autoComplete="username"
              className="w-full px-4 py-3 border border-gray-300"
              {...register('username')}
            />
            <div className="min-h-6">
              {errors.username && <p className="text-red-500">{errors.username.message}</p>}
            </div>
          </div>

          <div>
            <h4>비밀번호</h4>
            <input
              type="password"
              autoComplete="current-password"
              className="w-full px-4 py-3 border border-gray-300"
              {...register('password')}
            />
            <div className="min-h-6">
              {errors.password && <p className="text-red-500">{errors.password.message}</p>}
            </div>
          </div>
        </div>

        {serverError && <p className="text-red-500 mb-4">{serverError}</p>}

        <div className="flex justify-end gap-2">
          <Link to="/" className="cursor-pointer px-3 py-2 hover:underline">
            취소
          </Link>
          <button
            type="submit"
            disabled={isSubmitting}
            className="cursor-pointer px-3 py-2 hover:underline disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isSubmitting ? '로그인 중…' : '로그인'}
          </button>
        </div>
      </form>
    </main>
  )
}

export default LoginPage
