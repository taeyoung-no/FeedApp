import { zodResolver } from '@hookform/resolvers/zod'
import axios from 'axios'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { signup } from '../api/member'
import { signupSchema, type SignupFormData } from '../schemas/signup'

function SignupPage() {
  const navigate = useNavigate()
  const [serverError, setServerError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<SignupFormData>({
    resolver: zodResolver(signupSchema),
  })

  const onSubmit = async (values: SignupFormData) => {
    setServerError(null)

    try {
      await signup({
        username: values.username,
        password: values.password,
      })
      navigate('/')
    } catch (error) {
      if (axios.isAxiosError(error)) {
        setServerError(error.response?.data?.message ?? '회원가입 실패')
        return
      }
      setServerError('회원가입 실패')
    }
  }

  return (
    <main className="max-w-2xl mx-auto w-full">
      <h2 className="text-xl mb-4">회원가입하세요</h2>

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
              autoComplete="new-password"
              className="w-full px-4 py-3 border border-gray-300"
              {...register('password')}
            />
            <div className="min-h-6">
              {errors.password && <p className="text-red-500">{errors.password.message}</p>}
            </div>
          </div>

          <div>
            <h4>비밀번호 확인</h4>
            <input
              type="password"
              autoComplete="new-password"
              className="w-full px-4 py-3 border border-gray-300"
              {...register('passwordConfirm')}
            />
            <div className="min-h-6">
              {errors.passwordConfirm && (
                <p className="text-red-500">{errors.passwordConfirm.message}</p>
              )}
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
            {isSubmitting ? '가입 중…' : '가입'}
          </button>
        </div>
      </form>
    </main>
  )
}

export default SignupPage
