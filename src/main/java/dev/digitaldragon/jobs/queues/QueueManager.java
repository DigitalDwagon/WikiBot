package dev.digitaldragon.jobs.queues;

import dev.digitaldragon.jobs.JobManager;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class QueueManager {
    private static String DATABASE_NAME = "queues.sqlite3";
    private Map<String, Queue> queues = new HashMap<>();

    public QueueManager() {
        if (!Files.exists(Path.of(DATABASE_NAME))) { return; }

        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DATABASE_NAME);
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM queues");
            while (resultSet.next()) {
                String name = resultSet.getString("name");
                Integer concurrency = resultSet.getInt("concurrency");
                Integer priority = resultSet.getInt("priority");
                queues.put(name, new Queue(name, concurrency, priority));
            }

        } catch (SQLException exception) {
            //
        }


        if (this.getQueue("default") == null) {
            this.addOrChangeQueue(new Queue("default", 15, 0));
        }
    }

    @Nullable
    public Queue getQueue(@NotNull String name) {
        return queues.get(name);
    }


    public void addOrChangeQueue(Queue queue) {
        queues.put(queue.getName(), queue);
        JobManager.launchJobs();

        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DATABASE_NAME);
            connection.createStatement().execute("CREATE TABLE IF NOT EXISTS queues (name TEXT PRIMARY KEY, concurrency INTEGER, priority INTEGER)");

            PreparedStatement statement = connection.prepareStatement("INSERT OR REPLACE INTO queues (name, concurrency, priority) VALUES (?, ?, ?)");
            statement.setString(1, queue.getName());
            statement.setInt(2, queue.getConcurrency());
            statement.setInt(3, queue.getPriority());
            statement.execute();

        } catch (SQLException e) {
            //
        }
    }
}
