package api

import io.iohk.atala.prism.api.CredentialClaim
import io.iohk.atala.prism.api.KeyGenerator
import io.iohk.atala.prism.api.models.AtalaOperationId
import io.iohk.atala.prism.api.models.AtalaOperationStatus
import io.iohk.atala.prism.api.node.NodeAuthApiImpl
import io.iohk.atala.prism.api.node.NodePayloadGenerator
import io.iohk.atala.prism.common.PrismSdkInternal
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.crypto.MerkleInclusionProof
import io.iohk.atala.prism.crypto.Sha256Digest
import io.iohk.atala.prism.crypto.derivation.KeyDerivation
import io.iohk.atala.prism.crypto.keys.ECKeyPair
import io.iohk.atala.prism.identity.*
import io.iohk.atala.prism.protos.GetOperationInfoRequest
import io.iohk.atala.prism.protos.GrpcClient
import io.iohk.atala.prism.protos.GrpcOptions
import io.iohk.atala.prism.protos.NodeServiceCoroutine
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import pbandk.ByteArr

val environment = "ppp.atalaprism.io"
val grpcOptions = GrpcOptions("https", environment, 50053)
val nodeAuthApi = NodeAuthApiImpl(grpcOptions)

class HolderSeedFile(val filename: String? = "holderSeed")

// Generates a seed file that can be downloaded and stored for making subsiquent requests.
fun createSeedFile(filename: String? = "seed") {
    val seed = KeyDerivation.binarySeed(KeyDerivation.randomMnemonicCode(), "passphrase")
    File(filename).writeBytes(seed)
    println("wrote seed to file to $filename")
}

fun readSeedFile(filename: String? = "seed"): ByteArray {
    val seed =
            try {
                File(filename).readBytes()
            } catch (e: Exception) {
                throw Exception("Unable to read seed file")
            }

    return seed
}

class IssuerKeys(
        val issuerMasterKeyPair: ECKeyPair,
        val issuerIssuingKeyPair: ECKeyPair,
        val issuerRevocationKeyPair: ECKeyPair
)

fun getIssuerKeyPairs(): IssuerKeys {
    val seed = readSeedFile()
    val issuerMasterKeyPair = KeyGenerator.deriveKeyFromFullPath(seed, 0, MasterKeyUsage, 0)
    val issuerIssuingKeyPair = KeyGenerator.deriveKeyFromFullPath(seed, 0, IssuingKeyUsage, 0)
    val issuerRevocationKeyPair = KeyGenerator.deriveKeyFromFullPath(seed, 0, RevocationKeyUsage, 0)

    return IssuerKeys(issuerMasterKeyPair, issuerIssuingKeyPair, issuerRevocationKeyPair)
}

fun buildIssuerLongFormDid(): LongFormPrismDid {
    val issuerKeys = getIssuerKeyPairs()
    val issuerUnpublishedDid =
            PrismDid.buildExperimentalLongFormFromKeys(
                    issuerKeys.issuerMasterKeyPair.publicKey,
                    issuerKeys.issuerIssuingKeyPair.publicKey,
                    issuerKeys.issuerRevocationKeyPair.publicKey
            )
    return issuerUnpublishedDid
}

fun buildHolderLongFormDid(filename: String?): LongFormPrismDid {
    val holderSeed = readSeedFile(filename ?: "holderSeed")
    val holderMasterKeyPair = KeyGenerator.deriveKeyFromFullPath(holderSeed, 0, MasterKeyUsage, 0)
    val holderUnpublishedDid =
            PrismDid.buildLongFormFromMasterPublicKey(holderMasterKeyPair.publicKey)
    return holderUnpublishedDid
}

class UnpublishedDidResult(val canonical: String, val longForm: String)

@PrismSdkInternal
fun getIssuerDid(): UnpublishedDidResult {
    val issuerUnpublishedDid = buildIssuerLongFormDid()

    val issuerDidCanonical = issuerUnpublishedDid.asCanonical().did
    val issuerDidLongForm = issuerUnpublishedDid.did

    return UnpublishedDidResult("$issuerDidCanonical", "$issuerDidLongForm")
}

