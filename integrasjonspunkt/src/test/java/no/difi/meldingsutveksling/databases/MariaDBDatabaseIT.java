package no.difi.meldingsutveksling.databases;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MariaDBContainer;
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
public class MariaDBDatabaseIT {

    // manually connect to running container, and log in with :
    // mariadb -uroot -pchangeit integrasjonspunkt
    // then run sql like : select * from conversation;

    @Container
    @ServiceConnection
    static MariaDBContainer<?> mariaDbContainer = new MariaDBContainer<>("mariadb:10.5.5")
        .withDatabaseName("integrasjonspunkt")
        .withUsername("root")
        .withPassword("changeit")
        .withInitScript("db/init.sql");

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void verify_database_has_legacy_sequence() {
        var result = entityManager.createNativeQuery("SELECT NEXTVAL(hibernate_sequence)").getSingleResult();
        assertThat(result).isNotNull();
    }

    @Test
    void verify_database_is_correct_version() {
        var result = entityManager.createNativeQuery("SELECT VERSION()").getSingleResult();
        assertThat(result.toString()).contains("MariaDB");
    }

    @Test
    @SuppressWarnings("unchecked")
    void verify_all_database_tables() {
        var result = entityManager
            .createNativeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE' AND TABLE_SCHEMA = DATABASE()", String.class)
            .getResultList();
        assertThat(result).contains(CommonDatabase.TABLES);
    }

}
