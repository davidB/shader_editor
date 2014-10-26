package sandbox_jfx;

import java.util.TreeMap;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

/**
 * A simple Components'store for ECS (Entity-Component-System).
 *
 * Usage :
 *
 * @author davidB
 *
 * @param <Id> the type of Entity (eg Long, String)
 * @TODO sample and documentation
 */
class Components<Id> {
	public final ObservableMap<Class<?>, ObservableMap<Id, ?>> data = FXCollections.observableMap(new TreeMap<Class<?>, ObservableMap<Id, ?>>());

	public <T> ObservableMap<Id, T> find(Class<T> clazz) {
		ObservableMap<Id, T> b = (ObservableMap<Id, T>) data.get(clazz);
		if (b == null) {
			b = FXCollections.observableMap(new TreeMap<Id, T>());
			data.put(clazz, b);
		}
		return b;
	}

	public <T> T find(Class<T> clazz, Id entity) {
		return find(clazz).get(entity);
	}

	public <T> T set(T component, Id entity) {
		return find((Class<T>)component.getClass()).put(entity, component);
	}

	public <T> T set(Class<T> clazz, Id entity) {
		return find(clazz).remove(entity);
	}
}
