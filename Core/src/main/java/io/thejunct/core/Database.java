/*
 * Copyright (c) 2016 The Junction Network. All Rights Reserved.
 * Created by PantherMan594.
 */

package io.thejunct.core;

import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by david on 4/25.
 */
public class Database {

    private Connection connection;
    private String host, database, username, password;
    private int port;

    /**
     * Sets up a connection with the MySQL database.
     * @param host The host of the MySQL database.
     * @param database The name of the database.
     * @param username The database user's username.
     * @param password The database user's password.
     * @param port The port of the MySQL database
     */
    public Database(String host, String database, String username, String password, int port) {
        this.host = host;
        this.database = database;
        this.username = username;
        this.password = password;
        this.port = port;

        Bukkit.getScheduler().runTaskAsynchronously(Core.getInstance(), () -> {
            try {
                openConnection();
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Opens the connection with the MySQL database.
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    private void openConnection() throws SQLException, ClassNotFoundException {
        if (connection != null && !connection.isClosed()) {
            return;
        }

        synchronized (this) {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + host+ ":" + port + "/" + database, username, password);
        }
    }

    public Statement getStatement() {
        try {
            return connection.createStatement();
        } catch (SQLException e) {
            Core.getInstance().getLogger().warning("Unable to create a statement.");
            e.printStackTrace();
        }
        return null;
    }
}
