package wybs.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import wybs.lang.Build;
import wybs.lang.SyntacticHeap;
import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.lang.Path.ID;

public class SimpleBuildSystem implements Build.System<SimpleBuildSystem.State> {
	private State state;
		
	public SimpleBuildSystem(State state) {
		this.state = state;
	}
	
	@Override
	public void apply(Function<State, State> transformer) {
		// FIXME: need to do more here, like error handling.
		state = transformer.apply(state);
	}
	
	public static class State implements Build.State<State> {
		private final ArrayList<Entry<?>> items = new ArrayList<>();

		public State(Entry<?>... entries) {
			for (int i = 0; i != entries.length; ++i) {
				items.add(entries[i]);
			}
		}

		private State(State s) {
			this.items.addAll(s.items);
		}
		
//		public <T> List<Build.Entry<T>> selectAll(Predicate<Entry> query) {
//			ArrayList<Entry> es = new ArrayList<>();
//			for (Entry e : items) {
//				if (query.test(e)) {
//					es.add(e);
//				}
//			}
//			return es;
//		}
		
		public <T> List<Build.Entry<T>> selectAll(Content.Type<T> ct) {
			ArrayList<Build.Entry<T>> es = new ArrayList<>();
			for (Entry<?> e : items) {
				if (e.getContentType().equals(ct)) {
					es.add((Build.Entry<T>) e);
				}
			}
			return es;
		}
		
		public <T> Build.Entry<T> get(Content.Type<T> ct, Path.ID id) {
			for (Entry<?> e : items) {
				if (e.getContentType().equals(ct) && e.getID().equals(id)) {
					return (Build.Entry<T>) e;
				}
			}
			return null;
		}
		
		@Override
		public <T> State put(Content.Type<T> ct, Path.ID id,T contents) {
			// Construct new internal entry
			Entry<T> ne = new Entry<T>(ct,id,contents);
			// Clone state
			State s = new State(this);
			// Update state
			for (int i=0;i!=items.size();++i) {
				Entry<?> e = items.get(i);
				if (e.getContentType().equals(ct) && e.getID().equals(id)) {
					// Overwrite existing entry
					s.items.set(i, ne);
					return s;
				}
			}
			// Create new entry
			s.items.add(ne);
			// Done
			return s;
		}
	}
	
	public static class Entry<T> implements Build.Entry<T> {
		private final Content.Type<T> type;
		private final Path.ID id;
		private final T contents;
		
		public Entry(Content.Type<T> ct, Path.ID id, T item) {
			this.id = id;
			this.type = ct;
			this.contents = item;
		}
		
		@Override
		public ID getID() {
			return id;
		}

		@Override
		public Content.Type<T> getContentType() {
			return type;
		}
		
		@Override
		public T getContent() {
			return contents;
		}
	}
}
