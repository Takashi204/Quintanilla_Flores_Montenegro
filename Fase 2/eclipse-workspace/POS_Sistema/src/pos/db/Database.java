package pos.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.File;

public final class Database {

    private static final String DEFAULT_PATH = System.getProperty("user.home")
            + File.separator + ".pos_demo" + File.separator + "pos_cache.db";

    private static final String DEFAULT_URL = "jdbc:sqlite:" + DEFAULT_PATH;

    
    public static String url() {
        String override = System.getProperty("pos.db.url");
        return (override != null && !override.isBlank()) ? override : DEFAULT_URL;
    }

    
    public static Connection get() throws SQLException {
        String u = url();
        if (u.startsWith("jdbc:sqlite:")) {
            String path = u.substring("jdbc:sqlite:".length());
            File dir = new File(path).getParentFile();
            if (dir != null) dir.mkdirs();
        }
        
        return DriverManager.getConnection(u);
    }

    private Database() { /* no instanciable */ }
}

