package bgu.spl.net.srv;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class ConnectionsImpl<T> implements Connections<T>{
    private HashMap<Integer, Pair<ConnectionHandler<T>,String>> loggedInHandlers;
    private HashMap<Integer, ConnectionHandler<T>> notLoggedInHandlers;
    private HashSet<String> connectedNames;
    public ConnectionsImpl()
    {
        loggedInHandlers = new HashMap<>();
        notLoggedInHandlers = new HashMap<>();
        connectedNames = new HashSet<>();
    }
    @Override
    public void connect(int connectionId, ConnectionHandler<T> handler) {
        if(notLoggedInHandlers.put(connectionId,handler)!=null||loggedInHandlers.containsKey(connectionId))
        {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public boolean login(int connectionId,String name) {
        if(!connectedNames.contains(name)) {
            ConnectionHandler handler = (notLoggedInHandlers.remove(connectionId));
            if (handler == null) {
                throw new IllegalArgumentException();
            }
            loggedInHandlers.put(connectionId, new Pair<>(handler,name));
            connectedNames.add(name);
            return true;
        }
        return false;

    }

    @Override
    public boolean send(int connectionId, T msg) {
        Pair<ConnectionHandler<T>,String> pair = loggedInHandlers.get(connectionId);
        if(pair!=null)
        {
            pair.first.send(msg);
            return true;
        }
        else
        {
            ConnectionHandler<T> handler = notLoggedInHandlers.get(connectionId);
            if(handler!=null)
            {
                handler.send(msg);
                return true;
            }
        }
        return false;
    }


    @Override
    public void disconnect(int connectionId,String name) {
        if(loggedInHandlers.remove(connectionId)!=null)
        {
            notLoggedInHandlers.remove(connectionId);
            connectedNames.remove(name);
        }
    }

    @Override
    public void broadcast(T msg) {
        Iterator<Pair<ConnectionHandler<T>,String>> it = loggedInHandlers.values().iterator();
        while(it.hasNext())
        {
            Pair<ConnectionHandler<T>,String> p = it.next();
            if(!p.first.send(msg)) {
                connectedNames.remove(p.second);
                it.remove();
            }
        }
    }
    private class  Pair<A,B>{
        final public A first;
        final public B second;
        Pair(A first,B second)
        {
            this.first = first;
            this.second = second;
        }

    }
}
