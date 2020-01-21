package main

import (
	"fmt"
	"strings"
	"testing"

	"github.com/hyperledger/fabric/core/chaincode/shim"
	"github.com/stretchr/testify/assert"
)

func Test_GetAssetInvalidID(t *testing.T) {
	// Arrange
	scc := new(SimpleChaincode)
	stub := shim.NewMockStub("", scc)

	// Act
	res := stub.MockInvoke(
		"1",
		[][]byte{
			[]byte("getAsset"),
			[]byte("1")},
	)

	// Assert
	assert.Equal(t, shim.ERROR, int(res.Status))
}

func Test_Query(t *testing.T) {
	// Arrange
	scc := new(SimpleChaincode)
	stub := shim.NewMockStub("", scc)
	//asset := Asset{}
	//asset.Name = "john"
	//assetAsBytes, _ := json.Marshal(asset)
	value1 := "100"
	stub.MockTransactionStart("CREATE A")
	stub.PutState("a", []byte(value1))

	// Act
	res := stub.MockInvoke(
		"1",
		[][]byte{
			[]byte("query"),
			[]byte("a")},
	)

	// Assert
	assert.Equal(
		t,
		//strings.Contains(string(res.Payload), "\"name\":\"John\""),
		strings.Contains(string(res.Payload), "100"),
		true,
	)
	assert.Equal(t, shim.OK, int(res.Status))
}

func Test_Invoke(t *testing.T) {

	scc := new(SimpleChaincode)
	stub := shim.NewMockStub("", scc)

	value1 := "100"
	value2 := "200"
	stub.MockTransactionStart("CREATE A")
	stub.PutState("a", []byte(value1))
	stub.MockTransactionStart("CREATE B")
	stub.PutState("b", []byte(value2))

	res := stub.MockInvoke(
		"1",
		[][]byte{
			[]byte("invoke"),
			[]byte("a"),
			[]byte("b"),
			[]byte("10")},
	)
	fmt.Println(res.Status)

	res2 := stub.MockInvoke(
		"1",
		[][]byte{
			[]byte("query"),
			[]byte("a")},
	)

	// Assert
	assert.Equal(
		t,
		//strings.Contains(string(res.Payload), "\"name\":\"John\""),
		strings.Contains(string(res2.Payload), "90"),
		true,
	)
	assert.Equal(t, shim.OK, int(res2.Status))
}
