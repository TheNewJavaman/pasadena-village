package net.javaman.pasadenavillage

data class BlogModel(
    val url: String,
    val title: String,
    val description: String,
    val posts: List<PostModel>
)

data class PostModel(
    val url: String,
    val title: String,
    val author: String,
    val postedAt: Long,
    val contentHtml: String
)