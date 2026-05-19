import { useState, useEffect, useRef } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'

function ChatRoom() {
  const location = useLocation()
  const navigate = useNavigate()
  const [messages, setMessages] = useState([])
  const [users, setUsers] = useState([])
  const [inputMessage, setInputMessage] = useState('')
  const [systemMessage, setSystemMessage] = useState('')
  const [wsConnected, setWsConnected] = useState(false)
  const wsRef = useRef(null)
  const messagesEndRef = useRef(null)

  const params = new URLSearchParams(location.search)
  const roomNumber = params.get('room')
  const nickname = params.get('nickname')
  const sessionId = params.get('sessionId')

  useEffect(() => {
    if (!roomNumber || !nickname || !sessionId) {
      navigate('/')
      return
    }

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    const wsUrl = `${protocol}//${host}/ws/chat?roomNumber=${encodeURIComponent(roomNumber)}&nickname=${encodeURIComponent(nickname)}&sessionId=${encodeURIComponent(sessionId)}`
    let ws
    
    try {
      ws = new WebSocket(wsUrl)
      wsRef.current = ws
    } catch (err) {
      alert('创建连接失败，请返回重试')
      navigate('/')
      return
    }

    ws.onopen = () => {
      setWsConnected(true)
    }

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        
        if (data.type === 'SYSTEM') {
          setSystemMessage(data.content)
          setTimeout(() => setSystemMessage(''), 3000)
        } else if (data.type === 'USER_LIST') {
          const userList = JSON.parse(data.content)
          setUsers(Array.isArray(userList) ? userList : [])
        } else if (data.type === 'CHAT') {
          setMessages(prev => [...prev, data])
        }
      } catch (err) {
        console.error('消息解析失败', err)
      }
    }

    ws.onclose = (event) => {
      setWsConnected(false)
    }

    ws.onerror = (event) => {
      console.error('WebSocket 错误', event)
      alert('连接失败，请返回重试')
      navigate('/')
    }

    return () => {
      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.close()
      }
    }
  }, [roomNumber, nickname, sessionId, navigate])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const sendMessage = () => {
    if (!inputMessage.trim() || !wsConnected) return

    const message = {
      content: inputMessage.trim()
    }
    
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(message))
      setInputMessage('')
    }
  }

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      sendMessage()
    }
  }

  const leaveRoom = () => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.close()
    }
    navigate('/')
  }

  const fetchUsers = async () => {
    try {
      const response = await fetch(`/api/room/users/${roomNumber}`)
      const data = await response.json()
      setUsers(data)
    } catch (err) {
      console.error('获取用户列表失败', err)
    }
  }

  if (!roomNumber || !nickname) {
    return (
      <div className="loading">
        <div className="spinner"></div>
        <p>加载中...</p>
      </div>
    )
  }

  return (
    <div className="chat-container">
      <div className="chat-main">
        <div className="chat-header">
          <div className="room-info">
            <h3>房间 {roomNumber}</h3>
            <span className="online-count">{users.length} 人在线</span>
          </div>
          <button onClick={leaveRoom}>退出房间</button>
        </div>

        {systemMessage && (
          <div className="system-message">{systemMessage}</div>
        )}

        <div className="chat-messages">
          {messages.map((msg, index) => (
            <div 
              key={index} 
              className={`message ${msg.sender === nickname ? 'own' : 'other'}`}
            >
              <div className="bubble">
                <div className="sender">{msg.sender}</div>
                <div className="content">{msg.content}</div>
                <div className="timestamp">{msg.timestamp}</div>
              </div>
            </div>
          ))}
          <div ref={messagesEndRef} />
        </div>

        <div className="chat-input">
          <input
            type="text"
            value={inputMessage}
            onChange={(e) => setInputMessage(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="输入消息..."
            disabled={!wsConnected}
          />
          <button onClick={sendMessage} disabled={!wsConnected}>
            →
          </button>
        </div>
      </div>

      <div className="sidebar">
        <div className="sidebar-header">
          <h4>在线成员</h4>
          <div className="count">{users.length} 人在线</div>
        </div>
        <div className="user-list">
          {users.map((user) => (
            <div key={user.id || user.sessionId} className="user-item">
              <div className="avatar">
                {user.nickname ? user.nickname.charAt(0) : '?'}
              </div>
              <div className="nickname">{user.nickname}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

export default ChatRoom
