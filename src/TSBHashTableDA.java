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

public class TSBHashTableDA<K, V> implements Map<K, V>, Cloneable, Serializable {

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

    // conteo de operaciones de cambio de tamaño (fail-fast iterator)
    protected transient int modCount;

    // ************************ Constructores.
    // por defecto
    public TSBHashTableDA() {
        this(10, 0.7f);
    }

    // solo inicial capacity
    public TSBHashTableDA(int initial_capacity) {
        this(initial_capacity, 0.7f);
    }

    public TSBHashTableDA(int initial_capacity, float load_factor) {
        if (load_factor <= 0) {
            load_factor = 0.7f;
        }
        if (initial_capacity <= 0) {
            initial_capacity = 10;
        } else {
            if (initial_capacity > TSBHashTableDA.MAX_SIZE) {
                initial_capacity = TSBHashTableDA.MAX_SIZE;
            }
        }

        // Se crea la Tabla
        this.table = new Object[initial_capacity];

        for (int i = 0; i < table.length; i++) {
            table[i] = new Entry<>(); // no xq pito no lo encuentra
        }

        this.initial_capacity = initial_capacity;
        this.load_factor = load_factor;
        this.count = 0;
        this.modCount = 0;
    }

    /**
     * Retorna la cantidad de elementos contenidos en la tabla.
     * 
     * @return la cantidad de elementos de la tabla.
     */
    @Override
    public int size() {
        return this.count;
    }

    /**
     * Determina si la tabla está vacía (no contiene ningún elemento).
     * 
     * @return true si la tabla está vacía.
     */
    @Override
    public boolean isEmpty() {
        return (this.count == 0);
    }

    /**
     * Determina si la clave key está en la tabla.
     * 
     * @param key la clave a verificar.
     * @return true si la clave está en la tabla.
     * @throws NullPointerException si la clave es null.
     */
    @Override
    public boolean containsKey(Object key) {
        return (this.get((K) key) != null);
    }

    /**
     * Determina si alguna clave de la tabla está asociada al objeto value que
     * entra como parámetro. Equivale a contains().
     * 
     * @param value el objeto a buscar en la tabla.
     * @return true si alguna clave está asociada efectivamente a ese value.
     */
    @Override
    public boolean containsValue(Object value) {
        return this.contains(value);
    }

    /**
     * Retorna el objeto al cual está asociada la clave key en la tabla, o null
     * si la tabla no contiene ningún objeto asociado a esa clave.
     * 
     * @param key la clave que será buscada en la tabla.
     * @return el objeto asociado a la clave especificada (si existe la clave) o
     *         null (si no existe la clave en esta tabla).
     * @throws NullPointerException si key es null.
     * @throws ClassCastException   si la clase de key no es compatible con la
     *                              tabla.
     */
    @Override
    public V get(Object key) {
        if (key == null)
            throw new NullPointerException("get(): parámetro null");

        int ib = this.h(key.hashCode());
        int cont = 0;
        while (cont < this.table.length) {
            Entry<K, V> x = (Entry<K, V>) this.table[ib];
            if (x.estado == 0) {
                return null;
            } else {
                if (key.equals(x.getKey())) {
                    if (x.estado == 1) { // tumba
                        return null;
                    } else {
                        return x.getValue();
                    }
                }
            }
            ib++;
            if (ib == this.table.length)
                ib = 0;
        }

        return null;
    }

    /**
     * Asocia el valor (value) especificado, con la clave (key) especificada en
     * esta tabla. Si la tabla contenía previamente un valor asociado para la
     * clave, entonces el valor anterior es reemplazado por el nuevo (y en este
     * caso el tamaño de la tabla no cambia).
     * 
     * @param key   la clave del objeto que se quiere agregar a la tabla.
     * @param value el objeto que se quiere agregar a la tabla.
     * @return el objeto anteriormente asociado a la clave si la clave ya
     *         estaba asociada con alguno, o null si la clave no estaba antes
     *         asociada a ningún objeto.
     * @throws NullPointerException si key es null o value es null.
     */
    @Override
    public V put(K key, V value) {
        if (key == null || value == null)
            throw new NullPointerException("put(): parámetro null");

        int ib = this.h(key);

        V old = null;
        Map.Entry<K, V> x = this.search_for_entry((K) key, ib);
        if (x != null) {
            old = x.getValue();
            x.setValue(value);
        } else {
            if (this.averageLength() >= this.load_factor * 10)
                this.rehash();
            ib = this.h(key);
            bucket = this.table[ib];

            Map.Entry<K, V> entry = new Entry<>(key, value);
            bucket.add(entry);
            this.count++;
            this.modCount++;
        }

        return old;
    }

