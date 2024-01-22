import com.projectronin.database.helpers.MysqlVersionHelper
import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.sql.DriverManager

class MigrationIntegrationTest {

    @ParameterizedTest(name = "db migration test \"{0}\"")
    @ValueSource(strings = [MysqlVersionHelper.MYSQL_VERSION_OCI])
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

    @ParameterizedTest(name = "db migration test \"{0}\"")
    @MethodSource("com.projectronin.database.helpers.MysqlVersionHelper#versionsForTest()")
    fun `db migration test by method`(image: String) {
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
