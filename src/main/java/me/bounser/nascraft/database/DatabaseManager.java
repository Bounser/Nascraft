package me.bounser.nascraft.database;

import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.database.mysql.MySQL;
import me.bounser.nascraft.database.sqlite.SQLite;

public class DatabaseManager {


    private final DatabaseType databaseType;

    private Database database;

    private static DatabaseManager instance;

    public static DatabaseManager get() { return instance == null ? instance = new DatabaseManager() : instance; }

    public DatabaseManager() {

        databaseType = Config.getInstance().getDatabaseType();

        if (databaseType == null)
            throw new IllegalArgumentException("Database type not recognized!");

        switch (databaseType) {

            case SQLITE:
                database = SQLite.getInstance(); break;

            case MYSQL:
                database = new MySQL(
                        Config.getInstance().getHost(),
                        Config.getInstance().getPort(),
                        Config.getInstance().getDatabase(),
                        Config.getInstance().getUser(),
                        Config.getInstance().getPassword()
                );
                break;

            case REDIS:
                database = new Redis(
                        Config.getInstance().getHost(),
                        Config.getInstance().getPort(),
                        Config.getInstance().getPassword()
                ); break;
        }

        database.connect();
    }

    public Database getDatabase() {
        return database;
    }

}
