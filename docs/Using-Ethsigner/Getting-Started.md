description: Install EthSigner from binary distribution
<!--- END of page meta data -->

# Getting Started 

EthSigner requires a V3 Keystore key file and a password file. 

## Prerequisites 

* [EthSigner](../Installation/Install-Binaries.md)
* [Pantheon](https://docs.pantheon.pegasys.tech/en/stable/Installation/Install-Binaries/)
* [Node.js](https://nodejs.org/en/download/)
* [web3.js](https://github.com/ethereum/web3.js/)

!!! note
    The Ethereum client used in this documentation is Pantheon but EthSigner can be used with any Ethereum client.     


## Start Pantheon 

[Start Pantheon](https://docs.pantheon.pegasys.tech/en/stable/Getting-Started/Starting-Pantheon/) with the 
[`--rpc-http-port`](https://docs.pantheon.pegasys.tech/en/stable/Reference/Pantheon-CLI-Syntax/#rpc-http-port)
option set to `8590`. 

!!! example
    ```bash
    pantheon --network=dev --miner-enabled --miner-coinbase=0xfe3b557e8fb62b89f4916b721be55ceb828dbd73 --rpc-http-cors-origins="all" --host-whitelist=* --rpc-http-enabled --rpc-http-port=8590 --data-path=/tmp/tmpDatdir
    ```

## Create Password and Key Files 

Create a text file containing the password for the V3 Keystore key file to be created (for example, `passwordFile`). 

Use the [web3.js library](https://github.com/ethereum/web3.js/) to create a key file where: 

* `<AccountPrivateKey>` is the private key of the account with which EthSigner will sign transactions.  

* `<Password>` is the password for the key file being created. The password must match the password saved in the 
   password file created above (`passwordFile` in this example).

!!! example 

    ```javascript linenums="1" tab="Create Key File"
    const Web3 = require('web3')
    
    // Web3 initialization (should point to the JSON-RPC endpoint)
    const web3 = new Web3(new Web3.providers.HttpProvider('http://127.0.0.1:8590'))
    
    var V3KeyStore = web3.eth.accounts.encrypt("<AccountPrivateKey>", "<Password>");
    console.log(JSON.stringify(V3KeyStore));
    process.exit();
    ```
    
    ```javascript linenums="1" tab="Example"
    const Web3 = require('web3')
        
    // Web3 initialization (should point to the JSON-RPC endpoint)
    const web3 = new Web3(new Web3.providers.HttpProvider('http://127.0.0.1:8590'))
        
    var V3KeyStore = web3.eth.accounts.encrypt("0x8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63", "password");
    console.log(JSON.stringify(V3KeyStore));
    process.exit();
    ```
    
Copy and paste the example JS script to a file (for example, `createKeyFile.js`) and replace the placeholders. 

Use the JS script to display the text for the key file: 

```bash
node createKeyFile.js
```

Copy and paste the text to a file (for example, `keyFile`). The file is your V3 Keystore key file. 
    
## Start EthSigner

Start EthSigner with options specified as follows: 

* `chain-id` is the chain ID specified in the Pantheon genesis file. 

* `downstream-http-port` is the `rpc-http-port` specified for Pantheon (`8590` in this example). 

* `key-file` and `password-file` are the key and password files [created above](#create-password-and-key-files).  

!!! example
    ```
    ethsigner --chain-id=2018 --downstream-http-port=8590 --key-file=/mydirectory/keyFile --password-file=/mydirectory/passwordFile
    ```

## Confirm EthSigner Passing Requests to Pantheon 

Request the current block number using [`eth_blockNumber`](https://docs.pantheon.pegasys.tech/en/stable/Reference/JSON-RPC-API-Methods/#eth_blocknumber) with the EthSigner JSON-RPC endpoint (`8545` in this example): 

```bash
curl -X POST --data '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":51}' http://127.0.0.1:8545
```