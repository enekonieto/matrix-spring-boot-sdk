package net.folivo.matrix.bot.appservice

import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import net.folivo.matrix.appservice.api.AppserviceHandlerHelper
import net.folivo.matrix.bot.config.MatrixBotProperties.AutoJoinMode
import net.folivo.matrix.bot.config.MatrixBotProperties.AutoJoinMode.*
import net.folivo.matrix.bot.handler.AutoJoinService
import net.folivo.matrix.core.api.ErrorResponse
import net.folivo.matrix.core.api.MatrixServerException
import net.folivo.matrix.core.model.events.m.room.MemberEvent
import net.folivo.matrix.core.model.events.m.room.MemberEvent.MemberEventContent.Membership.INVITE
import net.folivo.matrix.restclient.MatrixClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus.FORBIDDEN
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@ExtendWith(MockKExtension::class)
class AutoJoinEventHandlerTest {

    @MockK(relaxed = true)
    lateinit var matrixClientMock: MatrixClient

    @MockK
    lateinit var autoJoinServiceMock: AutoJoinService

    @MockK
    lateinit var roomServiceMock: DefaultMatrixAppserviceRoomService

    @MockK
    lateinit var helperMock: AppserviceHandlerHelper

    @BeforeEach
    fun setupMocks() {
        every { matrixClientMock.roomsApi.joinRoom(allAny()) } returns Mono.just("!someRoomId:someServerName")
        every { matrixClientMock.roomsApi.leaveRoom(allAny()) } returns Mono.empty()
        every { roomServiceMock.saveRoomJoin(any(), any()) } returns Mono.empty()
        every { autoJoinServiceMock.shouldJoin(any(), any(), any()) } returns Mono.just(true)
        every { helperMock.registerAndSaveUser(any()) } returns Mono.just(true)
    }

    fun doInvite(userId: String, autoJoinMode: AutoJoinMode = ENABLED, roomId: String = "!someRoomId:someServerName") {
        val cut = AutoJoinEventHandler(
                autoJoinService = autoJoinServiceMock,
                matrixClient = matrixClientMock,
                roomService = roomServiceMock,
                autoJoin = autoJoinMode,
                serverName = "someServerName",
                usersRegex = listOf("unicorn_.*"),
                asUsername = "someAsUsername",
                helper = helperMock
        )
        val inviteEvent = mockk<MemberEvent>(relaxed = true) {
            every { content.membership } returns INVITE
            every { stateKey } returns userId
        }
        StepVerifier
                .create(cut.handleEvent(inviteEvent, roomId))
                .verifyComplete()
    }

    @Test
    fun `should support MemberEvent`() {
        val cut = AutoJoinEventHandler(
                autoJoinService = autoJoinServiceMock,
                matrixClient = matrixClientMock,
                roomService = roomServiceMock,
                autoJoin = DISABLED,
                serverName = "someServerName",
                usersRegex = emptyList(),
                asUsername = "someAsUsername",
                helper = helperMock
        )
        assertThat(cut.supports(MemberEvent::class.java)).isTrue()
    }

    @Test
    fun `should do nothing when autoJoin is disabled`() {
        doInvite("@someUserId:someServerName", DISABLED)
        verifyAll {
            matrixClientMock wasNot Called
            roomServiceMock wasNot Called
        }
    }

    @Test
    fun `should do reject invite from other server when autoJoin is restricted`() {
        doInvite("@someUserId:someServerName", RESTRICTED, "!someRoomId:foreignServer")
        verifyAll {
            matrixClientMock.roomsApi.leaveRoom("!someRoomId:foreignServer", "@someUserId:someServerName")
            roomServiceMock wasNot Called
        }
    }

    @Test
    fun `should join invited room when autoJoin is restricted and invited user is application service user`() {
        doInvite("@someAsUsername:someServerName", RESTRICTED)
        verifyAll {
            matrixClientMock.roomsApi.joinRoom("!someRoomId:someServerName")
            roomServiceMock.saveRoomJoin("!someRoomId:someServerName", "@someAsUsername:someServerName")
        }
    }

    @Test
    fun `should join invited room when autoJoin is enabled and invited user is application service user`() {
        doInvite("@someAsUsername:someServerName")
        verifyAll {
            matrixClientMock.roomsApi.joinRoom("!someRoomId:someServerName")
            roomServiceMock.saveRoomJoin("!someRoomId:someServerName", "@someAsUsername:someServerName")
        }
    }

