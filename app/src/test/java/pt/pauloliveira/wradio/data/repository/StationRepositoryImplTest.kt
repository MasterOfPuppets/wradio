package pt.pauloliveira.wradio.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import pt.pauloliveira.wradio.data.local.dao.StationDao
import pt.pauloliveira.wradio.data.local.entity.StationEntity
import pt.pauloliveira.wradio.data.remote.source.UnifiedSearchDataSource
import pt.pauloliveira.wradio.domain.model.Station

class StationRepositoryImplTest {

    private lateinit var dao: StationDao
    private lateinit var searchDataSource: UnifiedSearchDataSource
    private lateinit var repository: StationRepositoryImpl

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        searchDataSource = mockk(relaxed = true)
        repository = StationRepositoryImpl(dao, searchDataSource)
    }

    // === CREATE STATION ===

    @Test
    fun `createStation generates UUID and sets playtime to 0`() = runTest {
        val entitySlot = slot<StationEntity>()
        coEvery { dao.createStation(capture(entitySlot)) } returns Unit

        val result = repository.createStation(
            name = "Test Radio",
            streamUrl = "http://stream.test.com/live",
            logoBlob = null
        )

        val captured = entitySlot.captured
        assertNotNull(captured.uuid)
        assertEquals("Test Radio", captured.name)
        assertEquals("http://stream.test.com/live", captured.streamUrl)
        assertEquals(0L, captured.totalPlayTime)
        assertEquals(null, captured.lastPlayed)
        assertEquals(true, captured.isManuallyAdded)
        assertEquals(result.uuid, captured.uuid)
    }

    @Test
    fun `createStation trims name and url`() = runTest {
        val entitySlot = slot<StationEntity>()
        coEvery { dao.createStation(capture(entitySlot)) } returns Unit

        repository.createStation(
            name = "  Test Radio  ",
            streamUrl = "  http://stream.test.com/live  "
        )

        assertEquals("Test Radio", entitySlot.captured.name)
        assertEquals("http://stream.test.com/live", entitySlot.captured.streamUrl)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `createStation rejects empty name`() = runTest {
        repository.createStation(name = "", streamUrl = "http://stream.test.com")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `createStation rejects blank name`() = runTest {
        repository.createStation(name = "   ", streamUrl = "http://stream.test.com")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `createStation rejects empty url`() = runTest {
        repository.createStation(name = "Test", streamUrl = "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `createStation rejects blank url`() = runTest {
        repository.createStation(name = "Test", streamUrl = "   ")
    }

    // === IMPORT STATION ===

    @Test
    fun `importStation resets stats to zero`() = runTest {
        val entitySlot = slot<StationEntity>()
        coEvery { dao.createStation(capture(entitySlot)) } returns Unit

        val station = makeStation(
            uuid = "api-uuid-123",
            totalPlayTime = 999,
            lastPlayed = 123456L
        )

        repository.importStation(station)

        assertEquals(0L, entitySlot.captured.totalPlayTime)
        assertEquals(null, entitySlot.captured.lastPlayed)
        assertEquals("api-uuid-123", entitySlot.captured.uuid)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `importStation rejects empty name`() = runTest {
        val station = makeStation(name = "")
        repository.importStation(station)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `importStation rejects empty url`() = runTest {
        val station = makeStation(streamUrl = "")
        repository.importStation(station)
    }

    // === UPDATE STATION ===

    @Test
    fun `updateStation calls dao with correct parameters`() = runTest {
        repository.updateStation(
            uuid = "test-uuid",
            name = "New Name",
            streamUrl = "http://new.url/stream",
            logoBlob = byteArrayOf(1, 2, 3),
            countryCode = "PT",
            tags = listOf("rock", "alternative")
        )

        coVerify {
            dao.updateStation(
                uuid = "test-uuid",
                name = "New Name",
                streamUrl = "http://new.url/stream",
                logoBlob = byteArrayOf(1, 2, 3),
                countryCode = "PT",
                tags = "rock,alternative"
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `updateStation rejects empty name`() = runTest {
        repository.updateStation(uuid = "x", name = "", streamUrl = "http://valid.url")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `updateStation rejects empty url`() = runTest {
        repository.updateStation(uuid = "x", name = "Valid", streamUrl = "")
    }

    @Test
    fun `updateStation trims values`() = runTest {
        repository.updateStation(
            uuid = "test",
            name = "  Trimmed  ",
            streamUrl = "  http://url  "
        )

        coVerify {
            dao.updateStation(
                uuid = "test",
                name = "Trimmed",
                streamUrl = "http://url",
                logoBlob = null,
                countryCode = null,
                tags = ""
            )
        }
    }

    // === UPDATE STATS ===

    @Test
    fun `updateStats calls dao directly`() = runTest {
        repository.updateStats(uuid = "test-uuid", lastPlayed = 1000L, totalPlayTime = 60L)

        coVerify { dao.updateStats("test-uuid", 1000L, 60L) }
    }

    // === DELETE ===

    @Test
    fun `deleteStation calls dao with uuid`() = runTest {
        repository.deleteStation("test-uuid")

        coVerify { dao.deleteStation("test-uuid") }
    }

    // === Helpers ===

    private fun makeStation(
        uuid: String = "test-uuid",
        name: String = "Test Station",
        streamUrl: String = "http://stream.test.com",
        totalPlayTime: Long = 0,
        lastPlayed: Long? = null
    ) = Station(
        uuid = uuid,
        name = name,
        streamUrl = streamUrl,
        logoBlob = null,
        faviconUrl = null,
        countryCode = null,
        tags = emptyList(),
        lastPlayed = lastPlayed,
        totalPlayTime = totalPlayTime,
        isManuallyAdded = false,
        homepage = null,
        codec = null,
        bitrate = 0,
        clickCount = 0,
        votes = 0
    )
}
