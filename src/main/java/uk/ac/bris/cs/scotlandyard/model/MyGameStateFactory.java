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

		@Nonnull @Override public GameSetup getSetup() { return setup; }
		@Override public ImmutableSet<Piece> getPlayers() { return remaining; }
		@Override public GameState advance(Move move) { return null; }
		@Override public Optional<Integer> getDetectiveLocation(Detective detective) {
            for (Player player : detectives) {
				if (player.isDetective() && player.piece().equals(detective)) { return Optional.of(player.location()); }
			}
			return Optional.empty();
		}
		@Override public Optional<TicketBoard> getPlayerTickets(Piece piece) { return Optional.empty(); }
		@Override public ImmutableList<LogEntry> getMrXTravelLog() { return log; }
		@Override public ImmutableSet<Piece> getWinner() { return null; }
		@Override public ImmutableSet<Move> getAvailableMoves() { return moves; }

		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log; // holds travel log and counts Mr X's moves
		private Player mrX;
		private List<Player> detectives;
		private ImmutableSet<Move> moves; // currently available moves
		private ImmutableSet<Piece> winner;

		private MyGameState(final GameSetup setup, final ImmutableSet<Piece> remaining,
							final ImmutableList<LogEntry> log, final Player mrX, final List<Player> detectives) {

			if(setup.moves.isEmpty()) throw new IllegalArgumentException("Moves cannot be empty");
			if(setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Graph cannot be empty");
			this.setup = setup;


			this.remaining = remaining;
			this.log = log;

			if(mrX == null) throw new NullPointerException("MrX cannot be null");

			for (Player player : detectives) {
				if (player.piece().isMrX()) { throw new IllegalArgumentException("There must only be one MrX"); }
			}
			this.mrX = mrX;

            if(detectives.contains(null)) throw new NullPointerException("Detectives cannot contain null");

			List<Piece> pieces = new ArrayList<>(Collections.emptyList());
			List<Integer> locations = new ArrayList<>(Collections.emptyList());

			for (Player player : detectives) {
				if(pieces.contains(player.piece())) {throw new IllegalArgumentException("Detectives cannot have duplicate pieces"); }
				else { pieces.add(player.piece()); }
				if(locations.contains(player.location())) {throw new IllegalArgumentException("Multiple detectives cannot be at the same location"); }
				else { locations.add(player.location()); }
				if(player.has(Ticket.SECRET)) {throw new IllegalArgumentException("Detectives cannot have secret tickets"); }
			}

			this.detectives = detectives;

		}

	}

}