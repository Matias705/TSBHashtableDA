import java.io.Serializable;
import java.util.AbstractSet;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

public class TSBHashtableDA<K, V> implements Map<K, V>, Cloneable, Serializable {

    // ************************ Constantes (privadas o públicas).

    // el tamaño máximo que podrá tener el arreglo de soprte...
    private final static int MAX_SIZE = Integer.MAX_VALUE;

    // ************************ Atributos privados (estructurales).

    // la tabla hash
    private Object table[];

    // el tamaño inicial de la tabla (tamaño con el que fue creada)...
    private int initial_capacity;

    // la cantidad de objetos que contiene la tabla en TODAS sus listas...
    private int count;

    // el factor de carga para calcular si hace falta un rehashing...
    private float load_factor;

    // ************************ Atributos privados (para gestionar las vistas).

    /*
     * (Tal cual están definidos en la clase java.util.Hashtable)
     * Cada uno de estos campos se inicializa para contener una instancia de la
     * vista que sea más apropiada, la primera vez que esa vista es requerida.
     * La vista son objetos stateless (no se requiere que almacenen datos, sino
     * que sólo soportan operaciones), y por lo tanto no es necesario crear más
     * de una de cada una.
     */
    private transient Set<K> keySet = null;
    private transient Set<Map.Entry<K, V>> entrySet = null;
    private transient Collection<V> values = null;
}