    @Test
    fun `should join invited room when autoJoin is restricted and invited user is managed by application service`() {
        doInvite("@unicorn_star:someServerName", RESTRICTED)
        verifyAll {
            matrixClientMock.roomsApi.joinRoom("!someRoomId:someServerName", asUserId = "@unicorn_star:someServerName")
            roomServiceMock.saveRoomJoin("!someRoomId:someServerName", "@unicorn_star:someServerName")
        }
    }

    @Test
    fun `should do reject invite when services don't want to join it`() {
        every {
            autoJoinServiceMock.shouldJoin(
                    "!someRoomId:someServerName",
                    "@someAsUsername:someServerName",
                    true
            )
        } returns Mono.just(false)
        doInvite("@someAsUsername:someServerName")
        verifyAll {
            matrixClientMock.roomsApi.leaveRoom("!someRoomId:someServerName")
            roomServiceMock wasNot Called
        }
    }

    @Test
    fun `should join invited room when autoJoin is enabled and invited user is managed by application service`() {
        doInvite("@unicorn_star:someServerName")
        verifyAll {
            matrixClientMock.roomsApi.joinRoom("!someRoomId:someServerName", asUserId = "@unicorn_star:someServerName")
            roomServiceMock.saveRoomJoin("!someRoomId:someServerName", "@unicorn_star:someServerName")
        }
    }

    @Test
    fun `should do nothing when autoJoin is restricted but invited user is not managed by application service`() {
        doInvite("@dino_star:someServerName", RESTRICTED)
        verifyAll {
            matrixClientMock wasNot Called
            roomServiceMock wasNot Called
        }
    }

    @Test
    fun `should do nothing when autoJoin is enabled but invited user is not managed by application service`() {
        doInvite("@dino_star:someServerName")
        verifyAll {
            matrixClientMock wasNot Called
            roomServiceMock wasNot Called
        }
    }

    @Test
    fun `should try to register user when join is FORBIDDEN`() {
        every {
            matrixClientMock.roomsApi.joinRoom(
                    "!someRoomId:someServerName",
                    asUserId = "@unicorn_star:someServerName"
            )
        }.returnsMany(
                Mono.error(MatrixServerException(FORBIDDEN, ErrorResponse("FORBIDDEN"))),
                Mono.just("!someRoomId:someServerName")
        )

        doInvite("@unicorn_star:someServerName")
        verifyAll {
            helperMock.registerAndSaveUser("@unicorn_star:someServerName")
            roomServiceMock.saveRoomJoin("!someRoomId:someServerName", "@unicorn_star:someServerName")
        }
        verify(exactly = 2) {
            matrixClientMock.roomsApi.joinRoom(
                    "!someRoomId:someServerName",
                    asUserId = "@unicorn_star:someServerName"
            )
        }
    }

    @Test
    fun `should try to register user when leave is FORBIDDEN and not restricted`() {
        every {
            matrixClientMock.roomsApi.leaveRoom(
                    "!someRoomId:someServerName",
                    asUserId = "@unicorn_star:someServerName"
            )
        }.returnsMany(
                Mono.error(MatrixServerException(FORBIDDEN, ErrorResponse("FORBIDDEN"))),
                Mono.empty()
        )
        every { autoJoinServiceMock.shouldJoin(any(), any(), any()) } returns Mono.just(false)

        doInvite("@unicorn_star:someServerName")
        verify {
            helperMock.registerAndSaveUser("@unicorn_star:someServerName")
        }
        verify(exactly = 2) {
            matrixClientMock.roomsApi.leaveRoom(
                    "!someRoomId:someServerName",
                    asUserId = "@unicorn_star:someServerName"
            )
        }
    }

    @Test
    fun `should not try to register user when leave is FORBIDDEN and restricted`() {
        every {
            matrixClientMock.roomsApi.leaveRoom(
                    "!someRoomId:someOtherServerName",
                    asUserId = "@unicorn_star:someServerName"
            )
        }.returnsMany(
                Mono.error(MatrixServerException(FORBIDDEN, ErrorResponse("FORBIDDEN"))),
                Mono.empty()
        )
        every { autoJoinServiceMock.shouldJoin(any(), any(), any()) } returns Mono.just(false)

        doInvite("@unicorn_star:someServerName", RESTRICTED, "!someRoomId:someOtherServerName")
        verify(exactly = 0) { helperMock.registerAndSaveUser("@unicorn_star:someServerName") }
        verify(exactly = 1) {
            matrixClientMock.roomsApi.leaveRoom(
                    "!someRoomId:someOtherServerName",
                    asUserId = "@unicorn_star:someServerName"
            )
        }
    }
}