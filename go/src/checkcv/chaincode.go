/*
 * SPDX-License-Identifier: Apache-2.0

 Information from  go module. I deleted go module coming with the wizard of the IBM extension cause I got problems with it.
 	github.com/Knetic/govaluate v3.0.0+incompatible // indirect
	github.com/Shopify/sarama v1.25.0 // indirect
	github.com/fsouza/go-dockerclient v1.6.0 // indirect
	github.com/grpc-ecosystem/go-grpc-middleware v1.1.0 // indirect
	github.com/hashicorp/go-version v1.2.0 // indirect
	github.com/hyperledger/fabric v1.4.4
	github.com/hyperledger/fabric-amcl v0.0.0-20190902191507-f66264322317 // indirect
	github.com/miekg/pkcs11 v1.0.3 // indirect
	github.com/onsi/ginkgo v1.11.0 // indirect
	github.com/onsi/gomega v1.8.1 // indirect
	github.com/op/go-logging v0.0.0-20160315200505-970db520ece7 // indirect
	github.com/spf13/viper v1.5.0 // indirect
	github.com/sykesm/zap-logfmt v0.0.3 // indirect
	go.uber.org/zap v1.13.0 // indirect
	golang.org/x/crypto v0.0.0-20191117063200-497ca9f6d64f // indirect
	golang.org/x/net v0.0.0-20191116160921-f9c825593386 // indirect
	google.golang.org/grpc v1.25.1 // indirect
	gopkg.in/yaml.v2 v2.2.5 // indirect
)
*/

package main

import (
	"encoding/json"
	"fmt"

	"github.com/hyperledger/fabric/core/chaincode/shim"
	sc "github.com/hyperledger/fabric/protos/peer"
)

// Chaincode is the definition of the chaincode structure.
type Chaincode struct {
}

type student struct {
	ObjectType string `json:"docType"` //docType is used to distinguish the various types of objects in state database
	//pseudonym is mandatory cause GDPR
	HashStudent string `json:"hashStudent"`
	/*
		//Never use this kind of Data cause GDPR allows citizens to ask for deletion of their data and it is impossible with blockchain.
		FirstName    string `json:"firstName"`
		Name         string `json:"name"`
		BirthdayDate string `json:"birthdayDate"`
	*/
	School       string `json:"school"`
	SchoolStreet string `json:"schoolStreet"`
	SchoolZip    string `json:"schoolZip"`
	SchoolTown   string `json:"schoolTown"`
}

type diploma struct {
	ObjectType string `json:"docType"` //docType is used to distinguish the various types of objects in state database
	DiplomaID  string `json:"diplomaID"`
	Name       string `json:"name"` //actually it is not the name it is a pseudonym...impossible to use the name cause RGPD
	Owner      string `json:"owner"`
	Year       string `json:"year"`
	Mention    string `json:"mention"`
}

type note struct {
	ObjectType string `json:"docType"` //docType is used to distinguish the various types of objects in state database
	Diploma    string `json:"diploma"`
	Subject    string `json:"subject"`
	Note       string `json:"note"`
}

// Init is called when the chaincode is instantiated by the blockchain network.
func (cc *Chaincode) Init(stub shim.ChaincodeStubInterface) sc.Response {
	return shim.Success(nil)
}

// Invoke is called as a result of an application request to run the chaincode.
func (cc *Chaincode) Invoke(stub shim.ChaincodeStubInterface) sc.Response {
	fcn, params := stub.GetFunctionAndParameters()
	fmt.Println("Invoke()", fcn, params)

	// Handle different functions
	if fcn == "initStudent" { //create a new student
		return cc.initStudent(stub, params)
	} else if fcn == "readStudent" { //read a student
		return cc.readStudent(stub, params)
	}

	//return shim.Success(nil)
	fmt.Println("invoke did not find func: " + fcn) //error
	return shim.Error("Received unknown function invocation")
}

