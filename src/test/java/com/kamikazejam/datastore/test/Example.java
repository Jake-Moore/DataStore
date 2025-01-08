package com.kamikazejam.datastore.test;

import com.kamikazejam.datastore.framework.DocumentRepository;
import com.kamikazejam.datastore.test.entity.User;
import com.kamikazejam.datastore.test.entity.obj.DataClass;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Random;

@Slf4j
public class Example {
    public static final Random RANDOM = new Random();

    public static void main(String[] args) {
        String connectionString = System.getenv("TEST_MONGODB");
        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .serverApi(serverApi)
                .build();
        MongoClient mongoClient = MongoClients.create(settings);
        DocumentRepository<User> userRepo = new DocumentRepository<>(mongoClient, "test", "users", User.class);

        Logger logger = LoggerFactory.getLogger("MyApp");
        logger.error("===================================================");
        logger.error("Starting Tests...");
        logger.error("===================================================");

        // Create a new user through the repository
        logger.warn(" "); logger.warn(" "); logger.warn("CREATING NEW USER...");
        User newUser = userRepo.create(user -> {
            user.name.set("Initial Name");
            user.age.set(25);
            user.email.set("initial.email@example.com");
            user.map.get().put("one", 1);
            user.map.get().put("two", 2);
            user.map.get().put("three", 3);
            user.list.get().add("1");
            user.list.get().add("2");
        });
        logger.warn("\tNEW USER ID: {} VERSION: {}}", newUser.id.get(), newUser.version.get());

        // The returned user is read-only
        logger.warn(" "); logger.warn(" "); logger.warn("TESTING READ-ONLY...");
        try {
            newUser.name.set("Can't do this!");
        } catch (IllegalStateException e) {
            logger.warn("\tPASS: Caught expected error: {}", e.getMessage());
        }

        // Get a read-only version of the user
        logger.warn(" "); logger.warn(" ");
        logger.warn("GETTING READ-ONLY USER by ID: {}", newUser.id.get());
        User readOnlyUser = userRepo.read(newUser.id.get()).orElseThrow();
        logger.warn("\tREAD-ONLY USER DATA: {}, {}, {}, {}", readOnlyUser.name.get(), readOnlyUser.age.get(), readOnlyUser.email.get(), readOnlyUser.version.get());

        // Modify the user in a controlled context
        logger.warn(" "); logger.warn(" "); logger.warn("MODIFYING USER...");
        User updatedUser = userRepo.update(newUser.id.get(), user -> {
            // Inside this lambda, the user object is modifiable
            user.name.set("New Name");
            user.age.set(30);
            user.email.set("new.email@example.com");
            user.data.set(new DataClass("Kami", 20, "kamikazejam.yt@gmail.com"));
            user.list.get().add(Objects.toString(RANDOM.nextDouble(0, 2)));
        });
        logger.warn("\tUPDATED USER DATA: {}, {}, {}, {}", updatedUser.name.get(), updatedUser.age.get(), updatedUser.email.get(), updatedUser.version.get());

        // updatedUser is read-only

        // Later, we can fetch the user again (always returns read-only)
        logger.warn(" "); logger.warn(" ");
        logger.warn("GETTING READ-ONLY USER by ID: {}", updatedUser.id.get());
        User cachedUser = userRepo.read(updatedUser.id.get()).orElseThrow();
        logger.warn("\tCACHED? USER DATA: {}, {}, {}, {}", cachedUser.name.get(), cachedUser.age.get(), cachedUser.email.get(), cachedUser.version.get());

        // Can force cache invalidation if needed
        logger.warn(" "); logger.warn(" "); logger.warn("INVALIDATING CACHE...");
        userRepo.getCache().invalidate(newUser.id.get());
        logger.warn("\tCACHE INVALIDATED");

        // Example of a more complex update that depends on current state
        logger.warn(" "); logger.warn(" "); logger.warn("MODIFYING USER (COMPLEX UPDATE)...");
        updatedUser = userRepo.update(newUser.id.get(), user -> {
            // Even complex updates are safe because they're in a modifiable context
            user.name.set(user.name.get() + " (Modified)");
            user.age.set(user.age.get() + 1);
        });
        logger.warn("\tUPDATED USER DATA: {}, {}, {}, {}", updatedUser.name.get(), updatedUser.age.get(), updatedUser.email.get(), updatedUser.version.get());

        // Example of incorrect local version (forced failure)
        logger.warn(" "); logger.warn(" "); logger.warn("MODIFYING USER (FORCED FAILURE)... (first id: {})", updatedUser.uuid);
        DocumentRepository.BREAK = true;
        User updatedUser2 = userRepo.update(updatedUser.id.get(), user -> {
            user.name.set("New User Name Despite Version Mismatch");
            System.out.println("Now Time: " + user.data.get().getMap().get("now").toString());
            System.out.println("Map Entries:");
            for (Map.Entry<String, Integer> entry : user.map.get().entrySet()) {
                System.out.println("\t" + entry.getKey() + " : " + entry.getValue());
            }
            System.out.println("List Entries:");
            for (String s : user.list.get()) {
                System.out.println("\t" + s);
            }
        });
        DocumentRepository.BREAK = false;
        // TODO the original Java reference does not get updated with the changes after the modify call
        //  The return value (user2) has changes, but not the original user object
        logger.warn("\tUPDATED USER1 ({}) DATA: {}, {}, {}, {}", updatedUser.uuid, updatedUser.name.get(), updatedUser.age.get(), updatedUser.email.get(), updatedUser.version.get());
        logger.warn("\tUPDATED USER2 ({}) DATA: {}, {}, {}, {}", updatedUser2.uuid, updatedUser2.name.get(), updatedUser2.age.get(), updatedUser2.email.get(), updatedUser2.version.get());
    }
}