import { useEffect, useState } from 'react'
import axios from 'axios'
import './App.css'

function App() {
  const [message, setMessage] = useState('')

  useEffect(() => {
    const fetchHello = async () => {
      try {
        const res = await axios.get<string>('/api/hello', { responseType: 'text' })
        setMessage(res.data)
      } catch {
        setMessage('뭔가 잘못됨')
      }
    }
    void fetchHello()
  }, [])

  return <h1>{message}</h1>
}

export default App
