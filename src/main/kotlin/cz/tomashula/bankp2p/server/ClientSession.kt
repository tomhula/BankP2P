package cz.tomashula.bankp2p.server

data class ClientSession(
    val host: String,
    val port: Int,
)
{
    override fun toString(): String = "$host:$port"
}
