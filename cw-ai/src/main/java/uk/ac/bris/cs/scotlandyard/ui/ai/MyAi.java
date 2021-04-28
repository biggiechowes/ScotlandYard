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

	private ImmutableList<Integer> getDetectivesOptimalPath() {
		List<> detectivePieces = new ArrayList<>();
		this.board.getPlayers().stream().filter(Piece::isMrX).forEach(detectivePieces::add);
		List<Integer> locationsInPath = new ArrayList<>();

		for(Piece detective : detectivePieces) {
			this.getDetectivesOptimalPath(detective);
		}
	}

	private void setScore(){
		getDetectivesOptimalPath();
	}

	@Nonnull @Override public String name() { return "ScotFish"; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		this.board = board;
		initialiseScoreMap();
		var moves = board.getAvailableMoves().asList();
		return moves.get(new Random().nextInt(moves.size()));
	}
}