    /**
     * Elimina de la tabla la clave key (y su correspondiente valor asociado).
     * El método no hace nada si la clave no está en la tabla.
     * 
     * @param key la clave a eliminar.
     * @return El objeto al cual la clave estaba asociada, o null si la clave no
     *         estaba en la tabla.
     * @throws NullPointerException - if the key is null.
     */
    @Override
    public V remove(Object key) {
        if (key == null)
            throw new NullPointerException("remove(): parámetro null");

        int ib = this.h(key.hashCode());
        TSBArrayList<Map.Entry<K, V>> bucket = this.table[ib];

        int ik = this.search_for_index((K) key, bucket);
        V old = null;
        if (ik != -1) {
            old = bucket.remove(ik).getValue();
            this.count--;
            this.modCount++;
        }

        return old;
    }

    /**
     * Copia en esta tabla, todos los objetos contenidos en el map especificado.
     * Los nuevos objetos reemplazarán a los que ya existan en la tabla
     * asociados a las mismas claves (si se repitiese alguna).
     * 
     * @param m el map cuyos objetos serán copiados en esta tabla.
     * @throws NullPointerException si m es null.
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    /**
     * Elimina todo el contenido de la tabla, de forma de dejarla vacía. En esta
     * implementación además, el arreglo de soporte vuelve a tener el tamaño que
     * inicialmente tuvo al ser creado el objeto.
     */
    @Override
    public void clear() {
        this.table = new TSBArrayList[this.initial_capacity];
        for (int i = 0; i < this.table.length; i++) {
            this.table[i] = new TSBArrayList<>();
        }
        this.count = 0;
        this.modCount++;
    }

    /**
     * Retorna un Set (conjunto) a modo de vista de todas las claves (key)
     * contenidas en la tabla. El conjunto está respaldado por la tabla, por lo
     * que los cambios realizados en la tabla serán reflejados en el conjunto, y
     * viceversa. Si la tabla es modificada mientras un iterador está actuando
     * sobre el conjunto vista, el resultado de la iteración será indefinido
     * (salvo que la modificación sea realizada por la operación remove() propia
     * del iterador, o por la operación setValue() realizada sobre una entrada
     * de la tabla que haya sido retornada por el iterador). El conjunto vista
     * provee métodos para eliminar elementos, y esos métodos a su vez
     * eliminan el correspondiente par (key, value) de la tabla (a través de las
     * operaciones Iterator.remove(), Set.remove(), removeAll(), retainAll()
     * y clear()). El conjunto vista no soporta las operaciones add() y
     * addAll() (si se las invoca, se lanzará una UnsuportedOperationException).
     * 
     * @return un conjunto (un Set) a modo de vista de todas las claves
     *         mapeadas en la tabla.
     */
    @Override
    public Set<K> keySet() {
        if (keySet == null) {
            // keySet = Collections.synchronizedSet(new KeySet());
            keySet = new KeySet();
        }
        return keySet;
    }

    /**
     * Retorna una Collection (colección) a modo de vista de todos los valores
     * (values) contenidos en la tabla. La colección está respaldada por la
     * tabla, por lo que los cambios realizados en la tabla serán reflejados en
     * la colección, y viceversa. Si la tabla es modificada mientras un iterador
     * está actuando sobre la colección vista, el resultado de la iteración será
     * indefinido (salvo que la modificación sea realizada por la operación
     * remove() propia del iterador, o por la operación setValue() realizada
     * sobre una entrada de la tabla que haya sido retornada por el iterador).
     * La colección vista provee métodos para eliminar elementos, y esos métodos
     * a su vez eliminan el correspondiente par (key, value) de la tabla (a
     * través de las operaciones Iterator.remove(), Collection.remove(),
     * removeAll(), removeAll(), retainAll() y clear()). La colección vista no
     * soporta las operaciones add() y addAll() (si se las invoca, se lanzará
     * una UnsuportedOperationException).
     * 
     * @return una colección (un Collection) a modo de vista de todas los
     *         valores mapeados en la tabla.
     */
    @Override
    public Collection<V> values() {
        if (values == null) {
            // values = Collections.synchronizedCollection(new ValueCollection());
            values = new ValueCollection();
        }
        return values;
    }

