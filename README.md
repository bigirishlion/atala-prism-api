# Atala Prism REST API

## Install

    ./gradlew build

## Run the app

    ./gradlew run

## Run tests

    TODO: Add tests

## How to use:

1. Begin buy requesting the [/createIssuerSeedFile](#post-create-issuer-seed-file) endpoint. This will create a random seed file that will be used as the issuer to perform other actions.
2. Next, we'll need the holder did to create/verify/revoke credentials. Start by creating the seed file for the holder using the [/createHolderSeedFile](#post-create-holder-seed-file) endpoint and passing in `filename`.
3. Get the did by hitting the [/getHolderDid/:filename](#get-holder-did) endpoint. e.g. did:prism:e25665ac84b619a4a414888183016e510b498e0daecbd7c7a9c100a72dfb9fd5
4. Use the [/issueCredentials](#post-issue-credentials) endpoint using the Did from the previous step
5. The request is processing. To check the status, hit the [/operation/:operationId](#get-operation-status) endpoint to determine if the request has confirmed on-chain.
6. Once the credentials have been issues, we can verify the request by hitting [/verifyCredentials](#post-verify-credentials)

That's it! You have officially created credentials in Prism!

7. Once you've verified the credentials, try using the [/revokeCredentials](#post-revoke-credentials) endpoint to revoke the credentials and try the [/verifyCredentials](#post-verify-credentials) endpoint again to confirm that the credentials have been revoked.

# Did Endpoints

## Get Did

Get details about the Did.

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
                "value": [...]
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

## Get Issuer Did

Gets the Canonical Did and Long Form Did using the `seed` file

### Request

`GET /getIssuerDid`

    curl -i -H 'Accept: application/json' http://localhost:8484/getIssuerDid

### Response

    HTTP/1.1 200 OK
    Date: Thu, 24 Feb 2011 12:36:30 GMT
    Status: 200 OK
    Connection: close
    Content-Type: application/json
    Content-Length: 1361

    {
        "canonical": "did:prism:af1c10aee92c5c15d48e6ae02882fd28f89bc20c67c684f5b1d670c76932f751",
        "longForm": "did:prism:af1c10aee92c5c15d48e6ae02882fd28f89bc20c67c684f5b1d670c76932f751:Cr8BCrwBEjsKB21hc3RlcjAQAUouCglzZWNwMjU2azESIQNd2pTzFapP9ElpcAPkvUTgGm9ncXo2cPV0gzDNSupJYRI8Cghpc3N1aW5nMBACSi4KCXNlY3AyNTZrMRIhApzw32-EUabHTlvlCkEaShkXFLp5CFtRlz99Om4GXdt7Ej8KC3Jldm9jYXRpb24wEAVKLgoJc2VjcDI1NmsxEiEDThOfM0qhtHaWcPm4Bkdfb1BGDin7LMvbaaqU6lGCjwc"
    }

## Get Holder Did

Gets the Canonical Did and Long Form Did using the filename passed in as the seed file name.

This is helpful before issuing credentials.

### Request

`GET /getHolderDid/:filename`

    curl -i -H 'Accept: application/json' http://localhost:8484/getHolderDid/:filename

### Response

    HTTP/1.1 200 OK
    Date: Thu, 24 Feb 2011 12:36:30 GMT
    Status: 200 OK
    Connection: close
    Content-Type: application/json
    Content-Length: 1361

    {
        "canonical": "did:prism:af1c10aee92c5c15d48e6ae02882fd28f89bc20c67c684f5b1d670c76932f751",
        "longForm": "did:prism:af1c10aee92c5c15d48e6ae02882fd28f89bc20c67c684f5b1d670c76932f751:Cr8BCrwBEjsKB21hc3RlcjAQAUouCglzZWNwMjU2azESIQNd2pTzFapP9ElpcAPkvUTgGm9ncXo2cPV0gzDNSupJYRI8Cghpc3N1aW5nMBACSi4KCXNlY3AyNTZrMRIhApzw32-EUabHTlvlCkEaShkXFLp5CFtRlz99Om4GXdt7Ej8KC3Jldm9jYXRpb24wEAVKLgoJc2VjcDI1NmsxEiEDThOfM0qhtHaWcPm4Bkdfb1BGDin7LMvbaaqU6lGCjwc"
    }

## POST Publish Issuer Did

Publishes the issuer Did. This will likely only be called once unless you want to create multiple issuers

### Request

`POST /publishIssuerDid`

    curl -i -H 'Accept: application/json' http://localhost:8484/publishIssuerDid

### Response

    HTTP/1.1 200 OK
    Date: Thu, 24 Feb 2011 12:36:30 GMT
    Status: 200 OK
    Connection: close
    Content-Type: application/json
    Content-Length: 1361

    {
        "createDidOperationIdHex": "33297d02395f22a67f3013613259957680e716b3092a45eb970dcbd3319ecdde",
        "operationHash": "e87f7fd09ed4b8e3a0143900b4f1389bb735a82175445e9fee30c357f8782bd0"
    }

# Credentials Endpoints

## POST Issue Credentials

Issues credentials for a holder. Currently, this will create a credential claim using only `name` and `yearOfBirth`. This could be updated to your specific use case.

Return values include:

- `issueCredentialsOperationIdHex` - Used in the /operation/:operationId request to get the current status of the operation
- `operationHash` - Used to perform subsequent requests. For example, this parameter is needed to revoke the credentials
- `encodedSignedCredential` - Used to verify the credentials
- `proof` - Also used to verify the credentials
- `batchId` - Used to perform subsequent requests. For example, this parameter is needed to revoke the credentials

### Request

`POST /issueCredentials`

    curl -i -H 'Accept: application/json' -X POST -d 'holderDid=xxx&name=xxx&yearOfBirth=1950' http://localhost:8484/issueCredentials

### Response

    HTTP/1.1 200 OK
    Date: Thu, 24 Feb 2011 12:36:30 GMT
    Status: 200 OK
    Connection: close
    Content-Type: application/json
    Content-Length: 1361

    {
        "issueCredentialsOperationIdHex": "267c5ca5ad970d5401cff4529386f8cb4417c66a7cb686f4505c20982b37da50",
        "operationHash": "2c3828ee2fb437a142ff5fd726bf7f89e187dcc789d3c776eacbc18e8873444b",
        "encodedSignedCredential": "eyJpZCI6ImRpZDpwcmlzbTphZjFjMTBhZWU5MmM1YzE1ZDQ4ZTZhZTAyODgyZmQyOGY4OWJjMjBjNjdjNjg0ZjViMWQ2NzBjNzY5MzJmNzUxIiwia2V5SWQiOiJpc3N1aW5nMCIsImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im5hbWUiOiJUb20gQ3J1aXNlIiwieWVhck9mQmlydGgiOjE5NjcsImlkIjoiZGlkOnByaXNtOjJjOGUzNWIxNzVkZWU3MzhhODE2YThmN2NlNmQ2YTc4OTQzYWM5ZGVkNWUxOGFlZjY4ZjE0M2RhMzFmNDYxNDQifX0.MEUCID2WWYPLyDwKeQRNL3gj6FShFz1kW9jBezha8rCUg1Y1AiEAqN5tAzKeyMhgIVYV5wg82rtORy_cjQltz8cakBLZ4fE",
        "proof": "{\"hash\":\"fe2b768eb81bfd7bcc479ed6e3a824645c76cb1fd16b121244e8eb3d324b4ee3\",\"index\":0,\"siblings\":[]}",
        "batchId": "613d9d141a11a151b50c508dc9b69e333994cdb2ff7a074fcd61da68c8a1ba90",
        "signedCredentialHash": "fe2b768eb81bfd7bcc479ed6e3a824645c76cb1fd16b121244e8eb3d324b4ee3"
    }

## POST Verify Credentials

Verifies credentials using the `encodedSignedCredentials` and the `proof`

### Request

`POST /verifyCredentials`

    curl -i -H 'Accept: application/json' -X POST -d 'encodedSignedCredentials=xxx&proof=xxx' http://localhost:8484/verifyCredentials

### Response

    HTTP/1.1 200 OK
    Date: Thu, 24 Feb 2011 12:36:30 GMT
    Status: 200 OK
    Connection: close
    Content-Type: application/json
    Content-Length: 1361

    {
        "verified": true,
        "errors": [],
        "credentials": "details about the decoded credentials..."
    }

## POST Revoke Credentials

Revokes credentials

### Request

`POST /revokeCredentials`

    curl -i -H 'Accept: application/json' -X POST -d 'previousOperationHash=xxx&batchId=xxx&credentialHash=xxx' http://localhost:8484/revokeCredentials

### Response

    HTTP/1.1 200 OK
    Date: Thu, 24 Feb 2011 12:36:30 GMT
    Status: 200 OK
    Connection: close
    Content-Type: application/json
    Content-Length: 1361

    {
        "revokeCredentialsOperationIdHex": "33297d02395f22a67f3013613259957680e716b3092a45eb970dcbd3319ecdde",
        "operationHash": "e87f7fd09ed4b8e3a0143900b4f1389bb735a82175445e9fee30c357f8782bd0"
    }

# Helper Endpoints

## GET Operation Status

### Request

`GET /operation/:operationId`

    curl -i -H 'Accept: application/json' http://localhost:8484/operation/267c5ca5ad970d5401cff4529386f8cb4417c66a7cb686f4505c20982b37da50

### Response

    HTTP/1.1 200 OK
    Date: Thu, 24 Feb 2011 12:36:30 GMT
    Status: 200 OK
    Connection: close
    Content-Type: application/json
    Content-Length: 1361

    {
        "status": "CONFIRMED_AND_APPLIED"
    }

## GET Operation Status

### Request

`GET /transaction/:operationId`

    curl -i -H 'Accept: application/json' http://localhost:8484/transaction/267c5ca5ad970d5401cff4529386f8cb4417c66a7cb686f4505c20982b37da50

### Response

    HTTP/1.1 200 OK
    Date: Thu, 24 Feb 2011 12:36:30 GMT
    Status: 200 OK
    Connection: close
    Content-Type: application/json
    Content-Length: 1361

    {
        "transactionId": "f5136b17d34636b0e6148a427f4270ab88966ad547e6a813a1bcd37ac8121732"
    }

## POST Create Issuer Seed File

Creates an issuer seed file. This is probably a one-time helper endpoint to create an issuers using the master, issuer and revocation keys.

For security reasons, the request will fail if the `seed` file already exists.

### Request

`POST /createIssuerSeedFile`

    curl -i -H 'Accept: application/json' -X POST http://localhost:8484/createIssuerSeedFile

### Response

    HTTP/1.1 200 OK
    Date: Thu, 24 Feb 2011 12:36:30 GMT
    Status: 200 OK
    Connection: close
    Content-Type: application/json
    Content-Length: 1361

    {
        "status": "CREATED"
    }

## POST Create Holder Seed File

Creates an holder seed file.

For security reasons, the request will fail if the file already exists.

### Request

`POST /createHolderSeedFile`

    curl -i -H 'Accept: application/json' -X POST -d 'filename=seed-file-for-tom' http://localhost:8484/createHolderSeedFile

### Response

    HTTP/1.1 200 OK
    Date: Thu, 24 Feb 2011 12:36:30 GMT
    Status: 200 OK
    Connection: close
    Content-Type: application/json
    Content-Length: 1361

    {
        "status": "CREATED"
    }
