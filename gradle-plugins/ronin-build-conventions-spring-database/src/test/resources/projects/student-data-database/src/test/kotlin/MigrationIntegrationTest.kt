
import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.sql.DriverManager

class MigrationIntegrationTest {

    @ParameterizedTest(name = "db migration test \"{0}\"")
    @ValueSource(strings = ["mysql:8"])
    fun `db migration test`(image: String) {
        DriverManager.getConnection("jdbc:tc:$image:///db").use { conn ->
            val database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(
                JdbcConnection(conn)
            )
            val liquibase =
                Liquibase("db/changelog/db.changelog-master.yaml", ClassLoaderResourceAccessor(), database)
            liquibase.update(Contexts(), LabelExpression())
        }
    }
}