    /**
     * Retorna un Set (conjunto) a modo de vista de todos los pares (key, value)
     * contenidos en la tabla. El conjunto está respaldado por la tabla, por lo
     * que los cambios realizados en la tabla serán reflejados en el conjunto, y
     * viceversa. Si la tabla es modificada mientras un iterador está actuando
     * sobre el conjunto vista, el resultado de la iteración será indefinido
     * (salvo que la modificación sea realizada por la operación remove() propia
     * del iterador, o por la operación setValue() realizada sobre una entrada
     * de la tabla que haya sido retornada por el iterador). El conjunto vista
     * provee métodos para eliminar elementos, y esos métodos a su vez
     * eliminan el correspondiente par (key, value) de la tabla (a través de las
     * operaciones Iterator.remove(), Set.remove(), removeAll(), retainAll()
     * and clear()). El conjunto vista no soporta las operaciones add() y
     * addAll() (si se las invoca, se lanzará una UnsuportedOperationException).
     * 
     * @return un conjunto (un Set) a modo de vista de todos los objetos
     *         mapeados en la tabla.
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        if (entrySet == null) {
            // entrySet = Collections.synchronizedSet(new EntrySet());
            entrySet = new EntrySet();
        }
        return entrySet;
    }

    // ************************ Redefinición de métodos heredados desde Object.

    /**
     * Retorna una copia superficial de la tabla. Las listas de desborde o
     * buckets que conforman la tabla se clonan ellas mismas, pero no se clonan
     * los objetos que esas listas contienen: en cada bucket de la tabla se
     * almacenan las direcciones de los mismos objetos que contiene la original.
     * 
     * @return una copia superficial de la tabla.
     * @throws java.lang.CloneNotSupportedException si la clase no implementa la
     *                                              interface Cloneable.
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        TSBHashtable<K, V> t = (TSBHashtable<K, V>) super.clone();
        t.table = new TSBArrayList[table.length];
        for (int i = table.length; i-- > 0;) {
            t.table[i] = (TSBArrayList<Map.Entry<K, V>>) table[i].clone();
        }
        t.keySet = null;
        t.entrySet = null;
        t.values = null;
        t.modCount = 0;
        return t;
    }

    /**
     * Determina si esta tabla es igual al objeto espeficicado.
     * 
     * @param obj el objeto a comparar con esta tabla.
     * @return true si los objetos son iguales.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Map)) {
            return false;
        }

        Map<K, V> t = (Map<K, V>) obj;
        if (t.size() != this.size()) {
            return false;
        }

        try {
            Iterator<Map.Entry<K, V>> i = this.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<K, V> e = i.next();
                K key = e.getKey();
                V value = e.getValue();
                if (t.get(key) == null) {
                    return false;
                } else {
                    if (!value.equals(t.get(key))) {
                        return false;
                    }
                }
            }
        }

        catch (ClassCastException | NullPointerException e) {
            return false;
        }

        return true;
    }

    /**
     * Retorna un hash code para la tabla completa.
     * 
     * @return un hash code para la tabla.
     */
    @Override
    public int hashCode() {
        // HACER...
        int hc = super.hashCode();
        return hc;
    }

    /**
     * Devuelve el contenido de la tabla en forma de String. Sólo por razones de
     * didáctica, se hace referencia explícita en esa cadena al contenido de
     * cada una de las listas de desborde o buckets de la tabla.
     * 
     * @return una cadena con el contenido completo de la tabla.
     */
    @Override
    public String toString() {
        StringBuilder cad = new StringBuilder("");
        for (int i = 0; i < this.table.length; i++) {
            cad.append("\nLista ").append(i).append(":\n\t").append(this.table[i].toString());
        }
        return cad.toString();
    }

    // ************************ Métodos específicos de la clase.

    /**
     * Determina si alguna clave de la tabla está asociada al objeto value que
     * entra como parámetro. Equivale a containsValue().
     * 
     * @param value el objeto a buscar en la tabla.
     * @return true si alguna clave está asociada efectivamente a ese value.
     */
    public boolean contains(Object value) {
        if (value == null)
            return false;

        for (TSBArrayList<Map.Entry<K, V>> bucket : this.table) {
            Iterator<Map.Entry<K, V>> it = bucket.iterator();
            while (it.hasNext()) {
                Map.Entry<K, V> entry = it.next();
                if (value.equals(entry.getValue()))
                    return true;
            }
        }
        return false;
    }

