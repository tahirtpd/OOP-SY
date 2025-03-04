package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.*;
import javax.annotation.Nonnull;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;


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
			/*move.commencedBy()
			move.tickets()
			 
			*/
			if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);

	

			return null; 
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

			allMoves.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
			allMoves.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));

			// for (Player player : detectives) {
			// 	allMoves.addAll(makeSingleMoves(setup, detectives, player, player.location()));
			// }

			return ImmutableSet.copyOf(allMoves);

		}

		private static Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
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

		private static Set<DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player mrX, int source){
			if (mrX.isDetective()) {
				throw new IllegalArgumentException("Player is detective");
			}

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