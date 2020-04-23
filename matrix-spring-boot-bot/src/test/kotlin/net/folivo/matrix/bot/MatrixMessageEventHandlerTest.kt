package net.folivo.matrix.bot

import io.mockk.Called
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import net.folivo.matrix.bot.handler.MatrixMessageContentHandler
import net.folivo.matrix.bot.handler.MatrixMessageEventHandler
import net.folivo.matrix.restclient.MatrixClient
import net.folivo.matrix.restclient.model.events.RoomEvent
import net.folivo.matrix.restclient.model.events.StateEvent
import net.folivo.matrix.restclient.model.events.m.room.AliasesEvent
import net.folivo.matrix.restclient.model.events.m.room.message.MessageEvent
import net.folivo.matrix.restclient.model.events.m.room.message.TextMessageEventContent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class MatrixMessageEventHandlerTest {

    @MockK
    lateinit var matrixClientMock: MatrixClient

    @RelaxedMockK
    lateinit var messageContentHandler1: MatrixMessageContentHandler

    @RelaxedMockK
    lateinit var messageContentHandler2: MatrixMessageContentHandler

    @Test
    fun `should support message events`() {
        val cut = MatrixMessageEventHandler(listOf())
        assertThat(cut.supports(MessageEvent::class.java)).isTrue()
    }

    @Test
    fun `should delegate message events to each handler`() {
        val cut = MatrixMessageEventHandler(
                listOf(
                        messageContentHandler1,
                        messageContentHandler2
                )
        )
        val content = TextMessageEventContent("test")
        cut.handleEvent(
                MessageEvent(
                        content = content,
                        roomId = "someRoomId",
                        id = "someMessageId",
                        sender = "someSender",
                        originTimestamp = 1234,
                        unsigned = RoomEvent.UnsignedData()
                ), "roomId", matrixClientMock
        )
        verify { messageContentHandler1.handleMessage(content, any()) }
        verify { messageContentHandler1.handleMessage(content, any()) }
    }

    @Test
    fun `should not delegate non message events`() {
        val cut = MatrixMessageEventHandler(
                listOf(
                        messageContentHandler1,
                        messageContentHandler2
                )
        )
        cut.handleEvent(
                AliasesEvent(
                        content = AliasesEvent.AliasesEventContent(),
                        roomId = "someRoomId",
                        id = "someMessageId",
                        sender = "someSender",
                        originTimestamp = 1234,
                        stateKey = "",
                        unsigned = StateEvent.UnsignedData()
                ), "roomId", matrixClientMock
        )
        verify { messageContentHandler1 wasNot Called }
        verify { messageContentHandler2 wasNot Called }
    }
}