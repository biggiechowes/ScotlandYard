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
		private ImmutableSet<Piece> everyone;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner = ImmutableSet.of();

		//Constructor
		private MyGameState(final GameSetup setup, final ImmutableSet<Piece> remaining, final ImmutableList<
				LogEntry> log, final Player mrX, final List<Player> detectives) {

			// Tests to ensure parameters are not null
			if (setup.rounds.isEmpty()) throw new IllegalArgumentException("Rounds is empty!");
			if (remaining.isEmpty()) throw new IllegalArgumentException("Remaining is empty!");
			if (mrX.isDetective()) throw new IllegalArgumentException("mrX is empty!");
			if (mrX == null) throw new IllegalArgumentException("mrX is null");
			if (detectives.isEmpty()) throw new IllegalArgumentException("detectives is empty!");
			if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("empty graph!");

			List<Player> encounteredDetectives = new ArrayList<>();
			List<Integer> encounteredLocations = new ArrayList<>();
			for (Player p : detectives) {
				if (p.isMrX()) throw new IllegalArgumentException("detective is mrX!");
				if (encounteredDetectives.contains(p)) throw new IllegalArgumentException("duplicate detectives!");
				if (encounteredLocations.contains(p.location())) throw new IllegalArgumentException("detective location overlap!");

				if (p.tickets().get(Ticket.SECRET) > 0) throw new IllegalArgumentException("detective has secret ticket!");
				if (p.tickets().get(Ticket.DOUBLE) > 0) throw new IllegalArgumentException("detective has double ticket!");


				encounteredDetectives.add(p);
				encounteredLocations.add(p.location());
			}

			List<Piece> pieceList = new ArrayList<>();
			for (Player p : detectives) {
				pieceList.add(p.piece());
			}
			pieceList.add(mrX.piece());



			// Initialisation
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			//detectives.add(mrX);
			this.everyone = ImmutableSet.copyOf(pieceList);

		}
		// Methods
		@Override public GameState advance(Move move) {  return null;  }

		// Getters
		@Nonnull @Override public GameSetup getSetup() {
			return setup;
		}

		@Nonnull @Override public ImmutableSet<Piece> getPlayers() {
			return this.everyone;
		}

		@Nonnull @Override public Optional<Integer> getDetectiveLocation(Detective detective) {
			for (Player p : this.detectives) {

				if (p.piece().webColour().equals(detective.webColour())) {
					return Optional.of(p.location());
				}
			}
			return Optional.empty();
		}
		@Nonnull @Override public Optional<TicketBoard> getPlayerTickets(Piece piece) {


			class MyTicketBoard implements TicketBoard {

				final Player p;

				MyTicketBoard(Player p) {
					this.p = p;
				}

				public int getCount(@Nonnull Ticket ticket) {
					return this.p.tickets().get(ticket);
				}
			}

			for (Player p : this.detectives) {
				if (p.piece() == piece) {
					return Optional.of(new MyTicketBoard(p));
				}
			}
			if (piece == this.mrX.piece()) {
				return Optional.of(new MyTicketBoard(this.mrX));
			}
			return Optional.empty();
		}


		@Nonnull @Override public ImmutableList<LogEntry> getMrXTravelLog() {
			return log;
		}
		@Override public ImmutableSet<Piece> getWinner() {
			return this.winner;
		}
		@Override public ImmutableSet<Move> getAvailableMoves() { return null; }


	}
}