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
			this.everyone = ImmutableSet.copyOf(pieceList);

		}
		// Methods
		@Nonnull @Override
		public GameState advance(Move move) {
			if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);

			Visitor<Boolean> isDouble = new Visitor<Boolean>() {
				@Override
				public Boolean visit(SingleMove move) {
					return false;
				}

				@Override
				public Boolean visit(DoubleMove move) {
					return true;
				}
			};

			updateRemaining(move);
			updateLocations(move);
			updateLog(move);
			updateTickets(move);

		}

		private void updateRemaining(Move move) {
			List<Piece> remaining = new ArrayList<Piece>();
			if (move.commencedBy().isMrX()) {
				detectives.forEach(detective -> remaining.add(detective.piece()));
				this.remaining = ImmutableSet.copyOf(remaining);
			}

			else{
				this.remaining.stream().
						filter(x -> x != move.commencedBy()).
						forEach(x -> detectives.stream()
							.filter(detective -> detective.piece() != x &&
										Arrays.stream(Ticket.values()).
												anyMatch(detective::has))
							.map(detective -> x)
							.forEach(remaining::add)
						);
			}
			if(remaining.isEmpty()) this.remaining = ImmutableSet.of(mrX.piece());
			else this.remaining = ImmutableSet.copyOf(remaining);

		}

		private void updateLocations(Move move) {

			Visitor<Integer> destination = new Visitor<Integer>() {
				@Override
				public Integer visit(SingleMove move) {
					return move.destination;
				}

				@Override
				public Integer visit(DoubleMove move) {
					return move.destination2;
				}
			};

			if (move.commencedBy().isMrX()){
				mrX = mrX.at(move.visit(destination));
			}
			else {
				for (Integer i = 0; i <= this.detectives.size() - 1; i++) {
					if(this.detectives.get(i).piece() == move.commencedBy()) {
						this.detectives.set(i, this.detectives.get(i).at(move.visit(destination)));
					}
				}
			}
		}

		private void updateLog(Move move) {

			Visitor<ImmutableList<Ticket>> tickets = new Visitor<ImmutableList<Ticket>>() {
				@Override
				public ImmutableList<Ticket> visit(SingleMove move) {
					return ImmutableList.copyOf(move.tickets());
				}

				@Override
				public ImmutableList<Ticket> visit(DoubleMove move) {
					return ImmutableList.copyOf(move.tickets());
				}
			};

			Visitor<ImmutableList<Integer>> destinations = new Visitor<ImmutableList<Integer>>() {
				@Override
				public ImmutableList<Integer> visit(SingleMove move) {
					return ImmutableList.of(move.destination);
				}

				@Override
				public ImmutableList<Integer> visit(DoubleMove move) {
					return ImmutableList.of(move.destination1, move.destination2);
				}
			};

			if(move.commencedBy().isMrX()){

				updateLog(move);

			}

		}

		private void updateTickets(Move move) {

			Visitor<List<Ticket>> tickets = new Visitor<List<Ticket>>() {
				@Override
				public ImmutableList<Ticket> visit(SingleMove move) {
					return ImmutableList.copyOf(move.tickets());
				}

				@Override
				public ImmutableList<Ticket> visit(DoubleMove move) {
					return ImmutableList.copyOf(move.tickets());
				}
			};

			if (move.commencedBy().isMrX()) {
				for (Ticket ticket : move.visit(tickets)) {
					mrX.use(ticket);
				}
			}
			else {
				for (Ticket ticket : move.visit(tickets)) {
					for (Integer i = 0; i <= this.detectives.size() - 1; i++) {
						if (this.detectives.get(i).piece() == move.commencedBy()) {
							this.detectives.get(i).use(ticket);
							mrX.give(ticket);
						}
					}
				}
			}

		}

		// Getters
		@Nonnull @Override
		public GameSetup getSetup() {
			return setup;
		}

		@Nonnull @Override
		public ImmutableSet<Piece> getPlayers() {
			return this.everyone;
		}

		@Nonnull @Override
		public Optional<Integer> getDetectiveLocation(Detective detective) {
			for (Player p : this.detectives) {

				if (p.piece().webColour().equals(detective.webColour())) {
					return Optional.of(p.location());
				}
			}
			return Optional.empty();
		}
		@Nonnull @Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {


			class MyTicketBoard implements TicketBoard {

				final Player player;

				MyTicketBoard(Player player) {
					this.player = player;
				}

				public int getCount(@Nonnull Ticket ticket) {
					return this.player.tickets().get(ticket);
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


		@Nonnull @Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return log;
		}

		@Override
		public ImmutableSet<Piece> getWinner() {
			return this.winner;
		}

		private ImmutableSet<SingleMove> getSingleMoves(
				GameSetup setup,
				List<Player> detectives,
				Player player,
				int source){
			var singleMoves = new ArrayList<SingleMove>();
			ArrayList<Integer> detectiveLocations = new ArrayList<>();

			for (Player p : this.detectives) {
				detectiveLocations.add(p.location());
			}

			for(int destination : setup.graph.adjacentNodes(source)) {

				if (!(detectiveLocations.contains(destination))) {

					for (Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {

						if (player.has(t.requiredTicket())) {
							singleMoves.add(new SingleMove(player.piece(), source, t.requiredTicket(), destination));
						}
					}

					if (player.has(Ticket.SECRET)) {
						singleMoves.add(new SingleMove(player.piece(), source, Ticket.SECRET, destination));
					}
				}
			}
			return ImmutableSet.copyOf(singleMoves);
		}

		private ImmutableSet<DoubleMove> getDoubleMoves(
				GameSetup setup,
				List<Player> detectives,
				Player player,
				int source) {

			var doubleMoves = new ArrayList<DoubleMove>();
			final var firstSingleMoves = getSingleMoves(setup, detectives, player, source);

			if (player.has(Ticket.DOUBLE)) {

				for (SingleMove sMove1 : firstSingleMoves) {

					var secondSingleMoves =getSingleMoves(setup, detectives, player, sMove1.destination);


					for (SingleMove sMove2 : secondSingleMoves) {

						if(sMove1.ticket != sMove2.ticket || player.hasAtLeast(sMove1.ticket, 2)) {
							DoubleMove doubleMove = new DoubleMove(
									player.piece(),
									sMove1.source(),
									sMove1.ticket,
									sMove1.destination,
									sMove2.ticket,
									sMove2.destination
							);

							doubleMoves.add(doubleMove);
						}
					}
				}
			}

			return ImmutableSet.copyOf(doubleMoves);

		}

		 @Nonnull @Override
		 public ImmutableSet<Move> getAvailableMoves() {
			//List<SingleMove> sMoves = new ArrayList<>();
			/*for (Player p : this.detectives) {
				 List<SingleMove> sPlayerMoves = List.copyOf(this.getSingleMoves(
						 this.setup,
						 this.detectives,
						 p,
						 p.location()));

				 sPlayerMoves.forEach(move -> sMoves.add(move));
			 }*/



			 ImmutableSet<SingleMove> sMoves = getSingleMoves(setup, detectives, mrX, mrX.location());
			List<DoubleMove> dMoves = List.copyOf(getDoubleMoves(
					this.setup,
					this.detectives,
					mrX,
					mrX.location()));

			List<Move> moves = new ArrayList<>(List.copyOf(sMoves));
			moves.addAll(dMoves);

			return ImmutableSet.copyOf(moves);



			//return this.makeSingleMoves(this.setup, this.detectives, )
		}


	}
}
