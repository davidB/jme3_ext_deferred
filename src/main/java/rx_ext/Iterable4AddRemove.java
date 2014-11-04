package rx_ext;

import java.util.Iterator;
import java.util.LinkedList;

public class Iterable4AddRemove<T> implements Iterable<T> {
	public final Observable4AddRemove<T> ar;
	public final LinkedList<T> data = new LinkedList<>();

	public Iterable4AddRemove(Observable4AddRemove<T> ar) {
		this.ar = ar;
		ar.add.subscribe((v) -> data.add(v));
		ar.remove.subscribe((v) -> data.remove(v));
	}

	@Override
	public Iterator<T> iterator() {
		return data.iterator();
	}
}
