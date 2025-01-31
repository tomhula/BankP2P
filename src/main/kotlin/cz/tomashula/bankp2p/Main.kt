package cz.tomashula.bankp2p

fun main()
{
    val server = TelnetServer("localhost", 5000) { client, input ->
        "From: ${client.host}:${client.port} Parrot: $input"
    }
    server.start()
}