    /**
     * Incrementa el tamaño de la tabla y reorganiza su contenido. Se invoca
     * automaticamente cuando se detecta que la cantidad promedio de nodos por
     * lista supera a cierto el valor critico dado por (10 * load_factor). Si el
     * valor de load_factor es 0.8, esto implica que el límite antes de invocar
     * rehash es de 8 nodos por lista en promedio, aunque seria aceptable hasta
     * unos 10 nodos por lista.
     */
    protected void rehash() {
        int old_length = this.table.length;

        // nuevo tamaño: doble del anterior, más uno para llevarlo a impar...
        int new_length = old_length * 2 + 1;

        // no permitir que la tabla tenga un tamaño mayor al límite máximo...
        // ... para evitar overflow y/o desborde de índices...
        if (new_length > TSBHashtable.MAX_SIZE) {
            new_length = TSBHashtable.MAX_SIZE;
        }

        // crear el nuevo arreglo con new_length listas vacías...
        TSBArrayList<Map.Entry<K, V>> temp[] = new TSBArrayList[new_length];
        for (int j = 0; j < temp.length; j++) {
            temp[j] = new TSBArrayList<>();
        }

        // notificación fail-fast iterator... la tabla cambió su estructura...
        this.modCount++;

        // recorrer el viejo arreglo y redistribuir los objetos que tenia...
        for (int i = 0; i < this.table.length; i++) {
            // entrar en la lista numero i, y recorrerla con su iterador...
            Iterator<Map.Entry<K, V>> it = this.table[i].iterator();
            while (it.hasNext()) {
                // obtener un objeto de la vieja lista...
                Map.Entry<K, V> x = it.next();

                // obtener su nuevo valor de dispersión para el nuevo arreglo...
                K key = x.getKey();
                int y = this.h(key, temp.length);

                // insertarlo en el nuevo arreglo, en la lista numero "y"...
                temp[y].add(x);
            }
        }

        // cambiar la referencia table para que apunte a temp...
        this.table = temp;
    }

    // ************************ Métodos privados.

    /*
     * Función hash. Toma una clave entera k y calcula y retorna un índice
     * válido para esa clave para entrar en la tabla.
     */
    private int h(int k) {
        return h(k, this.table.length);
    }

    /*
     * Función hash. Toma un objeto key que representa una clave y calcula y
     * retorna un índice válido para esa clave para entrar en la tabla.
     */
    private int h(K key) {
        return h(key.hashCode(), this.table.length);
    }

    /*
     * Función hash. Toma un objeto key que representa una clave y un tamaño de
     * tabla t, y calcula y retorna un índice válido para esa clave dedo ese
     * tamaño.
     */
    private int h(K key, int t) {
        return h(key.hashCode(), t);
    }

    /*
     * Función hash. Toma una clave entera k y un tamaño de tabla t, y calcula y
     * retorna un índice válido para esa clave dado ese tamaño.
     */
    private int h(int k, int t) {
        if (k < 0)
            k *= -1;
        return k % t;
    }

    /**
     * Calcula la longitud promedio de las listas de la tabla.
     * 
     * @return la longitud promedio de la listas contenidas en la tabla.
     */
    private int averageLength() {
        return this.count / this.table.length;
    }

    /*
     * Busca en la lista bucket un objeto Entry cuya clave coincida con key.
     * Si lo encuentra, retorna ese objeto Entry. Si no lo encuentra, retorna
     * null.
     */
    private Map.Entry<K, V> search_for_entry(K key, TSBArrayList<Map.Entry<K, V>> bucket) {
        Iterator<Map.Entry<K, V>> it = bucket.iterator();
        while (it.hasNext()) {
            Map.Entry<K, V> entry = it.next();
            if (key.equals(entry.getKey()))
                return entry;
        }
        return null;
    }

    /*
     * Busca en la lista bucket un objeto Entry cuya clave coincida con key.
     * Si lo encuentra, retorna su posicíón. Si no lo encuentra, retorna -1.
     */
    private int search_for_index(K key, TSBArrayList<Map.Entry<K, V>> bucket) {
        Iterator<Map.Entry<K, V>> it = bucket.iterator();
        for (int i = 0; it.hasNext(); i++) {
            Map.Entry<K, V> entry = it.next();
            if (key.equals(entry.getKey()))
                return i;
        }
        return -1;
    }
}