package pt.pauloliveira.wradio.domain.repository

import pt.pauloliveira.wradio.domain.model.SourceConfig

interface SourceConfigRepository {

    /**
     * Returns the current source config (local cache > bundled fallback).
     */
    suspend fun getConfig(): SourceConfig

    /**
     * Checks remote for a newer version and updates local cache if found.
     * Returns true if an update was applied.
     */
    suspend fun refreshFromRemote(): Boolean
}

