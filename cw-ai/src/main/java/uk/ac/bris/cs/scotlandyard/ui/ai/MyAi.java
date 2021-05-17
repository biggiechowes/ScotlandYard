package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;


import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Pair;
import org.controlsfx.control.tableview2.filter.filtereditor.SouthFilter;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAi implements Ai {

	Board board;
	Map<Integer, Integer> scoreMap = new HashMap<>();
	ImmutableList<Piece.Detective> detectives;
	Piece mrX;

	private void initialiseScoreMap(){
		for(int i = 0; i <= 199; i++) {
			scoreMap.put(i, 0);
		}
	}

	private ImmutableList<Integer> getLocations() {
		List<Integer> locations = new ArrayList<>();
		this.detectives.forEach(x -> locations.add(this.board.getDetectiveLocation(x).get()));
		return ImmutableList.copyOf(locations);
	}

	private void setDetectivesOptimalPathScore() {


	}

	private void setFerryNodeScore() {
		List<Integer> ferryNodes = Arrays.asList(194, 157, 115, 108);
		if(this.board.getPlayerTickets(this.mrX).get().getCount(ScotlandYard.Ticket.SECRET) > 0) {
			for(Integer node : ferryNodes) {
				this.scoreMap.replace(node, scoreMap.get(node) + 100);
				if(this.board.getAvailableMoves().stream().anyMatch(x-> x.source() == node)){
					this.scoreMap.replace(node, scoreMap.get(node) - 100);
				}
			}
		}
	}

	public void setAdjacentNodeScore(Integer original) {

		List<Integer> distanceOneLocation = new ArrayList<>();

		for(Integer adjacentNode : this.board.getSetup().graph.adjacentNodes(original)) {
			scoreMap.replace(adjacentNode, scoreMap.get(adjacentNode) - 100);
			distanceOneLocation.add(adjacentNode);
		}

		for(Integer node : distanceOneLocation) {
			for(Integer adjacentNode : this.board.getSetup().graph.adjacentNodes(node)) {
				if (!distanceOneLocation.contains(adjacentNode) && !original.equals(adjacentNode)) {
					scoreMap.replace(adjacentNode, scoreMap.get(adjacentNode) - 50);
				}
			}
		}
	}

	private void setDetectivesAdjacentNodesScore() {
		for(Integer location : getLocations()) {
			scoreMap.replace(location, scoreMap.get(location) - 500);
			setAdjacentNodeScore(location);
		}
	}

	private void setScoreMap() {
		setDetectivesOptimalPathScore();
		setDetectivesAdjacentNodesScore();
		setFerryNodeScore();
	}

	private ImmutableList<Move> getHighestValueMoves() {
		List<Move> highestValueMoves = new ArrayList<>();
		int maxScore = 0;
		for (var move : board.getAvailableMoves()){
			if (move.commencedBy().isMrX()) {
				Move.Visitor<Integer> destination = new Move.Visitor<Integer>() {
					@Override
					public Integer visit(Move.SingleMove move) {
						return move.destination;
					}

					@Override
					public Integer visit(Move.DoubleMove move) {
						return move.destination2;
					}
				};
				if(scoreMap.get(move.visit(destination)) >= maxScore) {
					maxScore = scoreMap.get(move.visit(destination));
				}
			}
		}
		for (var move : board.getAvailableMoves()){
			if (move.commencedBy().isMrX()) {
				Move.Visitor<Integer> destination = new Move.Visitor<Integer>() {
					@Override
					public Integer visit(Move.SingleMove move) {
						return move.destination;
					}

					@Override
					public Integer visit(Move.DoubleMove move) {
						return move.destination2;
					}
				};
				if(scoreMap.get(move.visit(destination)) == maxScore) {
					highestValueMoves.add(move);
				}
			}
		}

		return ImmutableList.copyOf(highestValueMoves);
	}

	private void setPlayers() {
		List<Piece> detectivePieces = new ArrayList<>();
		List<Piece.Detective> bufferDetectives = new ArrayList<>();

		this.board.getPlayers().stream().filter(Piece::isDetective).forEach(detectivePieces::add);
		this.board.getPlayers().stream().filter(Piece::isMrX).forEach(x -> this.mrX = x);

		for(var detective : Piece.Detective.values()){

			for (Piece piece : detectivePieces) {

				if (piece.webColour().equals(detective.webColour())) {
					bufferDetectives.add(detective);
				}
			}
		}

		this.detectives = ImmutableList.copyOf(bufferDetectives);
	}

	private Move getSelectedMove() {

		ImmutableList<Move> highestValueMoves = getHighestValueMoves();
		ImmutableList<Move> moves = board.getAvailableMoves().asList();

		if(highestValueMoves.isEmpty()) {
			return moves.get(new Random().nextInt(moves.size()));
		}

		List<Move> normalMoves = new ArrayList<>();
		List<Move> doubleMoves = new ArrayList<>();
		List<Move> secretMoves = new ArrayList<>();

		for(var move : moves){
			Move.Visitor<ArrayList<ScotlandYard.Ticket>> tickets = new Move.Visitor<ArrayList<ScotlandYard.Ticket>>() {
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
			if(move.visit(tickets).size() > 1) {
				doubleMoves.add(move);
			}
			if(move.visit(tickets).contains(ScotlandYard.Ticket.SECRET)) {
				secretMoves.add(move);
			}
			if(!(doubleMoves.contains(move) || secretMoves.contains(move))) {
				normalMoves.add(move);
			}
		}
		var rand = Math.random();
		if (rand < 0.8) {
			if (normalMoves.isEmpty()) return secretMoves.get(new Random().nextInt(secretMoves.size()));
			return normalMoves.get(new Random().nextInt(normalMoves.size()));
		}
		else if(rand < 0.9) {
			if (doubleMoves.isEmpty()) {
				if(normalMoves.isEmpty()) return secretMoves.get(new Random().nextInt(secretMoves.size()));
				return normalMoves.get(new Random().nextInt(normalMoves.size()));
			}
			return doubleMoves.get(new Random().nextInt(doubleMoves.size()));
		}
		else {
			if (secretMoves.isEmpty()) return normalMoves.get(new Random().nextInt(normalMoves.size()));
			return secretMoves.get(new Random().nextInt(secretMoves.size()));
		}

	}

	@Nonnull @Override public String name() { return "ScotFish"; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {

		this.board = board;
		initialiseScoreMap();
		setPlayers();
		setScoreMap();
		System.out.println(scoreMap);
		return getSelectedMove();
	}
}
