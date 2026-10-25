package com.example.karaoke

/** 歌词数据结构 */
data class LrcWord(val time: Long, val text: String)
data class LrcLine(val time: Long, val text: String, val words: List<LrcWord>) {
    /** 该行的结束时间（由下一行时间推算），由 LrcParser 计算 */
    var end: Long = 0L
}

/** 标准 LRC 与逐字高亮 LRC 解析器（与网页版逻辑一致） */
object LrcParser {

    data class Result(val meta: Map<String, String>, val lines: List<LrcLine>)

    private val re = Regex("""^\[(\d{1,2}):(\d{1,2})(?:[.:](\d{1,3}))?\]""")
    private val reWord = Regex("""<(\d{1,2}):(\d{1,2})(?:[.:](\d{1,3}))?>""")
    private val reTag = Regex("""^\[(ti|ar|al|by|offset):([^\]]*)\]\s*$""", RegexOption.IGNORE_CASE)

    /** [分:秒.厘秒] -> 毫秒 */
    private fun toSec(m: MatchResult): Long {
        val whole = m.groupValues[1].toLong() * 60 + m.groupValues[2].toLong()
        val frac = m.groupValues[3].let {
            if (it.isEmpty()) 0.0 else ("0." + it.take(3)).toDouble()
        }
        return whole * 1000 + (frac * 1000).toLong()
    }

    fun parse(txt: String): Result {
        val meta = LinkedHashMap<String, String>()
        val lines = mutableListOf<LrcLine>()

        txt.lines().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEach

            val tag = reTag.matchEntire(line)
            if (tag != null) {
                meta[tag.groupValues[1].lowercase()] = tag.groupValues[2].trim()
                return@forEach
            }

            val mm = re.find(line) ?: return@forEach
            val t = toSec(mm)
            val body = line.substring(mm.range.last + 1)

            val words = mutableListOf<LrcWord>()
            var lastEnd = 0
            var prevTime: Long? = null
            var hadWord = false

            for (m in reWord.findAll(body)) {
                hadWord = true
                val chunk = body.substring(lastEnd, m.range.first)
                if (chunk.isNotEmpty()) words.add(LrcWord(prevTime ?: t, chunk))
                lastEnd = m.range.last + 1
                prevTime = toSec(m)
            }
            val tail = body.substring(lastEnd)
            if (tail.isNotEmpty() && hadWord) words.add(LrcWord(prevTime ?: t, tail))

            val text = if (hadWord) words.joinToString("") { it.text } else body
            if (text.isNotBlank()) lines.add(LrcLine(t, text, words))
        }

        lines.sortBy { it.time }
        // 推算每行结束时间
        for (i in lines.indices) {
            lines[i].end = if (i + 1 < lines.size) lines[i + 1].time else lines[i].time + 4000
        }
        return Result(meta, lines)
    }
}
