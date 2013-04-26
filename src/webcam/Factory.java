package webcam;

public interface Factory<T> {
	public T newObject();

	public void reset(T t);
}
