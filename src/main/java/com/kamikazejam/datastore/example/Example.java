package com.kamikazejam.datastore.example;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.kamikazejam.datastore.example.entity.User;
import com.kamikazejam.datastore.example.framework.MongoRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class Example {
    public static boolean BREAK = false;
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
        MongoRepository<User> userRepo = new MongoRepository<>(mongoClient, "test", "users", User.class);

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
        });
        logger.warn("\tNEW USER ID: {} VERSION: {} initialized? {}", newUser.id.get(), newUser.version.get(), newUser.isInitialized());

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
        User readOnlyUser = userRepo.get(newUser.id.get());
        logger.warn("\tREAD-ONLY USER DATA: {}, {}, {}, {}", readOnlyUser.name.get(), readOnlyUser.age.get(), readOnlyUser.email.get(), readOnlyUser.version.get());
        logger.warn("\t\tinitialized? {}", readOnlyUser.isInitialized());

        // Modify the user in a controlled context
        logger.warn(" "); logger.warn(" "); logger.warn("MODIFYING USER...");
        User updatedUser = userRepo.modify(newUser.id.get(), user -> {
            // Inside this lambda, the user object is modifiable
            user.name.set("New Name");
            user.age.set(30);
            user.email.set("new.email@example.com");
        });
        logger.warn("\tUPDATED USER DATA: {}, {}, {}, {}", updatedUser.name.get(), updatedUser.age.get(), updatedUser.email.get(), updatedUser.version.get());
        logger.warn("\t\tinitialized? {}", updatedUser.isInitialized());

        // updatedUser is read-only

        // Later, we can fetch the user again (always returns read-only)
        logger.warn(" "); logger.warn(" ");
        logger.warn("GETTING READ-ONLY USER by ID: {}", updatedUser.id.get());
        User cachedUser = userRepo.get(updatedUser.id.get());
        logger.warn("\tCACHED? USER DATA: {}, {}, {}, {}", cachedUser.name.get(), cachedUser.age.get(), cachedUser.email.get(), cachedUser.version.get());
        logger.warn("\t\tinitialized? {}", cachedUser.isInitialized());

        // Can force cache invalidation if needed
        logger.warn(" "); logger.warn(" "); logger.warn("INVALIDATING CACHE...");
        userRepo.invalidateCache(newUser.id.get());
        logger.warn("\tCACHE INVALIDATED");

        // Example of a more complex update that depends on current state
        logger.warn(" "); logger.warn(" "); logger.warn("MODIFYING USER (COMPLEX UPDATE)...");
        updatedUser = userRepo.modify(newUser.id.get(), user -> {
            // Even complex updates are safe because they're in a modifiable context
            user.name.set(user.name.get() + " (Modified)");
            user.age.set(user.age.get() + 1);
        });
        logger.warn("\tUPDATED USER DATA: {}, {}, {}, {}", updatedUser.name.get(), updatedUser.age.get(), updatedUser.email.get(), updatedUser.version.get());
        logger.warn("\t\tinitialized? {}", updatedUser.isInitialized());

        // Example of incorrect local version (forced failure)
        logger.warn(" "); logger.warn(" "); logger.warn("MODIFYING USER (FORCED FAILURE)... (first id: {})", updatedUser.uuid);
        BREAK = true;
        User updatedUser2 = userRepo.modify(updatedUser.id.get(), user -> {
            user.name.set("New User Name Despite Version Mismatch");
        });
        // TODO the original Java reference does not get updated with the changes after the modify call
        //  The return value (user2) has changes, but not the original user object
        logger.warn("\tUPDATED USER1 ({}) DATA: {}, {}, {}, {}, {}", updatedUser.uuid, updatedUser.name.get(), updatedUser.age.get(), updatedUser.email.get(), updatedUser.version.get(), updatedUser.isInitialized());
        logger.warn("\tUPDATED USER2 ({}) DATA: {}, {}, {}, {}, {}", updatedUser2.uuid, updatedUser2.name.get(), updatedUser2.age.get(), updatedUser2.email.get(), updatedUser2.version.get(), updatedUser2.isInitialized());
    }
}