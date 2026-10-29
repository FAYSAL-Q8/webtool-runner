/*
 * Author: Faysal Alibrahim
 * GitHub: https://github.com/FAYSAL-Q8
 */
package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Tool
import com.example.engine.SampleTools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import androidx.room.migration.Migration

@Database(entities = [Tool::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun toolDao(): ToolDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tools ADD COLUMN projectType TEXT NOT NULL DEFAULT 'SINGLE_FILE'")
                db.execSQL("ALTER TABLE tools ADD COLUMN projectDir TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE tools ADD COLUMN entryPoint TEXT NOT NULL DEFAULT 'index.html'")
                db.execSQL("ALTER TABLE tools ADD COLUMN description TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "webtool_runner.db"
                )
                .addMigrations(MIGRATION_1_2)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate with default built-in tools on first creation
                        scope.launch {
                            val defaults = SampleTools.getDefaultTools(context)
                            if (defaults.isNotEmpty()) {
                                INSTANCE?.toolDao()?.insertTools(defaults)
                            }
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
