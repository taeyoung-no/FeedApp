import { Route, Routes } from 'react-router-dom'
import './App.css'
import Navbar from './components/Navbar'
import SignupPage from './pages/SignupPage'

function App() {
  return (
    <div className="flex flex-col min-h-screen">
      <Navbar />
      <Routes>
        <Route path="/" element={null} />
        <Route path="/signup" element={<SignupPage />} />
      </Routes>
    </div>
  )
}

export default App
