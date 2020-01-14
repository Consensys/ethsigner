tech.pegasys.ethsigner.tests.dsl.hashicorp.HashicorpVaultCerts defines PEM certificates which are used by Hashicorp
Vault in AT. The truststore (PKCS12) is required by hashicorp-signer to trust the above self-signed certificate. It is
created using following command:

keytool -importcert -storetype PKCS12 -keystore hashicorp_truststore.pfx \
  -storepass changeit -alias ca -file vault.crt -noprompt