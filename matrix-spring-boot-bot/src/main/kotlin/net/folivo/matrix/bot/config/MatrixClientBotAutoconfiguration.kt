package net.folivo.matrix.bot.config

import net.folivo.matrix.bot.client.MatrixClientBot
import net.folivo.matrix.bot.handler.AutoJoinService
import net.folivo.matrix.bot.handler.MatrixEventHandler
import net.folivo.matrix.restclient.MatrixClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
@ConditionalOnProperty(prefix = "matrix.bot", name = ["mode"], havingValue = "CLIENT", matchIfMissing = true)
class MatrixClientBotAutoconfiguration(private val botProperties: MatrixBotProperties) {

    @Bean
    @ConditionalOnMissingBean
    fun matrixClientBot(
            matrixClient: MatrixClient,
            matrixEventHandler: List<MatrixEventHandler>,
            autoJoinService: AutoJoinService
    ): MatrixClientBot {
        val matrixClientBot = MatrixClientBot(
                matrixClient,
                matrixEventHandler,
                botProperties,
                autoJoinService
        )
        matrixClientBot.start()
        return matrixClientBot
    }

}