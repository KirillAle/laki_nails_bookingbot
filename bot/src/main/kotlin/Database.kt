
import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.toList
import org.ktorm.schema.Table
import org.ktorm.schema.VarcharSqlType
import org.ktorm.schema.int
import org.ktorm.schema.uuid
import org.ktorm.schema.varchar
import java.util.UUID
import kotlin.uuid.Uuid


object Database {
    private val DATABASE_URL = System.getenv("DATABASE_URL")
    private val DATABASE_DRIVER = System.getenv("DATABASE_DRIVER")
    private val DATABASE_NAME = System.getenv("DATABASE_NAME")
    private val DATABASE_PASSWORD = System.getenv("DATABASE_PASSWORD")



    val db = Database.connect(DATABASE_URL, DATABASE_DRIVER, DATABASE_NAME, DATABASE_PASSWORD)


    fun select() {
        val testModel: List<TestModel> = db.sequenceOf(TestTable).toList()
        println(testModel.joinToString { it.testId.toString() })

        val accountModel: List<AccountFormModel> = db.sequenceOf(AccountFormTable).toList()
        println(accountModel.joinToString { it.id.toString() })


        println(DATABASE_DRIVER)
        println(DATABASE_URL)
        println(DATABASE_NAME)
        println(DATABASE_PASSWORD)
    }


    interface TestModel : Entity<TestModel> {
        val testId: Int
        val testName: String
    }

    interface AccountFormModel : Entity<AccountFormModel> {
        val id: UUID
        val telegramId: String
        val firstName: String
        val lastName: String
        val userName: String
        val role: String
    }

//    interface BookingModel : Entity<BookingModel> {
//    val id: UUID
//}


    object TestTable: Table<TestModel>("Test"){
        val id = int("testId").bindTo { it.testId }
        val name = varchar("testName").bindTo { it.testName }
    }

    object AccountFormTable: Table<AccountFormModel>("account_form"){
        val id = uuid("id").primaryKey().bindTo { it.id }
        val telegram_id = varchar("telegram_id").bindTo { it.telegramId }
        val first_name = varchar("first_name").bindTo { it.firstName }
        val last_name = varchar("last_name").bindTo{it.lastName}
        val user_name = varchar("user_name").bindTo {it.userName}
        val role = varchar("role").bindTo {it.role}
    }


}
