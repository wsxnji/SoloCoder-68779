import { useState, useEffect, useRef, useCallback } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'

function ChatRoom() {
  const location = useLocation()
  const navigate = useNavigate()
  const [messages, setMessages] = useState([])
  const [users, setUsers] = useState([])
  const [inputMessage, setInputMessage] = useState('')
  const [systemMessage, setSystemMessage] = useState('')
  const [wsConnected, setWsConnected] = useState(false)
  const [loadingHistory, setLoadingHistory] = useState(false)
  const [hasMoreHistory, setHasMoreHistory] = useState(true)
  const wsRef = useRef(null)
  const messagesEndRef = useRef(null)
  const messagesContainerRef = useRef(null)
  const fileInputRef = useRef(null)
  const initialLoadDone = useRef(false)

  const params = new URLSearchParams(location.search)
  const roomNumber = params.get('room')
  const nickname = params.get('nickname')
  const sessionId = params.get('sessionId')

  const getOldestMessageId = useCallback(() => {
    if (messages.length > 0) {
      const oldestMsg = messages[0]
      if (oldestMsg && oldestMsg.sessionId && !isNaN(parseInt(oldestMsg.sessionId))) {
        return parseInt(oldestMsg.sessionId)
      }
      if (oldestMsg && oldestMsg.id) {
        return oldestMsg.id
      }
    }
    return null
  }, [messages])

  const loadMoreHistory = useCallback(async () => {
    if (loadingHistory || !hasMoreHistory || !roomNumber) return

    const oldestId = getOldestMessageId()
    if (!oldestId) return

    setLoadingHistory(true)
    try {
      const url = `/api/room/messages/${encodeURIComponent(roomNumber)}?count=10&beforeId=${oldestId}`
      const response = await fetch(url)
      const historyMessages = await response.json()

      if (Array.isArray(historyMessages) && historyMessages.length > 0) {
        const newMessages = historyMessages.map(msg => ({
          ...msg,
          type: msg.type || 'CHAT',
          sessionId: msg.id ? msg.id.toString() : msg.sessionId
        }))
        setMessages(prev => [...newMessages, ...prev])
        if (historyMessages.length < 10) {
          setHasMoreHistory(false)
        }
      } else {
        setHasMoreHistory(false)
      }
    } catch (err) {
      console.error('加载历史消息失败', err)
    } finally {
      setLoadingHistory(false)
    }
  }, [loadingHistory, hasMoreHistory, roomNumber, getOldestMessageId])

  const handleScroll = useCallback((e) => {
    const { scrollTop } = e.target
    if (scrollTop <= 0 && !loadingHistory && hasMoreHistory && initialLoadDone.current) {
      loadMoreHistory()
    }
  }, [loadingHistory, hasMoreHistory, loadMoreHistory])

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
        } else if (data.type === 'HISTORY_END') {
          initialLoadDone.current = true
          messagesEndRef.current?.scrollIntoView({ behavior: 'auto' })
        } else if (data.type === 'CHAT' || data.type === 'IMAGE') {
          setMessages(prev => {
            const isDuplicate = prev.some(m => 
              m.sessionId === data.sessionId && 
              m.content === data.content && 
              m.timestamp === data.timestamp
            )
            if (isDuplicate) return prev
            return [...prev, data]
          })
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
    if (initialLoadDone.current) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
    }
  }, [messages])

  const sendMessage = () => {
    if (!inputMessage.trim() || !wsConnected) return

    const message = {
      content: inputMessage.trim(),
      type: 'CHAT'
    }
    
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(message))
      setInputMessage('')
    }
  }

  const handleImageUpload = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return

    if (file.size > 10 * 1024 * 1024) {
      alert('图片大小不能超过10MB')
      return
    }

    if (!file.type.startsWith('image/')) {
      alert('只能上传图片文件')
      return
    }

    const formData = new FormData()
    formData.append('file', file)

    try {
      const response = await fetch('/api/room/upload', {
        method: 'POST',
        body: formData
      })
      const data = await response.json()

      if (data.success && data.url) {
        const message = {
          content: data.url,
          type: 'IMAGE'
        }
        
        if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
          wsRef.current.send(JSON.stringify(message))
        }
      } else {
        alert(data.message || '图片上传失败')
      }
    } catch (err) {
      console.error('图片上传失败', err)
      alert('图片上传失败，请重试')
    } finally {
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
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

  if (!roomNumber || !nickname) {
    return (
      <div className="loading">
        <div className="spinner"></div>
        <p>加载中...</p>
      </div>
    )
  }

  const renderMessageContent = (msg) => {
    if (msg.type === 'IMAGE') {
      return (
        <img 
          src={msg.content} 
          alt="发送的图片" 
          className="chat-image"
          onClick={(e) => {
            e.stopPropagation()
            window.open(msg.content, '_blank')
          }}
        />
      )
    }
    return <div className="content">{msg.content}</div>
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

        <div 
          className="chat-messages" 
          ref={messagesContainerRef}
          onScroll={handleScroll}
        >
          {loadingHistory && (
            <div className="loading-more">加载中...</div>
          )}
          {!hasMoreHistory && messages.length > 0 && (
            <div className="no-more">没有更多历史消息了</div>
          )}
          {messages.map((msg, index) => (
            <div 
              key={`${msg.sessionId}-${index}`} 
              className={`message ${msg.sender === nickname ? 'own' : 'other'}`}
            >
              <div className="bubble">
                <div className="sender">{msg.sender}</div>
                {renderMessageContent(msg)}
                <div className="timestamp">{msg.timestamp}</div>
              </div>
            </div>
          ))}
          <div ref={messagesEndRef} />
        </div>

        <div className="chat-input">
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleImageUpload}
            accept="image/*"
            style={{ display: 'none' }}
          />
          <button 
            className="image-btn"
            onClick={() => fileInputRef.current?.click()}
            disabled={!wsConnected}
            title="发送图片"
          >
            📷
          </button>
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
