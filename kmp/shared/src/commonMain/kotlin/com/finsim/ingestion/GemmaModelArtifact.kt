package com.finsim.ingestion

object GemmaModelArtifact {
    const val repository = "litert-community/gemma-4-E2B-it-litert-lm"
    const val revision = "7fa1d78473894f7e736a21d920c3aa80f950c0db"
    const val fileName = "gemma-4-E2B-it.litertlm"
    const val sizeBytes = 2_583_085_056L
    const val sha256 = "ab7838cdfc8f77e54d8ca45eadceb20452d9f01e4bfade03e5dce27911b27e42"
    const val downloadUrl = "https://huggingface.co/$repository/resolve/$revision/$fileName?download=true"

    fun validationError(fileName: String, sizeBytes: Long, sha256: String): String? = when {
        fileName != this.fileName || !fileName.endsWith(".litertlm") -> "Unexpected model filename: $fileName."
        sizeBytes != this.sizeBytes -> "Model size mismatch: expected ${this.sizeBytes} bytes, found $sizeBytes."
        !sha256.equals(this.sha256, ignoreCase = true) -> "Model SHA-256 does not match revision $revision."
        else -> null
    }
}
