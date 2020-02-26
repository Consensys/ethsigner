# Changelog

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

