# BankP2P
**(this is a school project and has no real purpose or use-case)**

A bank server that follows a text protocol and stores accounts by number and their integer balance.

### Running

The program requires **Java 21 or newer**. 
Make sure the correct version of Java is on the path with `java --version`

1. Download the latest release from [Github releases](https://github.com/tomhula/BankP2P/releases)
2. Open a terminal and navigate to the directory with the downloaded jar
3. Run `java -jar <file-name>`
4. Stop the program (CTRL+C)
5. A `bank.conf` configuration file will appear in the current directory.
6. Open it and configure the values based on the comments and your preferences.
7. Run the program again: `java -jar <file-name>`

## Protocol

(the protocol is a modified version of the required one, however the implementation can be configured to comply with it)

The communication between client and server is based on one-line UTF-8 text commands and one-line UTF-8 text responses transported over TCP.
Commands are delimited by a new line, (line feed) each command receives an also single-line response.
Banks can (but don't have to) send CRLF instead of plain LF to enhance user experience.

### Commands

`<>` - denotes required part  
`[]` - denotes optional part

Commands have the following form:
```
<command-code> [arguments..]
```
Commands are case-sensitive.
When a command is successful the response has the following form:
```
<issued-command-code> [command-specific-message]
```
`<issued-command-code>` - is the code of the command that was issued.  
If the command was not successful, an error of the following form is returned:
```
ER [message]
```
(the ER is also case-sensitive)

These are all the commands:

| Name                   | Description                                          | Code | Syntax                                   | Success response    |
|------------------------|------------------------------------------------------|------|------------------------------------------|---------------------|
| Bank code              | Returns the bank of the code                         | BC   | BC                                       | BC <bank-code>      |
| Account create         | Creates a new account and returns its new number     | AC   | AC                                       | AC <account-number> |
| Account deposit        | Deposit money to an account                          | AD   | AD <account-number>/<bank-code> <amount  | AD                  |
| Account withdrawal     | Withdraw money from an account                       | AW   | AW <account-number>/<bank-code> <amount> | AW                  |
| Account balance        | Returns the current balance of an account            | AB   | AB <account-number>/<bank-code>          | AB <balance>        |
| Account remove         | Removes an account                                   | AR   | AR <account-number>/<bank-code>          | AR                  |
| Bank (total) amount    | Returns the total amount of money stored in the bank | BA   | BA                                       | BA <number>         |
| Bank number of clients | Returns the total number of clients in a bank        | BN   | BN                                       | BN <number>         |

`<account-number>` - A positive 32bit (signed) integer number representing an account in a specific bank
`<bank-code>` - A hostname (ip/domain) a bank is reachable at
`<amount>` - A positive 64bit (signed) integer amount of money

### Proxy

When a bank server receives a command, that works with an account on another bank, it has to proxy the command to that bank.
It needs to scan the destination ports, find one that reacts to `BC` command and forward the command to it and then pass back the response.
The protocol does not exactly restrict which ports the banks can run on.
It can (but does not have to) be negotiated when setting up a network.

## Implementation

This bank server stores accounts and their balances to a file, which is configurable.
This bank server also supports these commands:

| Name           | Description                                                                                                                     | Code | Syntax      | Success response          |
|----------------|---------------------------------------------------------------------------------------------------------------------------------|------|-------------|---------------------------|
| Bank robbery   | Scans a network for banks and plans which banks to rob to get as close to target money while robbing as few clients as possible | BR   | BR <target> | BR <rob-plan-description> |
| Debug (thread) | Returns the name of the server thread that handles the client who issued the command                                            | DE   | DE thread   | DE <thread-name>          |

## Sources

Insights on possible algorithm for bank robbery:
https://chatgpt.com/share/67a6653c-5e34-800e-8186-1662dcf4c88c
