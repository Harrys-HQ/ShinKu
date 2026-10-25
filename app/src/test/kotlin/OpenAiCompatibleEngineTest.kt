import com.shinku.reader.domain.ai.OpenAiCompatibleEngine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OpenAiCompatibleEngineTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun verifyModelResolutionDefaults() {
        assertEquals("google/gemini-2.0-flash-001", OpenAiCompatibleEngine.resolveModel("auto", "openrouter"))
        assertEquals("google/gemini-2.0-flash-001", OpenAiCompatibleEngine.resolveModel("", "openrouter"))
        assertEquals("deepseek-chat", OpenAiCompatibleEngine.resolveModel("auto", "deepseek"))
        assertEquals("llama-3.3-70b-versatile", OpenAiCompatibleEngine.resolveModel("auto", "groq"))
        assertEquals("gpt-4o-mini", OpenAiCompatibleEngine.resolveModel("auto", "openai"))
        assertEquals("gpt-4o-mini", OpenAiCompatibleEngine.resolveModel("auto", "custom"))

        // Explicit model preserved
        assertEquals("anthropic/claude-3.5-sonnet", OpenAiCompatibleEngine.resolveModel("anthropic/claude-3.5-sonnet", "openrouter"))
        assertEquals("meta-llama/llama-3.3-70b-instruct:free", OpenAiCompatibleEngine.resolveModel("meta-llama/llama-3.3-70b-instruct:free", "openrouter"))
        assertEquals("deepseek-reasoner", OpenAiCompatibleEngine.resolveModel("deepseek-reasoner", "deepseek"))
    }

    @Test
    fun parseOpenAiStandardChatCompletion() {
        val sampleResponse = """
            {
              "id": "gen-12345",
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "[\"Berserk\", \"Vinland Saga\", \"Vagabond\"]"
                  }
                }
              ]
            }
        """.trimIndent()

        val parsed = json.parseToJsonElement(sampleResponse)
        val content = parsed.jsonObject["choices"]?.jsonArray?.getOrNull(0)
            ?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content

        assertEquals("[\"Berserk\", \"Vinland Saga\", \"Vagabond\"]", content)
    }

    @Test
    fun parseMarkdownWrappedJsonArray() {
        val rawLlmOutput = """
            Here are the recommended manga titles matching your vibe:
            ```json
            ["Monster", "20th Century Boys", "Pluto"]
            ```
            Hope you enjoy reading them!
        """.trimIndent()

        val start = rawLlmOutput.indexOf("[")
        val end = rawLlmOutput.lastIndexOf("]") + 1
        assertTrue(start != -1 && end > start)

        val jsonArrayStr = rawLlmOutput.substring(start, end)
        val titles = json.decodeFromString<List<String>>(jsonArrayStr)

        assertEquals(listOf("Monster", "20th Century Boys", "Pluto"), titles)
    }

    @Test
    fun parseErrorPayload() {
        val errorResponse = """
            {
              "error": {
                "message": "Provider returned error: Insufficient credits on OpenRouter",
                "code": 402
              }
            }
        """.trimIndent()

        val errObj = json.parseToJsonElement(errorResponse).jsonObject["error"]?.jsonObject
        val message = errObj?.get("message")?.jsonPrimitive?.content

        assertEquals("Provider returned error: Insufficient credits on OpenRouter", message)
    }
}
