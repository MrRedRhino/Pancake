package org.pipeman.pancake.rest;

import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import org.jetbrains.annotations.NotNull;
import org.pipeman.pancake.Database;
import org.pipeman.pancake.Main;

import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Map;

public class JobsApi {
    public static void getJobs(Context ctx) {
        long serverId = ctx.pathParamAsClass("server-id", Long.class).get();

        List<Job> jobs = Database.getJdbi().withHandle(h -> h.createQuery("""
                        SELECT id, name, config
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
        PatchJobPayload body = ctx.bodyAsClass(PatchJobPayload.class);
        long jobId = Main.generateNewId();
        JobConfig jobConfig = new JobConfig(body.interval, body.tasks);

        Database.getJdbi().withHandle(h -> h.createUpdate("""
                        INSERT INTO jobs (id, name, server_id, config)
                        VALUES (:id, :name, :server_id, :config)
                        """)
                .bind("id", jobId)
                .bind("name", body.name)
                .bind("server_id", serverId)
                .bind("config", Main.serialize(jobConfig))
                .execute());

        ctx.json(Map.of("id", jobId));
    }

    public static void patchJob(@NotNull Context ctx) {
        long jobId = ctx.pathParamAsClass("job-id", Long.class).get();
        PatchJobPayload body = ctx.bodyAsClass(PatchJobPayload.class);

        Database.getJdbi().withHandle(h -> h.createUpdate("""
                        UPDATE jobs
                        SET name   = :name,
                            config = :config
                        WHERE id = :id
                        """)
                .bind("id", jobId)
                .bind("name", body.name)
                .bind("config", Main.serialize(new JobConfig(body.interval, body.tasks)))
                .execute());
    }

    public record Job(long id, String name, PatchJobPayload.Interval interval, List<PatchJobPayload.Task> tasks) {
        @ConstructorProperties({"id", "name", "config"})
        public Job(long id, String name, String config) {
            this(id, name, Main.deserialize(config, JobConfig.class));
        }

        private Job(long id, String name, JobConfig config) {
            this(id, name, config.interval, config.tasks);
        }
    }

    private record PatchJobPayload(Interval interval, String name, List<Task> tasks) {
        private record Interval(int interval, int day, int timeOfDay) {
        }

        private record Task(String type, JsonNode config) {

        }
    }

    private record JobConfig(PatchJobPayload.Interval interval, List<PatchJobPayload.Task> tasks) {

    }
}
