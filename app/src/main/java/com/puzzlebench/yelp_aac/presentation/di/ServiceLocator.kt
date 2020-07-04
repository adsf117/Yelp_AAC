package com.puzzlebench.yelp_aac.presentation.di

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.Room
import com.puzzlebench.yelp_aac.data.local.LocalDataBaseBusiness
import com.puzzlebench.yelp_aac.data.local.LocalDataBaseBusinessDetail
import com.puzzlebench.yelp_aac.data.local.LocalDataBaseBusinessDetailImpl
import com.puzzlebench.yelp_aac.data.local.LocalDataBaseBusinessImpl
import com.puzzlebench.yelp_aac.data.local.room.YelpDataBase
import com.puzzlebench.yelp_aac.data.local.room.dao.BusinessDao
import com.puzzlebench.yelp_aac.data.mapper.BusinessDetailMapper
import com.puzzlebench.yelp_aac.data.mapper.BusinessMapper
import com.puzzlebench.yelp_aac.data.remote.RemoteFetchBusinessDetailsByIdImpl
import com.puzzlebench.yelp_aac.data.remote.RemoteFetchSwitzerlandBusinesses
import com.puzzlebench.yelp_aac.data.remote.RemoteFetchSwitzerlandBusinessesImpl
import com.puzzlebench.yelp_aac.data.remote.retrofit.RetrofitClient
import com.puzzlebench.yelp_aac.repository.*

object ServiceLocator {

    private var database: YelpDataBase? = null

    @Volatile
    var businessRepository: BusinessRepository? = null
        @VisibleForTesting set

    fun provideBusinessRepository(context: Context): BusinessRepository {
        synchronized(this) {
            return businessRepository
                ?: createBusinessRepository(
                    context
                )
        }
    }

    @Volatile
    var dbusinessDetailsRepository: BusinessDetailsRepository? = null
        @VisibleForTesting set

    fun provideBusinessDetailsRepository(context: Context): BusinessDetailsRepository {
        synchronized(this) {
            return dbusinessDetailsRepository
                ?: createBusinessDetailsRepository(
                    context
                )
        }
    }

    private fun provideRemoteFetchSwitzerlandBusinesses(): RemoteFetchSwitzerlandBusinesses {
        return RemoteFetchSwitzerlandBusinessesImpl(
            provideYelpApiV3(),
            provideBusinessMapper()
        )
    }

    private fun provideRemoteFetchBusinessDetailsByIdImpl() =
        RemoteFetchBusinessDetailsByIdImpl(
            provideYelpApiV3(),
            provideBusinessDetailMapper()
        )

    private fun provideBusinessLocalDataSource(context: Context): LocalDataBaseBusiness {
        val database = database
            ?: createDataBase(context)
        return LocalDataBaseBusinessImpl(
            database.businessDao(),
            BusinessMapper()
        )
    }

    fun provideBusinessDao(context: Context): BusinessDao {
        val database = database
            ?: createDataBase(context)
        return database.businessDao()
    }

    private fun provideLocalDataBaseBusinessDetail(context: Context): LocalDataBaseBusinessDetail {
        val database = database
            ?: createDataBase(context)
        return LocalDataBaseBusinessDetailImpl(
            database.businessDao(),
            database.categoriesDao(),
            database.photoDao(),
            provideBusinessDetailMapper(),
            provideBusinessMapper()
        )
    }

    private fun createBusinessRepository(context: Context): BusinessRepository {
        val businessRepositoryImpl =
            BusinessRepositoryImpl(
                provideRemoteFetchSwitzerlandBusinesses(),
                provideBusinessLocalDataSource(
                    context
                )
            )
        businessRepository = businessRepositoryImpl
        return businessRepositoryImpl
    }

    private fun createBusinessDetailsRepository(context: Context): BusinessDetailsRepository {
        val businessDetailsRepositoryImpl =
            BusinessDetailsRepositoryImpl(
                provideRemoteFetchBusinessDetailsByIdImpl(),
                provideLocalDataBaseBusinessDetail(context)
            )
        dbusinessDetailsRepository = businessDetailsRepositoryImpl
        return businessDetailsRepositoryImpl
    }

    private fun createDataBase(context: Context): YelpDataBase {
        val result = Room.databaseBuilder(
            context.applicationContext,
            YelpDataBase::class.java, "Yep.db"
        ).build()
        database = result
        return result
    }

    private fun provideYelpApiV3() = RetrofitClient.makeServiceYelpApiV3()
    private fun provideBusinessMapper() = BusinessMapper()
    private fun provideBusinessDetailMapper() = BusinessDetailMapper()

    fun provideFetchBusinessCallback(context: Context): FetchBusinessCallback {
        return FetchBusinessCallback(
            provideBusinessLocalDataSource(context),
            provideYelpApiV3(),
            provideBusinessMapper()
        )
    }
}
