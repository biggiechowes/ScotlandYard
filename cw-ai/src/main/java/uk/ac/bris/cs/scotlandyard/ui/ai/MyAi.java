package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;


import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

public class MyAi implements Ai {

	Board board;
	Map<Integer, Integer> scoreMap = new HashMap<>();
	ImmutableList<Piece.Detective> detectives;

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

		List<Integer> locationsInPath = new ArrayList<>();

	}

	public void setAdjacentNodeScore(Integer location, Integer original) {
		final int N = 100;
		final double F = 2;

		if (location.equals(original) || this.board.getSetup().graph.adjacentNodes(original)
				.contains(location)) {
			if(location.equals(original) ) {
				scoreMap.replace(location, scoreMap.get(location) - N);
			}
			else scoreMap.replace(location, scoreMap.get(location) - N/2);
			for (Integer adjacentNode : this.board.getSetup().graph.adjacentNodes(location)) {

				setAdjacentNodeScore(adjacentNode, original);
			}
		}
	}

	private void setDetectivesAdjacentNodesScore() {
		for(Integer location : getLocations()) {
			scoreMap.replace(location, scoreMap.get(location) - 500);
			setAdjacentNodeScore(location, location);
		}
	}

	private void setScoreMap() {
		setDetectivesOptimalPathScore();
		setDetectivesAdjacentNodesScore();
	}

	private Move getHighestValueMove() {
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
				if(scoreMap.get(destination) >= maxScore) {
					maxScore = scoreMap.get(destination);
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
				if(scoreMap.get(destination) == maxScore) {
					highestValueMoves.add(move);
				}
			}
		}
		return highestValueMoves.get(new Random().nextInt(highestValueMoves.size()));
	}

	private void setDetectives() {
		List<Piece> detectivePieces = new ArrayList<>();
		List<Piece.Detective> bufferDetectives = new ArrayList<>();

		this.board.getPlayers().stream().filter(Piece::isMrX).forEach(detectivePieces::add);

		for(var detective : Piece.Detective.values()){

			for (Piece piece : detectivePieces) {

				if (piece.webColour().equals(detective.webColour())) {
					bufferDetectives.add(detective);
				}
			}
		}
		this.detectives = ImmutableList.copyOf(bufferDetectives);
	}

	private void setScore(){
		setDetectivesOptimalPathScore();
		setDetectivesAdjacentNodesScore();
	}

	@Nonnull @Override public String name() { return "ScotFish"; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {

		this.board = board;
		initialiseScoreMap();
		setDetectives();
		return getHighestValueMove();
	}
}
