package com.placetrain.chess.engine;

import java.util.*;

/*
  ChessGameFull.java
  A complete console-based chess with:
   - All piece movement
   - Check, Checkmate, Stalemate detection
   - Castling, En Passant, Promotion
   - Board display
   - Player turns, move validation (can't leave own king in check)
*/

enum PieceColor {
    WHITE, BLACK
}

enum PieceType {
    PAWN, ROOK, KNIGHT, BISHOP, QUEEN, KING
}

class Position {
    int r, c; // r: 0..7 rows (0 is rank 8), c: 0..7 columns (0 is 'a')

    Position(int r, int c) {
        this.r = r;
        this.c = c;
    }

    Position(String coord) { // e.g. "e2"
        if (coord.length() != 2)
            throw new IllegalArgumentException("Bad coord");
        this.c = coord.charAt(0) - 'a';
        int file = coord.charAt(1) - '0';
        this.r = 8 - file;
    }

    boolean equalsPos(Position p) {
        return this.r == p.r && this.c == p.c;
    }

    boolean inBounds() {
        return r >= 0 && r < 8 && c >= 0 && c < 8;
    }

    public String toString() {
        return "" + (char) ('a' + c) + (8 - r);
    }
}

abstract class Piece {
    PieceColor color;
    PieceType type;
    boolean hasMoved = false;

    Piece(PieceColor color, PieceType type) {
        this.color = color;
        this.type = type;
    }

    // basic move check (ignores checks/pins). Subclasses implement.
    abstract boolean canMove(Position from, Position to, Board b);

    // used when checking attack squares (pawns attack differently than move).
    boolean attacksSquare(Position from, Position to, Board b) {
        return canMove(from, to, b);
    }

    @Override
    public String toString() {
        switch (this.color) {
            case WHITE:
                if (this instanceof King)
                    return "♔";
                if (this instanceof Queen)
                    return "♕";
                if (this instanceof Rook)
                    return "♖";
                if (this instanceof Bishop)
                    return "♗";
                if (this instanceof Knight)
                    return "♘";
                if (this instanceof Pawn)
                    return "♙";
            case BLACK:
                if (this instanceof King)
                    return "♚";
                if (this instanceof Queen)
                    return "♛";
                if (this instanceof Rook)
                    return "♜";
                if (this instanceof Bishop)
                    return "♝";
                if (this instanceof Knight)
                    return "♞";
                if (this instanceof Pawn)
                    return "♟";
        }
        return " ";
    }

}

/* ------- Concrete Pieces ------- */

class Pawn extends Piece {
    Pawn(PieceColor c) {
        super(c, PieceType.PAWN);
    }

    @Override
    boolean canMove(Position from, Position to, Board b) {
        int dir = color == PieceColor.WHITE ? -1 : 1;
        int startRow = color == PieceColor.WHITE ? 6 : 1;
        int dr = to.r - from.r;
        int dc = to.c - from.c;
        Piece dest = b.get(to);

        // Forward one
        if (dc == 0 && dr == dir && dest == null)
            return true;
        // Forward two from start
        if (dc == 0 && dr == 2 * dir && from.r == startRow) {
            Position mid = new Position(from.r + dir, from.c);
            return dest == null && b.get(mid) == null;
        }
        // Capture
        if (Math.abs(dc) == 1 && dr == dir) {
            if (dest != null && dest.color != this.color)
                return true;
            // en passant
            if (dest == null && b.enPassantTarget != null && b.enPassantTarget.r == to.r
                    && b.enPassantTarget.c == to.c) {
                return true;
            }
        }
        return false;
    }

    @Override
    boolean attacksSquare(Position from, Position to, Board b) { // pawns attack diagonally only
        int dir = color == PieceColor.WHITE ? -1 : 1;
        int dr = to.r - from.r;
        int dc = to.c - from.c;
        return dr == dir && Math.abs(dc) == 1;
    }
}

