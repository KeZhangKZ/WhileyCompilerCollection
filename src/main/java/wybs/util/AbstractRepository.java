package wybs.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import wybs.lang.Build;
import wyfs.lang.Content;
import wyfs.lang.Path;

public class AbstractRepository implements Build.Repository<AbstractRepository.State> {
	private State state;
		
	public AbstractRepository(State state) {
		this.state = state;
	}
	
	public State get() {
		return state;
	}
	
	@Override
	public void apply(Function<State, State> transformer) {
		// FIXME: need to do more here, like error handling.
		state = transformer.apply(state);
	}
	
	public static class State implements Build.State<State>, Iterable<Build.Entry> {
		private final ArrayList<Build.Entry> items = new ArrayList<>();

		public State(Build.Entry... entries) {
			for (int i = 0; i != entries.length; ++i) {
				items.add(entries[i]);
			}
		}

		private State(State s) {
			this.items.addAll(s.items);
		}

		public <T extends Build.Entry> List<T> selectAll(Content.Type<T> ct, Predicate<T> query) {
			ArrayList<T> es = new ArrayList<>();
			for (Build.Entry e : items) {
				if (e.getContentType().equals(ct) && query.test((T) e)) {
					es.add((T) e);
				}
			}
			return es;
		}

		public <T extends Build.Entry> List<T> selectAll(Content.Type<T> ct) {
			ArrayList<T> es = new ArrayList<>();
			for (Build.Entry e : items) {
				if (e.getContentType().equals(ct)) {
					es.add((T) e);
				}
			}
			return es;
		}
		
		public <T extends Build.Entry> T get(Content.Type<T> ct, Path.ID id) {
			for (Build.Entry e : items) {
				if (e.getContentType().equals(ct) && e.getID().equals(id)) {
					return (T) e;
				}
			}
			return null;
		}
		
		@Override
		public State put(Build.Entry entry) {
			// Clone state
			State s = new State(this);
			// Update state
			for (int i = 0; i != items.size(); ++i) {
				Build.Entry e = items.get(i);
				if (e.getContentType().equals(entry.getContentType()) && e.getID().equals(entry.getID())) {
					// Overwrite existing entry
					s.items.set(i, entry);
					return s;
				}
			}
			// Create new entry
			s.items.add(entry);
			// Done
			return s;
		}

		@Override
		public Iterator<Build.Entry> iterator() {
			return items.iterator();
		}
	}
}
