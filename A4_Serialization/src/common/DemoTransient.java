import java.io.*;
import java.lang.reflect.Proxy;
import java.sql.Connection;

/** Shows what happens if a live Connection is NOT transient. */
public class DemoTransient {
    static class BadResident implements Serializable {
        String name = "Arun"; Connection con;               // NOT transient
    }
    /** Stand-in for the live database connection handler (a real Connection is not Serializable either). */
    static class LiveConnectionHandler implements java.lang.reflect.InvocationHandler {
        public Object invoke(Object p, java.lang.reflect.Method m, Object[] x) { return null; }
    }
    public static void main(String[] a) throws Exception {
        BadResident r = new BadResident();
        r.con = (Connection) Proxy.newProxyInstance(DemoTransient.class.getClassLoader(), new Class<?>[]{Connection.class}, new LiveConnectionHandler());
        try (ObjectOutputStream o = new ObjectOutputStream(new ByteArrayOutputStream())) {
            o.writeObject(r);
            System.out.println("saved (unexpected)");
        } catch (NotSerializableException e) {
            System.out.println("Without 'transient' on the Connection field -> java.io.NotSerializableException: " + e.getMessage());
        }
    }
}
