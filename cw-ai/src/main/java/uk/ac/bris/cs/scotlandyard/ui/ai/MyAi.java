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

	//TO DO!
	//make a global detective list

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
		final double F = 0.5;

		if (location.equals(original) || this.board.getSetup().graph.adjacentNodes(original)
				.contains(location)) {
			if(location.equals(original) ) {
				scoreMap.replace(location, scoreMap.get(location) - N);
			}
			else scoreMap.replace(location, scoreMap.get(location) - 50);
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

	private void setDetectives(Board board) {
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
		setDetectives(board);
		var moves = board.getAvailableMoves().asList();
		return moves.get(new Random().nextInt(moves.size()));
	}
}
