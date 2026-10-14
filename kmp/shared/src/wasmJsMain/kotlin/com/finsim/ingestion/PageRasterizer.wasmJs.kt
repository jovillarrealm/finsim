package com.finsim.ingestion

actual fun getDefaultPageRasterizer(): PageRasterizer = InMemoryPageRasterizer()
