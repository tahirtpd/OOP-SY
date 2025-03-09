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

	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {
		return new MyModel(setup, mrX, detectives);
	}

	private static class MyModel implements Model {
		private ImmutableSet<Observer> observerList;
		private GameState gameState;

		MyModel(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
			this.gameState = new MyGameStateFactory().build(setup, mrX, detectives);
			this.observerList = ImmutableSet.of();
		}

		@Nonnull
		@Override
		public Board getCurrentBoard() {
			return gameState;
		}

		@Override
		public void registerObserver(@Nonnull Observer observer) {
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

		@Override
		public void unregisterObserver(@Nonnull Observer observer) {
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

		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() {
			return observerList;
		}

		@Override
		public void chooseMove(@Nonnull Move move) {
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