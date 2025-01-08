//package com.kamikazejam.datastore.test1;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.mongodb.ConnectionString;
//import com.mongodb.MongoClientSettings;
//import com.mongodb.ServerApi;
//import com.mongodb.ServerApiVersion;
//import com.mongodb.client.MongoClient;
//import com.mongodb.client.MongoClients;
//
//import lombok.extern.slf4j.Slf4j;
//
//@Slf4j
//public class Example2 {
//    private static final String TARGET_ID = "86765ac9-c5af-494d-a3ef-e96f403e8b31";
//
//    public static void main(String[] args) {
//        String connectionString = System.getenv("TEST_MONGODB");
//        ServerApi serverApi = ServerApi.builder()
//                .version(ServerApiVersion.V1)
//                .build();
//        MongoClientSettings settings = MongoClientSettings.builder()
//                .applyConnectionString(new ConnectionString(connectionString))
//                .serverApi(serverApi)
//                .build();
//        MongoClient mongoClient = MongoClients.create(settings);
//        DocumentRepository<User> userRepo = new DocumentRepository<>(mongoClient, "test", "users", User.class);
//
//        Logger logger = LoggerFactory.getLogger("MyApp");
//        logger.error("===================================================");
//        logger.error("Starting Example2 Tests with ID: " + TARGET_ID);
//        logger.error("===================================================");
//
//        // 1. Read and print the object details
//        logger.warn(" "); logger.warn("1. READING EXISTING USER...");
//        User user = userRepo.read(TARGET_ID).orElseThrow();
//        printUserDetails(logger, "INITIAL", user);
//
//        // 2. Modify the user
//        logger.warn(" "); logger.warn("2. MODIFYING USER...");
//        User modifiedUser = userRepo.update(TARGET_ID, user2 -> {
//            user2.name.set(user2.name.get() + " (Modified by Example2)");
//            user2.age.set(user2.age.get() + 5);
//            user2.list.get().add("Added by Example2");
//            user2.map.get().put("example2", 42);
//        });
//        printUserDetails(logger, "MODIFIED", modifiedUser);
//
//        // 3. Invalidate cache
//        logger.warn(" "); logger.warn("3. INVALIDATING CACHE...");
//        userRepo.getCache().invalidate(TARGET_ID);
//        logger.warn("\tCACHE INVALIDATED");
//
//        // 4. Read the object again after cache invalidation
//        logger.warn(" "); logger.warn("4. READING USER AFTER CACHE INVALIDATION...");
//        User refreshedUser = userRepo.read(TARGET_ID).orElseThrow();
//        printUserDetails(logger, "REFRESHED", refreshedUser);
//
//        // 5. Attempt update with version conflict
//        logger.warn(" "); logger.warn("5. ATTEMPTING UPDATE WITH VERSION CONFLICT...");
//        DocumentRepository.BREAK = true;
//        User conflictUser = userRepo.update(TARGET_ID, user2 -> {
//            user2.name.set("This update should fail due to version conflict");
//            user2.age.set(RANDOM.nextInt(100));
//            user2.list.get().add("Should be added");
//            user2.map.get().put("new_data_despite_conflict", 200);
//        });
//        logger.warn("\tUNEXPECTED: Update succeeded despite version conflict");
//        printUserDetails(logger, "CONFLICT RESOLUTION", conflictUser);
//        DocumentRepository.BREAK = false;
//    }
//
//    private static void printUserDetails(Logger logger, String prefix, User user) {
//        logger.warn("\t{} USER DATA: name={}, age={}, email={}, version={}",
//                prefix, user.name.get(), user.age.get(), user.email.get(), user.version.get());
//        logger.warn("\t{} MAP ENTRIES:", prefix);
//        for (var entry : user.map.get().entrySet()) {
//            logger.warn("\t\t{} -> {}", entry.getKey(), entry.getValue());
//        }
//        logger.warn("\t{} LIST ENTRIES:", prefix);
//        for (var item : user.list.get()) {
//            logger.warn("\t\t{}", item);
//        }
//    }
//}