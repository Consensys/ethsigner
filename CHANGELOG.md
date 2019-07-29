# Changelog

### Breaking Change 

In v0.2.0, the `nonce` field of `eea_SendTransaction` is mandatory (rather than optional as specified by the EEA standard). Use [`priv_getTransactionCount`]( (https://docs.pantheon.pegasys.tech/en/latest/Reference/Pantheon-API-Methods/#priv_gettransactioncount))
to calculate.

### Java 11

EthSigner is supported on Java 11+. Java 8 support is deprecated and will be removed in a future release.

## 0.2.0

### Additions and Improvements 

- [EthSigner documentation](https://docs.ethsigner.pegasys.tech/en/latest/)
- Docker image [\#112](https://github.com/PegaSysEng/ethsigner/pull/112) 
- Azure KeyVault \(cloud based software/HSM signing service\) signer support [\#109](https://github.com/PegaSysEng/ethsigner/pull/109) (thanks to [jimthematrix](https://github.com/jimthematrix))
- Adding an UpCheck endpoint [\#96](https://github.com/PegaSysEng/ethsigner/pull/96) 
- Hashi vault [\#87](https://github.com/PegaSysEng/ethsigner/pull/87) 
- EEA sendTransaction [\#78](https://github.com/PegaSysEng/ethsigner/pull/78) 
- Jar files are available from the EthSigner bintray repository.

### Technical Improvements 

- Change command line to use milliseconds instead of seconds
- Cmd line reworked with sub-commands
