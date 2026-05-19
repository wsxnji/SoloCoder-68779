import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

function generateSessionId() {
  return Date.now().toString(36) + Math.random().toString(36).substr(2)
}

function Login() {
  const [tab, setTab] = useState('create')
  const [nickname, setNickname] = useState('')
  const [roomNumber, setRoomNumber] = useState('')
  const [createdRoomNumber, setCreatedRoomNumber] = useState('')
  const [password, setPassword] = useState('')
  const [roomPassword, setRoomPassword] = useState('')
  const [passwordRequired, setPasswordRequired] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const createRoom = async () => {
    try {
      setLoading(true)
      setError('')
      const response = await fetch('/api/room/create', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          password: password.trim() || undefined
        })
      })
      if (!response.ok) {
        throw new Error('创建房间失败')
      }
      const data = await response.json()
      if (data.roomNumber) {
        setCreatedRoomNumber(data.roomNumber)
        setRoomNumber(data.roomNumber)
      } else {
        throw new Error('房间号生成失败')
      }
    } catch (err) {
      setError('创建房间失败，请重试')
      setCreatedRoomNumber('')
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    if (!nickname.trim()) {
      setError('请输入昵称')
      return
    }

    if (nickname.trim().length < 2 || nickname.trim().length > 50) {
      setError('昵称长度必须在2-50个字符之间')
      return
    }

    if (tab === 'create' && !createdRoomNumber) {
      setError('房间号正在生成中，请稍候...')
      return
    }

    const roomNum = (tab === 'create' ? createdRoomNumber : roomNumber) || ''
    if (!roomNum.trim()) {
      setError('房间号不能为空')
      return
    }

    const trimmedRoomNum = roomNum.trim()
    if (trimmedRoomNum.length !== 6 || !/^\d{6}$/.test(trimmedRoomNum)) {
      setError('房间号必须是6位数字')
      return
    }

    setLoading(true)
    try {
      const checkResponse = await fetch(`/api/room/check/${encodeURIComponent(trimmedRoomNum)}`)
      const checkData = await checkResponse.json()

      if (!checkData.exists) {
        setError('房间不存在，请检查房间号')
        setLoading(false)
        return
      }

      const submitPassword = tab === 'create' ? password : roomPassword
      if (tab === 'join' && checkData.passwordRequired && !submitPassword.trim()) {
        setError('该房间需要密码')
        setLoading(false)
        return
      }

      const sessionId = generateSessionId()
      const joinResponse = await fetch('/api/room/join', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          roomNumber: trimmedRoomNum,
          nickname: nickname.trim(),
          sessionId,
          password: submitPassword.trim() || undefined
        })
      })

      const joinData = await joinResponse.json()
      if (joinData.success) {
        navigate(`/chat?room=${trimmedRoomNum}&nickname=${encodeURIComponent(nickname.trim())}&sessionId=${sessionId}`)
      } else {
        setError(joinData.message || '加入房间失败')
      }
    } catch (err) {
      setError('网络错误，请重试')
    } finally {
      setLoading(false)
    }
  }

  const checkRoomPasswordRequired = async (roomNum) => {
    if (roomNum.trim().length === 6 && /^\d{6}$/.test(roomNum.trim())) {
      try {
        const response = await fetch(`/api/room/check/${encodeURIComponent(roomNum.trim())}`)
        const data = await response.json()
        setPasswordRequired(data.passwordRequired || false)
      } catch (err) {
        setPasswordRequired(false)
      }
    } else {
      setPasswordRequired(false)
    }
  }

  return (
    <div className="login-container">
      <div className="login-card">
        <h2>即时聊天室</h2>
        <p>免注册，一键加入</p>
        
        <div className="tabs">
          <div 
            className={`tab ${tab === 'create' ? 'active' : ''}`}
            onClick={() => {
              setTab('create')
              setCreatedRoomNumber('')
              setRoomNumber('')
              setPassword('')
              setRoomPassword('')
              setPasswordRequired(false)
              setError('')
            }}
          >
            创建房间
          </div>
          <div 
            className={`tab ${tab === 'join' ? 'active' : ''}`}
            onClick={() => {
              setTab('join')
              setCreatedRoomNumber('')
              setRoomNumber('')
              setPassword('')
              setRoomPassword('')
              setPasswordRequired(false)
              setError('')
            }}
          >
            加入房间
          </div>
        </div>

        {error && <div className="error">{error}</div>}

        {tab === 'create' && (
          <div className="room-preview">
            {createdRoomNumber ? (
              <div>
                <p style={{ marginBottom: '8px', color: '#666', fontSize: '14px' }}>您的房间号</p>
                <span>{createdRoomNumber}</span>
              </div>
            ) : (
              <div>
                <div className="form-group" style={{ marginBottom: '12px' }}>
                  <label>房间密码（可选）</label>
                  <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="不设置密码可直接加入"
                    maxLength={50}
                  />
                </div>
                <button 
                  className="btn btn-secondary"
                  onClick={createRoom}
                  disabled={loading}
                >
                  {loading ? '生成中...' : '生成房间号'}
                </button>
              </div>
            )}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          {tab === 'join' && (
            <>
              <div className="form-group">
                <label>房间号</label>
                <input
                  type="text"
                  value={roomNumber}
                  onChange={(e) => {
                    setRoomNumber(e.target.value)
                    checkRoomPasswordRequired(e.target.value)
                  }}
                  placeholder="请输入6位房间号"
                  maxLength={6}
                />
              </div>
              {passwordRequired && (
                <div className="form-group">
                  <label>房间密码</label>
                  <input
                    type="password"
                    value={roomPassword}
                    onChange={(e) => setRoomPassword(e.target.value)}
                    placeholder="请输入房间密码"
                    maxLength={50}
                  />
                </div>
              )}
            </>
          )}
          
          <div className="form-group">
            <label>匿名昵称</label>
            <input
              type="text"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              placeholder="请输入您的昵称（2-50个字符）"
              maxLength={50}
            />
            <div style={{ fontSize: '12px', color: '#999', marginTop: '4px' }}>
              {nickname.trim().length}/50
            </div>
          </div>

          <button 
            type="submit" 
            className="btn btn-primary"
            disabled={loading || (tab === 'create' && !createdRoomNumber)}
          >
            {loading ? '处理中...' : (tab === 'create' ? '进入房间' : '加入房间')}
          </button>
        </form>
      </div>
    </div>
  )
}

export default Login
