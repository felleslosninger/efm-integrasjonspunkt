package no.difi.meldingsutveksling.databases;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
@DataJpaTest(properties = {
        "spring.liquibase.enabled=true",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.id.db_structure_naming_strategy=legacy"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class MySQLDatabaseIT {

    // manually connect to running container, and log in with :
    // mysql -uroot -pchangeit integrasjonspunkt
    // then run sql like : select * from conversation;

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0.36")
        .withDatabaseName("integrasjonspunkt")
        .withUsername("root")
        .withPassword("changeit")
        .withInitScript("db/init.sql");

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void verify_database_has_legacy_sequence() {
        // MySQL does not support sequences, it uses auto_increment or a table called called hibernate_sequence
        var result = entityManager.createNativeQuery("SELECT * FROM hibernate_sequence").getSingleResult();
        assertThat(result).isNotNull();
    }

    @Test
    void verify_database_is_correct_version() {
        var result = entityManager.createNativeQuery("SELECT VERSION()").getSingleResult();
        assertThat(result.toString()).contains("8.0.36");
    }

    @Test
    @SuppressWarnings("unchecked")
    void verify_all_database_tables() {
        var result = entityManager
            .createNativeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE'", String.class)
            .getResultList();
        assertThat(result).contains(CommonDatabase.TABLES);
    }

}
