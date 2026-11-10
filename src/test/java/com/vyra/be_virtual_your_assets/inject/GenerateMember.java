package com.vyra.be_virtual_your_assets.inject;

import com.github.javafaker.Faker;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;

/**
 * Development tool (run its main method from the IDE): writes inject/source/members_seed.csv for the
 * inject-member profile. Lives in the test sources so javafaker stays out of the application jar.
 */
@Slf4j
public class GenerateMember {
    private static final Faker faker = new Faker(new Locale("en"));
    private static final Random random = new Random();

    private static final String RAW_PIN = "123456";

    public static void main(String[] args) {
        String outputFile = "src/main/java/com/vyra/be_virtual_your_assets/inject/source/members_seed.csv";
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        LocalDateTime currentLoopTime = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
        LocalDateTime endTime = LocalDateTime.now();

        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)))) {
            log.info("START GENERATE CSV FROM {} TO {}", currentLoopTime.format(fmt), endTime.format(fmt));

            pw.println("phone_number,email,first_name,last_name,pin,created_at");

            int counter = 0;

            while (currentLoopTime.isBefore(endTime)) {
                String phone = randomPhoneNumber();
                String firstName = cleanName(faker.name().firstName());
                String lastName = cleanName(faker.name().lastName());
                String email = (firstName + lastName + random.nextInt(100000)).toLowerCase() + "@gmail.com";

                pw.printf(
                        "%s,%s,%s,%s,%s,%s%n",
                        phone,
                        email,
                        firstName,
                        lastName,
                        RAW_PIN,
                        currentLoopTime.format(fmt)
                );

                currentLoopTime = currentLoopTime.plusHours(1);
                counter++;
                if (counter % 1000 == 0) {
                    log.info("Generated {} rows. Current timestamp in loop: {}", counter, currentLoopTime.format(fmt));
                }
            }

            log.info("DONE: {}. Total data generated: {}", outputFile, counter);

        } catch (IOException e) {
            log.error("FAILED GENERATE CSV", e);
        }
    }

    private static String cleanName(String name) {
        return name.replaceAll("[^a-zA-Z]", "");
    }

    private static String randomPhoneNumber() {
        StringBuilder sb = new StringBuilder("628");
        while (sb.length() < 12) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}