import { Link } from 'react-router-dom'

function Navbar() {
  return (
    <header className="sticky top-0 bg-white border-b border-gray-200 mb-5">
      <nav className="max-w-2xl mx-auto py-4 flex items-center justify-between">
        <div className="flex items-center gap-4">
            <h1 className="text-2xl">Feed</h1>
          <button type="button" className="cursor-pointer hover:underline">
            글 발행
          </button>
        </div>

        <div className="flex items-center gap-3">
          <Link to="/signup" className="cursor-pointer hover:underline">
            회원가입
          </Link>
          <button type="button" className="cursor-pointer hover:underline">
            로그인
          </button>
        </div>
      </nav>
    </header>
  )
}

export default Navbar
