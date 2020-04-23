package net.folivo.matrix.restclient.api.rooms

import com.fasterxml.jackson.annotation.JsonProperty

internal data class CreateRoomResponse(
        @JsonProperty("room_id")
        val roomId: String
)