fun getHolderDid(filename: String?): UnpublishedDidResult {
    val holderUnpublishedDid = buildHolderLongFormDid(filename)

    val holderDidCanonical = holderUnpublishedDid.asCanonical().did
    val holderDidLongForm = holderUnpublishedDid.did

    return UnpublishedDidResult("$holderDidCanonical", "$holderDidLongForm")
}

fun getDidDocument(didString: String?): PrismDidDataModel {
    if (didString == null) {
        throw Exception("Did not specify a did")
    }

    val did =
            try {
                Did.fromString(didString)
            } catch (e: Exception) {
                throw Exception("illegal DID: $didString")
            }
    val prismDid =
            try {
                PrismDid.fromDid(did)
            } catch (e: Exception) {
                throw Exception("not a Prism DID: $did")
            }

    try {
        val model = runBlocking { nodeAuthApi.getDidDocument(prismDid) }
        println(model.didData.publicKeys.size)
        println(model.didData.didDataModel)
        return model.didData
    } catch (e: Exception) {
        throw Exception("unknown prism DID")
    }
}

class PublishDidResult(val createDidOperationIdHex: String, val operationHash: String)

fun publishIssuerDid(): PublishDidResult {
    val issuerUnpublishedDid = buildIssuerLongFormDid()
    var issuerKeys = getIssuerKeyPairs()

    var nodePayloadGenerator =
            NodePayloadGenerator(
                    issuerUnpublishedDid,
                    mapOf(
                            PrismDid.DEFAULT_MASTER_KEY_ID to
                                    issuerKeys.issuerMasterKeyPair.privateKey,
                            PrismDid.DEFAULT_ISSUING_KEY_ID to
                                    issuerKeys.issuerIssuingKeyPair.privateKey,
                            PrismDid.DEFAULT_REVOCATION_KEY_ID to
                                    issuerKeys.issuerRevocationKeyPair.privateKey
                    )
            )
    val createDidInfo = nodePayloadGenerator.createDid()
    val createDidOperationId = runBlocking {
        nodeAuthApi.createDid(
                createDidInfo.payload,
                issuerUnpublishedDid,
                PrismDid.DEFAULT_MASTER_KEY_ID
        )
    }

    return PublishDidResult(
            "${createDidOperationId.hexValue()}",
            "${createDidInfo.operationHash.hexValue}"
    )
}

class IssueCredentials(val holderDid: String, val name: String, val yearOfBirth: Int)

class IssueCredentialsResult(
        val issueCredentialsOperationIdHex: String,
        val operationHash: String,
        val encodedSignedCredential: String,
        val proof: String,
        val batchId: String,
        val signedCredentialHash: String
)

@PrismSdkInternal
fun issueCredentials(credentials: IssueCredentials): IssueCredentialsResult {
    val issuerUnpublishedDid = buildIssuerLongFormDid()
    val issuerKeys = getIssuerKeyPairs()

    val nodePayloadGenerator =
            NodePayloadGenerator(
                    issuerUnpublishedDid,
                    mapOf(
                            PrismDid.DEFAULT_ISSUING_KEY_ID to
                                    issuerKeys.issuerIssuingKeyPair.privateKey
                    )
            )

    var holderDid = PrismDid.fromString(credentials.holderDid)

    val credentialClaim =
            CredentialClaim(
                    subjectDid = holderDid,
                    content =
                            JsonObject(
                                    mapOf(
                                            Pair("name", JsonPrimitive(credentials.name)),
                                            Pair(
                                                    "yearOfBirth",
                                                    JsonPrimitive(credentials.yearOfBirth)
                                            )
                                    )
                            )
            )

    val credentialsInfo =
            nodePayloadGenerator.issueCredentials(
                    PrismDid.DEFAULT_ISSUING_KEY_ID,
                    arrayOf(credentialClaim)
            )

    val issueCredentialsOperationId = runBlocking {
        nodeAuthApi.issueCredentials(
                credentialsInfo.payload,
                issuerUnpublishedDid.asCanonical(),
                PrismDid.DEFAULT_ISSUING_KEY_ID,
                credentialsInfo.merkleRoot
        )
    }

    val batchId = credentialsInfo.batchId.id
    val firstCredential = credentialsInfo.credentialsAndProofs.first()
    val holderSignedCredentialContent = firstCredential.signedCredential.canonicalForm
    val proof = firstCredential.inclusionProof.encode()
    val signedCredentialHash = firstCredential.signedCredential.hash().hexValue

    return IssueCredentialsResult(
            "${issueCredentialsOperationId.hexValue()}",
            "${credentialsInfo.operationHash.hexValue}",
            "$holderSignedCredentialContent",
            "$proof",
            "$batchId",
            "$signedCredentialHash"
    )
}

