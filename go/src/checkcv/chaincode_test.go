/*
 * SPDX-License-Identifier: Apache-2.0
 */

package main

import (
	"encoding/json"
	"strings"
	"testing"

	"github.com/hyperledger/fabric/core/chaincode/shim"
	"github.com/stretchr/testify/assert"
)

func Test_GetAssetInvalidID(t *testing.T) {
	// Arrange
	scc := new(Chaincode)
	stub := shim.NewMockStub("", scc)

	// Act
	res := stub.MockInvoke(
		"1",
		[][]byte{
			[]byte("readStudent"),
			[]byte("1")},
	)

	// Assert
	assert.Equal(t, shim.ERROR, int(res.Status))
}

func Test_ReadStudent(t *testing.T) {
	// Create
	scc := new(Chaincode)
	stub := shim.NewMockStub("", scc)

	student := student{}
	student.HashStudent = "hash12345"
	student.School = "ESILV"
	student.SchoolStreet = "12 avenue Léonard de Vinci"
	student.SchoolZip = "92400"
	student.SchoolTown = "Courbevoie"

	studentAsBytes, _ := json.Marshal(student)

	stub.MockTransactionStart("CREATE STUDENT C.Pruvost")
	stub.PutState(student.HashStudent, studentAsBytes)

	// Read
	res := stub.MockInvoke(
		"1",
		[][]byte{
			[]byte("readStudent"),
			[]byte(student.HashStudent)},
	)

	// Assert
	assert.Equal(
		t,
		strings.Contains(string(res.Payload), "\"school\":\"ESILV\""),
		true,
	)
	assert.Equal(t, shim.OK, int(res.Status))
}

/*func TestInit(t *testing.T) {
	cc := new(Chaincode)
	stub := shim.NewMockStub("chaincode", cc)
	res := stub.MockInit("1", [][]byte{[]byte("initFunc")})
	if res.Status != shim.OK {
		t.Error("Init failed", res.Status, res.Message)
	}
}

func TestInvoke(t *testing.T) {
	cc := new(Chaincode)
	stub := shim.NewMockStub("chaincode", cc)
	res := stub.MockInit("1", [][]byte{[]byte("initFunc")})
	if res.Status != shim.OK {
		t.Error("Init failed", res.Status, res.Message)
	}
	res = stub.MockInvoke("1", [][]byte{[]byte("invokeFunc")})
	if res.Status != shim.OK {
		t.Error("Invoke failed", res.Status, res.Message)
	}
}*/
