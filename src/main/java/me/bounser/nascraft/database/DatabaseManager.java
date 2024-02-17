package me.bounser.nascraft.database;

import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.database.mysql.MySQL;
import me.bounser.nascraft.database.sqlite.SQLite;

public class DatabaseManager {


    private final DatabaseType databaseType;

    private Database database;

    private static DatabaseManager instance;

    public static DatabaseManager getInstance() { return instance == null ? instance = new DatabaseManager() : instance; }

    public DatabaseManager() {
        databaseType = Config.getInstance().getDatabaseType();

        switch (databaseType) {

            case SQLITE:
                database = new SQLite();
                return;

            case MYSQL:
                database = new MySQL(
                        Config.getInstance().getHost(),
                        Config.getInstance().getPort(),
                        Config.getInstance().getDatabase(),
                        Config.getInstance().getUser(),
                        Config.getInstance().getPassword()
                );
                return;
        }
    }

    public Database getDatabase() { return database; }

}
