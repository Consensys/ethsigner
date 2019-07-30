# Changelog

## 0.2.0

### Known Issues
- Ethsigner is unable to derive an appropriate nonce for a private transaction, as such the `nonce` field of `eea_SendTransaction` is mandatory - if a private transaction is submitted without a nonce an error will be returned. DApps can use the [`priv_getTransactionCount`]( (https://docs.pantheon.pegasys.tech/en/latest/Reference/Pantheon-API-Methods/#priv_gettransactioncount)) JSON RPC to determine the correct nonce prior to transaction transmission.

### Breaking Changes
- Change command line to use milliseconds instead of seconds
- Cmd line reworked with sub-commands
- EthSigner is supported on Java 11+. Java 8 support is deprecated and will be removed in a future release.

### Features Added
- Created [EthSigner documentation](https://docs.ethsigner.pegasys.tech/en/latest/)
- Allow EthSigner to be deployed as a Docker image
- Support signing transaction with keys stored in an Azure KeyVault \(cloud based software/HSM signing service\) (thanks to [jimthematrix](https://github.com/jimthematrix))
- Added an Upcheck endpoint
- Support signing transactions stored in a Hashicorp vault
- Sign private transaction submitted via eea_SendTransaction
- Jar files are available from the EthSigner bintray repository.

### Bugs Fixed
- N/A