class VerifyCredentials(val encodedSignedCredential: String, val proof: String)

class VerifyCredentialsResult(
        val verified: Boolean,
        val errors: List<String>,
        val credentials: JsonBasedCredential
)

@PrismSdkInternal
fun verifyCredentials(args: VerifyCredentials): VerifyCredentialsResult {
    val credential = JsonBasedCredential.fromString(args.encodedSignedCredential)
    val proof = MerkleInclusionProof.decode(args.proof)

    val result = runBlocking { nodeAuthApi.verify(credential, proof) }

    val hasErrors = result.verificationErrors.isNotEmpty()
    val errors = result.verificationErrors.map { it.errorMessage }

    return VerifyCredentialsResult(!hasErrors, errors, credential)
}

class RevokeCredentials(
        val previousOperationHash: String,
        val batchId: String,
        val credentialHash: String
)

class RevokeCredentialsResults(
        val revokeCredentialsOperationIdHex: String,
        val operationHash: String
)

fun revokeCredentials(revokeCredentials: RevokeCredentials): RevokeCredentialsResults {
    val issuerUnpublishedDid = buildIssuerLongFormDid()
    val issuerKeys = getIssuerKeyPairs()

    val nodePayloadGenerator =
            NodePayloadGenerator(
                    issuerUnpublishedDid,
                    mapOf(
                            PrismDid.DEFAULT_REVOCATION_KEY_ID to
                                    issuerKeys.issuerRevocationKeyPair.privateKey
                    )
            )

    val oldHash = Sha256Digest.fromHex(revokeCredentials.previousOperationHash)
    val credentials = Sha256Digest.fromHex(revokeCredentials.credentialHash)
    val credentialBatchId =
            try {
                CredentialBatchId.fromString(revokeCredentials.batchId)!!
            } catch (e: Exception) {
                throw Exception("Invalid batch id")
            }

    val revokeInfo =
            nodePayloadGenerator.revokeCredentials(
                    PrismDid.DEFAULT_REVOCATION_KEY_ID,
                    oldHash,
                    credentialBatchId.id,
                    arrayOf(credentials)
            )

    val revokeOperationId = runBlocking {
        nodeAuthApi.revokeCredentials(
                revokeInfo.payload,
                issuerUnpublishedDid.asCanonical(),
                PrismDid.DEFAULT_REVOCATION_KEY_ID,
                oldHash,
                credentialBatchId.id,
                arrayOf(credentials)
        )
    }

    return RevokeCredentialsResults(
            "${revokeOperationId.hexValue()}",
            "${revokeInfo.operationHash.hexValue}"
    )
}

fun pollOperation(operationIdHex: String?): String {
    if (operationIdHex == null) {
        throw Exception("Did not specify an operation id")
    }

    var operationId = AtalaOperationId.fromHex(operationIdHex)
    var status = runBlocking { nodeAuthApi.getOperationStatus(operationId) }

    return AtalaOperationStatus.asString(status)
}

@PrismSdkInternal
fun transactionId(operationIdHex: String?): String {
    if (operationIdHex == null) {
        throw Exception("Did not specify an operation id")
    }

    var operationId = AtalaOperationId.fromHex(operationIdHex)
    val node = NodeServiceCoroutine.Client(GrpcClient(grpcOptions))
    val response = runBlocking {
        node.GetOperationInfo(GetOperationInfoRequest(ByteArr(operationId.value())))
    }
    return response.transactionId
}

