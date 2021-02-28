package dto

data class PostComment (
    val post: Post,
    val comments: List<Comment>
){
}