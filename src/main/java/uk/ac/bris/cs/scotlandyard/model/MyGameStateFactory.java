package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
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

		// TO DO
		@Override public ImmutableSet<Move> getAvailableMoves() {

			// return all available single moves
			// and mrX's special moves (double, secret)

			// doesn't pass tests needs double moves also

			Set<Move> allMoves = Set.of();

			allMoves.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));

			for (Player player : detectives) {
				allMoves.addAll(makeSingleMoves(setup, detectives, player, player.location()));
			}

			return ImmutableSet.copyOf(allMoves);

		}

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

		// idk if this working
		private static Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){

			// TODO create an empty collection of some sort, say, HashSet, to store all the SingleMove we generate
			HashSet<SingleMove> moves = new HashSet<>();
			boolean occupied = false;
			for(int destination : setup.graph.adjacentNodes(source)) {
				// TODO find out if destination is occupied by a detective
				//  if the location is occupied, don't add to the collection of moves to return
				for (Player p : detectives) {
                    if (destination == p.location()) {
                        occupied = true;
                        break;
                    }
				}

				if(!occupied) {
					for(Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()) ) {
						// TODO find out if the player has the required tickets
						//  if it does, construct a SingleMove and add it the collection of moves to return

						if (player.has(t.requiredTicket())) {
							moves.add(new SingleMove(player.piece(), source, t.requiredTicket(), destination));
						}
						else if (player.isMrX() && player.has(Ticket.SECRET)) { // secret ticket if
							moves.add(new SingleMove(player.piece(), source, Ticket.SECRET, destination));
						}
					}
				}

				occupied = false;

				// TODO consider the rules of secret moves here
				//  add moves to the destination via a secret ticket if there are any left with the player
			}

			// TODO return the collection of moves
            return moves;
        }

		// TO DO
		private static Set<DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source){

			HashSet<DoubleMove> moves = new HashSet<>();

			// mr x double move

            return moves;
        }
	}

}