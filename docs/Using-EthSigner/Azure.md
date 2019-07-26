description: Signing transactions with key stored in Azure Key Vault
<!--- END of page meta data -->

# Using EthSigner with Azure Key Vault 

EthSigner supports storing the signing key in an [Azure Key Vault](https://azure.microsoft.com/en-au/services/key-vault/). 

## Storing Private Key in Azure Key Vault 

Create a SECP256k1 key in the [Azure Key Vault](https://docs.microsoft.com/en-us/azure/key-vault/)
and register EthSigner as an application for the key. 

Take note of the following to specify when starting EthSigner: 

* Key vault name 
* Key name 
* Key version 
* Client ID 
* File containing client secret for the client ID 

## Start Pantheon 

[Start Pantheon](https://docs.pantheon.pegasys.tech/en/stable/Getting-Started/Starting-Pantheon/) with the 
[`--rpc-http-port`](https://docs.pantheon.pegasys.tech/en/stable/Reference/Pantheon-CLI-Syntax/#rpc-http-port)
option set to `8590` to avoid conflict with the default EthSigner listening port (`8545`). 

!!! example
    ```bash
    pantheon --network=dev --miner-enabled --miner-coinbase=0xfe3b557e8fb62b89f4916b721be55ceb828dbd73 --rpc-http-cors-origins="all" --host-whitelist=* --rpc-http-enabled --rpc-http-port=8590 --data-path=/tmp/tmpDatdir
    ```

## Start EthSigner with Azure Key Vault Signing 

Start EthSigner.

!!! example  
    ```bash
    ethsigner --chain-id=2018 --downstream-http-port=8590 azure-signer --client-id=<ClientID> --client-secret-path=mypath/mysecretfile --key-name=<KeyName> --key-version=<KeyVersion> --keyvault-name=<KeyVaultName>
    ```

!!! tip
    Use the [--http-listen-port](../Reference/EthSigner-CLI.md#http-listen-port) option to change the
    EthSigner listening port if `8545` is in use.  

You can now [use EthSigner to sign transactions](Using-EthSigner.md) with the key stored in the Azure Key Vault.  
