description: EthSigner command line interface reference
<!--- END of page meta data -->

# EthSigner Command Line

This reference describes the syntax of the EthSigner Command Line Interface (CLI) options. EthSigner 
signs transaction with a key stored in an encrypted file or an external vault (for example, Hashicorp): 

* `ethsigner [Options] file-based-signer [File Options]`
* `ethsigner [Options] hashicorp-signer [Hashicorp Options]`

!!! tip
    To view the command line help for the subcommands: 
    
    * `ethsigner help file-based-signer`
    * `ethsigner help hashicorp-signer` 

## Options

### chain-id

Chain ID of the network to receive the signed transactions. 

```bash tab="Syntax"
--chain-id=<chainId>
```

```bash tab="Example"
--chain-id=2017
```

### data-path

Directory in which to store temporary files.  

```bash tab="Syntax"
--data-path=<PATH>
```

```bash tab="Example"
--data-path=/Users/me/my_node/data
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

```bash tab="Syntax"
--http-listen-host=<httpListenHost>
```

```bash tab="Example"
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

### logging

Logging verbosity levels. Options are: `OFF`, `FATAL`, `WARN`, `INFO`, `DEBUG`, `TRACE`, `ALL`. 
Default is `INFO`.  

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

## File Options 

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

## Hashicorp Options 

### auth-file

File containing authentication data for Hashicorp Vault. The authentication data is the [root token displayed by
the Hashicorp Vault server](../Using-EthSigner/Hashicorp.md#storing-private-key-in-hashcorp-vault). 

```bash tab="Syntax"
--auth-file=<authFile>
```

```bash tab="Example"
--auth-file=/Users/me/my_node/auth_file
```

### host

Host of the Hashicorp Vault server. Default is `localhost`. 

```bash tab="Syntax"
--host=<serverHost>
```

```bash tab="Example"
--host="http://host.com"
```

### port

Port of the Hashicorp Vault server. Default is 8200. 

```bash tab="Syntax"
--port=<serverPort>
```

```bash tab="Example"
--port=23000
```

### signing-key-path

Path to secret in the Hashicorp Vault containing the private key for signing transactions. Default is
` /secret/data/ethsignerSigningKey`. 

```bash tab="Syntax"
--signing-key-path=<signingKeyPath>
```

```bash tab="Example"
--signing-key-path=/my_secret/ethsignerSigningKey
```

### timeout

Timeout in milliseconds for requests to the Hashicorp Vault server. Default is 10000. 

```bash tab="Syntax"
--timeout=<timeout>
```

```bash tab="Example"
--timeout=5000
```
