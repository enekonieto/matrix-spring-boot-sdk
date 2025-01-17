package net.folivo.matrix.appservice

import net.folivo.matrix.appservice.api.event.AppserviceEventService
import net.folivo.matrix.appservice.api.room.AppserviceRoomService
import net.folivo.matrix.appservice.api.room.CreateRoomParameter
import net.folivo.matrix.appservice.api.user.AppserviceUserService
import net.folivo.matrix.appservice.api.user.RegisterUserParameter
import net.folivo.matrix.appservice.config.AppserviceAutoconfiguration
import net.folivo.matrix.core.model.MatrixId.*
import net.folivo.matrix.core.model.events.Event
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.RequestPredicates.accept
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions.route
import org.springframework.web.reactive.function.server.ServerResponse


@SpringBootConfiguration
@EnableAutoConfiguration
@Import(AppserviceAutoconfiguration::class)
class TestApplication {

    @Bean
    fun something(): RouterFunction<ServerResponse> {
        return route(GET("/_matrix/something").and(accept(APPLICATION_JSON)), HandlerFunction {
            ServerResponse.ok().build()
        })
    }

    @Bean
    fun testController(): TestRestController {
        return TestRestController()
    }

    @Bean
    fun noOpAppserviceEventService(): AppserviceEventService {
        return object : AppserviceEventService {
            override suspend fun eventProcessingState(
                tnxId: String,
                eventId: EventId
            ): AppserviceEventService.EventProcessingState {
                return AppserviceEventService.EventProcessingState.PROCESSED
            }

            override suspend fun onEventProcessed(tnxId: String, eventId: EventId) {
            }

            override suspend fun processEvent(event: Event<*>) {
            }
        }
    }

    @Bean
    fun noOpAppserviceRoomService(): AppserviceRoomService {
        return object : AppserviceRoomService {
            override suspend fun roomExistingState(roomAlias: RoomAliasId): AppserviceRoomService.RoomExistingState {
                return AppserviceRoomService.RoomExistingState.DOES_NOT_EXISTS
            }

            override suspend fun getCreateRoomParameter(roomAlias: RoomAliasId): CreateRoomParameter {
                return CreateRoomParameter()
            }

            override suspend fun onCreatedRoom(roomAlias: RoomAliasId, roomId: RoomId) {

            }
        }
    }

    @Bean
    fun noOpAppserviceUserService(): AppserviceUserService {
        return object : AppserviceUserService {
            override suspend fun userExistingState(userId: UserId): AppserviceUserService.UserExistingState {
                return AppserviceUserService.UserExistingState.DOES_NOT_EXISTS
            }

            override suspend fun getRegisterUserParameter(userId: UserId): RegisterUserParameter {
                return RegisterUserParameter()
            }

            override suspend fun onRegisteredUser(userId: UserId) {

            }
        }
    }

}