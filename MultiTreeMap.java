import java.util.TreeMap;
import java.util.Collection;
import java.util.ArrayList;

/***
 * Navigable Map that allows multiple entries per key, and has case-insensitive keys, while returning a Tuple that includes the case-sensitive key.
 ***/

public class MultiTreeMap<V> extends TreeMap<String, Collection<Tuple<String, V>>>{
    public Collection<Tuple<String, V>> put(String s, Collection<Tuple<String, V>> val) {
        Collection<Tuple<String, V>> old = get(s);
            super.put(s.toLowerCase(), val);
            return old;
        }
        public Collection<Tuple<String, V>> get(String s) {
        return super.get(s.toLowerCase());
    }

    public Collection<Tuple<String, V>> putSingle(String s, V val) {
        Collection<Tuple<String, V>> t = get(s);
        if (t == null) {
            t = new ArrayList<Tuple<String, V>>();
        }
        t.add(new Tuple<String, V>(s, val));
        return super.put(s.toLowerCase(), t);
    }
}