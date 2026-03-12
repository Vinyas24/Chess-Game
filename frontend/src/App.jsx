import { useState, useEffect } from 'react'
import Confetti from 'react-confetti'
import ChessBoard from './components/ChessBoard'
import { RefreshCw, Swords } from 'lucide-react'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'https://chess-game-8x4p.onrender.com';

function App() {
  const [gameState, setGameState] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [windowSize, setWindowSize] = useState({ width: window.innerWidth, height: window.innerHeight })
  
  const [gameMode, setGameMode] = useState(null) // 'pvp' | 'pve'
  const [playerColor, setPlayerColor] = useState('WHITE') // 'WHITE' | 'BLACK'

  useEffect(() => {
    const handleResize = () => setWindowSize({ width: window.innerWidth, height: window.innerHeight })
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  useEffect(() => {
    if (!gameState || gameMode !== 'pve') return;
    if (gameState.checkmate || gameState.stalemate) return;
    
    if (gameState.sideToMove !== playerColor) {
      const fetchAiMove = async () => {
        try {
          const res = await fetch(`${API_BASE_URL}/api/chess/ai-move`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ gameId: gameState.gameId })
          })
          const data = await res.json()
          if (data.error) alert(data.error)
          else setGameState(data)
        } catch (err) {
          console.error(err)
        }
      }
      const timer = setTimeout(fetchAiMove, 600)
      return () => clearTimeout(timer)
    }
  }, [gameState, gameMode, playerColor])

  const fetchNewGame = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await fetch(`${API_BASE_URL}/api/chess/new`, { method: 'POST' })
      if (!res.ok) throw new Error('Failed to start a new game')
      const data = await res.json()
      setGameState(data)
      localStorage.setItem('chessGameId', data.gameId)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const fetchState = async (gameId) => {
    try {
      const res = await fetch(`${API_BASE_URL}/api/chess/state?gameId=${gameId}`)
      if (!res.ok) throw new Error('Game not found')
      const data = await res.json()
      setGameState(data)
    } catch (err) {
      fetchNewGame()
    }
  }

  useEffect(() => {
    const savedId = localStorage.getItem('chessGameId')
    if (savedId) {
      // If we had a game, we don't know the mode for sure in this simple app, 
      // but we will just leave it at the menu screen so the user can choose.
      // fetchState(savedId)
    }
  }, [])

  const startGame = (mode, color = 'WHITE') => {
    setGameMode(mode);
    setPlayerColor(color);
    fetchNewGame();
  }

  const handleMove = async (from, to, promotion = null) => {
    try {
      const res = await fetch(`${API_BASE_URL}/api/chess/move`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ gameId: gameState.gameId, from, to, promotion })
      })
      const data = await res.json()
      if (data.error) {
        alert(data.error)
      } else {
        setGameState(data)
      }
    } catch (err) {
      console.error(err)
    }
  }

  return (
    <>
      {gameState && gameState.checkmate && (
        <Confetti
          width={windowSize.width}
          height={windowSize.height}
          recycle={false}
          numberOfPieces={500}
          gravity={0.15}
          className="z-50"
        />
      )}
      
      <div className="min-h-screen flex flex-col items-center justify-center p-4">
        {gameMode === null ? (
          <div className="glass-panel max-w-lg w-full p-10 flex flex-col gap-8 text-center relative z-10 transition-all">
            <div className="absolute top-0 left-0 w-32 h-32 bg-blue-500 rounded-full blur-[100px] opacity-20 pointer-events-none"></div>
            <div className="flex justify-center">
              <Swords className="text-blue-400 w-16 h-16 drop-shadow-[0_0_15px_rgba(96,165,250,0.5)]"/>
            </div>
            <h1 className="text-5xl font-extrabold pb-2 bg-clip-text text-transparent bg-gradient-to-r from-blue-400 to-purple-400">
              Premium Chess
            </h1>
            <p className="text-slate-400">Select a game mode to begin</p>
            
            <div className="flex flex-col gap-4 mt-4">
              <button 
                onClick={() => startGame('pvp')}
                className="w-full py-4 bg-slate-800 hover:bg-slate-700 border border-slate-600 rounded-xl font-bold text-lg text-white transition-all hover:scale-105 shadow-lg"
              >
                Pass and Play (Local 1v1)
              </button>
              
              <div className="h-px w-full bg-gradient-to-r from-transparent via-slate-600 to-transparent my-2"></div>
              
              <h3 className="text-slate-300 font-bold">Play vs Artificial Intelligence</h3>
              <div className="flex gap-4">
                <button 
                  onClick={() => startGame('pve', 'WHITE')}
                  className="flex-1 py-4 bg-slate-200 hover:bg-white text-slate-900 border border-slate-300 rounded-xl font-bold text-lg transition-all hover:scale-105 shadow-lg"
                >
                  Play White ♙
                </button>
                <button 
                  onClick={() => startGame('pve', 'BLACK')}
                  className="flex-1 py-4 bg-slate-900 hover:bg-black border border-slate-700 rounded-xl font-bold text-lg text-white transition-all hover:scale-105 shadow-lg"
                >
                  Play Black ♟
                </button>
              </div>
            </div>
          </div>
        ) : (
          <div className="glass-panel w-full max-w-6xl p-6 md:p-10 flex flex-col lg:flex-row gap-10 items-center justify-center relative z-10">
        
        {/* Decorative elements */}
        <div className="absolute top-0 left-0 w-32 h-32 bg-blue-500 rounded-full blur-[100px] opacity-20 pointer-events-none"></div>
        <div className="absolute bottom-0 right-0 w-48 h-48 bg-purple-500 rounded-full blur-[120px] opacity-20 pointer-events-none"></div>
        
        {/* Left Side: Board */}
        <div className="flex-1 flex justify-center items-center">
          {loading ? (
            <div className="flex flex-col items-center gap-4">
              <div className="animate-spin rounded-full h-16 w-16 border-t-4 border-b-4 border-blue-400"></div>
              <p className="text-blue-200 font-medium tracking-wide animate-pulse">Initializing Board...</p>
            </div>
          ) : error ? (
            <div className="text-red-400 bg-red-900/30 p-6 rounded-2xl border border-red-500/50">
              {error}
              <button 
                onClick={fetchNewGame}
                className="mt-4 w-full py-2 bg-red-800 hover:bg-red-700 rounded-lg text-white"
              >
                Retry
              </button>
            </div>
          ) : gameState ? (
            <ChessBoard 
              gameState={gameState} 
              onMove={handleMove} 
              isPlayerTurn={gameMode === 'pvp' || gameState.sideToMove === playerColor} 
            />
          ) : null}
        </div>

        {/* Right Side: Info Panel */}
        <div className="w-full lg:w-[350px] flex flex-col gap-6 bg-slate-800/50 p-6 rounded-2xl border border-white/5">
          <div className="text-center lg:text-left flex items-center justify-center lg:justify-start gap-3">
            <Swords className="text-blue-400 w-8 h-8"/>
            <div>
              <h1 className="text-3xl font-extrabold bg-clip-text text-transparent bg-gradient-to-r from-blue-400 to-purple-400">
                Premium Chess
              </h1>
              <p className="text-slate-400 text-sm font-medium tracking-wide">
                {gameMode === 'pvp' ? 'Pass and Play Mode' : `vs AI (Playing as ${playerColor})`}
              </p>
            </div>
          </div>

          <div className="h-px w-full bg-gradient-to-r from-transparent via-slate-600 to-transparent my-2"></div>

          {gameState && !loading && (
            <div className="flex flex-col gap-5 flex-1 justify-center">
              
              {!gameState.checkmate && !gameState.stalemate && (
                <div className={`p-5 rounded-xl border relative overflow-hidden transition-all duration-500 ${gameState.sideToMove === 'WHITE' ? 'bg-slate-200 text-slate-900 border-white' : 'bg-slate-900 text-white border-slate-700'}`}>
                  <div className={`absolute top-0 right-0 w-24 h-24 rounded-full blur-2xl blur-opacity-50 -mr-10 -mt-10 ${gameState.sideToMove === 'WHITE' ? 'bg-white' : 'bg-black'}`}></div>
                  <h3 className="text-sm uppercase tracking-widest font-bold opacity-70 mb-1">To Move</h3>
                  <p className="capitalize text-3xl font-black drop-shadow-md relative z-10 flex items-center gap-3">
                    {gameState.sideToMove.toLowerCase()}
                    <span className="text-4xl">{gameState.sideToMove === 'WHITE' ? '♙' : '♟'}</span>
                  </p>
                </div>
              )}

              {gameState.inCheck && !gameState.checkmate && (
                <div className="p-4 rounded-xl bg-red-500/20 border border-red-500/50 text-red-200 font-bold flex items-center gap-3 animate-pulse">
                  <span className="text-2xl">⚠️</span> Check!
                </div>
              )}
              
              {gameState.checkmate && (
                <div className="p-6 rounded-xl bg-gradient-to-br from-red-600 to-red-900 border border-red-400 text-white shadow-[0_0_30px_rgba(220,38,38,0.5)] transform hover:scale-105 transition-transform duration-300">
                  <h3 className="text-xl font-bold mb-2 flex items-center gap-2">
                    <span className="text-3xl">🎉</span> Checkmate!
                  </h3>
                  <p className="text-lg">
                    <span className="font-extrabold capitalize text-yellow-300">{gameState.sideToMove === 'WHITE' ? 'Black' : 'White'}</span> is victorious!
                  </p>
                </div>
              )}

              {gameState.stalemate && (
                <div className="p-6 rounded-xl bg-gradient-to-br from-orange-500 to-amber-700 border border-orange-400 text-white font-bold text-lg shadow-[0_0_30px_rgba(249,115,22,0.5)]">
                  🤝 Stalemate. The game is drawn!
                </div>
              )}

              <button 
                onClick={() => setGameMode(null)}
                className="mt-6 flex items-center justify-center gap-3 py-4 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 transition-all rounded-xl font-bold shadow-[0_0_20px_rgba(59,130,246,0.4)] hover:shadow-[0_0_30px_rgba(59,130,246,0.6)] text-white hover:-translate-y-1 active:translate-y-0"
              >
                <RefreshCw size={20} className={loading ? "animate-spin" : ""} /> Main Menu
              </button>
            </div>
          )}

        </div>
        </div>
        )}
      </div>
    </>
  )
}

export default App
