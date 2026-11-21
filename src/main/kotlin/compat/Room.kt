// Annotation stand-ins so Seal's entity and DAO declarations compile unchanged. Storage itself is
// implemented in io.github.pratyuttar.aeras.JsonVideoInfoDao.
package androidx.room

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE) annotation class Entity(val tableName: String = "")

@Retention(AnnotationRetention.SOURCE) annotation class PrimaryKey(val autoGenerate: Boolean = false)

@Retention(AnnotationRetention.SOURCE)
annotation class ColumnInfo(val name: String = "", val defaultValue: String = "")

@Retention(AnnotationRetention.SOURCE) annotation class Ignore

@Retention(AnnotationRetention.SOURCE) annotation class Dao

@Retention(AnnotationRetention.SOURCE) annotation class Insert

@Retention(AnnotationRetention.SOURCE) annotation class Update

@Retention(AnnotationRetention.SOURCE) annotation class Delete

@Retention(AnnotationRetention.SOURCE) annotation class Transaction

@Retention(AnnotationRetention.SOURCE) annotation class Query(val value: String)
