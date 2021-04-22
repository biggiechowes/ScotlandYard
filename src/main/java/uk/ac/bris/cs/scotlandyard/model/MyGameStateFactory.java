package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.*;
import javax.annotation.Nonnull;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;
import uk.ac.bris.cs.scotlandyard.model.LogEntry.*;

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
		private ImmutableList<Boolean> remainingRounds;


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

			//Test to ensure valid player states
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




			// Initialisation
			this.setup = setup;
			this.remaining = remaining;

			//remainingRounds requires a buffer variable
			List<Boolean> bufferRemainingRounds = new ArrayList<>(this.setup.rounds);
			for(LogEntry logEntry : log){
				bufferRemainingRounds.remove(0);
			}

			this.remainingRounds = ImmutableList.copyOf(bufferRemainingRounds);
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;

			//everyone requires a buffer variable
			List<Piece> bufferEveryone = new ArrayList<>();
			for (Player p : detectives) {
				bufferEveryone.add(p.piece());
			}
			bufferEveryone.add(mrX.piece());

			this.everyone = ImmutableSet.copyOf(bufferEveryone);
			this.moves = this.getMoves();

		}
		// Methods
		@Nonnull @Override
		public GameState advance(Move move) {

			//Test for illegal move
			if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);


			//advance calls helper methods to update required attributes and then returns
			//a new MyGameState object
			updateLog(move);
			updateRemaining(move);
			updateLocations(move);
			updateTickets(move);



			return new MyGameState(this.setup, this.remaining, this.log,
					this.mrX, this.detectives);

		}

		private void updateRemaining(@Nonnull Move move) {

			List<Piece> bufferRemaining = new ArrayList<>();

			//this.remaining alternates between the remaining detectives and mrX


			//if mrX is the one who moved, the remaining players in the round are all the detectives
			if (move.commencedBy().isMrX()) {
				detectives.forEach(detective -> bufferRemaining.add(detective.piece()));
				this.remaining = ImmutableSet.copyOf(bufferRemaining);
			}

			else{
				//this lambda function adds all the remaining players to bufferRemaining
				this.remaining.stream()
						.filter(x -> x != move.commencedBy())//the player who commenced the move is filtered out
						.forEach(x -> detectives.stream()//for each player in remaining all the detectives are streamed
								.filter(detective -> detective.piece() == x &&
										Arrays.stream(Ticket.values()).anyMatch(detective::has))//the detectives who have no tickets are filtered out
								.map(detective -> x)//the detectives are mapped back into this.remaining
								.forEach(bufferRemaining::add)//and then they are added tho the local list
						);
			}
			//if the list of remaining players is empty that means that all detective have used their turn
			//and it is the start of a new round, so the remaining player is mrX
			if(bufferRemaining.isEmpty()) this.remaining = ImmutableSet.of(mrX.piece());
			else this.remaining = ImmutableSet.copyOf(bufferRemaining);

		}

		private void updateLocations(@Nonnull Move move) {

			//visitor pattern is used here to get the final destination of the move
			Visitor<Integer> destination = new Visitor<Integer>() {
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
				for (int i = 0; i <= this.detectives.size() - 1; i++) {
					if(this.detectives.get(i).piece() == move.commencedBy()) {
						List<Player> bufferDetectives = new ArrayList<>(this.detectives);
						bufferDetectives.set(i, this.detectives.get(i).at(move.visit(destination)));
						this.detectives = bufferDetectives;
					}
				}
			}
		}

		private void updateLog(@Nonnull Move move) {


			//visitor pattern is used to get the tickets and the destinations of the move as lists
			//to avoid special cases for single or double moves
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


			//the log concerns only mrX
			if(move.commencedBy().isMrX()) {
				List<LogEntry> bufferLog = new ArrayList<>(this.log);

				for (int i = 0; i <= move.visit(destinations).size() - 1; i++) {
					if (this.remainingRounds.get(i)) { // if current round is reveal the log entry is added accordingly
						bufferLog.add(LogEntry.reveal(move.visit(tickets).get(i),
								move.visit(destinations).get(i)));
					} else {
						bufferLog.add(LogEntry.hidden(move.visit(tickets).get(i)));
					}
				}
				this.log = ImmutableList.copyOf(bufferLog);
			}
		}

		private void updateTickets(@Nonnull Move move) {

			//visitor pattern is used to get the tickets as a list
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
					mrX = mrX.use(ticket);
				}
			}
			else {
				for (Ticket ticket : move.visit(tickets)) {
					for (int i = 0; i <= this.detectives.size() - 1; i++) {
						if (this.detectives.get(i).piece() == move.commencedBy()) {
							//tickets used by detectives are given to mrX
							this.detectives.set(i, this.detectives.get(i).use(ticket));
							mrX = mrX.give(ticket);
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

			//internal subclass that implements TicketBoard
			class MyTicketBoard implements TicketBoard {

				final Player player;

				MyTicketBoard(Player player) {
					this.player = player;
				}

				public int getCount(@Nonnull Ticket ticket) {
					return this.player.tickets().get(ticket);
				}
			}

			if (piece == this.mrX.piece()) {
				return Optional.of(new MyTicketBoard(this.mrX));
			}
			for (Player p : this.detectives) {
				if (p.piece() == piece) {
					return Optional.of(new MyTicketBoard(p));
				}
			}

			return Optional.empty();
		}


		@Nonnull @Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return log;
		}

		@Nonnull @Override
		public ImmutableSet<Piece> getWinner() {

			List<Piece> winner = new ArrayList<>();
			//since getMoves returns the moves for remaining players, a copy of this.remaining is
			//necessary to store the initial value
			ImmutableSet<Piece> bufferRemaining = this.remaining;
			List<Piece> detectivePieces = new ArrayList<>();
			this.detectives.forEach(x -> detectivePieces.add(x.piece()));

			// Detective Winning Scenarios

			// mrX is captured
			for (Player detective : this.detectives) {
				if (detective.location() == this.mrX.location()) {
					winner = detectivePieces;
					break;
				}
			}

			// mrX is stuck or has no tickets
			this.remaining = ImmutableSet.of(this.mrX.piece());//this.remaining is updated so that getMoves only returns mrX's possible moves
			if (getMoves().isEmpty()) {
				winner = detectivePieces;
			}
			this.remaining = bufferRemaining;


			// MrX Winning Scenarios

			//there are no more remaining rounds
			if(this.remainingRounds.isEmpty() && this.remaining.contains(mrX.piece())){
				winner.add(this.mrX.piece());
			}

			//if detectives are out of tickets or stuck
			this.remaining = ImmutableSet.copyOf(detectivePieces);
			if(getMoves().isEmpty()){
				winner.add(this.mrX.piece());
			}
			this.remaining = bufferRemaining;


			return ImmutableSet.copyOf(winner);
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

			for(int destination : setup.graph.adjacentNodes(source)) {//for adjacent node to the starting location

				if (!(detectiveLocations.contains(destination))) {//if there is no detective there already

					for (Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {//for every transport type to said adjacent node

						if (player.has(t.requiredTicket())) {//if the player has the required ticket
							singleMoves.add(new SingleMove(player.piece(), source, t.requiredTicket(), destination));//yhe move is added to the possible single moves list
						}
					}

					if (player.has(Ticket.SECRET)) {//if the player has a 'secret' ticket, than it can reach said adjacent node regardless of normal required ticket
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
			final var firstSingleMoves = getSingleMoves(setup, detectives, player, source);//possible single moves computed by getSingleMoves method

			if (player.has(Ticket.DOUBLE) && this.remainingRounds.size() > 1) {//if there are enough rounds left for 2 moves and player has a 'double' ticket

				for (SingleMove sMove1 : firstSingleMoves) {//for every possible single move

					var secondSingleMoves =getSingleMoves(setup, detectives, player, sMove1.destination);//another list if possible single moves is created


					for (SingleMove sMove2 : secondSingleMoves) {//for every possible second single move

						if(sMove1.ticket != sMove2.ticket || player.hasAtLeast(sMove1.ticket, 2)) {//if both tickets used are of the same type, then the player needs al least 2 of that ticket type
							DoubleMove doubleMove = new DoubleMove(//the two single moves are combined into one double move
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

		private ImmutableSet<Move> getMoves() {

			List<Move> moves = new ArrayList<>();
			if (this.remaining.contains(this.mrX.piece())) { // get mrX moves
				ImmutableSet<SingleMove> sMoves = getSingleMoves(
						setup,
						detectives,
						mrX,
						mrX.location()
				);
				List<DoubleMove> dMoves = List.copyOf(getDoubleMoves(
						setup,
						detectives,
						mrX,
						mrX.location()
				));
				moves.addAll(sMoves);
				moves.addAll(dMoves);
			} else { // get detective moves
				for (Player p : this.detectives) {
					if (this.remaining.contains(p.piece())) {
						ImmutableSet<SingleMove> sMoves = getSingleMoves(
								setup,
								detectives,
								p,
								p.location()
						);
						moves.addAll(sMoves);
					}
				}
			}
			return ImmutableSet.copyOf(moves);
		}

		@Nonnull @Override
		public ImmutableSet<Move> getAvailableMoves() {

			//getAvailableMoves returns an empty list if the game is over, i.e. winners are declared
			if (!getWinner().isEmpty()) return ImmutableSet.of();
			//getMoves is a helper method that return all available moves
			return getMoves();
		}

	}
}
