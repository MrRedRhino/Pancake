package org.pipeman.pancake.rest;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import org.jetbrains.annotations.NotNull;
import org.pipeman.pancake.Database;
import org.pipeman.pancake.Main;

import java.util.List;
import java.util.Map;

public class JobsApi {
    public static void getJobs(Context ctx) {
        long serverId = ctx.pathParamAsClass("server-id", Long.class).get();

        List<Job> jobs = Database.getJdbi().withHandle(h -> h.createQuery("""
                        SELECT *
                        FROM jobs
                        WHERE server_id = :server_id
                        """)
                .bind("server_id", serverId)
                .mapTo(Job.class)
                .list());

        ctx.json(jobs);
    }

    public static void deleteJob(Context ctx) {
        long jobId = ctx.pathParamAsClass("job-id", Long.class).get();

        int changes = Database.getJdbi().withHandle(h -> h.createUpdate("""
                        DELETE
                        FROM jobs
                        WHERE id = :id
                        """)
                .bind("id", jobId)
                .execute());

        if (changes == 0) {
            throw new BadRequestResponse("No such job");
        }
    }

    public static void createJob(@NotNull Context ctx) {
        long serverId = ctx.pathParamAsClass("server-id", Long.class).get();
        long jobId = Main.generateNewId();
        String name = "New Job";
        String config = "{}";

        Database.getJdbi().withHandle(h -> h.createUpdate("""
                        INSERT INTO jobs (id, name, server_id, config)
                        VALUES (:id, :name, :server_id, :config)
                        """)
                .bind("id", jobId)
                .bind("name", name)
                .bind("server_id", serverId)
                .bind("config", config)
                .execute());

        ctx.json(new Job(jobId, name, serverId, config));
    }

    public static void patchJob(@NotNull Context ctx) {

    }

    private record Job(long id, String name, long serverId, String config) {
    }
}
