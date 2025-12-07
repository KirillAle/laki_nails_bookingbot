
import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.toList
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar


object Database {
    private val DATABASE_URL = System.getenv("DATABASE_URL")
//    private val DATABASE_DRIVER =
    private val DATABASE_NAME = "postgres"
    private val DATABASE_PASSWORD = "postgres"

    val db = Database.connect(DATABASE_URL,"org.postgresql.Driver",DATABASE_NAME, DATABASE_PASSWORD)

//    fun getConnection(): java.sql.Connection? {
//        return DriverManager.getConnection(DATABASE_URL,DATABASE_NAME, DATABASE_PASSWORD)
//    }

    fun select(){
        val model: List<Testmodel> = db.sequenceOf(TestTable).toList()
        println(model.joinToString { it.testId.toString() })
    }

    interface Testmodel: Entity<Testmodel> {
        val testId: Int
        val testName: String
    }


    object TestTable: Table<Testmodel>("Test"){
        val id = int("testId").bindTo { it.testId }
        val name = varchar("testName").bindTo { it.testName }
    }
}
