package uk.ac.bris.cs.scotlandyard.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.checkerframework.checker.nullness.qual.NonNull;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Move.DoubleMove;
import uk.ac.bris.cs.scotlandyard.model.Move.FunctionalVisitor;
import uk.ac.bris.cs.scotlandyard.model.Move.SingleMove;
import uk.ac.bris.cs.scotlandyard.model.Piece.Detective;
import uk.ac.bris.cs.scotlandyard.model.Piece.MrX;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Transport;

public final class MyGameStateFactory implements Factory<GameState> {

	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);
	}

	private final class MyGameState implements GameState {

		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log; // holds travel log and counts Mr X's moves
		private Player mrX;
		private List<Player> detectives;
		private ImmutableSet<Move> moves; // CURRENTLY UNUSED - remove if never needed
		private ImmutableSet<Piece> winner;

		private MyGameState(
			final GameSetup setup, 
			final ImmutableSet<Piece> remaining,
			final ImmutableList<LogEntry> log, 
			final Player mrX, 
			final List<Player> detectives
		) {
			if (setup.moves.isEmpty()) throw new IllegalArgumentException("Moves cannot be empty");
			if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Graph cannot be empty");
			if (mrX == null) throw new NullPointerException("MrX cannot be null");
			if (detectives.contains(null)) throw new NullPointerException("Detectives cannot contain null");

			List<Piece> pieces = new ArrayList<>(Collections.emptyList());
			List<Integer> locations = new ArrayList<>(Collections.emptyList());

			for (Player player : detectives) {
				if (pieces.contains(player.piece())) {
					throw new IllegalArgumentException("Detectives cannot have duplicate pieces"); 
				} else { 
					pieces.add(player.piece()); 
				}
				
				if (locations.contains(player.location())) {
					throw new IllegalArgumentException("Multiple detectives cannot be at the same location");
				} else { 
					locations.add(player.location()); 
				}
				
				if (player.has(Ticket.SECRET)) {
					throw new IllegalArgumentException("Detectives cannot have secret tickets"); 
				}

				if (player.isDetective() && player.has(Ticket.DOUBLE)){
					throw new IllegalArgumentException("Detectives cannot have double tickets");
				}

				if (player.piece().isMrX()) {
					throw new IllegalArgumentException("There must only be one MrX");
				}
			}

			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			this.moves = ImmutableSet.of();
			this.winner = ImmutableSet.of();
		}

		@Nonnull @Override public GameSetup getSetup() { 
			return setup; 
		}

		@Nonnull @Override public ImmutableSet<Piece> getPlayers() {
			ImmutableSet.Builder<Piece> players = ImmutableSet.builder();
			for (Player player : detectives) {
				players.add(player.piece());
			}
			players.add(mrX.piece());
			return players.build();
		}
		
		@Nonnull @Override public GameState advance(Move move) {
			if (!getAvailableMoves().contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
			
			List<LogEntry> newLog = new ArrayList<>(log);
			List<Player> newDetectives = new ArrayList<>(detectives);
			FunctionalVisitor<Integer> v = new FunctionalVisitor<>(m -> m.destination, m -> m.destination2);
			int destination = move.accept(v);
			Player newMrX = null;
			if (move.commencedBy().isMrX()) {
				newMrX = mrX.at(destination).use(move.tickets());

				for (Ticket ticket : move.tickets()) {
					if (ticket != Ticket.DOUBLE) {
						if (setup.moves.get(newLog.size())) {
							newLog.add(LogEntry.reveal(ticket, destination));
						} else {
							newLog.add(LogEntry.hidden(ticket));
						}
					}
				}
			} else {
				for (Player det : detectives) {
					if (det.piece() == move.commencedBy()) {
						Player new_det = det.at(destination).use(move.tickets());
						newDetectives.set(detectives.indexOf(det), new_det);					
						newMrX = mrX.give(move.tickets());
					}
				}
			}
			Set<Piece> newRemaining = new HashSet<>(remaining);
			newRemaining.remove(move.commencedBy());
			if (newRemaining.isEmpty()) {
				newRemaining = getPlayers();
			}

			return new MyGameState(setup, ImmutableSet.copyOf(newRemaining), ImmutableList.copyOf(newLog), newMrX, ImmutableList.copyOf(newDetectives)); 
		}

		@Nonnull @Override public Optional<Integer> getDetectiveLocation(Detective detective) {
            for (Player player : detectives) {
				if (
					player.isDetective() && 
					player.piece().equals(detective)
				) { 
					return Optional.of(player.location()); 
				}
			}

			return Optional.empty();
		}

		@NonNull @Override public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			// from ImmutableBoard.java : 79
			if (piece.isDetective()) {
				return detectives.stream()
						.filter(detective -> detective.piece().equals(piece))
						.map(Player::tickets)
						.findFirst()
						.map(tickets -> ticket -> tickets.getOrDefault(ticket, 0));
			} else if (piece.equals(mrX.piece())) {
				return Optional.of(mrX.tickets())
						.map(tickets -> ticket -> tickets.getOrDefault(ticket, 0));
			}
			return Optional.empty();
		}

		@Nonnull @Override public ImmutableList<LogEntry> getMrXTravelLog() {
			return log; 
		}

		// UNFINISHED
		@Nonnull @Override public ImmutableSet<Piece> getWinner() {
			HashSet<Piece> winners = new HashSet<>();

			int win = 0;
			boolean canMove = false;
			boolean canWin = false;
			ImmutableSet<Move> availableMoves = getAvailableMoves();

			FunctionalVisitor<Integer> v = new FunctionalVisitor<>(m -> m.destination, m -> m.destination2);

			// mrx caught
			for (Player det : detectives) {
				if (det.location() == mrX.location()) {
					win = 1;
					break;
				}
			}

			// track whether detectives can move
			if (win == 0) {
				for( Move move : availableMoves ) {
					if (move.commencedBy().isDetective()) {
						canMove = true;

						// can capture in 1 move
						if (remaining.contains(move.commencedBy()) && move.accept(v).equals(mrX.location())) {
							canWin = true;
							break;
						}
					}
				}
			}

			// not last round
			if (win == 0 && (log.size() < setup.moves.size())) {
				// mrX cannot make a move
				if (remaining.contains(mrX.piece()) && availableMoves.stream().noneMatch(move -> move.commencedBy().equals(mrX.piece()))) {
					win = 1;
				}

				// detectives run out of moves
				if(remaining == getPlayers() && !canMove) {
					win = 2;
				}
			}
			else if (win == 0 && (log.size() == setup.moves.size()) && (remaining.size() == 1)) {
				// detectives cannot reach mrX in the last round
				if (!canWin) {
					win = 2;
				}
			}

			if (win == 1) {
				for (Player det : detectives) {
					winners.add(det.piece());
				}
			} else if (win == 2) {
				winners.add(mrX.piece());
			}

			winner = ImmutableSet.copyOf(winners);

			return ImmutableSet.copyOf(winners);
		}

		@Nonnull @Override public ImmutableSet<Move> getAvailableMoves() {
			HashSet<Move> allMoves = new HashSet<>();

			if(winner.isEmpty()) {
				if (remaining.contains(mrX.piece()) && remaining.size() == 1) {
					allMoves.addAll(makeSingleMoves(setup, detectives, mrX));
					allMoves.addAll(makeDoubleMoves(setup, detectives, mrX));
				}

				for (Player player : detectives) {
					if (remaining.contains(player.piece())) {
						allMoves.addAll(makeSingleMoves(setup, detectives, player));
					}
				}
			}
			moves = ImmutableSet.copyOf(allMoves);
			return ImmutableSet.copyOf(allMoves);
		}

		private static Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player){
			int source = player.location();
			HashSet<SingleMove> moves = new HashSet<>();
			destination_loop:
			for (int destination : setup.graph.adjacentNodes(source)) {
				for (Player p : detectives) {
                    if (destination == p.location()) {
                        continue destination_loop;
                    }
				}
				
				for (Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()) ) {
					if (player.has(t.requiredTicket())) {
						moves.add(new SingleMove(player.piece(), source, t.requiredTicket(), destination));
					}
				}

				if (player.isMrX() && player.has(Ticket.SECRET)) {
					moves.add(new SingleMove(player.piece(), source, Ticket.SECRET, destination));
				}
			}

            return moves;
        }

		private static Set<DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player mrX){
			if (mrX.isDetective()) {
				throw new IllegalArgumentException("Player is detective");
			}
			int source = mrX.location();

			HashSet<DoubleMove> doubleMoves = new HashSet<>();
			if (!mrX.has(Ticket.DOUBLE) || setup.moves.size() <= 1) {
				return doubleMoves;
			}

			destination1Loop:
			for (int destination1 : setup.graph.adjacentNodes(source)) {
				for (Player p : detectives) {
					if (destination1 == p.location()) {
						continue destination1Loop;
					}
				}
				
				HashSet<SingleMove> singleMoves = new HashSet<>();
				
				for (Transport t : setup.graph.edgeValueOrDefault(source, destination1, ImmutableSet.of()) ) {
					if (mrX.has(t.requiredTicket())) {
						singleMoves.add(new SingleMove(mrX.piece(), source, t.requiredTicket(), destination1));
					}
				}
				
				for (SingleMove move: singleMoves) {
					destination2Loop:
					for (int destination2 : setup.graph.adjacentNodes(move.destination)) {
						for (Player p : detectives) {
							if (destination2 == p.location()) {
								continue destination2Loop;
							}
						}
						
						for (Transport t : setup.graph.edgeValueOrDefault(destination1, destination2, ImmutableSet.of()) ) {
							if (!(move.ticket == t.requiredTicket() && mrX.tickets().get(move.ticket) <= 1)) {
								doubleMoves.add(new DoubleMove(mrX.piece(), source, move.ticket, move.destination, t.requiredTicket(), destination2));
							}

							if (mrX.has(Ticket.SECRET) && mrX.has(t.requiredTicket())) {
								doubleMoves.add(new DoubleMove(mrX.piece(), source, Ticket.SECRET, move.destination, t.requiredTicket(), destination2));
							}
						}

						if (mrX.has(Ticket.SECRET)) {
							doubleMoves.add(new DoubleMove(mrX.piece(), source, move.ticket, move.destination, Ticket.SECRET, destination2));
							
							if (mrX.tickets().get(Ticket.SECRET) >= 2) {
								doubleMoves.add(new DoubleMove(mrX.piece(), source, Ticket.SECRET, move.destination, Ticket.SECRET, destination2));
							}
						}
					}					
				}
			}

            return doubleMoves;
        }
	}
}