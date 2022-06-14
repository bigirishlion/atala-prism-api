package api

import io.iohk.atala.prism.api.CredentialClaim
import io.iohk.atala.prism.api.KeyGenerator
import io.iohk.atala.prism.api.models.AtalaOperationId
import io.iohk.atala.prism.api.models.AtalaOperationStatus
import io.iohk.atala.prism.api.node.NodeAuthApiImpl
import io.iohk.atala.prism.api.node.NodePayloadGenerator
import io.iohk.atala.prism.api.node.NodePublicApi
import io.iohk.atala.prism.common.PrismSdkInternal
import io.iohk.atala.prism.crypto.derivation.KeyDerivation
import io.iohk.atala.prism.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.crypto.MerkleInclusionProof
import io.iohk.atala.prism.identity.*
import io.iohk.atala.prism.protos.GetOperationInfoRequest
import io.iohk.atala.prism.protos.GrpcClient
import io.iohk.atala.prism.protos.GrpcOptions
import io.iohk.atala.prism.protos.NodeServiceCoroutine
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.plugins.contentnegotiation.*

import pbandk.ByteArr
import java.io.*

val environment = "ppp.atalaprism.io"
val grpcOptions = GrpcOptions("https", environment, 50053)
val nodeAuthApi = NodeAuthApiImpl(grpcOptions)

// Generates a seed file that can be downloaded and stored for making subsiquent requests.
fun generateSeedFile() {
    val seed = KeyDerivation.binarySeed(KeyDerivation.randomMnemonicCode(), "passphrase")
    File("seed").writeBytes(seed)
    println("wrote seed to file")
}

class UnpublishedDidResult(val canonical: String, val longForm: String)

// Create a DidDocument from a seed file.
fun createUnpublishedDid(): UnpublishedDidResult {
    print("reading seed");
    val seed = try { File("seed").readBytes()  } catch (e: Exception) { throw Exception("Unable to read seed file") }

    val masterKeyPair = KeyGenerator.deriveKeyFromFullPath(seed, 0, MasterKeyUsage, 0)
    val unpublishedDid = PrismDid.buildLongFormFromMasterPublicKey(masterKeyPair.publicKey)

    val didCanonical = unpublishedDid.asCanonical().did
    val didLongForm = unpublishedDid.did

    println("canonical: $didCanonical")
    println("long form: $didLongForm")
    println()

    return UnpublishedDidResult("$didCanonical", "$didLongForm")
}

class PublishDidResult(val createDidOperationIdHex: String, val operationHash: String)

fun publishDid(did: String) {
    // convert did string to PrismDid
    val unpublishedDid = PrismDid.fromDid(did)

    var nodePayloadGenerator = NodePayloadGenerator(
            unpublishedDid,
            mapOf(PrismDid.DEFAULT_MASTER_KEY_ID to masterKeyPair.privateKey))
    val createDidInfo = nodePayloadGenerator.createDid()
    val createDidOperationId = runBlocking {
            nodeAuthApi.createDid(
                createDidInfo.payload,
                unpublishedDid,
                PrismDid.DEFAULT_MASTER_KEY_ID)
    }

    return PublishDidResult("${createDidOperationId.hexValue()}", "${createDidInfo.operationHash.hexValue}")
}

fun getDid(didString: String?): Boolean {
    if (didString == null) {
        throw Exception("Did not specify a did")
    }

    val did = try { Did.fromString(didString) } catch (e: Exception) { throw Exception("illegal DID: $didString") }
    val prismDid = try { PrismDid.fromDid(did) } catch (e: Exception) { throw Exception("not a Prism DID: $did") }

    println("trying to retrieve document for $did")
    try {
        val model = runBlocking { nodeAuthApi.getDidDocument(prismDid) }
        println(model.didData.publicKeys.size)
        println(model.didData.didDataModel)
        return true
    } catch (e: Exception) {
        println("unknown prism DID")
        return false
    }
}

fun addCredentials() {
    throw Exception("Not implemented");
}

class VerifyCredentials(val encodedSignedCredential: String, val proof: String)

@PrismSdkInternal
fun verifyCredentials(args: VerifyCredentials) {
    val credential = JsonBasedCredential.fromString(args.encodedSignedCredential)
    val proof = MerkleInclusionProof.decode(args.proof)

    val result = runBlocking {
            nodeAuthApi.verify(credential, proof)
    }

    println("verification result: $result")
}

fun pollOperation(operationId: AtalaOperationId): String {
    var status = runBlocking {
        nodeAuthApi.getOperationStatus(operationId)
    }

    return AtalaOperationStatus.asString(status);
}

@PrismSdkInternal
fun transactionId(oid: AtalaOperationId): String {
    val node = NodeServiceCoroutine.Client(GrpcClient(grpcOptions))
    val response = runBlocking {
        node.GetOperationInfo(GetOperationInfoRequest(ByteArr(oid.value())))
    }
    return response.transactionId
}

@PrismSdkInternal
fun main() {
    embeddedServer(Netty, port = 8484) {
        install(ContentNegotiation) {
            gson {
            }
        }
        routing {
            get("/") {
                call.respondText("Welcome to the Prism API")
            }
            get("/did/{didString}") {
                var didString = call.parameters["didString"]
                var result = getDid(didString)
                call.respond(mapOf("found" to "$result"))
            }
            get("/operation/{operation}") {
                // createDidOperationId.hexValue()
                var operation = call.parameters["operation"]
                // var result = pollOperation(AtalaOperationId(operation.toLong()))
                call.respond(mapOf("status" to "not implemented"))
            }
            post("/createSeedFile") {
                try {
                    generateSeedFile()
                    call.respond(mapOf("status" to "created"))
                } catch (e: Exception) {
                    call.respond(mapOf("status" to "error"))
                }
            }
            post("/createDid") {
                try {
                    val result = createUnpublishedDid()
                    call.respond(mapOf("canonical" to result.canonical, "longForm" to result.longForm))
                }
                catch(e: Exception) {
                    call.response.status(HttpStatusCode(400, "Bad Request"))
                    call.respond(mapOf("error" to "Error creating DID"))
                }
            }
            post("/publishDid") {
                try {
                    publishDid()
                    call.respond(mapOf("status" to "published"))
                }
                catch(e: Exception) {
                    call.response.status(HttpStatusCode(400, "Bad Request"))
                    call.respond(mapOf("error" to "Error publishing DID"))
                }
            }
            post("/verify") {
                var vars = call.receive<VerifyCredentials>()
                var result = verifyCredentials(vars)
                // print(vars.encodedSignedCredential);
                // var arg = "{\"hash\":\"44432e9db47e624078c2405ee7f49335adfa64cef0daed594e3567ae71274fb7\"}"// {"hash":"44432e9db47e624078c2405ee7f49335adfa64cef0daed594e3567ae71274fb7","index":0,"siblings":["2e8e1411a3f5acb8d83ee4ea9b1e04ca86fa2344d1550d1a260396ec6312f087","0d1f7f50702db94b16a21412291a806f75539c9f2739feb645f3427fb01aa269"]}
                // var json = Json.parseToJsonElement(arg)
                print(result)
                call.respondText("$result")
                // call.respond(json)
            }
        }
    }.start(wait = true)
}