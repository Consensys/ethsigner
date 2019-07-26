description: Signing transactions with key stored in Hashicorp Key Vault
<!--- END of page meta data -->

# Using EthSigner with Hashicorp Vault 

EthSigner supports storing the signing key in a [Hashicorp Vault](https://www.hashicorp.com/products/vault/). 

!!! caution 
    EthSigner with Hashicorp Vault does not support TLS. If using a remote Hashicorp Vault, apply appropriate 
    security. 

## Storing Private Key in Hashicorp Vault 

After installing [Hashicorp Vault](https://learn.hashicorp.com/vault/getting-started/install) and
[starting the server](https://learn.hashicorp.com/vault/getting-started/dev-server):

1.  Set the `VAULT_ADDR` environment variable using the command displayed after starting the server: 
    ```bash
    export VAULT_ADDR='http://127.0.0.1:8200'
    ```

2. Save the root token displayed after starting the server in a file called `authFile`. 

3. Put your signing key into the Hashicorp Vault: 

    ```bash tab="Command"
    vault kv put secret/ethsignerSigningKey value=<Private Key ex 0x)
    ```
       
    ```bash tab="Example"
    vault kv put secret/ethsignerSigningKey value=8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63
    ```

    The private key is stored in the default location for EthSigner. The key must be a base 64 encoded private 
    key for ECDSA for curve secp256k1.  

## Start Pantheon 

[Start Pantheon](https://docs.pantheon.pegasys.tech/en/stable/Getting-Started/Starting-Pantheon/) with the 
[`--rpc-http-port`](https://docs.pantheon.pegasys.tech/en/stable/Reference/Pantheon-CLI-Syntax/#rpc-http-port)
option set to `8590` to avoid conflict with the default EthSigner listening port (`8545`). 

!!! example
    ```bash
    pantheon --network=dev --miner-enabled --miner-coinbase=0xfe3b557e8fb62b89f4916b721be55ceb828dbd73 --rpc-http-cors-origins="all" --host-whitelist=* --rpc-http-enabled --rpc-http-port=8590 --data-path=/tmp/tmpDatdir
    ```

## Start EthSigner with Hashicorp Vault Signing 

Start EthSigner.

!!! example  
    ```bash
    ethsigner --chain-id=2018 --downstream-http-port=8590 hashicorp-signer --host=127.0.0.1 --port=8200 --auth-file=authFile
    ```

!!! tip
    Use the [--http-listen-port](../Reference/EthSigner-CLI.md#http-listen-port) option to change the
    EthSigner listening port if `8545` is in use.  

You can now [use EthSigner to sign transactions](Using-EthSigner.md) with the key stored in the Hashicorp Vault.  