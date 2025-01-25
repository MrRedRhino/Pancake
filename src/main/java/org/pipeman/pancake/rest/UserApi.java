package org.pipeman.pancake.rest;

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import org.pipeman.pancake.Database;
import org.pipeman.pancake.Main;
import org.pipeman.pancake.utils.RandomString;

import java.util.Map;
import java.util.Optional;

public class UserApi {
    private static final RandomString STRING_GENERATOR = new RandomString();

    public static void login(Context ctx) {
        LoginRequestBody body = ctx.bodyAsClass(LoginRequestBody.class);

        Optional<UserIdHashSnapshot> userSnapshot = Database.getJdbi().withHandle(h -> h.createQuery("""
                        SELECT id, password_hash
                        FROM users
                        WHERE name = :username
                        """)
                .bind("username", body.username())
                .mapTo(UserIdHashSnapshot.class)
                .findFirst());

        userSnapshot.orElseThrow(UnauthorizedResponse::new);

        boolean verified = BCrypt.verifyer().verify(body.password().toCharArray(), userSnapshot.get().passwordHash()).verified;
        if (!verified) throw new UnauthorizedResponse();

        String token = STRING_GENERATOR.nextString();

        Database.getJdbi().useHandle(h -> h.createUpdate("""
                        INSERT INTO sessions (token, user_id, user_agent, created_at, expires_at)
                        VALUES (:token, :user_id, :user_agent, current_timestamp, datetime(current_timestamp, '+3 days'))
                        """)
                .bind("token", token)
                .bind("user_id", userSnapshot.get().id())
                .bind("user_agent", ctx.userAgent())
                .execute());

        ctx.json(Map.of("token", token));
    }

    public static void getAccount(Context ctx) {
        String token = ctx.cookieMap().getOrDefault("authorization", ctx.cookie("Authorization"));
        if (token == null) throw new UnauthorizedResponse("Missing authorization cookie");

        User user = Database.getJdbi().withHandle(h -> h.createQuery("""
                        SELECT id, name, is_administrator
                        FROM users,
                             sessions
                        WHERE token = :token
                          AND id = user_id
                        """)
                .bind("token", token)
                .mapTo(User.class)
                .findFirst()).orElseThrow(UnauthorizedResponse::new);

        ctx.json(user);
    }

    public static void createAccount(String username, String password) {
        Database.getJdbi().useHandle(h -> h.createUpdate("""
                        INSERT INTO users (id, name, password_hash, is_administrator)
                        VALUES (:id, :name, :password_hash, false)
                        """)
                .bind("id", Main.generateNewId())
                .bind("name", username)
                .bind("password_hash", BCrypt.withDefaults().hash(12, password.getBytes()))
                .execute());
    }

    public static void logout(Context ctx) {
        String token = ctx.cookieMap().getOrDefault("authorization", ctx.cookie("Authorization"));
        if (token == null) throw new UnauthorizedResponse();

        Database.getJdbi().useHandle(h -> h.createUpdate("""
                        DELETE
                        FROM sessions
                        WHERE token = :token
                        """)
                .bind("token", token)
                .execute());
    }

    private record LoginRequestBody(String username, String password) {
    }

    public record UserIdHashSnapshot(long id, byte[] passwordHash) {
    }

    public record User(long id, String name, boolean isAdministrator) {
    }
}
