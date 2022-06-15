## Install

    ./gradlew build

## Run the app

    ./gradlew run

## Run the tests

    TODO: Add tests

## Get list of Things

### Request

`GET /did/:didString`

    curl -i -H 'Accept: application/json' http://localhost:8484/did/did:prism:af1c10aee92c5c15d48e6ae02882fd28f89bc20c67c684f5b1d670c76932f751

### Response

    HTTP/1.1 200 OK
    Date: Thu, 24 Feb 2011 12:36:30 GMT
    Status: 200 OK
    Connection: close
    Content-Type: application/json
    Content-Length: 1361

    {
        "did": {
            "did": {
                "method": {
                    "value": "prism"
                },
                "methodSpecificId": {
                    "value": "af1c10aee92c5c15d48e6ae02882fd28f89bc20c67c684f5b1d670c76932f751",
                    "sections": [
                        "af1c10aee92c5c15d48e6ae02882fd28f89bc20c67c684f5b1d670c76932f751"
                    ]
                }
            },
            "stateHash": {
                "value": [
                    -81,
                    28,
                    16,
                    -82,
                    -23,
                    44,
                    92,
                    21,
                    -44,
                    -114,
                    106,
                    -32,
                    40,
                    -126,
                    -3,
                    40,
                    -8,
                    -101,
                    -62,
                    12,
                    103,
                    -58,
                    -124,
                    -11,
                    -79,
                    -42,
                    112,
                    -57,
                    105,
                    50,
                    -9,
                    81
                ]
            }
        },
        "publicKeys": [
            {
                "didPublicKey": {
                    "id": "issuing0",
                    "usage": {},
                    "publicKey": {
                        "key": {
                            "algorithm": "EC",
                            "withCompression": false
                        }
                    }
                },
                "addedOn": {
                    "transactionId": "9f73b39df01cc39bc8133f80b630b00a483113b02ef573e0bd28750e95e42e37",
                    "ledger": 4,
                    "timestampInfo": {
                        "atalaBlockTimestamp": 1655240968000,
                        "atalaBlockSequenceNumber": 20,
                        "operationSequenceNumber": 0
                    }
                }
            },
            {
                "didPublicKey": {
                    "id": "master0",
                    "usage": {},
                    "publicKey": {
                        "key": {
                            "algorithm": "EC",
                            "withCompression": false
                        }
                    }
                },
                "addedOn": {
                    "transactionId": "9f73b39df01cc39bc8133f80b630b00a483113b02ef573e0bd28750e95e42e37",
                    "ledger": 4,
                    "timestampInfo": {
                        "atalaBlockTimestamp": 1655240968000,
                        "atalaBlockSequenceNumber": 20,
                        "operationSequenceNumber": 0
                    }
                }
            },
            {
                "didPublicKey": {
                    "id": "revocation0",
                    "usage": {},
                    "publicKey": {
                        "key": {
                            "algorithm": "EC",
                            "withCompression": false
                        }
                    }
                },
                "addedOn": {
                    "transactionId": "9f73b39df01cc39bc8133f80b630b00a483113b02ef573e0bd28750e95e42e37",
                    "ledger": 4,
                    "timestampInfo": {
                        "atalaBlockTimestamp": 1655240968000,
                        "atalaBlockSequenceNumber": 20,
                        "operationSequenceNumber": 0
                    }
                }
            }
        ]
    }
