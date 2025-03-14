package uk.ac.bris.cs.scotlandyard.model;

import java.util.*;

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

	// A factory that returns an implementation of the GameState interface
	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);
	}

	private static final class MyGameState implements GameState {

		private final GameSetup setup;
		private final ImmutableSet<Piece> remaining; // Players who have not yet moved in the current round
		private final ImmutableList<LogEntry> log; // Mr X's travel log
		private final Player mrX;
		private final List<Player> detectives; // All detectives present in the game
		private ImmutableSet<Piece> winner; // All winners at the end of the game

		// Constructs a new GameState
		private MyGameState(
			final GameSetup setup, 
			final ImmutableSet<Piece> remaining,
			final ImmutableList<LogEntry> log, 
			final Player mrX, 
			final List<Player> detectives
		) {
			// Verification to ensure parameters are valid
			if (setup.moves.isEmpty()) throw new IllegalArgumentException("Moves cannot be empty");
			if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Graph cannot be empty");
			if (mrX == null) throw new NullPointerException("Mr X cannot be null");
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
					throw new IllegalArgumentException("There must only be one Mr X");
				}
			}

			// Initialisation of values
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			this.winner = ImmutableSet.of();
		}

		// Returns the current GameSetup
		@Nonnull @Override public GameSetup getSetup() { return setup; }

		// Returns all players in the game
		@Nonnull @Override public ImmutableSet<Piece> getPlayers() {
			ImmutableSet.Builder<Piece> players = ImmutableSet.builder();
			for (Player player : detectives) {
				players.add(player.piece());
			}
			players.add(mrX.piece());
			return players.build();
		}

		// Returns a new state from the current GameState after processing a provided move
		@Nonnull @Override public GameState advance(Move move) {
			// Verify that move is valid
			if (!getAvailableMoves().contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
			
			List<LogEntry> newLog = new ArrayList<>(log); // Updated version of Mr X's travel log
			List<Player> newDetectives = new ArrayList<>(detectives); // Updated list of detectives with their altered attributes
			Player newMrX = null; // Updated version of Mr X

			// Visitor used to access the destination of a move
			FunctionalVisitor<Integer> v = new FunctionalVisitor<>(m -> m.destination, m -> m.destination2);
			int destination = move.accept(v);

			if (move.commencedBy().isMrX()) {
				// Update Mr X's location and remove used tickets
				newMrX = mrX.at(destination).use(move.tickets());

				for (Ticket ticket : move.tickets()) {
					if (ticket != Ticket.DOUBLE) {
						// Update Mr X's travel log
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
						// Update detective's location and remove used tickets
						Player new_det = det.at(destination).use(move.tickets());
						newDetectives.set(detectives.indexOf(det), new_det);

						// Give used ticket to Mr X
						newMrX = mrX.give(move.tickets());
					}
				}
			}

			// Update remaining to exclude who just moved and swap turns
			Set<Piece> newRemaining = new HashSet<>(remaining);
			newRemaining.remove(move.commencedBy());

			// Reset remaining for the next round after everybody has moved
			if (newRemaining.isEmpty()) {
				newRemaining = getPlayers();
			}

			return new MyGameState(setup, ImmutableSet.copyOf(newRemaining), ImmutableList.copyOf(newLog), newMrX, ImmutableList.copyOf(newDetectives)); 
		}

		// Returns the location of a given detective
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

		// Returns the tickets of a given player
		@NonNull @Override public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			// from ImmutableBoard.java : 79
			if (piece.isDetective()) {
				return detectives.stream()
						.filter(detective -> detective.piece().equals(piece))
						.map(Player::tickets)
						.findFirst()
						.map(tickets -> ticket -> Objects.requireNonNull(tickets.getOrDefault(ticket, 0)));
			} else if (piece.equals(mrX.piece())) {
				return Optional.of(mrX.tickets())
						.map(tickets -> ticket -> Objects.requireNonNull(tickets.getOrDefault(ticket, 0)));
			}
			return Optional.empty();
		}

		// Returns Mr X's travel log
		@Nonnull @Override public ImmutableList<LogEntry> getMrXTravelLog() {
			return log; 
		}

		// Returns all winners of the game
		@Nonnull @Override public ImmutableSet<Piece> getWinner() {
			HashSet<Piece> winners = new HashSet<>();

			// win is used to signify the winning state:
			// 0 = No winner (returns empty set)
			// 1 = Detectives win
			// 2 = Mr X wins

			int win = 0;
			ImmutableSet<Move> availableMoves = getAvailableMoves(); // Available moves stored to prevent multiple calls
			
			// A detective finishes a move on the same station as Mr X
			for (Player det : detectives) {
				if (det.location() == mrX.location()) {
					win = 1;
					break;
				}
			}
			
			// The detectives can no longer move any of their playing pieces : 1/2
			// Check if detectives are able to make at least one move
			boolean canMove = false;
			for (Move move : availableMoves ) {
				if (move.commencedBy().isDetective()) {
					// Verify it is not the beginning of the round
					if (remaining != getPlayers()) {
						canMove = true;
					}
				}
			}

			/*
			 * If canMove is true then no need to check tickets
			 * If canMove is false then need to check tickets FOR REMAINING PLAYERS 
			 * If canMove is false then need to check tickets
			 * ^ leads to bug: A player CANT move due to being blocked rather than not having a ticket
			*/
		
			// There are no unoccupied stations for Mr X to travel to
			// Check if Mr X is unable to move before the last round
			if (
				log.size() < setup.moves.size() && 
				remaining.contains(mrX.piece()) && 
				availableMoves.isEmpty()
			) {
				win = 1;
			} 

			// Mr X manages to fill the log and the detectives subsequently fail to catch him with their final moves
			// Check if detectives cannot move, and it is the last round
			if (!canMove && (log.size() == setup.moves.size())) {
				win = 2;
			}

			// The detectives can no longer move any of their playing pieces : 2/2
			// Check if detectives have all ran out of tickets
			boolean hasTicket = false;
			for (Player det : detectives) {
				for (Integer i : det.tickets().values()) {
                    if (i > 0) {
                        hasTicket = true;
                        break;
                    }
				}
			}

			// Mr X wins if detectives have no tickets
			if (!hasTicket) {
				win = 2;
			}

			// Add detectives to winners if Mr X loses, or add Mr X to winners if detectives lose
			if (win == 1) {
				for (Player det : detectives) {
					winners.add(det.piece());
				}
			} else if (win == 2) {
				winners.add(mrX.piece());
			}

			// Update winners for access in getAvailableMoves()
			winner = ImmutableSet.copyOf(winners);

			return ImmutableSet.copyOf(winners);
		}

		// Returns all moves players can make for a given GameState
		@Nonnull @Override public ImmutableSet<Move> getAvailableMoves() {
			HashSet<Move> allMoves = new HashSet<>();

			// Return empty if the game is over
			if(winner.isEmpty()) {
				// Remaining detectives moves added
				for (Player player : detectives) {
					if (remaining.contains(player.piece())) {
						allMoves.addAll(makeSingleMoves(setup, detectives, player));
					}
				}
				// Mr X moves added if it's his move or if detectives have no moves
				if ((remaining.contains(mrX.piece()) && remaining.size() == 1) || allMoves.isEmpty()) {
					allMoves.addAll(makeSingleMoves(setup, detectives, mrX));
					allMoves.addAll(makeDoubleMoves(setup, detectives, mrX));
				}
			}
			return ImmutableSet.copyOf(allMoves);
		}

		// Returns all single moves that can be played
		private static Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player) {
			int source = player.location();
			HashSet<SingleMove> moves = new HashSet<>();

			// Detective locations stored for fast lookup
			HashSet<Integer> detectiveLocations = new HashSet<>();
			for (Player p : detectives) {
				detectiveLocations.add(p.location());
			}

			for (int destination : setup.graph.adjacentNodes(source)) {
				// Ignore destinations blocked by detectives
				if (detectiveLocations.contains(destination)) {
					continue;
				}

				// Gets modes of transport to destination
				Set<Transport> transportModes = Objects.requireNonNull(setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()));

				// Adds valid moves based on available tickets
				for (Transport transport : transportModes) {
					if (player.has(transport.requiredTicket())) {
						moves.add(new SingleMove(player.piece(), source, transport.requiredTicket(), destination));
					}
				}

				// Adds Mr X's secret moves if a secret ticket is held
				if (player.isMrX() && player.has(Ticket.SECRET)) {
					moves.add(new SingleMove(player.piece(), source, Ticket.SECRET, destination));
				}
			}

			return moves;
		}

		// Returns all doubles moves that can be played
		private static Set<DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player mrX) {
			if (mrX.isDetective()) {
				throw new IllegalArgumentException("Player is detective");
			}
			int source = mrX.location();
			HashSet<DoubleMove> doubleMoves = new HashSet<>();

			// Return an empty set if no double moves can exist
			if (!mrX.has(Ticket.DOUBLE) || setup.moves.size() <= 1) {
				return doubleMoves;
			}

			// Detective locations stored for fast lookup
			HashSet<Integer> detectiveLocations = new HashSet<>();
			for (Player p : detectives) {
				detectiveLocations.add(p.location());
			}

			for (int destination1 : setup.graph.adjacentNodes(source)) {
				if (detectiveLocations.contains(destination1)) {
					// Ignore destinations blocked by detectives
					continue;
				}

				// Get modes of transport to the first (intermediate) destination
				Set<Transport> transportModes1 = Objects.requireNonNull(setup.graph.edgeValueOrDefault(source, destination1, ImmutableSet.of()));

				for (Transport transport1 : transportModes1) {
					if (!mrX.has(transport1.requiredTicket())) {
						continue;
					}

					for (int destination2 : setup.graph.adjacentNodes(destination1)) {
						if (detectiveLocations.contains(destination2)) {
							continue;
						}

						// Get modes of transport to the second (final) destination
						Set<Transport> transportModes2 = Objects.requireNonNull(setup.graph.edgeValueOrDefault(destination1, destination2, ImmutableSet.of()));

						for (Transport transport2 : transportModes2) {
							Ticket ticket1 = transport1.requiredTicket();
							Ticket ticket2 = transport2.requiredTicket();

							// Verify that Mr X has enough tickets to perform the double move
							if (!(ticket1 == ticket2 && Objects.requireNonNull(mrX.tickets().get(ticket1)) <= 1)) {
								doubleMoves.add(new DoubleMove(mrX.piece(), source, ticket1, destination1, ticket2, destination2));
							}

							// Handle secret ticket scenarios
							if (mrX.has(Ticket.SECRET)) {
								// Using secret ticket as the first ticket
								doubleMoves.add(new DoubleMove(mrX.piece(), source, Ticket.SECRET, destination1, ticket2, destination2));

								if (mrX.has(ticket2)) {
									// Using secret ticket as the second ticket
									doubleMoves.add(new DoubleMove(mrX.piece(), source, ticket1, destination1, Ticket.SECRET, destination2));
								}

								if (Objects.requireNonNull(mrX.tickets().get(Ticket.SECRET)) >= 2) {
									// Using secret ticket as both ticket
									doubleMoves.add(new DoubleMove(mrX.piece(), source, Ticket.SECRET, destination1, Ticket.SECRET, destination2));
								}
							}
						}
					}
				}
			}
			return doubleMoves;
		}
	}
}