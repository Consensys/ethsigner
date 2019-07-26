# Changelog

### Breaking Change 

In v0.2.0, `nonce` is required for `eea_sendTransaction`. Use [`priv_getTransactionCount`]( (https://docs.pantheon.pegasys.tech/en/latest/Reference/Pantheon-API-Methods/#priv_gettransactioncount))
to calculate.

### Java 11

EthSigner is supported on Java 11+. Java 8 support is deprecated and will be removed in a future release.