# Oracle BlockChain Cloud Service Workshop

This workshop guides you through the setup, and the configuration of an Oracle Blockchain Network (Hyperledger) in order to deploy a Chain Code (using Go language) and to be able to call it via Rest Service (using Postman) or via the Hyperledger Java SDK  .

The steps are :

- Create an Oracle Blockchain Network on Oracle Cloud Infrastructure (OCI).
- Create a Go Chain Code named 'balancetransfer" with VSCode and write unit tests.
- Deploy the Go Chain Code and expose it via REST nodes.
- Call the Chain Code via REST Service with Postman
- Call the Chain Code using the Java SDK and VSCode.

Do not hesitate to use it and ask for enhancements if you have any ideas.

Remember that you need some prerequisites before doing that workshop :
- Be able to code using Go and Java.
- Be able to use Visual Studio Code (VSCode) with Java and Go.
- Understand basics on Hyperledger (this workshop does not explain the Blockchain technology and Hyperledger).


## Table of Contents

1. Creation of the Blockchain Network  [01-create.md](docs/01-create.md)
2. Configuration of `terraform`[02-terraform.md](docs/02-terraform.md)
3. Create a Database Instance on OCI in `Developer Cloud Service` [03-devcs-terraform.md](docs/03-devcs-terraform.md)
4. Patch a Database Instance on OCI in `Developer Cloud Service` [04-devcs-patch.md](docs/04-devcs-patch.md)
5. Create the schema QUIZFLYWAY on OCI in `Developer Cloud Service` [05-devcs-schema.md](docs/05-devcs-schema.md)
6. Create the pipeline in `Developer Cloud Service` [06-pipeline.md](docs/06-pipeline.md)
7. Connect to the schema with `Oracle SQL Developer`  [07-sqldev.md](docs/07-sqldev.md)
8. Option : remove the Database if needed  [08-deldb.md](docs/08-deldb.md)

Note : After this workshop you can do the workhop [Create all the objects of the schema QUIZFLYWAY with Flyway](https://github.com/cpruvost/devopsdbflyway) 

## Feedback

If you like this repository, do not hesitate to add a star. If you have any
questions or ideas to enhance it, open an issue. Have fun!