package bgu.spl.net.srv;

public interface Connections<T> {

    void connect(int connectionId, ConnectionHandler<T> handler);
    boolean login(int connectionId,String name);

    boolean send(int connectionId, T msg);

    void disconnect(int connectionId,String name);
    void broadcast(T msg);
}
