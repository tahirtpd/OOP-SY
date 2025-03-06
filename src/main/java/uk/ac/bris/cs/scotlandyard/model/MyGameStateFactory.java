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

import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Move.DoubleMove;
import uk.ac.bris.cs.scotlandyard.model.Move.FunctionalVisitor;
import uk.ac.bris.cs.scotlandyard.model.Move.SingleMove;
import uk.ac.bris.cs.scotlandyard.model.Piece.Detective;
import uk.ac.bris.cs.scotlandyard.model.Piece.MrX;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Transport;


/**
 * cw-model
 * Stage 1: Complete this class
 */
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
		private ImmutableSet<Move> moves; // currently available moves
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

			// temporary lists used by checks below
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

			// assign values
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

		@Override public ImmutableSet<Piece> getPlayers() { 
			ImmutableSet.Builder<Piece> players = ImmutableSet.builder();
			for (Player player : detectives) {
				players.add(player.piece());
			}
			players.add(mrX.piece());
			return players.build();
		}

		@Override public GameState advance(Move move) {
			if (!getAvailableMoves().contains(move)) throw new IllegalArgumentException("Illegal move: "+move);

			List<LogEntry> new_log = new ArrayList<>(log);
			Set<Piece> new_remaining = new HashSet<>(remaining);

			List<Player> new_detectives = new ArrayList<>();

			FunctionalVisitor<Integer> v = new FunctionalVisitor<>(
					m -> {
						return m.destination;
					},
					m -> {
						return m.destination2;
					}
			);

			if (move.commencedBy().isMrX()) {
				if (log.size() < setup.moves.size()) {
					int destination = move.accept(v);
					mrX = mrX.at(destination);

					for (Ticket ticket : move.tickets()) {
						if (mrX.has(ticket)) {
							if (ticket == Ticket.SECRET) {
								new_log.add(LogEntry.hidden(ticket));
							}
							else {
								new_log.add(LogEntry.reveal(ticket, destination));
							}
							break;
						}
					}
					mrX = mrX.use(move.tickets());
				}
				// else game end

				// swap to det turn?
			}
			else {
				for (Player det : detectives) {
					if (det.piece() == move.commencedBy()) {
						for (Ticket ticket : move.tickets()) {
							if (det.has(ticket)) {
								det = det.use(ticket);
								det = det.at(move.accept(v));
								mrX.give(ticket);
								break;
							}
						}
					}
					new_detectives.add(det);
				}
			}
			new_remaining.remove(move.commencedBy());

			return new MyGameState(setup, ImmutableSet.copyOf(new_remaining), ImmutableList.copyOf(new_log), mrX, new_detectives);
		}

		@Override public Optional<Integer> getDetectiveLocation(Detective detective) {
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

		@Override public Optional<TicketBoard> getPlayerTickets(Piece piece) { 
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

		@Override public ImmutableList<LogEntry> getMrXTravelLog() { 
			return log; 
		}
		
		@Override public ImmutableSet<Piece> getWinner() { 
			return winner;
		}

		@Override public ImmutableSet<Move> getAvailableMoves() {
			HashSet<Move> allMoves = new HashSet<>();

			// needs to only return moves for the current player
			// works for mrX but not detectives, all detective tests return no moves

			if (remaining.stream().findFirst().equals(Optional.of(mrX.piece()))) {
				allMoves.addAll(makeSingleMoves(setup, detectives, mrX));
				allMoves.addAll(makeDoubleMoves(setup, detectives, mrX));
			}
			else {
				for (Player player : detectives) {
					if (remaining.stream().findFirst().equals(Optional.of(player.piece()))) // maybe wrong?
					{
						allMoves.addAll(makeSingleMoves(setup, detectives, player));
					}
				}
			}

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