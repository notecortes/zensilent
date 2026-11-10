package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.AudioLog
import com.example.model.AudioMode
import com.example.model.GeofenceZone
import com.example.model.ScheduleRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ScheduleRule::class, GeofenceZone::class, AudioLog::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AudioZenDatabase : RoomDatabase() {
    abstract fun scheduleRuleDao(): ScheduleRuleDao
    abstract fun geofenceZoneDao(): GeofenceZoneDao
    abstract fun audioLogDao(): AudioLogDao

    companion object {
        @Volatile
        private var INSTANCE: AudioZenDatabase? = null

        fun getInstance(context: Context): AudioZenDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AudioZenDatabase::class.java,
                    "audiozen_database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Prepopulate default helpful rules
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = getInstance(context).scheduleRuleDao()
                                dao.insertRule(
                                    ScheduleRule(
                                        title = "Descanso Nocturno",
                                        daysOfWeek = "1,2,3,4,5,6,7", // All days
                                        startHour = 23,
                                        startMinute = 0,
                                        endHour = 7,
                                        endMinute = 0,
                                        targetMode = AudioMode.SILENT,
                                        restoreMode = AudioMode.NORMAL,
                                        isEnabled = true
                                    )
                                )
                                dao.insertRule(
                                    ScheduleRule(
                                        title = "Trabajo / Oficina",
                                        daysOfWeek = "2,3,4,5,6", // Mon-Fri
                                        startHour = 9,
                                        startMinute = 0,
                                        endHour = 18,
                                        endMinute = 0,
                                        targetMode = AudioMode.VIBRATE,
                                        restoreMode = AudioMode.NORMAL,
                                        isEnabled = false
                                    )
                                )

                                val zoneDao = getInstance(context).geofenceZoneDao()
                                zoneDao.insertZone(
                                    GeofenceZone(
                                        name = "Oficina / Trabajo",
                                        latitude = 40.4168,
                                        longitude = -3.7038,
                                        radiusMeters = 200f,
                                        enterMode = AudioMode.VIBRATE,
                                        exitMode = AudioMode.NORMAL,
                                        isEnabled = false
                                    )
                                )
                                zoneDao.insertZone(
                                    GeofenceZone(
                                        name = "Biblioteca / Estudio",
                                        latitude = 40.4200,
                                        longitude = -3.7100,
                                        radiusMeters = 150f,
                                        enterMode = AudioMode.SILENT,
                                        exitMode = AudioMode.NORMAL,
                                        isEnabled = false
                                    )
                                )
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