@PrismSdkInternal
fun main() {
    embeddedServer(Netty, port = 8484) {
                install(ContentNegotiation) { gson {} }
                routing {
                    get("/") { call.respondText("Welcome to the Prism API") }

                    // Did resolvers
                    get("/did/{didString}") {
                        var didString = call.parameters["didString"]
                        var result = getDidDocument(didString)
                        call.respond(result)
                    }
                    get("/getIssuerDid") {
                        try {
                            val result = getIssuerDid()
                            call.respond(result)
                        } catch (e: Exception) {
                            call.response.status(HttpStatusCode(400, "Bad Request"))
                            call.respond(mapOf("error" to "Error creating DID"))
                        }
                    }
                    get("/geHolderDid/{filename}") {
                        try {
                            var filename = call.parameters["filename"]
                            val result = getHolderDid(filename)
                            call.respond(result)
                        } catch (e: Exception) {
                            call.response.status(HttpStatusCode(400, "Bad Request"))
                            call.respond(mapOf("error" to "Error creating DID"))
                        }
                    }
                    post("/publishIssuerDid") {
                        try {
                            val result = publishIssuerDid()
                            call.respond(result)
                        } catch (e: Exception) {
                            call.response.status(HttpStatusCode(400, "Bad Request"))
                            call.respond(mapOf("error" to "Error publishing DID"))
                        }
                    }

                    // Credential resolvers
                    post("/issueCredentials") {
                        try {
                            val args = call.receive<IssueCredentials>()
                            val result = issueCredentials(args)
                            call.respond(result)
                        } catch (e: Exception) {
                            call.response.status(HttpStatusCode(400, "Bad Request"))
                            call.respond(mapOf("error" to "Error verifying credentials"))
                        }
                    }
                    post("/verifyCredentials") {
                        try {
                            val args = call.receive<VerifyCredentials>()
                            val result = verifyCredentials(args)
                            call.respond(result)
                        } catch (e: Exception) {
                            println(e)
                            call.response.status(HttpStatusCode(400, "Bad Request"))
                            call.respond(mapOf("error" to "Error verifying credentials"))
                        }
                    }
                    post("/revokeCredentials") {
                        try {
                            val args = call.receive<RevokeCredentials>()
                            val result = revokeCredentials(args)
                            call.respond(result)
                        } catch (e: Exception) {
                            call.response.status(HttpStatusCode(400, "Bad Request"))
                            call.respond(mapOf("error" to "Error revoking credentials"))
                        }
                    }

                    // Helper resolvers
                    get("/operation/{operationId}") {
                        try {
                            val operationId = call.parameters["operationId"]
                            val result = pollOperation(operationId)
                            call.respond(mapOf("status" to result))
                        } catch (e: Exception) {
                            call.response.status(HttpStatusCode(400, "Bad Request"))
                            call.respond(mapOf("error" to "Error polling operation"))
                        }
                    }
                    get("/transaction/{operationId}") {
                        try {
                            val operationId = call.parameters["operationId"]
                            val result = transactionId(operationId)
                            call.respond(mapOf("transactionId" to result))
                        } catch (e: Exception) {
                            print("$e")
                            call.response.status(HttpStatusCode(400, "Bad Request"))
                            call.respond(mapOf("error" to "Error getting transaction id"))
                        }
                    }
                    post("/createIssuerSeedFile") {
                        try {
                            createSeedFile()
                            call.respond(mapOf("status" to "created issuer seed file"))
                        } catch (e: Exception) {
                            call.respond(mapOf("status" to "error"))
                        }
                    }
                    post("/createHolderSeedFile") {
                        try {
                            val args = call.receive<HolderSeedFile>()
                            createSeedFile(args.filename)
                            call.respond(mapOf("status" to "created holder seed file"))
                        } catch (e: Exception) {
                            call.respond(mapOf("status" to "error"))
                        }
                    }
                }
            }
            .start(wait = true)
}
