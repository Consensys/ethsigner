description: Using EthSigner
<!--- END of page meta data -->

# Using EthSigner

EthSigner provides transaction signing and access to your keystore by implementing the following JSON-RPC 
methods:   

* [`eea_sendTransaction`](#eea_sendtransaction)
* [`eth_accounts`](#eth_accounts)
* [`eth_sendTransaction`](#eth_sendtransaction)

All other JSON-RPC requests are passed through EthSigner to Pantheon and the result from Pantheon returned 
by EthSigner. 

!!! note 
    EthSigner supports the JSON-RPC service over HTTP only. 

# eea_sendTransaction 

Creates and signs a [private transaction](https://docs.pantheon.pegasys.tech/en/stable/Privacy/Privacy-Overview/)
using the [keystore account](../Using-EthSigner/Getting-Started.md#create-password-and-key-files). 

EthSigner submits the signed transaction to Pantheon using [`eea_sendRawTransaction`](https://docs.pantheon.pegasys.tech/en/stable/Reference/Pantheon-API-Methods/#eea_sendrawtransaction). 

**Parameters**

Transaction object for private transactions: 

| Key             | Type                | Required/Optional                  | Value                                                                                                                         |
|-----------------|--:-:----------------|----------------------------------- |-------------------------------------------------------------------------------------------------------------------------------|
| **from**        | Data, 20&nbsp;bytes | Required                           | Address of the sender. Must be the address of the keystore account.                                                                                                        |
| **to**          | Data, 20&nbsp;bytes | Not required for contract creation | `null` for contract creation transaction. Contract address for contract invocation transactions.                                                           |
| **gas**         | Quantity            | Optional                           | Gas provided by the sender. Default is `90000`.                                                                               |
| **gasPrice**    | Quantity            | Optional                           | Gas price provided by the sender in Wei. Default is `0`.                                                                      |
| **nonce**       | Quantity            | Optional                           | Number of transactions sent from the `from` account before this one.                                                                    |
| **data**        | Quantity            | Optional                           | Compiled contract code or hash of the invoked method signature and encoded parameters.                                        |
| **privateFrom** | Data, 20&nbsp;bytes | Required                           | Orion address of the sender                                                                                                         |
| **privateFor**  | Array of data       | Required                           | Orion addresses of recipients                                                                                                       |
| **restriction** | String              | Required                           | Must be [`restricted`](https://docs.pantheon.pegasys.tech/en/stable/Privacy/Privacy-Overview/#private-transaction-attributes) |

!!! tip
    Submitting a transaction with the same nonce as a pending transaction and a higher gas price replaces 
    the pending transaction with the new one. If not attempting to replace a pending transaction, do not 
    include the `nonce` in the private transaction object and nonce management is handled automatically. 

!!! note
    If a `value` is included in the transaction object, an error is returned.  Ether transfers cannot 
    be private transactions. 

**Returns**

`result` : `data` - Transaction hash

!!! example
    ```bash tab="curl HTTP request"
    curl -X POST --data '{"jsonrpc":"2.0","method":"eea_sendTransaction","params":[{"from": "0xfe3b557e8fb62b89f4916b721be55ceb828dbd73","data": "0x608060405234801561001057600080fd5b5060dc8061001f6000396000f3006080604052600436106049576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680633fa4f24514604e57806355241077146076575b600080fd5b348015605957600080fd5b50606060a0565b6040518082815260200191505060405180910390f35b348015608157600080fd5b50609e6004803603810190808035906020019092919050505060a6565b005b60005481565b80600081905550505600a165627a7a723058202bdbba2e694dba8fff33d9d0976df580f57bff0a40e25a46c398f8063b4c00360029", "privateFrom": "negmDcN2P4ODpqn/6WkJ02zT/0w0bjhGpkZ8UP6vARk=","privateFor": ["g59BmTeJIn7HIcnq8VQWgyh/pDbvbt2eyP0Ii60aDDw="],"restriction": "restricted"}], "id":1}' http://127.0.0.1:8545
    ```
    
    ```json tab="JSON result"
    {
       "jsonrpc": "2.0",
       "id": 1,
       "result": "0x6052dd2131667ef3e0a0666f2812db2defceaec91c470bb43de92268e8306778"
    }
    ```

# eth_accounts

Returns the account address with which EthSigner is signing transactions. That is, the account of the [keystore key file](../Using-EthSigner/Getting-Started.md#create-password-and-key-files).

**Parameters**

None

**Returns**

`Array of data` : Account address with which EthSigner is signing transactions.

!!! example
    ```bash tab="curl HTTP request"
    curl -X POST --data '{"jsonrpc":"2.0","method":"eth_accounts","params":[],"id":1}' http://127.0.0.1:8545
    ```
        
    ```json tab="JSON result"
    {
      "jsonrpc":"2.0",
      "id":1,
      "result":["0xfe3b557e8fb62b89f4916b721be55ceb828dbd73"]
    }
    ```

# eth_sendTransaction 

Creates and signs a transaction using the [keystore account](../Using-EthSigner/Getting-Started.md#create-password-and-key-files). 

EthSigner submits the signed transaction to Pantheon using [`eth_sendRawTransaction`](https://docs.pantheon.pegasys.tech/en/stable/Reference/JSON-RPC-API-Methods/#eth_sendrawtransaction). 

**Parameters**

Transaction object: 

| Key          | Type                | Required/Optional              | Value                                                                                  |
|--------------|-:-:-----------------|--------------------------------|----------------------------------------------------------------------------------------|
| **from**     | Data, 20&nbsp;bytes | Required                       | Address of the sender.                                                                 |
| **to**       | Data, 20&nbsp;bytes | Optional for contract creation | Address of the receiver. `null` if a contract creation transaction.                    |
| **gas**      | Quantity            | Optional                       | Gas provided by the sender. Default is `90000`.                                                           |
| **gasPrice** | Quantity            | Optional                       | Gas price provided by the sender in Wei. Default is `0`.                                              |
| **nonce**    | Quantity            | Optional                       | Number of transactions made by the sender before this one.                             |
| **value**    | Quantity            | Optional                       | Value transferred in Wei.                                                              |
| **data**     | Quantity            | Optional                       | Compiled contract code or hash of the invoked method signature and encoded parameters. |

!!! tip
    Submitting a transaction with the same nonce as a pending transaction and a higher gas price replaces 
    the pending transaction with the new one. 

**Returns**

`result` : `data` - 32-byte transaction hash

!!! example
    ```bash tab="curl HTTP request"
    curl -X POST --data '{"jsonrpc":"2.0","method":"eth_sendTransaction","params":[{"from": "0xfe3b557e8fb62b89f4916b721be55ceb828dbd73","to": "0xd46e8dd67c5d32be8058bb8eb970870f07244567","gas": "0x7600","gasPrice": "0x9184e72a000","value": "0x9184e72a"}], "id":1}' http://127.0.0.1:8545
    ```
    
    ```json tab="JSON result"
    {
       "jsonrpc": "2.0",
       "id": 1,
       "result": "0x6052dd2131667ef3e0a0666f2812db2defceaec91c470bb43de92268e8306778"
    }
    ```
