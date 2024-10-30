package org.pipeman.pancake.jobs;

import org.json.JSONObject;
import org.pipeman.pancake.Database;
import org.pipeman.pancake.MinecraftServer;
import org.pipeman.pancake.ServerManager;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class JobManager {
    private static final Executor executor = Executors.newCachedThreadPool();



    public static void runJob(long jobId) {
        Job job = Database.getJdbi().withHandle(h -> h.createQuery("""
                        SELECT config, server_id
                        FROM jobs
                        WHERE id = :job_id;
                        """)
                .bind("job_id", jobId)
                .mapTo(Job.class)
                .first());
        MinecraftServer server = ServerManager.getServerById(job.serverId);
        JSONObject jsonConfig = new JSONObject(job.config);

        final long startTime = System.currentTimeMillis();
        JobRuntime runtime = new JobRuntime(jsonConfig, server, executor,
                (log, success) -> storeJobLog(log, success, startTime, jobId));
        runtime.start();
    }

    private static void storeJobLog(String log, boolean success, long startTime, long jobId) {
        Database.getJdbi().useHandle(h -> h.createUpdate("""
                        INSERT INTO job_logs (job_id, started_at, log, success)
                        VALUES (:job_id, :started_at, :log, :success)
                        """)
                .bind("job_id", jobId)
                .bind("started_at", startTime)
                .bind("log", log)
                .bind("success", success)
                .execute());
    }

    public record Job(long id, String name, long serverId, String config) {

    }
}
