package grpcclient

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"go.uber.org/zap"
)

func TestValidClientClose(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	client, err := NewEngineClient(65534, log.Sugar())
	assert.Nil(t, err)
	err = client.CloseConn()
	assert.Nil(t, err)
}

func TestMultipleClose(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	client, err := NewEngineClient(65534, log.Sugar())
	fmt.Println(client.Client())
	assert.Nil(t, err)
	err = client.CloseConn()
	assert.Nil(t, err)
	err = client.CloseConn()
	fmt.Println(err)
	assert.NotNil(t, err)
	assert.NotNil(t, client.Client())
}
