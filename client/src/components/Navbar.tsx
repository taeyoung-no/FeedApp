import { Link, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'

function Navbar() {
  const navigate = useNavigate()
  const user = useAuthStore((state) => state.user)
  const isLoading = useAuthStore((state) => state.isLoading)
  const logout = useAuthStore((state) => state.logout)

  const handleLogout = async () => {
    await logout()
    navigate('/')
  }

  const handleCreatePost = () => {
    if (user) {
      navigate('/posts/new')
    } else {
      navigate('/login')
    }
  }

  return (
    <header className="sticky top-0 bg-white border-b border-gray-200 mb-5">
      <nav className="max-w-2xl mx-auto py-4 flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Link to="/" className="text-2xl cursor-pointer hover:underline">
            Feed
          </Link>
          <button type="button" onClick={handleCreatePost} className="cursor-pointer hover:underline">
            글 발행
          </button>
        </div>

        {!isLoading &&
          (user ? (
            <div className="flex items-center gap-3">
              <span>{user.username}님</span>
              <button type="button" onClick={handleLogout} className="cursor-pointer hover:underline">
                로그아웃
              </button>
            </div>
          ) : (
            <div className="flex items-center gap-3">
              <Link to="/signup" className="cursor-pointer hover:underline">
                회원가입
              </Link>
              <Link to="/login" className="cursor-pointer hover:underline">
                로그인
              </Link>
            </div>
          ))}
      </nav>
    </header>
  )
}

export default Navbar