class Rook extends Piece {
    Rook(PieceColor c) {
        super(c, PieceType.ROOK);
    }

    @Override
    boolean canMove(Position from, Position to, Board b) {
        if (from.r != to.r && from.c != to.c)
            return false;
        return b.clearPath(from, to);
    }
}

class Knight extends Piece {
    Knight(PieceColor c) {
        super(c, PieceType.KNIGHT);
    }

    @Override
    boolean canMove(Position from, Position to, Board b) {
        int dr = Math.abs(to.r - from.r), dc = Math.abs(to.c - from.c);
        return (dr == 1 && dc == 2) || (dr == 2 && dc == 1);
    }
}

class Bishop extends Piece {
    Bishop(PieceColor c) {
        super(c, PieceType.BISHOP);
    }

    @Override
    boolean canMove(Position from, Position to, Board b) {
        if (Math.abs(from.r - to.r) != Math.abs(from.c - to.c))
            return false;
        return b.clearPath(from, to);
    }
}

class Queen extends Piece {
    Queen(PieceColor c) {
        super(c, PieceType.QUEEN);
    }

    @Override
    boolean canMove(Position from, Position to, Board b) {
        if (from.r == to.r || from.c == to.c || Math.abs(from.r - to.r) == Math.abs(from.c - to.c)) {
            return b.clearPath(from, to);
        }
        return false;
    }
}

class King extends Piece {
    King(PieceColor c) {
        super(c, PieceType.KING);
    }

    @Override
    boolean canMove(Position from, Position to, Board b) {
        int dr = Math.abs(from.r - to.r), dc = Math.abs(from.c - to.c);
        if (dr <= 1 && dc <= 1)
            return true;
        // Castling
        if (!hasMoved && from.r == to.r && dr == 0 && Math.abs(from.c - to.c) == 2) {
            int dir = to.c - from.c > 0 ? 1 : -1;
            Position rookPos = new Position(from.r, dir > 0 ? 7 : 0);
            Piece rook = b.get(rookPos);
            if (rook == null || rook.type != PieceType.ROOK || rook.color != this.color || rook.hasMoved)
                return false;
            // path between king and rook must be clear
            int c = from.c + dir;
            while (c != rookPos.c) {
                if (b.grid[from.r][c] != null)
                    return false;
                c += dir;
            }
            // also cannot castle through or into check — handled by overall legality check
            // in Board.makeMove (we check king safe for intermediate squares)
            return true;
        }
        return false;
    }
}

/* ------- Move and Board ------- */

class Move {
    Position from, to;
    Piece moved;
    Piece captured;
    boolean wasEnPassant = false;
    Position enPassantCapturedPos = null;
    boolean wasCastling = false;
    Position rookFrom = null, rookTo = null;
    boolean wasPromotion = false;
    Piece promotedTo = null;
    boolean movedHasMovedBefore = false;
    boolean rookHasMovedBefore = false;

    Move(Position f, Position t, Piece m) {
        from = f;
        to = t;
        moved = m;
    }
}

class Board {
    Piece[][] grid = new Piece[8][8];
    PieceColor sideToMove = PieceColor.WHITE;
    Position enPassantTarget = null; // if last move was double pawn, this is the square behind pawn that can be
                                     // captured
    Stack<Move> history = new Stack<>();

    Board() {
        init();
    }

    void init() {
        // Clear
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 8; j++)
                grid[i][j] = null;

        // Setup black
        grid[0][0] = new Rook(PieceColor.BLACK);
        grid[0][1] = new Knight(PieceColor.BLACK);
        grid[0][2] = new Bishop(PieceColor.BLACK);
        grid[0][3] = new Queen(PieceColor.BLACK);
        grid[0][4] = new King(PieceColor.BLACK);
        grid[0][5] = new Bishop(PieceColor.BLACK);
        grid[0][6] = new Knight(PieceColor.BLACK);
        grid[0][7] = new Rook(PieceColor.BLACK);
        for (int j = 0; j < 8; j++)
            grid[1][j] = new Pawn(PieceColor.BLACK);

