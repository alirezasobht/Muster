package app.muster.data.repository

import app.muster.APP_VERSION
import app.muster.data.dto.FeatureFlagDto
import app.muster.data.dto.GetFeatureFlagsDto
import app.muster.data.local.FeatureFlagLocalSource
import app.muster.data.mapper.mapErrors
import app.muster.data.mapper.toFlagMap
import app.muster.data.platform.platformName
import app.muster.domain.model.FeatureFlag
import app.muster.domain.repository.FeatureFlagRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow

internal class FeatureFlagRepositoryImpl(
    private val client: SupabaseClient,
    private val local: FeatureFlagLocalSource
) : FeatureFlagRepository {

    override fun observe(flag: FeatureFlag): Flow<Boolean> = local.observe(flag.key)

    override suspend fun sync() {
        val flags = mapErrors {
            client.postgrest.rpc(GET_FEATURE_FLAGS_FUNCTION, GetFeatureFlagsDto(platformName, APP_VERSION))
                .decodeList<FeatureFlagDto>()
                .toFlagMap()
        }
        local.replaceAll(flags)
    }

    private companion object {
        const val GET_FEATURE_FLAGS_FUNCTION = "get_feature_flags"
    }
}