// ============================================================
// initStudent - create a new student , store into chaincode state
// ============================================================
func (cc *Chaincode) initStudent(stub shim.ChaincodeStubInterface, args []string) sc.Response {
	var err error

	// data model without recall fields
	//   student 		0       		1      		2     	   				3			4							  6			7
	// 				["hash12345", "ESILV", "12 avenue Léonard de Vinci", "92400", "Courbevoie"]

	// @MODIFY_HERE extend to expect 7 arguements
	if len(args) != 5 {
		return shim.Error("Incorrect number of arguments. Expecting 6")
	}

	// ==== Input sanitation ====
	fmt.Println("- start init student")
	if len(args[0]) <= 0 {
		return shim.Error("1st argument must be a non-empty string")
	}
	if len(args[1]) <= 0 {
		return shim.Error("2nd argument must be a non-empty string")
	}
	if len(args[2]) <= 0 {
		return shim.Error("3rd argument must be a non-empty string")
	}
	if len(args[3]) <= 0 {
		return shim.Error("4rd argument must be a non-empty string")
	}
	if len(args[4]) <= 0 {
		return shim.Error("5th argument must be a non-empty string")
	}

	hashStudent := args[0]
	school := args[1]
	schoolStreet := args[2]
	schoolZip := args[3]
	schoolTown := args[4]

	// ==== Check if student already exists ====
	studentAsBytes, err := stub.GetState(hashStudent)
	if err != nil {
		return shim.Error("Failed to get student: " + err.Error())
	} else if studentAsBytes != nil {
		return shim.Error("This student already exists: " + hashStudent)
	}

	// @MODIFY_HERE parts recall fields
	// ==== Create vehicle object and marshal to JSON ====
	objectType := "student"
	student := &student{objectType, hashStudent, school, schoolStreet, schoolZip, schoolTown}
	studentJSONasBytes, err := json.Marshal(student)
	if err != nil {
		return shim.Error(err.Error())
	}

	// === Save student to state ===
	err = stub.PutState(hashStudent, studentJSONasBytes)
	if err != nil {
		return shim.Error(err.Error())
	}
	//  ==== Index the students parts to enable assember & owner-based range queries, e.g. return all tata parts ====
	//  An 'index' is a normal key/value entry in state.
	//  The key is a composite key, with the elements that you want to range query on listed first.
	//  In our case, the composite key is based on indexName~assember~chassisNumber.
	//  This will enable very efficient state range queries based on composite keys matching indexName~color~*
	indexName := "student-ST"

	err = cc.createIndex(stub, indexName, []string{student.School, student.SchoolTown})
	if err != nil {
		return shim.Error(err.Error())
	}

	// ==== student part saved and indexed. Return success ====
	fmt.Println("- end init student")
	return shim.Success(nil)
}

// ===============================================
// readStudent- read a student from chaincode state
// ===============================================
func (cc *Chaincode) readStudent(stub shim.ChaincodeStubInterface, args []string) sc.Response {
	var hashStudent, jsonResp string
	var err error

	if len(args) != 1 {
		return shim.Error("Incorrect number of arguments. Expecting Hash Student to query")
	}

	hashStudent = args[0]
	valAsbytes, err := stub.GetState(hashStudent) //get the student from chaincode state
	if err != nil {
		jsonResp = "{\"Error\":\"Failed to get state for " + hashStudent + "\"}"
		return shim.Error(jsonResp)
	} else if valAsbytes == nil {
		jsonResp = "{\"Error\":\"Student does not exist: " + hashStudent + "\"}"
		return shim.Error(jsonResp)
	}

	return shim.Success(valAsbytes)
}

// ===============================================
// createIndex - create search index for ledger
// ===============================================
func (cc *Chaincode) createIndex(stub shim.ChaincodeStubInterface, indexName string, attributes []string) error {
	fmt.Println("- start create index")
	var err error
	//  ==== Index the object to enable range queries, e.g. return all parts made by supplier b ====
	//  An 'index' is a normal key/value entry in state.
	//  The key is a composite key, with the elements that you want to range query on listed first.
	//  This will enable very efficient state range queries based on composite keys matching indexName~color~*
	indexKey, err := stub.CreateCompositeKey(indexName, attributes)
	if err != nil {
		return err
	}
	//  Save index entry to state. Only the key name is needed, no need to store a duplicate copy of object.
	//  Note - passing a 'nil' value will effectively delete the key from state, therefore we pass null character as value
	value := []byte{0x00}
	stub.PutState(indexKey, value)

	fmt.Println("- end create index")
	return nil
}