        // Setup white
        grid[7][0] = new Rook(PieceColor.WHITE);
        grid[7][1] = new Knight(PieceColor.WHITE);
        grid[7][2] = new Bishop(PieceColor.WHITE);
        grid[7][3] = new Queen(PieceColor.WHITE);
        grid[7][4] = new King(PieceColor.WHITE);
        grid[7][5] = new Bishop(PieceColor.WHITE);
        grid[7][6] = new Knight(PieceColor.WHITE);
        grid[7][7] = new Rook(PieceColor.WHITE);
        for (int j = 0; j < 8; j++)
            grid[6][j] = new Pawn(PieceColor.WHITE);

        sideToMove = PieceColor.WHITE;
        enPassantTarget = null;
        history.clear();
    }

    Piece get(Position p) {
        if (!p.inBounds())
            return null;
        return grid[p.r][p.c];
    }

    void set(Position p, Piece piece) {
        grid[p.r][p.c] = piece;
    }

    boolean clearPath(Position from, Position to) {
        int dr = Integer.compare(to.r, from.r);
        int dc = Integer.compare(to.c, from.c);
        int r = from.r + dr, c = from.c + dc;
        while (r != to.r || c != to.c) {
            if (grid[r][c] != null)
                return false;
            r += dr;
            c += dc;
        }
        return true;
    }

    Position findKing(PieceColor color) {
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 8; j++) {
                Piece p = grid[i][j];
                if (p != null && p.type == PieceType.KING && p.color == color)
                    return new Position(i, j);
            }
        return null;
    }

    boolean isSquareAttacked(Position square, PieceColor byColor) {
        // iterate opponent pieces and see if they attack 'square' (use attack rules -
        // pawns differ)
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 8; j++) {
                Piece p = grid[i][j];
                if (p != null && p.color == byColor) {
                    Position from = new Position(i, j);
                    if (p.type == PieceType.PAWN) {
                        if (((Pawn) p).attacksSquare(from, square, this))
                            return true;
                    } else if (p.type == PieceType.KING) {
                        // king attacks one square regardless of moving into check
                        int dr = Math.abs(from.r - square.r), dc = Math.abs(from.c - square.c);
                        if (dr <= 1 && dc <= 1)
                            return true;
                    } else {
                        if (p.canMove(from, square, this))
                            return true;
                    }
                }
            }
        return false;
    }

    boolean isInCheck(PieceColor color) {
        Position kingPos = findKing(color);
        if (kingPos == null)
            return false; // should not happen
        return isSquareAttacked(kingPos, opposite(color));
    }

    PieceColor opposite(PieceColor c) {
        return c == PieceColor.WHITE ? PieceColor.BLACK : PieceColor.WHITE;
    }

    // Generate all legal moves for sideToMove (used for checkmate/stalemate
    // detection)
    List<Move> generateLegalMoves(PieceColor color) {
        List<Move> legal = new ArrayList<>();
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 8; j++) {
                Piece p = grid[i][j];
                if (p == null || p.color != color)
                    continue;
                Position from = new Position(i, j);
                for (int r = 0; r < 8; r++)
                    for (int c = 0; c < 8; c++) {
                        Position to = new Position(r, c);
                        if (tryMakePseudoMoveIfLegal(from, to)) {
                            Move mv = new Move(from, to, p);
                            legal.add(mv);
                        }
                    }
            }
        return legal;
    }

    // checks basic move validity and that it doesn't leave own king in check.
    boolean tryMakePseudoMoveIfLegal(Position from, Position to) {
        Piece p = get(from);
        if (p == null)
            return false;
        if (!to.inBounds())
            return false;
        Piece dest = get(to);
        // cannot capture own
        if (dest != null && dest.color == p.color)
            return false;

        // Basic movement (including castling/enpassant)
        if (!p.canMove(from, to, this))
            return false;

        // perform move on board (with minimal info) and test check
        Move m = makeMoveObject(from, to);
        applyMove(m);
        boolean leavesInCheck = isInCheck(p.color);
        undoMove();
        return !leavesInCheck;
    }

    // creates Move object with flags set (but doesn't push to history). Used by
    // applyMove.
    Move makeMoveObject(Position from, Position to) {
        Piece p = get(from);
        Move m = new Move(from, to, p);
        m.captured = get(to);
        m.movedHasMovedBefore = p.hasMoved;

        // en passant detection for pawn captures
        if (p.type == PieceType.PAWN) {
            if (get(to) == null && from.c != to.c) { // diagonal move to empty square -> en passant
                m.wasEnPassant = true;
                int capR = from.r;
                int capC = to.c;
                m.enPassantCapturedPos = new Position(capR, capC);
                m.captured = get(m.enPassantCapturedPos);
            }
        }

        // castling
        if (p.type == PieceType.KING && Math.abs(to.c - from.c) == 2) {
            m.wasCastling = true;
            int dir = to.c - from.c > 0 ? 1 : -1;
            Position rookFrom = new Position(from.r, dir > 0 ? 7 : 0);
            Position rookTo = new Position(from.r, from.c + dir);
            m.rookFrom = rookFrom;
            m.rookTo = rookTo;
            Piece rook = get(rookFrom);
            if (rook != null)
                m.rookHasMovedBefore = rook.hasMoved;
        }

        // promotion detection
        if (p.type == PieceType.PAWN) {
            if ((p.color == PieceColor.WHITE && to.r == 0) || (p.color == PieceColor.BLACK && to.r == 7)) {
                m.wasPromotion = true;
            }
        }

        return m;
    }

    // Apply move and push to history
    void applyMove(Move m) {
        // reset en passant target (will be set if double pawn)
        Position prevEnPassant = enPassantTarget;
        m.wasPromotion = m.wasPromotion; // already set
        m.captured = (m.wasEnPassant ? get(m.enPassantCapturedPos) : get(m.to));
        // Move piece
        set(m.to, m.moved);
        set(m.from, null);

        // en passant capture removal
        if (m.wasEnPassant && m.enPassantCapturedPos != null) {
            m.captured = get(m.enPassantCapturedPos);
            set(m.enPassantCapturedPos, null);
        }

        // castling rook move
        if (m.wasCastling && m.rookFrom != null && m.rookTo != null) {
            Piece rook = get(m.rookFrom);
            set(m.rookTo, rook);
            set(m.rookFrom, null);
        }

        // promotion: we'll prompt in game loop; here set to Queen as default
        // (temporary)
        if (m.wasPromotion) {
            // set temporarily to queen of same color; Game will adjust by prompting user
            set(m.to, new Queen(m.moved.color));
            m.promotedTo = get(m.to);
        }

        // update moved flag
        m.movedHasMovedBefore = m.moved.hasMoved;
        m.moved.hasMoved = true;
        if (m.wasCastling && m.rookTo != null) {
            Piece rook = get(m.rookTo);
            if (rook != null)
                rook.hasMoved = true;
        }

        // update enPassantTarget if double pawn move
        if (m.moved.type == PieceType.PAWN && Math.abs(m.to.r - m.from.r) == 2) {
            enPassantTarget = new Position((m.from.r + m.to.r) / 2, m.from.c);
        } else {
            enPassantTarget = null;
        }

        history.push(m);
        // switch side to move
        sideToMove = opposite(sideToMove);
    }

    void undoMove() {
        if (history.isEmpty())
            return;
        Move m = history.pop();
        // switch side back
        sideToMove = opposite(sideToMove);
        // revert moved piece
        set(m.from, m.moved);
        set(m.to, null);

        // if en passant captured, restore captured pawn
        if (m.wasEnPassant && m.enPassantCapturedPos != null) {
            set(m.enPassantCapturedPos, m.captured);
        } else {
            set(m.to, m.captured);
        }

        // revert castling rook
        if (m.wasCastling && m.rookFrom != null && m.rookTo != null) {
            Piece rook = get(m.rookTo);
            set(m.rookFrom, rook);
            set(m.rookTo, null);
            if (rook != null)
                rook.hasMoved = m.rookHasMovedBefore;
        }

        // revert moved flags
        m.moved.hasMoved = m.movedHasMovedBefore;
        // revert enPassantTarget to previous (we can't reconstruct prior target easily
        // without storing it; for legality checks this implementation suffices because
        // we only need enPassantTarget while move in history. To fully revert prior
        // enPassantTarget restore we'd need to store it in Move—keep simple here.)
        // To be precise, store prevEnPassant into Move (not done earlier). For
        // correctness in nested undos, we should store: do that now:
        // (Better approach would be to store prev enPassant in Move — but for typical
        // usage we rarely undo beyond one level in checks; for safety store it)
        // (In this implementation, enPassantTarget is recomputed by inspecting last
        // move in history)
        if (history.isEmpty())
            enPassantTarget = null;
        else {
            // recompute enPassantTarget from last move
            Move last = history.peek();
            if (last.moved.type == PieceType.PAWN && Math.abs(last.to.r - last.from.r) == 2) {
                enPassantTarget = new Position((last.from.r + last.to.r) / 2, last.from.c);
            } else
                enPassantTarget = null;
        }
    }

    // perform a move if legal (including check safety). returns status message or
    // null if successful.
    String makeMoveIfLegal(Position from, Position to, Scanner sc) {
        if (!from.inBounds() || !to.inBounds())
            return "Out of bounds!";
        Piece p = get(from);
        if (p == null)
            return "No piece at " + from;
        if (p.color != sideToMove)
            return "It's " + sideToMove + "'s turn.";
        Piece dest = get(to);
        if (dest != null && dest.color == p.color)
            return "Cannot capture your own piece.";

        // basic movement
        if (!p.canMove(from, to, this))
            return "Illegal move for " + p.type;

        // for castling check intermediate squares not under attack
        if (p.type == PieceType.KING && Math.abs(to.c - from.c) == 2) {
            int dir = to.c - from.c > 0 ? 1 : -1;
            Position step1 = new Position(from.r, from.c + dir);
            Position step2 = new Position(from.r, from.c + 2 * dir);
            // king cannot be in check, nor step on attacked squares
            if (isInCheck(p.color))
                return "Cannot castle while in check.";
            if (isSquareAttacked(step1, opposite(p.color)) || isSquareAttacked(step2, opposite(p.color)))
                return "Cannot castle through or into check.";
        }

        // make move and test for leaving own king in check
        Move m = makeMoveObject(from, to);
        applyMove(m);
        boolean leavesInCheck = isInCheck(p.color);
        if (leavesInCheck) {
            undoMove();
            return "Move would leave own king in check.";
        }

        // handle promotion: ask user
        if (m.wasPromotion) {
            System.out.println("Pawn promotion! Choose (q/r/b/n): ");
            char ch = 'q';
            while (true) {
                String line = sc.nextLine().trim().toLowerCase();
                if (line.isEmpty())
                    continue;
                ch = line.charAt(0);
                if (ch == 'q' || ch == 'r' || ch == 'b' || ch == 'n')
                    break;
                System.out.println("Invalid choice. Choose q/r/b/n: ");
            }
            Piece promoted;
            switch (ch) {
                case 'q':
                    promoted = new Queen(p.color);
                    break;
                case 'r':
                    promoted = new Rook(p.color);
                    break;
                case 'b':
                    promoted = new Bishop(p.color);
                    break;
                case 'n':
                    promoted = new Knight(p.color);
                    break;
                default:
                    promoted = new Queen(p.color);
            }
            set(m.to, promoted);
            // update history promotedTo
            Move last = history.peek();
            last.promotedTo = promoted;
        }

        return null; // success
    }

    boolean hasAnyLegalMove(PieceColor color) {
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 8; j++) {
                Piece p = grid[i][j];
                if (p == null || p.color != color)
                    continue;
                Position from = new Position(i, j);
                for (int r = 0; r < 8; r++)
                    for (int c = 0; c < 8; c++) {
                        Position to = new Position(r, c);
                        if (tryMakePseudoMoveIfLegal(from, to))
                            return true;
                    }
            }
        return false;
    }

    static final String RESET = "\u001B[0m";
    static final String LIGHT_BG = "\u001B[46m"; // Cyan
    static final String DARK_BG = "\u001B[44m"; // Blue
    static final String WHITE_PIECE = "\u001B[97m"; // Bright White
    static final String BLACK_PIECE = "\u001B[33m"; // Yellow

    void display() {
    System.out.println("     a    b    c    d    e    f    g    h");
    for (int i = 0; i < 8; i++) {
        // Top border of row
        System.out.print("   ");
        for (int j = 0; j < 8; j++) {
            String bg = ((i + j) % 2 == 0) ? LIGHT_BG : DARK_BG;
            System.out.print(bg + "     " + RESET);
        }
        System.out.println();

        // Row with pieces
        System.out.print((8 - i) + "  ");
        for (int j = 0; j < 8; j++) {
            String bg = ((i + j) % 2 == 0) ? LIGHT_BG : DARK_BG;
            Piece piece = grid[i][j];
            String symbol = " ";
            String color = "";
            if (piece != null) {
                symbol = piece.toString();
                color = (piece.color == PieceColor.WHITE) ? WHITE_PIECE : BLACK_PIECE;
            }
            System.out.print(bg + "  " + color + symbol + RESET + bg + "  " + RESET);
        }
        System.out.println(" " + (8 - i));

        // Bottom border of row
        System.out.print("   ");
        for (int j = 0; j < 8; j++) {
            String bg = ((i + j) % 2 == 0) ? LIGHT_BG : DARK_BG;
            System.out.print(bg + "     " + RESET);
        }
        System.out.println();
    }
    System.out.println("     a    b    c    d    e    f    g    h");
}

}

