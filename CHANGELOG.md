# Changelog

## 22.1.3
### Breaking Changes
- Updated Metrics service default port from 8546 to 9546.

### Features Added
- Updated various dependent libraries versions

---
## 22.1.0
### Features Added
- Updated Tuweni dependency to version 2.1.0 [#432](https://github.com/ConsenSys/ethsigner/pull/432)
- Updated Besu dependency to version 22.1.0 [#436](https://github.com/ConsenSys/ethsigner/pull/436)
---
## 21.10.9
### Breaking Changes
- Update EthSigner docker image user to use `ethsigner` instead of `root`. It may result in backward compatibility/permission issues with existing directory mounts.

### Bugs Fixed
- Update Vertx to 4.x and various other dependencies to their latest versions. [#415](https://github.com/ConsenSys/ethsigner/issues/415)

---
## 21.10.4
### Bugs Fixed
- Updated to log4j 2.17.1. Resolves two potential vulnerabilities which are only exploitable when using custom log4j configurations that are either writable by untrusted users or log data from the `ThreadContext`.

---
## 21.10.3
### Bugs Fixed
- Updated log4j to 2.17.0 to mitigate potential DOS vulnerability when the logging configuration uses a non-default Pattern Layout with a Context Lookup.

--- 

## 21.10.2
### Bugs Fixed
- Updated log4j to 2.16.0 to mitigate JNDI attack via thread context.

---

## 21.10.1
### Bugs Fixed
- Updated log4j and explicitly disabled format message lookups.

---
## 21.10.0
### Breaking Changes
- Upgrade to signers 1.0.19 removes support for deprecated SECP256K1 curve in Azure remote signing [#386](https://github.com/ConsenSys/ethsigner/pull/386)

### Bugs Fixed
- Adding configuration files should load new accounts automatically without restarting EthSigner [#390](https://github.com/ConsenSys/ethsigner/issues/390)
- eth_sign signing of hex data [#393](https://github.com/ConsenSys/ethsigner/issues/393)
- Upgrade web3j to latest version for fix to handle large chainids in eip155 transactions [#382](https://github.com/ConsenSys/ethsigner/pull/382)

### Features Added
- Add validation for GoQuorum transactions with value [#377](https://github.com/ConsenSys/ethsigner/pull/377)
- Add publishing to docker namespace "consensys/ethsigner" and deprecate docker namespace "consensys/quorum-ethsigner" [#384](https://github.com/ConsenSys/ethsigner/issues/384)
- Upgrade to signers 1.0.19 allows empty password files to be read when creating a Signer [#372](https://github.com/ConsenSys/ethsigner/issues/372)
- Upgrade besu to 21.10.0 for acceptance tests

## 21.3.2

### Bugs Fixed
- Fix nonce too low retries and added support for "replacement underpriced" and "Known transaction" RPC responses

## 21.3.1

### Features Added
- Update transaction signing to handle GoQuorum private transactions
- Azure remote signing now supports new Azure keys using the curve name P-256K and signature algorithm
  name ES256K. The deprecated keys using the SECP256K and signature algorithm name ECDSA256 are still supported.

### Bugs Fixed
- Fixing nonce too low retries and added support for replacement underpriced rpc response

## 21.3.0

### Features Added
- Upgraded besu-metrics library
- Publish ethsigner module jars to cloudsmith maven repo
- Update Besu latest version for Acceptance Tests
- Add gzip compression support

## 21.1.0

### Features Added
- Publish artifacts to [cloudsmith](https://cloudsmith.io/~consensys/repos/ethsigner).
- Added support for sending GoQuorum private transactions via "eth_sendTransaction" JSON-RPC
- Improve error message if HTTP server fails to start

## 20.10.0

### Features Added
- Added "eth_signTransaction" JSON-RPC
- Docker namespace updated to reflect "consensys/quorum-ethsigner"
- Strip ACCESS_CONTROL_ALLOW_ORIGIN header from responses received from the web3provider
- Added a Prometheus metrics endpoint, reporting basic application metrics

### Bugs Fixed
- Transactions were not being handled in parallel, triggering poor performance under load

## 0.7.1

### Features Added
- Support for using config file and environment variables as default values for cli options
- Updated signers library to the latest version
- Accessing Azure signing service requires tenant id as part of Azure configuration
- Communication details moved to Discord

### Bugs Fixed
- Prevent multiple transmission exceptions propagation upwards [#312](https://github.com/PegaSysEng/ethsigner/pull/312)
- Resolve failures in the application of CORS headers [#286](https://github.com/PegaSysEng/ethsigner/pull/286)

## 0.7.0

### Features Added
- Added "eth_sign" JSON RPC
- Added "--http-cors-origins" commandline option to allow browser based apps (remix/metamask) to connect to EthSigner
- Added "--downstream-http-path" commandline option to allow Ethsigner to connect to a downstream web3 provider not on root path (eg web3 provider running in infura)
- If inbound request contains the "Host" header, it is renamed to "X-Forwarded-Host" and added to downstream request
- Code base split, crypto operations moved to "Signers" [repository](https://github.com/PegaSysEng/signers)
- First line of Password file (stripping EOL) is treated as the password (rather than whole file content)

### Bugs Fixed
- Create invalid signature when Signature field was treated as negative BigInteger [#247](https://github.com/PegaSysEng/ethsigner/issues/247)

## 0.6.0

Changed CLI option name from `--downstream-http-tls-ca-auth-disabled` to `--downstream-http-tls-ca-auth-enabled` https://github.com/PegaSysEng/ethsigner/pull/230

## 0.5.0

### Features Added
- [Added TLS support for incoming and outgoing RPC endpoints](https://docs.ethsigner.pegasys.tech/en/latest/Concepts/TLS/)
- [Added TLS support for connecting to Hashicorp vault](https://docs.ethsigner.pegasys.tech/en/latest/Concepts/TLS/)
- Upgraded PicoCLI to 4.1.4

### Bugs Fixed 
- Received headers are now forwarded to the web3 provider, resolving an issue where JWT token was not being passed in header https://github.com/PegaSysEng/ethsigner/pull/208
- Resolved an issue where private transactions using privacyGroupId without a nonce failed https://github.com/PegaSysEng/ethsigner/pull/215

## 0.4.0

### Features Added
- Multi-key signing: Ethsigner is initialised with a directory containing a number of TOML metadata files, each of which describe a key which may be used for signing. Upon reception of a Transaction, Ethsigner loads the corresponding metadata file, and signs the Transaction with the key defined therein.
- Relaxed definition of 'optional' when parsing eth_SendTransaction (empty string, null an "0x" are deemed a missing optional parameter).
- All endpoints (not just "/") are proxied to the downstream web3j provider (eg. "/login")
- CI moved from Jenkins to CircleCI
- Updated to Web3j 4.5.5
- Updated to JUnit 5

### Bugs Fixed
- When a private transaction is submitted without a nonce, a nonce is generated and inserted. However, if the supplied nonce is too low, the transaction is not resubmitted with a new nonce. Rather an error is returned to the caller (resolved in Besu 1.2.5).
- Removed intermittent "out of memory" failure during integration testing.
- Resolved an issue whereby a missing optional field in eth_SendTransaction would fail

## 0.3.0

### Known Issues
- When a private transaction is submitted without a nonce, a nonce is generated and inserted. However, if the supplied nonce is too low, the transaction is not resubmitted with a new nonce. Rather an error is returned to the caller.

### Features Added
- Updated to use Web3j 4.5.0
- Accepts Private Transactions addressed with "PrivacyGroupId", not just "PrivateFor"

### Bugs Fixed
- Private Transactions without nonces are now accepted and the nonce populated (see "Known Issues")

## 0.2.0

### Known Issues
- When a private transaction is submitted without a nonce, then transaction will be rejected. Ethsigner is unable to derive an appropriate nonce for a private transaction, as such the `nonce` field of `eea_SendTransaction` is mandatory - if a private transaction is submitted without a nonce an error will be returned. DApps can use the [`priv_getTransactionCount`]( (https://docs.pantheon.pegasys.tech/en/latest/Reference/Pantheon-API-Methods/#priv_gettransactioncount)) JSON RPC to determine the correct nonce prior to transaction transmission.

### Breaking Changes
- Command line reworked to specify the source of the key used for transaction signing.
- EthSigner is supported on Java 11+ only; Java 8 is no longer supported.

### Features Added
- Created [EthSigner documentation](https://docs.ethsigner.pegasys.tech/en/latest/)
- Allow EthSigner to be deployed as a Docker image
- Support signing transaction with a key stored in an Azure KeyVault \(cloud based software/HSM signing service\) (thanks to [jimthematrix](https://github.com/jimthematrix))
- Added an Upcheck endpoint
- Support signing transactions with a key stored in a Hashicorp vault
- Sign private transaction submitted via eea_SendTransaction
- Jar files are available from the EthSigner bintray repository.

### Bugs Fixed
- N/A

