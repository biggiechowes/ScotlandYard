package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;


import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAi implements Ai {

	//Attributes

	Board board;
	Map<Integer, Integer> scoreMap = new HashMap<>();
	ImmutableList<Piece.Detective> detectives;
	Piece mrX;


	//Setters

	//Setting the score for nodes adjacent to origin to a distance of two
	public void setAdjacentNodeScore(Integer origin,
									 final int N, //score modifier for this method
									 final int F //modifying factor
									) {


		List<Integer> distanceOneLocations = new ArrayList<>();//nodes with distance one to detective locations as a list

		for(Integer adjacentNode : this.board.getSetup().graph.adjacentNodes(origin)) {//for every node adjacent to origin
			scoreMap.replace(adjacentNode, scoreMap.get(adjacentNode) - N);//score is decreased br N points
			distanceOneLocations.add(adjacentNode);//the node is added to the distanceOneLocation list
		}

		for(Integer node : distanceOneLocations) {//for every node in distanceOneLocation
			for(Integer adjacentNode : this.board.getSetup().graph.adjacentNodes(node)) {//for every node adjacent to it
				if ( !(distanceOneLocations.contains(adjacentNode) || origin.equals(adjacentNode)) ) {//if the adjacent node is not the origin or in the distanceOneLocation list
					scoreMap.replace(adjacentNode, scoreMap.get(adjacentNode) - (N / F) );//score is decreased by N/F points
				}
			}
		}
	}

	//Setting the score for the detective location nodes and for those that are adjacent to them
	private void setDetectivesAdjacentNodesScore(int N, int F) {

		for(Integer location : getDetectiveLocations()) {//for every detective location
			scoreMap.replace(location, scoreMap.get(location) - (N * 5) );//the score is decreased by N * 5 points
			setAdjacentNodeScore(location, N, F);//setAdjacentNodeScore helper function to set the score of adjacent nodes
		}
	}

	//Set the score for the ferry nodes
	private void setFerryNodeScore(final int N /*score modifier for this method*/) {

		List<Integer> ferryNodes = Arrays.asList(194, 157, 115, 108);

		if(this.board.getPlayerTickets(this.mrX).get().getCount(ScotlandYard.Ticket.SECRET) > 0) {//if mrX has at least one secret ticket
			for(Integer node : ferryNodes) {//for every ferry node
				this.scoreMap.replace(node, scoreMap.get(node) + N);//the score is increased by N

				//If mrX is on a ferry node, then that node's score is reduced by N, resulting in a net score increase
				//of 0. This avoids the case of mrX taking a double move to the same spot.
				if(this.board.getAvailableMoves().stream().anyMatch(x-> x.source() == node)){
					this.scoreMap.replace(node, scoreMap.get(node) - N);
				}
			}
		}
	}

	//Setting the score for all nodes
	private void setScoreMap() {
		setDetectivesAdjacentNodesScore(100, 2);
		setFerryNodeScore(125);
	}

	//Setting thr players
	private void setPlayers() {

		List<Piece> detectivePieces = new ArrayList<>();
		List<Piece.Detective> bufferDetectives = new ArrayList<>();

		this.board.getPlayers().stream().filter(Piece::isDetective).forEach(detectivePieces::add);//detective pieces are added to the detectivePieces list
		this.board.getPlayers().stream().filter(Piece::isMrX).forEach(x -> this.mrX = x);//this.mrX is set as mrX's piece

		for(var detective : Piece.Detective.values()){//for each detective in Piece.Detective enum
			for (Piece piece : detectivePieces) {//for each piece in detectivePieces
				if (piece.webColour().equals(detective.webColour())) {//if the piece's colour matches the detective's colour
					bufferDetectives.add(detective);//the detective is added to the buffetDetective list
				}
			}
		}

		this.detectives = ImmutableList.copyOf(bufferDetectives);//this.detectives is updated
	}


	//Getters

	//The moves that have the highest value are returned as a immutable list
	private ImmutableList<Move> getHighestValueMoves() {
		
		List<Move> highestValueMoves = new ArrayList<>();
		int maxScore = -2147483648;//maxScore initialised with the lowest int value
		
		for (var move : board.getAvailableMoves()){//for every available move
			if (move.commencedBy().isMrX()) {//if the move is commenced by mrX
				
				//The visitor pattern is used to get the final destination of the move
				Move.Visitor<Integer> destination = new Move.Visitor<>() {
					@Override
					public Integer visit(Move.SingleMove move) {
						return move.destination;
					}

					@Override
					public Integer visit(Move.DoubleMove move) {
						return move.destination2;
					}
				};

				if(scoreMap.get(move.visit(destination)) >= maxScore) {//if the score of the move's destination node is greater than maxScore
					maxScore = scoreMap.get(move.visit(destination));//maxScore is updated
				}
			}
		}
		for (var move : board.getAvailableMoves()){
			if (move.commencedBy().isMrX()) {

				//The visitor pattern is used to get the final destination of the move again
				Move.Visitor<Integer> destination = new Move.Visitor<>() {
					@Override
					public Integer visit(Move.SingleMove move) {
						return move.destination;
					}

					@Override
					public Integer visit(Move.DoubleMove move) {
						return move.destination2;
					}
				};

				if(scoreMap.get(move.visit(destination)) == maxScore) {//if the move's destination node has a value equal to maxScore
					highestValueMoves.add(move);//the move is added to the highestValueMoves list
				}
			}
		}

		return ImmutableList.copyOf(highestValueMoves);
	}

	//Returns the move that is considered the best based on the destination node's score
	private Move getBestMove() {

		ImmutableList<Move> highestValueMoves = getHighestValueMoves();
		ImmutableList<Move> moves = board.getAvailableMoves().asList();

		if(highestValueMoves.isEmpty()) {//if the highestValueMoves list is empty
			return moves.get(new Random().nextInt(moves.size()));//return a random possible move
		}

		List<Move> normalMoves = new ArrayList<>();
		List<Move> doubleMoves = new ArrayList<>();
		List<Move> secretMoves = new ArrayList<>();

		for(var move : moves){

			//The visitor pattern is used to get the move's tickets as a list
			Move.Visitor<ArrayList<ScotlandYard.Ticket>> tickets = new Move.Visitor<>() {
				@Override
				public ArrayList<ScotlandYard.Ticket> visit(Move.SingleMove move) {
					ArrayList<ScotlandYard.Ticket> tickets = new ArrayList<>();
					tickets.add(move.ticket);
					return tickets;
				}

				@Override
				public ArrayList<ScotlandYard.Ticket> visit(Move.DoubleMove move) {
					ArrayList<ScotlandYard.Ticket> tickets = new ArrayList<>();
					tickets.add(move.ticket1);
					tickets.add(move.ticket2);
					return tickets;
				}
			};

			if(move.visit(tickets).size() > 1) {//if a move uses more than one ticket
				doubleMoves.add(move);//it is added to the doubleMoves list
			}
			if(move.visit(tickets).contains(ScotlandYard.Ticket.SECRET)) {//if a move uses a secret ticket
				secretMoves.add(move);//it is added to the secretMoves list
			}
			if(!(doubleMoves.contains(move) || secretMoves.contains(move))) {//if a move is not part of the secretMoves or doubleMoves list
				normalMoves.add(move);//it is added to the normalMoves list
			}
		}
		double rand = Math.random();//a random double with the value between 0 and 1 is selected

		if (rand < 0.8) {//if the value is less than 0.8 (80% chance)
			//if there are no normal moves available, there must be at least one secret move available
			if (normalMoves.isEmpty()) return secretMoves.get(new Random().nextInt(secretMoves.size()));

			return normalMoves.get(new Random().nextInt(normalMoves.size()));//return a random normal move
		}
		else if(rand < 0.9) {//if the value is greater than 0.8, but less than 0.9 (10% chance)
			//if there are no double moves available, there must be at least one normal or secret move available
			if (doubleMoves.isEmpty()) {
				//if there are no normal moves available, there must be at least one secret move available
				if(normalMoves.isEmpty()) return secretMoves.get(new Random().nextInt(secretMoves.size()));//if there are no normal moves, return a secret move

				return normalMoves.get(new Random().nextInt(normalMoves.size()));//return a normal move
			}
			return doubleMoves.get(new Random().nextInt(doubleMoves.size()));
		}
		else {//if the value is greater than 0.9 (10% chance)
			//if there are no secret moves available, there must be at least one normal or double move available
			if (secretMoves.isEmpty()) {
				//if there are no normal moves available, there must be at least one double move available
				if(normalMoves.isEmpty()) return  doubleMoves.get(new Random().nextInt(doubleMoves.size()));

				return normalMoves.get(new Random().nextInt(normalMoves.size()));
			}

			return secretMoves.get(new Random().nextInt(secretMoves.size()));//return a secret move
		}

	}

	//Return the locations of detectives as a immutable list
	private ImmutableList<Integer> getDetectiveLocations() {
		List<Integer> locations = new ArrayList<>();
		this.detectives.forEach(x -> locations.add(this.board.getDetectiveLocation(x).get()));
		return ImmutableList.copyOf(locations);
	}


	//Methods

	//Initialise all map nodes with the initial score of 0
	private void initialiseScoreMap(){
		for(int i = 1; i <= 199; i++) {
			scoreMap.put(i, 0);
		}
	}

	@Nonnull @Override public String name() { return "ScotFish"; }//ScotFish is a pun on the popular chess engine Stockfish

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {

		this.board = board;//update the board attribute
		initialiseScoreMap();//initialise the score map with 0s
		setPlayers();//set the players
		setScoreMap();//set the score of all the nodes

		return getBestMove();//return best move
	}
}
