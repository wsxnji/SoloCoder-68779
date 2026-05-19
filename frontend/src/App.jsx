import { Routes, Route } from 'react-router-dom'
import Login from './pages/Login.jsx'
import ChatRoom from './pages/ChatRoom.jsx'

function App() {
  return (
    <div className="app">
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/chat" element={<ChatRoom />} />
      </Routes>
    </div>
  )
}

export default App
