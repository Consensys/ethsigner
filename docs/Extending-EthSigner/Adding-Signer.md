description: Adding an external signer to EthSigner
<!--- END of page meta data -->

# Creating an External Signer for EthSigner

EthSigner supports the implementation of additional external signers in same way as Hashicorp Vault. 

To create an external signer: 

1. Clone the [EthSigner repository](https://github.com/PegaSysEng/ethsigner). 

1. Create a gradle module for the new signer in the `ethsigner/signer` directory. 

1. To ensure the module for the new signer is built, add the module to the `settings.gradle` file.

1. In the new module, create a concrete child class of `SignerSubCommand`.

    The new `SignerSubCommand` must include the parameters (tagged with PicoCLI annotations) to initialize your signer.

1. Update the `createSigner()` function to be the entry point to create and return your signer. 
   Your signer is exposed only as a `TransactionSigner`.

    !!! note
        The new signing class is responsible for fulfilling the `TransactionSigner` interface.  
        To fulfill the `TransactionSigner` interface, the class must be able to create a signature for 
        a block of bytes and provide the address associated with the key pair in the signer.

1. In `EthSignerApp.java`, register the new `SignerSubCommand` with the `cmdLineParser` as for existing signers.

1. Update the EthSigner::app module dependency list to include the module created in step 2.

