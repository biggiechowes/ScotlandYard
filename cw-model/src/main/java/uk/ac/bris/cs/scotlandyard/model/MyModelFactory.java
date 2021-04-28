package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;

import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

	private ImmutableSet<Model.Observer> observers;
	private Board.GameState state;

	//subclass that implements Model
	public Model myModel = new Model() {

		@Nonnull
		@Override
		public Board getCurrentBoard() {
			return state;
		}

		@Override
		public void registerObserver(Model.Observer observer) {
			if(observer.equals(null)) throw new NullPointerException("Cannot register null observer!");
			if(observers.contains(observer)) throw new IllegalArgumentException("Observer already registered!");

			HashSet<Observer> bufferObservers = new HashSet<>(observers);
			bufferObservers.add(observer);
			observers = ImmutableSet.copyOf(bufferObservers);
		}

		@Override
		public void unregisterObserver(Model.Observer observer) {
			if(observer.equals(null)) throw new NullPointerException("Cannot unregister null observer");
			if(!observers.contains(observer)) throw new IllegalArgumentException("Observer not registered!");


			HashSet<Observer> bufferObservers = new HashSet<>(observers);
			bufferObservers.remove(observer);
			observers = ImmutableSet.copyOf(bufferObservers);
		}

		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() {
			return observers;
		}

		@Override
		public void chooseMove(@Nonnull Move move) {
			state = state.advance(move);
			Observer.Event event;

			if(state.getWinner().isEmpty()) event = Observer.Event.MOVE_MADE;
			else event = Observer.Event.GAME_OVER;

			for(Observer observer : observers) {
				observer.onModelChanged(state, event);
			}

		}
	};

	@Nonnull @Override
	public Model build(GameSetup setup,
					   Player mrX,
					   ImmutableList<Player> detectives) {

		observers = ImmutableSet.of();
		state = new MyGameStateFactory().build(setup, mrX, detectives);
		return  myModel;
	}
}