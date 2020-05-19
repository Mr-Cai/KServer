package database

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import java.math.BigInteger
import java.security.MessageDigest

fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    val md5Str = BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
    return md5Str.substring(0, 16)
}

@Suppress("SpellCheckingInspection")
object Info : Table("info") {
    val id: Column<String> = varchar("id", 24)                  // 账号
    val avatar: Column<String> = varchar("avatar", 255)         // 头像
    val nick_name: Column<String> = varchar("nick_name", 32)    // 昵称
    val real_name: Column<String> = varchar("real_name", 32)    // 姓名
    val age: Column<Int> = integer("age")                       // 年龄
    val gender: Column<String> = varchar("gender", 8)           // 性别
    val phone: Column<String> = varchar("phone", 11)            // 手机
    val password: Column<String> = varchar("password", 16)      // 密码
    val job: Column<String> = varchar("job", 32)                // 职业
    val love: Column<String> = varchar("love", 8)               // 恋爱
    val qq: Column<String> = varchar("qq", 16)                  // QQ
    val wechat: Column<String> = varchar("wechat", 16)          // 微信

    override val primaryKey = PrimaryKey(id, name = "id")       // 主键
}