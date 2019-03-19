description: EthFirewall command line interface reference
<!--- END of page meta data -->

# EthFirewall Command Line

This reference describes the syntax of the EthFirewall Command Line Interface (CLI) options.

```
ethfirewall --chain-id=<chainId> --downstream-http-port=<port> --key-file=<keyFile> --password-file=<passwordFile> [options]
```

Runs EthFirewall transaction signing and firewall application.

## Options

### chain-id

Chain ID that is the intended recipient for the signed transactions. 

#### Syntax 
```
--chain-id=<chainId>
```

#### Example 
```
--chain-id=2017
```

### downstream-http-host

Endpoint to which received requests are forwarded. Default is `localhost`. 

#### Syntax 
```
--downstream-http-host=<downstreamHttpHost>
```

#### Example 
```
--downstream-http-host=8.8.8.8
```

### downstream-http-port

Endpoint to which received requests are forwarded. 

#### Syntax 
```
--downstream-http-port=<downstreamHttpPort>
```

#### Example 
```
--downstream-http-port=6174
```

### downstream-http-request-timeout

Timeout period (in milliseconds) for downstream requests. Default is 5000. 

#### Syntax 
```
--downstream-http-request-timeout=<downstreamHttpRequestTimeout>
```

#### Example 
```
--downstream-http-request-timeout=3000
```

### http-listen-host

Host on which JSON-RPC HTTP listens. Default is `localhost`. 

#### Syntax 
```
--http-listen-host=<httpListenHost>
```

#### Example 
```
--http-listen-host=8.8.8.8
```

### http-listen-port

Port on which JSON-RPC HTTP listens. Default is 8545. 

#### Syntax 
```
--http-listen-port=<httpListenPort>
```

#### Example 
```
--http-lisentport=6174
```

### key-file

File containing key used to sign transactions.  

#### Syntax 
```
-k, --key-file=<keyFile>
```

#### Example 
```
--key-file=/Users/me/my_node/transactionKey
```

### password-file

File containing password to decrypt the [key used to sign transactions](#key-file).  

#### Syntax 
```
-k, --password-file=<passwordFile>
```

#### Example 
```
-p, --password-file=/Users/me/my_node/password
```

### logging

Logging verbosity levels. Options are: `OFF`, `FATAL`, `WARN`, `INFO`, `DEBUG`, `TRACE`, `ALL`. The 
default is `INFO`.  

#### Syntax 
```
-l, --logging=<LOG VERBOSITY LEVEL>
```

### help

Displays the help and exits.  

#### Syntax 
```
-h, --help
```

### version

Displays the version and exits.  

#### Syntax 
```
-V, --version
```
