description: Install Ethsigner from binary distribution
<!--- END of page meta data -->

# Getting Started 


## Create Key File 

Ethsigner requires a V3 Keystore key file and a password file. 

You can use the [web3.js library](https://github.com/ethereum/web3.js/) to create a key file. 

!!! example 

    ```javascript linenums="1" tab="Create Key File"
    const Web3 = require('web3')
    
    // Web3 initialization (should point to the JSON-RPC endpoint)
    const web3 = new Web3(new Web3.providers.HttpProvider('http://127.0.0.1:8545'))
    
    var V3KeyStore = web3.eth.accounts.encrypt("<AccountPrivateKey>", "<PasswordKeyFile>");
    console.log(JSON.stringify(V3KeyStore));
    process.exit();
    ```
    
    ```javascript linenums="1" tab="Example"
    const Web3 = require('web3')
        
    // Web3 initialization (should point to the JSON-RPC endpoint)
    const web3 = new Web3(new Web3.providers.HttpProvider('http://127.0.0.1:8545'))
        
    var V3KeyStore = web3.eth.accounts.encrypt("0x8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63", "password");
    console.log(JSON.stringify(V3KeyStore));
    process.exit();
    ```

## Start Pantheon 

[Start Pantheon](https://docs.pantheon.pegasys.tech/en/stable/Getting-Started/Starting-Pantheon/) with the 
[`--rpc-http-port`](https://docs.pantheon.pegasys.tech/en/stable/Reference/Pantheon-CLI-Syntax/#rpc-http-port)
option set to `8590`. 

!!! example
    ```bash
    pantheon --network=dev --miner-enabled --miner-coinbase=0xfe3b557e8fb62b89f4916b721be55ceb828dbd73 --rpc-http-cors-origins="all" --host-whitelist=* --rpc-ws-enabled --rpc-http-enabled --data-path=/tmp/tmpDatdir
    ```
    
## Start Ethsigner

   