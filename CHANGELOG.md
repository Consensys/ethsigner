# Changelog


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

