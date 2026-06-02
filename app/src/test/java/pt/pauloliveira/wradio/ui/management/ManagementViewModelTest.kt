package pt.pauloliveira.wradio.ui.management

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import pt.pauloliveira.wradio.domain.model.Station
import pt.pauloliveira.wradio.domain.repository.StationRepository
import pt.pauloliveira.wradio.service.connection.PlayerState
import pt.pauloliveira.wradio.service.connection.WRadioPlayerClient

@OptIn(ExperimentalCoroutinesApi::class)
class ManagementViewModelTest {

    private lateinit var repository: StationRepository
    private lateinit var playerClient: WRadioPlayerClient
    private lateinit var viewModel: ManagementViewModel

    private val testDispatcher = UnconfinedTestDispatcher()
    private val playerState = MutableStateFlow(PlayerState())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        playerClient = mockk(relaxed = true)
        every { repository.getAllStations() } returns flowOf(emptyList())
        every { playerClient.playerState } returns playerState
        viewModel = ManagementViewModel(repository, playerClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // === DELETE ===

    @Test
    fun `deleteStation stops player when playing that station`() = runTest {
        val station = makeStation("uuid-1")
        playerState.value = PlayerState(station = station, isPlaying = true)

        viewModel.deleteStation(station)

        coVerify { playerClient.stopAndClear() }
        coVerify { repository.deleteStation("uuid-1") }
    }

    @Test
    fun `deleteStation does not stop player when playing different station`() = runTest {
        val stationToDelete = makeStation("uuid-1")
        val playingStation = makeStation("uuid-2")
        playerState.value = PlayerState(station = playingStation, isPlaying = true)

        viewModel.deleteStation(stationToDelete)

        coVerify(exactly = 0) { playerClient.stopAndClear() }
        coVerify { repository.deleteStation("uuid-1") }
    }

    @Test
    fun `deleteStation does not stop player when nothing playing`() = runTest {
        val station = makeStation("uuid-1")
        playerState.value = PlayerState()

        viewModel.deleteStation(station)

        coVerify(exactly = 0) { playerClient.stopAndClear() }
        coVerify { repository.deleteStation("uuid-1") }
    }

    // === UPDATE ===

    @Test
    fun `updateStation stops and restarts when playing that station`() = runTest {
        val station = makeStation("uuid-1")
        playerState.value = PlayerState(station = station, isPlaying = true)
        coEvery { repository.getStation("uuid-1") } returns station.copy(name = "Updated")

        viewModel.updateStation(station, "Updated", "http://new.url")

        coVerify(ordering = io.mockk.Ordering.ORDERED) {
            playerClient.stopAndClear()
            repository.updateStation(
                uuid = "uuid-1",
                name = "Updated",
                streamUrl = "http://new.url",
                logoBlob = any(),
                countryCode = any(),
                tags = any()
            )
            repository.getStation("uuid-1")
            playerClient.play(any<Station>(), preview = any())
        }
    }

    @Test
    fun `updateStation does not touch player when playing different station`() = runTest {
        val station = makeStation("uuid-1")
        val playingStation = makeStation("uuid-2")
        playerState.value = PlayerState(station = playingStation, isPlaying = true)

        viewModel.updateStation(station, "New Name", "http://url")

        coVerify(exactly = 0) { playerClient.stopAndClear() }
        coVerify(exactly = 0) { playerClient.play(any<Station>(), preview = any()) }
        coVerify { repository.updateStation(uuid = "uuid-1", name = any(), streamUrl = any(), logoBlob = any(), countryCode = any(), tags = any()) }
    }

    // === CREATE ===

    @Test
    fun `addManualStation delegates to repository createStation`() = runTest {
        viewModel.addManualStation("My Radio", "http://stream.url")

        coVerify {
            repository.createStation(
                name = "My Radio",
                streamUrl = "http://stream.url",
                logoBlob = null
            )
        }
    }

    // === Helpers ===

    private fun makeStation(uuid: String = "test-uuid") = Station(
        uuid = uuid,
        name = "Test Station",
        streamUrl = "http://stream.test.com",
        logoBlob = null,
        faviconUrl = null,
        countryCode = null,
        tags = emptyList(),
        lastPlayed = null,
        totalPlayTime = 0,
        isManuallyAdded = false,
        homepage = null,
        codec = null,
        bitrate = 0,
        clickCount = 0,
        votes = 0
    )
}
