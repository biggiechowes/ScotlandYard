package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;


import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Pair;
import org.controlsfx.control.tableview2.filter.filtereditor.SouthFilter;
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

		int rand = new Random().nextInt(highestValueMoves.size());
		if(rand < 0) rand = rand * (-1);
		
		return highestValueMoves.get(rand);
	}

	private void setDetectives() {
		List<Piece> detectivePieces = new ArrayList<>();
		List<Piece.Detective> bufferDetectives = new ArrayList<>();

		this.board.getPlayers().stream().filter(Piece::isDetective).forEach(detectivePieces::add);

		for(var detective : Piece.Detective.values()){

			for (Piece piece : detectivePieces) {

				if (piece.webColour().equals(detective.webColour())) {
					bufferDetectives.add(detective);
				}
			}
		}

		this.detectives = ImmutableList.copyOf(bufferDetectives);
	}

	@Nonnull @Override public String name() { return "ScotFish"; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {

		this.board = board;
		initialiseScoreMap();
		setDetectives();
		setScoreMap();
		System.out.println(scoreMap);
		return getHighestValueMove();
	}
}