/* ------- Game loop ------- */

public class ChessLogic {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Board board = new Board();

        System.out.println("Console Chess — fully functional");
        System.out.println("Enter moves like: e2 e4");
        System.out.println("Type 'exit' to quit.");

        while (true) {
            board.display();
            // check for check/checkmate/stalemate
            boolean inCheck = board.isInCheck(board.sideToMove);
            boolean anyLegal = board.hasAnyLegalMove(board.sideToMove);
            if (inCheck && !anyLegal) {
                System.out.println("Checkmate! " + board.opposite(board.sideToMove) + " wins.");
                break;
            } else if (!inCheck && !anyLegal) {
                System.out.println("Stalemate! Game is a draw.");
                break;
            } else if (inCheck) {
                System.out.println("Check!");
            }

            System.out.print("Move (" + (board.sideToMove == PieceColor.WHITE ? "White" : "Black") + "): ");
            String line = sc.nextLine().trim();
            if (line.equalsIgnoreCase("exit")) {
                System.out.println("Goodbye.");
                break;
            }
            if (line.length() < 4) {
                System.out.println("Enter moves like: e2 e4");
                continue;
            }
            String[] parts = line.split("\\s+");
            if (parts.length < 2) {
                System.out.println("Enter in format: e2 e4");
                continue;
            }
            try {
                Position from = new Position(parts[0]);
                Position to = new Position(parts[1]);
                String err = board.makeMoveIfLegal(from, to, sc);
                if (err != null)
                    System.out.println("Illegal: " + err);
            } catch (Exception ex) {
                System.out.println("Invalid input: " + ex.getMessage());
            }
        }
        sc.close();
    }
}
