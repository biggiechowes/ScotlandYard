package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import javax.annotation.Nonnull;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import java.util.HashSet;

public final class MyModelFactory implements Factory<Model> {

	private ImmutableSet<Model.Observer> observers;
	private Board.GameState state;

	//subclass pattern that implements Model
	public Model myModel = new Model() {

		@Nonnull
		@Override
		public Board getCurrentBoard() {
			return state;
		} // getter function, returns state of type board

		@Override
		public void registerObserver(@Nonnull Model.Observer observer) { // nonnull argument, registering a observer for a passed nonnull parameter
			if(observers.contains(observer)) throw new IllegalArgumentException("Observer already registered!");

			HashSet<Observer> bufferObservers = new HashSet<>(observers); // lossless merging of current observer into observers
			bufferObservers.add(observer);
			observers = ImmutableSet.copyOf(bufferObservers);
		}

		@Override
		public void unregisterObserver(Model.Observer observer) {
			if(observer.equals(null)) throw new NullPointerException("Cannot unregister null observer"); //warning here is inevitable
			if(!observers.contains(observer)) throw new IllegalArgumentException("Observer not registered!");


			HashSet<Observer> bufferObservers = new HashSet<>(observers);
			bufferObservers.remove(observer);
			observers = ImmutableSet.copyOf(bufferObservers);
		}

		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() {
			return observers;
		} // getter function for observer list

		@Override
		public void chooseMove(@Nonnull Move move) {
			state = state.advance(move); // advances to next game state, processes the move
			Observer.Event event;

			if(state.getWinner().isEmpty()) event = Observer.Event.MOVE_MADE; // game is not over
			else event = Observer.Event.GAME_OVER; // game is over

			for(Observer observer : observers) { // changes observers according to game state
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