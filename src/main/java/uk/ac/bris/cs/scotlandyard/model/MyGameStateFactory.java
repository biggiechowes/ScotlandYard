package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.*;
import javax.annotation.Nonnull;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;

public final class MyGameStateFactory implements Factory<GameState> {
	@Nonnull @Override public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);
	}
	private final class MyGameState implements GameState {

		// Attributes
		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private ImmutableList<Player> everyone;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;

		//Constructor
		private MyGameState(final GameSetup setup, final ImmutableSet<Piece> remaining, final ImmutableList<
				LogEntry> log, final Player mrX, final List<Player> detectives) {

			// Tests to ensure parameters are not null
			if (setup.rounds.isEmpty()) throw new IllegalArgumentException("Rounds is empty!");
			if (remaining.isEmpty()) throw new IllegalArgumentException("Remaining is empty!");
			if (mrX.isDetective()) throw new IllegalArgumentException("mrX is empty!");
			if (detectives.isEmpty()) throw new IllegalArgumentException("detectives is empty!");


			// Initialisation
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			detectives.add(mrX);
			this.everyone = ImmutableList.copyOf(detectives);

		}
		// Methods
		@Override public GameState advance(Move move) {  return null;  }

		// Getters
		@Nonnull @Override public GameSetup getSetup() {
			return setup;
		}

		@Override public ImmutableSet<Piece> getPlayers() {
			return null;
		}

		@Nonnull @Override public Optional<Integer> getDetectiveLocation(Detective detective) {
			for (Player p : detectives) {
				System.out.println("Player: " + p.piece().webColour());
				if (p.piece() == detective) {
					return Optional.of(p.location());
				}
			}
			return Optional.empty();
		}
		@Override public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			/*
			for (Player p : everyone) {
				if (p.piece() == piece) {
					return Optional.of();
				}
			}
			*/
			return null;
			//test
		}
		@Nonnull @Override public ImmutableList<LogEntry> getMrXTravelLog() {
			return log;
		}
		@Override public ImmutableSet<Piece> getWinner() { return null; }
		@Override public ImmutableSet<Move> getAvailableMoves() { return null; }


	}
}