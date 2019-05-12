description: Install EthSigner from binary distribution
<!--- END of page meta data -->

# Install Binary Distribution

## Prerequisites

* [Java JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html)

!!!important
    EthSigner requires Java 8+ to compile; earlier versions are not supported.
    
## Install Binaries

Download the EthSigner [packaged binaries](https://bintray.com/consensys/pegasys-repo/ethsigner/_latestVersion#files).

Unpack the downloaded files and change into the `ethsigner-<release>` directory. 

Display EthSigner command line help to confirm installation: 

```bash tab="Linux/macOS"
$ bin/ethsigner --help
```

```bat tab="Windows"
bin\ethsigner --help
```