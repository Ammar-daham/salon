package com.example.salon.datasource;

import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    /**
     * Migrates an empty database, or one whose flyway_schema_history says what it already has (BE-38).
     * A database with tables but no history is refused: it's the wrong database, or a restore that lost
     * the history, and baselining it would mark it as already at V1 and run the rest on top. Adopting a
     * database that predates Flyway is a one-off, opted into with app.flyway.baseline-on-migrate.
     */
    @Bean(initMethod = "migrate")
    public Flyway flyway(HikariDataSource dataSource,
            @Value("${app.flyway.baseline-on-migrate:false}") boolean baselineOnMigrate) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(baselineOnMigrate)
                // A file Flyway can't parse, such as V14_name.sql, would otherwise be skipped without a word.
                .validateMigrationNaming(true)
                .load();
    }
}
