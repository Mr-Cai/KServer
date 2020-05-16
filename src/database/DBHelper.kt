package database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigInteger
import java.security.MessageDigest

fun main() {
    Database.connect(
        url = "jdbc:mysql://106.53.106.142:3306/users",
        driver = "com.mysql.cj.jdbc.Driver",
        user = "root",
        password = "153580"
    )

    transaction {
        Info.insert {
            it[id] = "".md5()
            it[nick_name] = "Name"
            it[real_name] = "RName"
            it[age] = 18
            it[gender] = "男"
            it[phone] = "18922483934"
            it[password] = "32rksadfkl"
            it[job] = "客服"
            it[love] = "单身"
        }

        Info.deleteWhere {
            Info.nick_name eq "Name9"
        }

        Info.update({ Info.nick_name eq "Name10" }) {
            it[nick_name] = "Hello"
        }

        Info.selectAll().map {
            println(it)
        }
    }
}

fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    val md5Str = BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
    return md5Str.substring(0, 16)
}

object Info : Table("info") {
    val id: Column<String> = varchar("id", 24)
    val nick_name: Column<String> = varchar("nick_name", 32)
    val real_name: Column<String> = varchar("real_name", 32)
    val age: Column<Int> = integer("age")
    val gender: Column<String> = varchar("gender", 8)
    val phone: Column<String> = varchar("phone", 11)
    val password: Column<String> = varchar("password", 16)
    val job: Column<String> = varchar("job", 32)
    val love: Column<String> = varchar("love", 8)

    override val primaryKey = PrimaryKey(id, name = "id")
}