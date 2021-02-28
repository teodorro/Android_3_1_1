import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dto.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.coroutines.EmptyCoroutineContext

val client = OkHttpClient.Builder().build()
const val servAddress = "http://localhost:9999/api/"

fun main(args: Array<String>) {
    printCommentAuthorNames()
    Thread.sleep(10_000)
}

fun printCommentAuthorNames() {
    with(CoroutineScope(EmptyCoroutineContext)) {
        launch {
            val postsComments = makeApiCall<List<Post>>(servAddress + "slow/posts")
                .map { post ->
                    async {
                        PostComment(post, makeApiCall<List<Comment>>(servAddress + "posts/" + post.id + "/comments"))
                    }
                }.awaitAll()

            val postAuthors = postsComments
                .map { postsComment ->
                    async {
                        PostAuthor(
                            postsComment.post,
                            makeApiCall<Author>(servAddress + "authors/" + postsComment.post.authorId)
                        )
                    }
                }.awaitAll()

            val commentAuthors = postsComments.map { x -> x.comments }.flatten()
                .map { comment ->
                    async {
                        CommentAuthor(comment, makeApiCall<Author>(servAddress + "authors/" + comment.authorId))
                    }
                }.awaitAll()

            postsComments.forEach {
                println("post.Id: " + it.post.id + "  ::  post.authorId: " + it.post.authorId
                        + "  ::  author.name: " + postAuthors.first { x -> x.author.id == it.post.authorId }.author.name)
                it.comments.forEach {
                    println("comment.Id: " + it.id + "  ::  comment.authorId: " + it.authorId
                            + "  ::  author.name: " + commentAuthors.first { x -> x.author.id == it.authorId }.author.name)
                }
            }
        }
    }
}

// 1st task. Not needed
private fun printPostAuthorNames() {
    with(CoroutineScope(EmptyCoroutineContext)) {
        launch {
            val postAuthors = makeApiCall<List<Post>>(servAddress + "slow/posts")
                .map { post ->
                    async {
                        PostAuthor(post, makeApiCall<Author>(servAddress + "authors/" + post.authorId))
                    }
                }.awaitAll()
            postAuthors.forEach { x -> println("post.Id: " + x.post.id + "  ::  post.authorId: " + x.post.authorId + "  ::  author.name: " + x.author.name) }
        }
    }
}


suspend inline fun <reified T> makeApiCall(url: String, gson: Gson = Gson()): T =
    withContext(Dispatchers.IO) {
        var response = Request.Builder()
            .url(url)
            .build()
            .let {
                client.newCall(it)
            }.execute()
        val body = response.body ?: throw RuntimeException("empty body")
        gson.fromJson(body.string(), object : TypeToken<T>() {}.type)
    }


