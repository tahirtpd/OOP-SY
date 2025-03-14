package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

	// Builds a new game Model from given setup, Mr X, and detectives
	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {
		return new MyModel(setup, mrX, detectives);
	}

	// An implementation of Model which manages the observers and current GameState
	private static class MyModel implements Model {
		private ImmutableSet<Observer> observerList;
		private GameState gameState;

		// Constructs a new game Model
		MyModel(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
			// Initialisation of values
			this.gameState = new MyGameStateFactory().build(setup, mrX, detectives);
			this.observerList = ImmutableSet.of();
		}

		// Return the current board (GameState)
		@Nonnull @Override public Board getCurrentBoard() {
			return gameState;
		}

		// Register a given observer to the model to receive updates
		@Override public void registerObserver(@Nonnull Observer observer) {
			if (observer == null) {
				throw new NullPointerException("Observe cannot be null");
			}
			if (observerList.contains(observer)) {
				throw new IllegalArgumentException("Observer already registered");
			}
			observerList = ImmutableSet.<Observer>builder()
					.addAll(observerList)
					.add(observer).build();
		}

		// Unregister a given observer from the model to stop receiving updates
		@Override public void unregisterObserver(@Nonnull Observer observer) {
			if (observer == null) {
				throw new NullPointerException("Observer cannot be null");
			}

			ImmutableSet.Builder<Observer> builder = ImmutableSet.builder();

			if(observerList.contains(observer)) {
				for(Observer o : observerList) {
					if(o != observer) {
						builder.add(o);
					}
				}
			}
			else {
				throw new IllegalArgumentException("Observer is not registered");
			}

			observerList = builder.build();
		}

		// Return all registered observers
		@Nonnull @Override public ImmutableSet<Observer> getObservers() {
			return observerList;
		}

		// Processes a move and notifies observers of the event
		@Override public void chooseMove(@Nonnull Move move) {
			gameState = gameState.advance(move);
			if (gameState.getWinner().isEmpty()) {
				for (Observer o : observerList) {
					o.onModelChanged(gameState, Observer.Event.MOVE_MADE);
				}
			}
			else {
				for (Observer o : observerList) {
					o.onModelChanged(gameState, Observer.Event.GAME_OVER);
				}
			}
		}
	}
}