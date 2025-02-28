package uk.ac.bris.cs.scotlandyard.ui.model;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;

import java.time.Duration;
import java.util.Objects;

import io.atlassian.fugue.Option;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import uk.ac.bris.cs.scotlandyard.ResourceManager;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Transport;

import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;

public class ModelProperty {

	private final ObjectProperty<Duration> mrXTimeout = new SimpleObjectProperty<>();
	private final ObjectProperty<Duration> detectiveTimeout = new SimpleObjectProperty<>();
	private final ObservableList<Boolean> revealRounds = FXCollections.observableArrayList();

	private final ObjectProperty<Option<Ai>> mrXAi =
			new SimpleObjectProperty<>(Option.none());
	private final ObjectProperty<Option<Ai>> detectivesAi =
			new SimpleObjectProperty<>(Option.none());

	private final ObservableList<PlayerProperty<? super Piece>> players =
			FXCollections.observableArrayList();
	private final ObjectProperty<ImmutableValueGraph<Integer, ImmutableSet<Transport>>> graph =
			new SimpleObjectProperty<>();

	public ModelProperty(
			Duration mrXTimeout,
			Duration detectiveTimeout,
			ImmutableList<Boolean> revealRounds,
			ImmutableList<? extends PlayerProperty<? super Piece>> players,
			ImmutableValueGraph<Integer, ImmutableSet<Transport>> graph,
			Option<Ai> mrXAi,
			Option<Ai> detectivesAi
	) {
		this.mrXTimeout.set(Objects.requireNonNull(mrXTimeout));
		this.detectiveTimeout.set(Objects.requireNonNull(detectiveTimeout));
		this.revealRounds.addAll(Objects.requireNonNull(revealRounds));
		this.players.addAll(Objects.requireNonNull(players));
		this.graph.set(Objects.requireNonNull(graph));
		this.mrXAi.set(Objects.requireNonNull(mrXAi));
		this.detectivesAi.set(Objects.requireNonNull(detectivesAi));
	}

	public static ModelProperty createDefault(ResourceManager manager) {
		return new ModelProperty(
				Duration.ofSeconds(20),
				Duration.ofSeconds(10),
				ScotlandYard.STANDARD24MOVES,
				ScotlandYard.ALL_PIECES.stream()
						.map(PlayerProperty::new)
						.collect(ImmutableList.toImmutableList()),
				manager.getGraph(),
				Option.none(),
				Option.none()
		);
	}

	public ObjectProperty<Duration> mrXTimeoutProperty() { return mrXTimeout; }
	public ObjectProperty<Duration> detectiveTimeoutProperty() { return detectiveTimeout; }
	public ObservableList<Boolean> revealRounds() { return revealRounds; }

	public Duration timeout(ImmutableSet<Piece> pieces) {
		return pieces.equals(ImmutableSet.of(MRX))
				? mrXTimeout.get()
				: detectiveTimeout.get();
	}

	public ObjectProperty<ImmutableValueGraph<Integer, ImmutableSet<Transport>>> graphProperty() {
		return graph;
	}

	public PlayerProperty<? super Piece> mrX() {
		return players.stream()
				.filter(PlayerProperty::mrX)
				.findFirst().orElseThrow();
	}

	public ImmutableList<PlayerProperty<? super Piece>> detectives() {
		return players.stream()
				.filter(PlayerProperty::detective)
				.collect(ImmutableList.toImmutableList());
	}

	public ImmutableList<PlayerProperty<? super Piece>> everyone() {
		return ImmutableList.copyOf(players);
	}

	public Option<Ai> getMrXAi() { return mrXAi.get(); }
	public ObjectProperty<Option<Ai>> mrXAiProperty() { return mrXAi; }
	public Option<Ai> getDetectivesAi() { return detectivesAi.get(); }
	public ObjectProperty<Option<Ai>> detectivesAiProperty() { return detectivesAi; }

	@Override public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("timeout(mrx)", mrXTimeout)
				.add("timeout(detective)", detectiveTimeout)
				.add("revealRounds", revealRounds)
				.add("players", players).toString();
	}
}
