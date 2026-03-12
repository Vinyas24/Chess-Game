import React, { useState } from 'react';

const PIECE_UNICODE = {
  WHITE: { KING: "♔", QUEEN: "♕", ROOK: "♖", BISHOP: "♗", KNIGHT: "♘", PAWN: "♙" },
  BLACK: { KING: "♚", QUEEN: "♛", ROOK: "♜", BISHOP: "♝", KNIGHT: "♞", PAWN: "♟" }
};

const ChessBoard = ({ gameState, onMove }) => {
  const [selectedSquare, setSelectedSquare] = useState(null);
  const [promotionMove, setPromotionMove] = useState(null); // { from, to }

  const handleSquareClick = (r, c) => {
    if (gameState.checkmate || gameState.stalemate) return;
    
    const clickedPos = String.fromCharCode(97 + c) + (8 - r);
    
    // If waiting for promotion, ignore clicks on board
    if (promotionMove) return;

    if (selectedSquare) {
      if (selectedSquare === clickedPos) {
        setSelectedSquare(null); // deselect
        return;
      }
      
      const pieceAtSelected = gameState.board.find(p => p.pos === selectedSquare);
      const isValidTarget = gameState.validMoves[selectedSquare]?.includes(clickedPos);
      
      if (isValidTarget) {
        // Check if it's a promotion move (Pawn reaching 0 or 7 rank)
        if (pieceAtSelected && pieceAtSelected.type === 'PAWN' && (r === 0 || r === 7)) {
          setPromotionMove({ from: selectedSquare, to: clickedPos });
        } else {
          onMove(selectedSquare, clickedPos);
          setSelectedSquare(null);
        }
      } else {
        // Select new piece if it belongs to current player
        const clickedPiece = gameState.board.find(p => p.pos === clickedPos);
        if (clickedPiece && clickedPiece.color === gameState.sideToMove) {
          setSelectedSquare(clickedPos);
        } else {
          setSelectedSquare(null);
        }
      }
    } else {
      const clickedPiece = gameState.board.find(p => p.pos === clickedPos);
      if (clickedPiece && clickedPiece.color === gameState.sideToMove) {
        setSelectedSquare(clickedPos);
      }
    }
  };

  const executePromotion = (promType) => {
    onMove(promotionMove.from, promotionMove.to, promType);
    setPromotionMove(null);
    setSelectedSquare(null);
  };

  // Helper to find piece at r,c
  const getPieceAt = (r, c) => gameState.board.find(p => p.r === r && p.c === c);
  
  // Find King pos for check highlight
  const kingInCheckPos = () => {
    if (!gameState.inCheck) return null;
    const king = gameState.board.find(p => p.type === 'KING' && p.color === gameState.sideToMove);
    return king ? king.pos : null;
  };

  const validTargets = selectedSquare ? (gameState.validMoves[selectedSquare] || []) : [];
  const checkPos = kingInCheckPos();

  return (
    <div className="relative">
      <div className="grid grid-cols-8 grid-rows-8 border-[6px] border-[#2A2D34] rounded-sm overflow-hidden shadow-2xl">
        {[...Array(8)].map((_, r) => (
          [...Array(8)].map((_, c) => {
            const isLight = (r + c) % 2 === 0;
            const pos = String.fromCharCode(97 + c) + (8 - r);
            const piece = getPieceAt(r, c);
            const isSelected = selectedSquare === pos;
            const isValidMove = validTargets.includes(pos);
            const isCheck = checkPos === pos;
            
            let bgClass = isLight ? 'square-light' : 'square-dark';
            if (isSelected) bgClass = '!bg-yellow-400/60';
            else if (isCheck) bgClass = 'square-check';
            else if (isValidMove && piece) bgClass = '!bg-red-400/50'; // attack
            else if (isValidMove) bgClass = '!bg-green-400/30';

            return (
              <div 
                key={`${r}-${c}`}
                onClick={() => handleSquareClick(r, c)}
                className={`w-10 h-10 sm:w-12 sm:h-12 md:w-16 md:h-16 lg:w-20 lg:h-20 flex justify-center items-center cursor-pointer select-none transition-colors relative ${bgClass}`}
              >
                {isValidMove && !piece && (
                  <div className="absolute w-3 h-3 sm:w-4 sm:h-4 bg-black/20 rounded-full shadow-inner pointer-events-none"></div>
                )}
                {piece && (
                  <span className={`text-[40px] sm:text-[48px] md:text-[60px] lg:text-[72px] leading-none pointer-events-none transition-transform ${isSelected ? 'scale-110 drop-shadow-xl' : 'drop-shadow-md'} ${piece.color === 'WHITE' ? 'text-[#fff]' : 'text-[#2b2b2b]'}`}>
                    {PIECE_UNICODE[piece.color][piece.type]}
                  </span>
                )}
                
                {/* Coordinates (bottom/left edges) */}
                {c === 0 && <span className={`absolute top-0.5 left-1 text-[10px] sm:text-xs font-bold opacity-60 pointer-events-none ${isLight ? 'text-slate-800' : 'text-slate-200'}`}>{8 - r}</span>}
                {r === 7 && <span className={`absolute bottom-0.5 right-1 text-[10px] sm:text-xs font-bold opacity-60 pointer-events-none ${isLight ? 'text-slate-800' : 'text-slate-200'}`}>{String.fromCharCode(97 + c)}</span>}
              </div>
            );
          })
        ))}
      </div>

      {promotionMove && (
        <div className="absolute inset-0 bg-black/50 backdrop-blur-md flex justify-center items-center z-20 rounded-lg">
          <div className="bg-slate-800 p-6 rounded-2xl shadow-2xl border border-slate-600 flex flex-col gap-4 text-center">
            <h3 className="text-white font-bold text-lg">Promote Pawn to:</h3>
            <div className="flex gap-3">
              {['q', 'r', 'b', 'n'].map(type => (
                <button
                  key={type}
                  onClick={() => executePromotion(type)}
                  className="w-16 h-16 sm:w-20 sm:h-20 bg-slate-700 hover:bg-slate-600 active:bg-slate-500 rounded-xl flex justify-center items-center text-5xl transition-all hover:scale-105 shadow-lg border border-slate-500"
                >
                  <span className={gameState.sideToMove === 'WHITE' ? 'text-white' : 'text-[#2b2b2b]'}>
                    {PIECE_UNICODE[gameState.sideToMove][type === 'q' ? 'QUEEN' : type === 'r' ? 'ROOK' : type === 'b' ? 'BISHOP' : 'KNIGHT']}
                  </span>
                </button>
              ))}
            </div>
            <button 
              onClick={() => setPromotionMove(null)}
              className="mt-2 text-slate-400 hover:text-white text-sm"
            >
              Cancel
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default ChessBoard;
