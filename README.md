# Oracle BlockChain (Hyperledger) Cloud Service Workshop

This workshop guides you through the setup, and the configuration of an Oracle Blockchain Network (Hyperledger) in order to deploy a Chain Code (using Go language) and to be able to call it via Rest Service (using Postman) or via the Hyperledger Java SDK  .

The steps are :

- Create an Oracle Blockchain Network on Oracle Cloud Infrastructure (OCI).
- Create a Go Chain Code named 'balancetransfer" with VSCode and write unit tests.
- Deploy the Go Chain Code and expose it via REST nodes.
- Call the Chain Code via REST Service with POSTMAN.
- Call the Chain Code using the Java SDK and VSCode.
- Optional : Use Infra As Code to create Oracle Blockchain Network

Do not hesitate to use it and ask for enhancements if you have any ideas.

Remember that you need some prerequisites before doing that workshop :
- Be able to code using Go and Java.
- Be able to use git and github
- Be able to use Visual Studio Code (VSCode) with Java and Go.
- Be able to use POSTMAN (prefer standalone tool) in order to call REST API.
- Understand basics on Hyperledger (this workshop does not explain the Blockchain technology and Hyperledger).


## Table of Contents

1. Creation of the Blockchain Network (founder)  [create.md](docs/01-create.md), (participant)  [participant.md](docs/02-participant.md),(configurations)  [configuration.md](docs/03-configuration.md)
2. Deploy Chaincode [deploychaincode.md](docs/04-deploychaincode.md)
3. Invoke Chaincode via REST API [restapi.md](docs/05-restapi.md)
4. Invoke Chaincode via Java SDK [javasdk.md](docs/06-javasdk.md)
5. Optional : Use Infra As Code to create Oracle Blockchain Network [iac.md](docs/07-iac.md)
6. Optional : Use IBM Blockchain Platform Extension in VSCode [ibmbp.md](docs/08-ibmbp.md)

## Feedback

If you like this repository, do not hesitate to add a star. If you have any
questions or ideas to enhance it, open an issue. Have fun!