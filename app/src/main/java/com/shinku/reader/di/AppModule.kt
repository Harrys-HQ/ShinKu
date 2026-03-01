package com.shinku.reader.di

import android.app.Application
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.shinku.reader.domain.track.store.DelayedTrackingStore
import com.shinku.reader.BuildConfig
import com.shinku.reader.core.security.SecurityPreferences
import com.shinku.reader.data.cache.ChapterCache
import com.shinku.reader.data.cache.CoverCache
import com.shinku.reader.data.cache.PagePreviewCache
import com.shinku.reader.data.download.DownloadCache
import com.shinku.reader.data.download.DownloadManager
import com.shinku.reader.data.download.DownloadProvider
import com.shinku.reader.data.saver.ImageSaver
import com.shinku.reader.data.track.TrackerManager
import com.shinku.reader.extension.ExtensionManager
import eu.kanade.tachiyomi.network.JavaScriptEngine
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.AndroidSourceManager
import com.shinku.reader.util.storage.CbzCrypto
import com.shinku.reader.exh.eh.EHentaiUpdateHelper
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.serialization.XML
import com.shinku.reader.core.common.storage.AndroidStorageFolderProvider
import com.shinku.reader.core.common.storage.UniFileTempFileManager
import com.shinku.reader.data.AndroidDatabaseHandler
import com.shinku.reader.data.Database
import com.shinku.reader.data.DatabaseHandler
import com.shinku.reader.data.DateColumnAdapter
import com.shinku.reader.data.History
import com.shinku.reader.data.Mangas
import com.shinku.reader.data.StringListColumnAdapter
import com.shinku.reader.data.UpdateStrategyColumnAdapter
import com.shinku.reader.domain.manga.interactor.GetCustomMangaInfo
import com.shinku.reader.domain.source.service.SourceManager
import com.shinku.reader.domain.storage.service.StorageManager
import eu.kanade.tachiyomi.source.local.image.LocalCoverManager
import eu.kanade.tachiyomi.source.local.io.LocalSourceFileSystem
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

// SY -->
private const val LEGACY_DATABASE_NAME = "com.shinku.reader.db"
// SY <--

class AppModule(val app: Application) : InjektModule {
    // SY -->
    private val securityPreferences: SecurityPreferences by injectLazy()
    // SY <--

    override fun InjektRegistrar.registerInjectables() {
        addSingleton(app)

        addSingletonFactory<SqlDriver> {
            // SY -->
            if (securityPreferences.encryptDatabase().get()) {
                System.loadLibrary("sqlcipher")
            }

            // SY <--
            AndroidSqliteDriver(
                schema = Database.Schema,
                context = app,
                // SY -->
                name = if (securityPreferences.encryptDatabase().get()) {
                    CbzCrypto.DATABASE_NAME
                } else {
                    LEGACY_DATABASE_NAME
                },
                factory = if (securityPreferences.encryptDatabase().get()) {
                    SupportOpenHelperFactory(CbzCrypto.getDecryptedPasswordSql(), null, false, 25)
                } else if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Support database inspector in Android Studio
                    FrameworkSQLiteOpenHelperFactory()
                } else {
                    RequerySQLiteOpenHelperFactory()
                },
                // SY <--
                callback = object : AndroidSqliteDriver.Callback(Database.Schema) {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        setPragma(db, "foreign_keys = ON")
                        setPragma(db, "journal_mode = WAL")
                        setPragma(db, "synchronous = NORMAL")
                    }
                    private fun setPragma(db: SupportSQLiteDatabase, pragma: String) {
                        val cursor = db.query("PRAGMA $pragma")
                        cursor.moveToFirst()
                        cursor.close()
                    }
                },
            )
        }
        addSingletonFactory {
            Database(
                driver = get(),
                historyAdapter = History.Adapter(
                    last_readAdapter = DateColumnAdapter,
                ),
                mangasAdapter = Mangas.Adapter(
                    genreAdapter = StringListColumnAdapter,
                    update_strategyAdapter = UpdateStrategyColumnAdapter,
                ),
            )
        }
        addSingletonFactory<DatabaseHandler> { AndroidDatabaseHandler(get(), get()) }

        addSingletonFactory {
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }
        }
        addSingletonFactory {
            XML {
                defaultPolicy {
                    ignoreUnknownChildren()
                }
                autoPolymorphic = true
                xmlDeclMode = XmlDeclMode.Charset
                indent = 2
                xmlVersion = XmlVersion.XML10
            }
        }
        addSingletonFactory<ProtoBuf> {
            ProtoBuf
        }

        addSingletonFactory { UniFileTempFileManager(app) }

        addSingletonFactory { ChapterCache(app, get(), get()) }
        addSingletonFactory { CoverCache(app) }

        addSingletonFactory { NetworkHelper(app, get(), BuildConfig.DEBUG) }
        addSingletonFactory { JavaScriptEngine(app) }

        addSingletonFactory<SourceManager> { AndroidSourceManager(app, get(), get()) }
        addSingletonFactory { ExtensionManager(app) }

        addSingletonFactory { DownloadProvider(app) }
        addSingletonFactory { DownloadManager(app) }
        addSingletonFactory { DownloadCache(app) }

        addSingletonFactory { TrackerManager() }
        addSingletonFactory { DelayedTrackingStore(app) }

        addSingletonFactory { ImageSaver(app) }

        addSingletonFactory { AndroidStorageFolderProvider(app) }
        addSingletonFactory { LocalSourceFileSystem(get()) }
        addSingletonFactory { LocalCoverManager(app, get()) }
        addSingletonFactory { StorageManager(app, get()) }

        // SY -->
        addSingletonFactory { EHentaiUpdateHelper(app) }

        addSingletonFactory { PagePreviewCache(app) }
        // SY <--
    }
}

fun initExpensiveComponents(app: Application) {
    // Asynchronously init expensive components for a faster cold start
    CoroutineScope(Dispatchers.IO).launch {
        Injekt.get<NetworkHelper>()

        Injekt.get<SourceManager>()

        Injekt.get<Database>()

        Injekt.get<DownloadManager>()

        // SY -->
        Injekt.get<GetCustomMangaInfo>()
        // SY <--
    }
}
