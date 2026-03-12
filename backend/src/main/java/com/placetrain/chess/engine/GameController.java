package com.placetrain.chess.engine;

import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/chess")
@CrossOrigin(origins = "*") // Allow frontend access
public class GameController {

    private Map<String, Board> activeGames = new ConcurrentHashMap<>();

    @PostMapping("/new")
    public GameResponse newGame() {
        String id = UUID.randomUUID().toString();
        Board board = new Board();
        activeGames.put(id, board);
        return convertToResponse(id, board, null);
    }

    @GetMapping("/state")
    public GameResponse getState(@RequestParam String gameId) {
        Board board = activeGames.get(gameId);
        if (board == null)
            throw new RuntimeException("Game not found");
        return convertToResponse(gameId, board, null);
    }

    @PostMapping("/move")
    public GameResponse makeMove(@RequestBody MoveRequest req) {
        Board board = activeGames.get(req.gameId);
        if (board == null)
            throw new RuntimeException("Game not found");

        try {
            Position from = new Position(req.from);
            Position to = new Position(req.to);
            String prom = req.promotion == null ? "q\n" : req.promotion + "\n";
            String err = board.makeMoveIfLegal(from, to, new Scanner(prom));
            return convertToResponse(req.gameId, board, err);
        } catch (Exception e) {
            return convertToResponse(req.gameId, board, "Invalid move: " + e.getMessage());
        }
    }

    @PostMapping("/ai-move")
    public GameResponse aiMove(@RequestBody MoveRequest req) {
        Board board = activeGames.get(req.gameId);
        if (board == null)
            throw new RuntimeException("Game not found");
        if (board.isInCheck(board.sideToMove) && !board.hasAnyLegalMove(board.sideToMove))
            return convertToResponse(req.gameId, board, "Game over");
        if (!board.isInCheck(board.sideToMove) && !board.hasAnyLegalMove(board.sideToMove))
            return convertToResponse(req.gameId, board, "Game over");

        try {
            Move bestMove = calculateBestMove(board, board.sideToMove);
            if (bestMove != null) {
                String err = board.makeMoveIfLegal(bestMove.from, bestMove.to, new Scanner("q\n"));
                return convertToResponse(req.gameId, board, err);
            } else {
                return convertToResponse(req.gameId, board, "No valid moves for AI");
            }
        } catch (Exception e) {
            return convertToResponse(req.gameId, board, "AI error: " + e.getMessage());
        }
    }

    private Move calculateBestMove(Board board, PieceColor color) {
        List<Move> legalMoves = board.generateLegalMoves(color);
        if (legalMoves.isEmpty())
            return null;

        Move bestMove = null;
        int maxScore = Integer.MIN_VALUE;

        // Shuffle to add variety to AI's opening moves and equal evaluations
        Collections.shuffle(legalMoves, new Random());

        for (Move move : legalMoves) {
            Move m = board.makeMoveObject(move.from, move.to);
            board.applyMove(m);

            int score = evaluateBoard(board, color);

            // Prioritize checkmates heavily
            if (board.isInCheck(board.sideToMove) && !board.hasAnyLegalMove(board.sideToMove)) {
                score += 10000;
            }

            board.undoMove();

            if (score > maxScore) {
                maxScore = score;
                bestMove = move;
            }
        }
        return bestMove == null ? legalMoves.get(0) : bestMove;
    }

    private int evaluateBoard(Board board, PieceColor myColor) {
        int score = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.grid[r][c];
                if (p != null) {
                    int val = 0;
                    switch (p.type) {
                        case PAWN:
                            val = 10;
                            break;
                        case KNIGHT:
                            val = 30;
                            break;
                        case BISHOP:
                            val = 30;
                            break;
                        case ROOK:
                            val = 50;
                            break;
                        case QUEEN:
                            val = 90;
                            break;
                        case KING:
                            val = 900;
                            break;
                    }
                    if (p.color == myColor) {
                        score += val;
                    } else {
                        score -= val;
                    }
                }
            }
        }
        return score;
    }

    private GameResponse convertToResponse(String gameId, Board board, String errorMsg) {
        GameResponse res = new GameResponse();
        res.gameId = gameId;
        res.sideToMove = board.sideToMove.toString();
        res.error = errorMsg;
        res.inCheck = board.isInCheck(board.sideToMove);
        boolean anyLegal = board.hasAnyLegalMove(board.sideToMove);
        res.checkmate = res.inCheck && !anyLegal;
        res.stalemate = !res.inCheck && !anyLegal;

        List<PieceDTO> boardList = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.grid[r][c];
                if (p != null) {
                    PieceDTO dto = new PieceDTO();
                    dto.r = r;
                    dto.c = c;
                    dto.pos = new Position(r, c).toString();
                    dto.type = p.type.toString();
                    dto.color = p.color.toString();
                    boardList.add(dto);
                }
            }
        }
        res.board = boardList;

        Map<String, List<String>> validMoves = new HashMap<>();
        if (!res.checkmate && !res.stalemate) {
            List<Move> allLegal = board.generateLegalMoves(board.sideToMove);
            for (Move move : allLegal) {
                String f = move.from.toString();
                String t = move.to.toString();
                validMoves.computeIfAbsent(f, k -> new ArrayList<>()).add(t);
            }
        }
        res.validMoves = validMoves;

        return res;
    }

    public static class MoveRequest {
        public String gameId;
        public String from;
        public String to;
        public String promotion;
    }

    public static class GameResponse {
        public String gameId;
        public String sideToMove;
        public boolean inCheck;
        public boolean checkmate;
        public boolean stalemate;
        public String error;
        public List<PieceDTO> board;
        public Map<String, List<String>> validMoves;
    }

    public static class PieceDTO {
        public int r;
        public int c;
        public String pos; // e.g. "e2"
        public String type; // "PAWN", "KNIGHT", etc.
        public String color; // "WHITE", "BLACK"
    }
}
