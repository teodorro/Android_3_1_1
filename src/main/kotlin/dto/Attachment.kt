package dto

import enumeraion.AttachmentType

data class Attachment(
    val url: String,
    val description: String,
    val type: AttachmentType,
)