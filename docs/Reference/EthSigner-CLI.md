description: EthSigner command line interface reference
<!--- END of page meta data -->

# EthSigner Command Line

This reference describes the syntax of the EthSigner Command Line Interface (CLI) options.

## Options

### chain-id

Chain ID of the network to receive the signed transactions. 

```bash tab="Syntax"
--chain-id=<chainId>
```

```bash tab="Example"
--chain-id=2017
```

### downstream-http-host

Endpoint to which received requests are forwarded. Default is `localhost`. 

```bash tab="Syntax"
--downstream-http-host=<downstreamHttpHost>
```

```bash tab="Example"
--downstream-http-host=192.168.05.14
```

### downstream-http-port

Endpoint to which received requests are forwarded. 

```bash tab="Syntax"
--downstream-http-port=<downstreamHttpPort>
```

```bash tab="Example"
--downstream-http-port=6174
```

### downstream-http-request-timeout

Timeout period (in milliseconds) for downstream requests. Default is 5000. 

```bash tab="Syntax"
--downstream-http-request-timeout=<downstreamHttpRequestTimeout>
```

```bash tab="Example"
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

```bash tab="Syntax"
--http-listen-port=<httpListenPort>
```

```bash tab="Example"
--http-lisentport=6174
```

### key-file

File containing [key with which transactions are signed](../Using-EthSigner/Getting-Started.md#create-password-and-key-files).  

```bash tab="Syntax"
-k, --key-file=<keyFile>
```

```bash tab="Example"
--key-file=/Users/me/my_node/transactionKey
```

### password-file

File containing password for the [key with which transactions are signed](../Using-EthSigner/Getting-Started.md#create-password-and-key-files).  

```bash tab="Syntax"
-p, --password-file=<passwordFile>
```

```bash tab="Example"
--password-file=/Users/me/my_node/password
```

### logging

Logging verbosity levels. Options are: `OFF`, `FATAL`, `WARN`, `INFO`, `DEBUG`, `TRACE`, `ALL`. The 
default is `INFO`.  

```bash tab="Syntax"
-l, --logging=<LOG VERBOSITY LEVEL>
```

```bash tab="Example"
--logging=DEBUG
```

### help

Displays the help and exits.  

```bash tab="Syntax"
-h, --help
```

### version

Displays the version and exits.  

```bash tab="Syntax"
-V, --version
```