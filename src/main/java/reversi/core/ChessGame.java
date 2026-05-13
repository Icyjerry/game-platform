package reversi.core;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class ChessGame implements GameSession {
    private static final int SIZE = 8;
    private static final char EMPTY = '.';

    private final Board board;
    private final char[][] pieces;
    private Disc current;
    private boolean quit;
    private boolean over;
    private Disc winner;
    private String lastMessage;
    private Position enPassantTarget;
    private Position enPassantPawn;
    private Disc enPassantPlayer;
    private boolean blackKingMoved;
    private boolean whiteKingMoved;
    private boolean blackQueenRookMoved;
    private boolean blackKingRookMoved;
    private boolean whiteQueenRookMoved;
    private boolean whiteKingRookMoved;
    private int moveCount;

    private ChessGame() {
        this.board = Board.newEmptyBoard(SIZE);
        this.pieces = new char[SIZE][SIZE];
        this.current = Disc.WHITE;
        this.quit = false;
        this.over = false;
        this.winner = null;
        this.lastMessage = "White to move";
        this.moveCount = 0;
        initStartingPosition();
    }

    public static ChessGame newGame() {
        return new ChessGame();
    }

    @Override
    public GameMode mode() {
        return GameMode.CHESS;
    }

    @Override
    public Board board() {
        return board;
    }

    @Override
    public Disc current() {
        return current;
    }

    @Override
    public boolean isQuit() {
        return quit;
    }

    @Override
    public void quit() {
        quit = true;
    }

    @Override
    public boolean isOver() {
        return quit || over;
    }

    @Override
    public Map<Disc, Integer> counts() {
        Map<Disc, Integer> counts = new EnumMap<>(Disc.class);
        counts.put(Disc.BLACK, 0);
        counts.put(Disc.WHITE, 0);
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                char piece = pieces[row][col];
                if (piece == EMPTY) {
                    continue;
                }
                Disc side = sideOf(piece);
                counts.put(side, counts.get(side) + 1);
            }
        }
        return counts;
    }

    @Override
    public Set<Position> validMoves() {
        return Set.of();
    }

    @Override
    public Set<Position> validMoves(Disc disc) {
        return new LinkedHashSet<>();
    }

    @Override
    public ActionResult tryMove(Position p) {
        lastMessage = "Use move 1a 2a or m 1a 2a";
        return ActionResult.INVALID_MOVE;
    }

    @Override
    public ActionResult tryMove(Position from, Position to, Character promotionPiece) {
        if (isOver()) {
            lastMessage = "该局已结束";
            return ActionResult.GAME_OVER;
        }
        if (!isInside(from) || !isInside(to)) {
            lastMessage = "坐标越界或格式错误";
            return ActionResult.OUT_OF_BOUNDS;
        }

        char piece = pieceAt(from);
        if (piece == EMPTY || sideOf(piece) != current) {
            lastMessage = "源格无己方棋子";
            return ActionResult.INVALID_MOVE;
        }

        if (isKing(piece) && from.row() == to.row() && Math.abs(to.col() - from.col()) == 2) {
            return tryCastle(from, to, piece);
        }

        char target = pieceAt(to);
        if (target != EMPTY && sideOf(target) == current) {
            lastMessage = "目标格有己方棋子";
            return ActionResult.NOT_EMPTY;
        }

        Move move = validatePieceMove(from, to, piece, target);
        if (!move.valid) {
            lastMessage = move.message;
            return ActionResult.INVALID_MOVE;
        }

        char promotion = promotionFor(piece, to, promotionPiece);
        if (promotion == 0) {
            lastMessage = "兵升变时未指定合法棋子类型";
            return ActionResult.INVALID_MOVE;
        }

        char captured = target;
        char enPassantCaptured = EMPTY;
        if (move.enPassantCapture) {
            enPassantCaptured = pieceAt(enPassantPawn);
            setPiece(enPassantPawn, EMPTY);
            captured = enPassantCaptured;
        }

        setPiece(from, EMPTY);
        setPiece(to, promotion == EMPTY ? piece : promotion);
        if (isKingInCheck(current)) {
            setPiece(from, piece);
            setPiece(to, target);
            if (move.enPassantCapture) {
                setPiece(enPassantPawn, enPassantCaptured);
            }
            lastMessage = "移动后己方王被将军";
            return ActionResult.INVALID_MOVE;
        }

        markMoved(from, piece);
        clearEnPassant();
        if (isPawn(piece) && Math.abs(to.row() - from.row()) == 2) {
            enPassantTarget = new Position((from.row() + to.row()) / 2, from.col());
            enPassantPawn = to;
            enPassantPlayer = current.opposite();
        }

        if (isKing(captured)) {
            over = true;
            winner = current;
            moveCount++;
            lastMessage = colorName(current) + " wins by capturing the king";
            return ActionResult.OK;
        }

        current = current.opposite();
        var endResult = checkGameEnd(current);
        if (endResult != null) {
            moveCount++;
            return endResult;
        }
        moveCount++;
        lastMessage = "Moved " + label(from) + " to " + label(to);
        if (isKingInCheck(current)) {
            lastMessage += " — " + colorName(current.opposite()) + " gives check";
        }
        return ActionResult.OK;
    }

    @Override
    public ActionResult tryPass() {
        if (isOver()) {
            lastMessage = "该局已结束";
            return ActionResult.GAME_OVER;
        }
        lastMessage = "Pass is not supported in chess";
        return ActionResult.PASS_NOT_ALLOWED;
    }

    public char pieceAt(Position p) {
        if (!isInside(p)) {
            return EMPTY;
        }
        return pieces[p.row()][p.col()];
    }

    public String lastMessage() {
        return lastMessage;
    }

    public Disc winner() {
        return winner;
    }

    public int moveCount() {
        return moveCount;
    }

    public String resultSummary() {
        if (!over) {
            return "Game in progress";
        }
        StringBuilder sb = new StringBuilder();
        if (winner != null) {
            sb.append(colorName(winner)).append(" wins");
        } else {
            sb.append("Draw");
        }
        sb.append(" in ").append(moveCount).append(" moves");

        Map<Disc, Integer> cnt = counts();
        sb.append(" | White: ").append(cnt.getOrDefault(Disc.WHITE, 0)).append(" pieces");
        sb.append(", Black: ").append(cnt.getOrDefault(Disc.BLACK, 0)).append(" pieces");
        return sb.toString();
    }

    public Map<Disc, Map<Character, Integer>> pieceDetails() {
        Map<Disc, Map<Character, Integer>> result = new EnumMap<>(Disc.class);
        result.put(Disc.WHITE, new java.util.HashMap<>());
        result.put(Disc.BLACK, new java.util.HashMap<>());
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                char piece = pieces[row][col];
                if (piece == EMPTY) continue;
                Disc side = sideOf(piece);
                char type = Character.toUpperCase(piece);
                result.get(side).merge(type, 1, Integer::sum);
            }
        }
        return result;
    }

    private ActionResult tryCastle(Position from, Position to, char king) {
        Disc side = sideOf(king);
        int row = side == Disc.BLACK ? 0 : 7;
        if (from.row() != row || from.col() != 4 || to.row() != row) {
            lastMessage = "王车易位条件不满足";
            return ActionResult.INVALID_MOVE;
        }
        if ((side == Disc.BLACK && blackKingMoved) || (side == Disc.WHITE && whiteKingMoved)) {
            lastMessage = "王车易位条件不满足: 王已经移动过";
            return ActionResult.INVALID_MOVE;
        }
        if (isKingInCheck(side)) {
            lastMessage = "王车易位条件不满足: 王正被将军";
            return ActionResult.INVALID_MOVE;
        }

        boolean kingSide = to.col() == 6;
        boolean queenSide = to.col() == 2;
        if (!kingSide && !queenSide) {
            lastMessage = "王车易位条件不满足";
            return ActionResult.INVALID_MOVE;
        }

        int rookCol = kingSide ? 7 : 0;
        int rookTargetCol = kingSide ? 5 : 3;
        int[] emptyCols = kingSide ? new int[] {5, 6} : new int[] {1, 2, 3};
        int[] safeCols = kingSide ? new int[] {5, 6} : new int[] {3, 2};
        char rook = side == Disc.BLACK ? 'R' : 'r';
        if (pieceAt(new Position(row, rookCol)) != rook || rookMoved(side, kingSide)) {
            lastMessage = "王车易位条件不满足: 车已经移动过或不在原位";
            return ActionResult.INVALID_MOVE;
        }
        for (int col : emptyCols) {
            if (pieces[row][col] != EMPTY) {
                lastMessage = "王车易位条件不满足: 路径上有棋子";
                return ActionResult.INVALID_MOVE;
            }
        }
        for (int col : safeCols) {
            if (isSquareAttacked(new Position(row, col), side.opposite())) {
                lastMessage = "王车易位条件不满足: 王经过或到达的格子被攻击";
                return ActionResult.INVALID_MOVE;
            }
        }

        setPiece(from, EMPTY);
        setPiece(to, king);
        setPiece(new Position(row, rookCol), EMPTY);
        setPiece(new Position(row, rookTargetCol), rook);
        markMoved(from, king);
        if (side == Disc.BLACK) {
            if (kingSide) {
                blackKingRookMoved = true;
            } else {
                blackQueenRookMoved = true;
            }
        } else if (kingSide) {
            whiteKingRookMoved = true;
        } else {
            whiteQueenRookMoved = true;
        }
        clearEnPassant();
        current = current.opposite();
        var endResult = checkGameEnd(current);
        if (endResult != null) {
            moveCount++;
            return endResult;
        }
        moveCount++;
        lastMessage = kingSide ? "王侧易位完成" : "后侧易位完成";
        return ActionResult.OK;
    }

    private Move validatePieceMove(Position from, Position to, char piece, char target) {
        int dr = to.row() - from.row();
        int dc = to.col() - from.col();
        if (dr == 0 && dc == 0) {
            return Move.invalid("不符合该棋子的行棋规则");
        }

        char type = Character.toUpperCase(piece);
        return switch (type) {
            case 'K' -> Math.abs(dr) <= 1 && Math.abs(dc) <= 1
                ? Move.ok()
                : Move.invalid("不符合该棋子的行棋规则");
            case 'Q' -> validateSlidingMove(from, to, dr, dc, true, true);
            case 'R' -> validateSlidingMove(from, to, dr, dc, true, false);
            case 'B' -> validateSlidingMove(from, to, dr, dc, false, true);
            case 'N' -> (Math.abs(dr) == 2 && Math.abs(dc) == 1) || (Math.abs(dr) == 1 && Math.abs(dc) == 2)
                ? Move.ok()
                : Move.invalid("不符合该棋子的行棋规则");
            case 'P' -> validatePawnMove(from, to, piece, target);
            default -> Move.invalid("不符合该棋子的行棋规则");
        };
    }

    private Move validateSlidingMove(Position from, Position to, int dr, int dc, boolean straight, boolean diagonal) {
        boolean isStraight = dr == 0 || dc == 0;
        boolean isDiagonal = Math.abs(dr) == Math.abs(dc);
        if ((!straight || !isStraight) && (!diagonal || !isDiagonal)) {
            return Move.invalid("不符合该棋子的行棋规则");
        }
        if (!pathClear(from, to)) {
            return Move.invalid("移动路径被阻挡");
        }
        return Move.ok();
    }

    private Move validatePawnMove(Position from, Position to, char piece, char target) {
        Disc side = sideOf(piece);
        int direction = side == Disc.WHITE ? -1 : 1;
        int startRow = side == Disc.WHITE ? 6 : 1;
        int dr = to.row() - from.row();
        int dc = to.col() - from.col();

        if (dc == 0) {
            if (target != EMPTY) {
                return Move.invalid("不符合该棋子的行棋规则");
            }
            if (dr == direction) {
                return Move.ok();
            }
            Position skipped = new Position(from.row() + direction, from.col());
            if (from.row() == startRow && dr == direction * 2 && pieceAt(skipped) == EMPTY) {
                return Move.ok();
            }
            return Move.invalid("不符合该棋子的行棋规则");
        }

        if (Math.abs(dc) == 1 && dr == direction) {
            if (target != EMPTY && sideOf(target) == side.opposite()) {
                return Move.ok();
            }
            if (target == EMPTY && enPassantTarget != null && enPassantTarget.equals(to) && enPassantPlayer == side) {
                return Move.enPassant();
            }
        }
        return Move.invalid("不符合该棋子的行棋规则");
    }

    private char promotionFor(char piece, Position to, Character requested) {
        if (!isPawn(piece)) {
            return EMPTY;
        }
        Disc side = sideOf(piece);
        int lastRow = side == Disc.WHITE ? 0 : 7;
        if (to.row() != lastRow) {
            return EMPTY;
        }

        char choice = requested == null ? 'q' : Character.toLowerCase(requested);
        if (choice != 'q' && choice != 'r' && choice != 'b' && choice != 'n') {
            return 0;
        }
        return side == Disc.WHITE ? choice : Character.toUpperCase(choice);
    }

    private boolean pathClear(Position from, Position to) {
        int rowStep = Integer.compare(to.row(), from.row());
        int colStep = Integer.compare(to.col(), from.col());
        int row = from.row() + rowStep;
        int col = from.col() + colStep;
        while (row != to.row() || col != to.col()) {
            if (pieces[row][col] != EMPTY) {
                return false;
            }
            row += rowStep;
            col += colStep;
        }
        return true;
    }

    private boolean isKingInCheck(Disc side) {
        Position king = findKing(side);
        return king != null && isSquareAttacked(king, side.opposite());
    }

    private boolean isSquareAttacked(Position square, Disc attacker) {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                char piece = pieces[row][col];
                if (piece == EMPTY || sideOf(piece) != attacker) {
                    continue;
                }
                if (attacks(new Position(row, col), square, piece)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean attacks(Position from, Position to, char piece) {
        int dr = to.row() - from.row();
        int dc = to.col() - from.col();
        return switch (Character.toUpperCase(piece)) {
            case 'K' -> Math.abs(dr) <= 1 && Math.abs(dc) <= 1 && (dr != 0 || dc != 0);
            case 'Q' -> (dr == 0 || dc == 0 || Math.abs(dr) == Math.abs(dc)) && pathClear(from, to);
            case 'R' -> (dr == 0 || dc == 0) && pathClear(from, to);
            case 'B' -> Math.abs(dr) == Math.abs(dc) && pathClear(from, to);
            case 'N' -> (Math.abs(dr) == 2 && Math.abs(dc) == 1) || (Math.abs(dr) == 1 && Math.abs(dc) == 2);
            case 'P' -> dr == (sideOf(piece) == Disc.WHITE ? -1 : 1) && Math.abs(dc) == 1;
            default -> false;
        };
    }

    private Position findKing(Disc side) {
        char king = side == Disc.BLACK ? 'K' : 'k';
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (pieces[row][col] == king) {
                    return new Position(row, col);
                }
            }
        }
        return null;
    }

    private boolean rookMoved(Disc side, boolean kingSide) {
        if (side == Disc.BLACK) {
            return kingSide ? blackKingRookMoved : blackQueenRookMoved;
        }
        return kingSide ? whiteKingRookMoved : whiteQueenRookMoved;
    }

    private void markMoved(Position from, char piece) {
        if (piece == 'K') {
            blackKingMoved = true;
        } else if (piece == 'k') {
            whiteKingMoved = true;
        } else if (piece == 'R' && from.row() == 0 && from.col() == 0) {
            blackQueenRookMoved = true;
        } else if (piece == 'R' && from.row() == 0 && from.col() == 7) {
            blackKingRookMoved = true;
        } else if (piece == 'r' && from.row() == 7 && from.col() == 0) {
            whiteQueenRookMoved = true;
        } else if (piece == 'r' && from.row() == 7 && from.col() == 7) {
            whiteKingRookMoved = true;
        }
    }

    private void clearEnPassant() {
        enPassantTarget = null;
        enPassantPawn = null;
        enPassantPlayer = null;
    }

    private Disc sideOf(char piece) {
        return Character.isUpperCase(piece) ? Disc.BLACK : Disc.WHITE;
    }

    private boolean isPawn(char piece) {
        return Character.toUpperCase(piece) == 'P';
    }

    private boolean isKing(char piece) {
        return Character.toUpperCase(piece) == 'K';
    }

    private boolean isInside(Position p) {
        return p != null && p.row() >= 0 && p.row() < SIZE && p.col() >= 0 && p.col() < SIZE;
    }

    private void setPiece(Position p, char piece) {
        pieces[p.row()][p.col()] = piece;
    }

    private String label(Position p) {
        return (p.row() + 1) + String.valueOf((char) ('a' + p.col()));
    }

    private String colorName(Disc side) {
        return side == Disc.BLACK ? "Black" : "White";
    }

    private void initStartingPosition() {
        String[] rows = {
            "RNBQKBNR",
            "PPPPPPPP",
            "........",
            "........",
            "........",
            "........",
            "pppppppp",
            "rnbqkbnr"
        };
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                pieces[row][col] = rows[row].charAt(col);
            }
        }
    }

    private ActionResult checkGameEnd(Disc side) {
        if (!hasAnyLegalMove(side)) {
            over = true;
            if (isKingInCheck(side)) {
                winner = side.opposite();
                lastMessage = "Checkmate! " + colorName(winner) + " wins";
                return ActionResult.CHECKMATE;
            } else {
                winner = null;
                lastMessage = "Stalemate! The game is a draw";
                return ActionResult.STALEMATE;
            }
        }
        return null;
    }

    private boolean hasAnyLegalMove(Disc side) {
        for (int fromRow = 0; fromRow < SIZE; fromRow++) {
            for (int fromCol = 0; fromCol < SIZE; fromCol++) {
                char piece = pieces[fromRow][fromCol];
                if (piece == EMPTY || sideOf(piece) != side) {
                    continue;
                }
                Position from = new Position(fromRow, fromCol);

                for (int toRow = 0; toRow < SIZE; toRow++) {
                    for (int toCol = 0; toCol < SIZE; toCol++) {
                        Position to = new Position(toRow, toCol);
                        if (isMoveLegal(from, to, piece, true)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isMoveLegal(Position from, Position to, char piece, boolean includeCastling) {
        if (!isInside(from) || !isInside(to) || from.equals(to)) {
            return false;
        }

        Disc side = sideOf(piece);
        char target = pieceAt(to);
        if (target != EMPTY && sideOf(target) == side) {
            return false;
        }

        if (includeCastling && isKing(piece) && from.row() == to.row()
                && Math.abs(to.col() - from.col()) == 2) {
            return canCastle(from, to, piece);
        }

        Move move = validatePieceMove(from, to, piece, target);
        if (!move.valid) {
            return false;
        }

        boolean promotes = isPawn(piece) && isPromotionRank(to, side);
        if (!promotes) {
            return simulateMoveAndCheckCheck(from, to, piece, target, move, EMPTY);
        }

        char[] promos = side == Disc.WHITE
                ? new char[] {'q', 'r', 'b', 'n'}
                : new char[] {'Q', 'R', 'B', 'N'};
        for (char promo : promos) {
            if (simulateMoveAndCheckCheck(from, to, piece, target, move, promo)) {
                return true;
            }
        }
        return false;
    }

    private boolean canCastle(Position from, Position to, char king) {
        Disc side = sideOf(king);
        int row = side == Disc.BLACK ? 0 : 7;
        if (from.row() != row || from.col() != 4 || to.row() != row) {
            return false;
        }
        if ((side == Disc.BLACK && blackKingMoved) || (side == Disc.WHITE && whiteKingMoved)) {
            return false;
        }
        if (isKingInCheck(side)) {
            return false;
        }

        boolean kingSide = to.col() == 6;
        boolean queenSide = to.col() == 2;
        if (!kingSide && !queenSide) {
            return false;
        }

        int rookCol = kingSide ? 7 : 0;
        int[] emptyCols = kingSide ? new int[] {5, 6} : new int[] {1, 2, 3};
        int[] safeCols = kingSide ? new int[] {5, 6} : new int[] {3, 2};
        char rook = side == Disc.BLACK ? 'R' : 'r';

        if (pieceAt(new Position(row, rookCol)) != rook || rookMoved(side, kingSide)) {
            return false;
        }
        for (int col : emptyCols) {
            if (pieces[row][col] != EMPTY) {
                return false;
            }
        }
        for (int col : safeCols) {
            if (isSquareAttacked(new Position(row, col), side.opposite())) {
                return false;
            }
        }
        return true;
    }

    private boolean simulateMoveAndCheckCheck(Position from, Position to, char piece,
                                               char target, Move move, char promo) {
        Disc side = sideOf(piece);
        char enPassantCap = EMPTY;
        Position enPassantCapPos = null;

        if (move.enPassantCapture && enPassantPawn != null) {
            enPassantCapPos = enPassantPawn;
            enPassantCap = pieceAt(enPassantPawn);
            setPiece(enPassantPawn, EMPTY);
        }

        setPiece(from, EMPTY);
        setPiece(to, promo == EMPTY ? piece : promo);
        boolean inCheck = isKingInCheck(side);

        setPiece(from, piece);
        setPiece(to, target);
        if (move.enPassantCapture && enPassantCapPos != null) {
            setPiece(enPassantCapPos, enPassantCap);
        }

        return !inCheck;
    }

    private boolean isPromotionRank(Position pos, Disc side) {
        int lastRow = side == Disc.WHITE ? 0 : 7;
        return pos.row() == lastRow;
    }

    private record Move(boolean valid, boolean enPassantCapture, String message) {
        static Move ok() {
            return new Move(true, false, "");
        }

        static Move enPassant() {
            return new Move(true, true, "");
        }

        static Move invalid(String message) {
            return new Move(false, false, message);
        }
    }
}
