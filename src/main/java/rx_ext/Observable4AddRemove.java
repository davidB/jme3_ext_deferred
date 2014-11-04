package rx_ext;

import rx.subjects.PublishSubject;

public class Observable4AddRemove<T> {
	public final PublishSubject<T> add = PublishSubject.create();
	public final PublishSubject<T> remove = PublishSubject.create();
}
