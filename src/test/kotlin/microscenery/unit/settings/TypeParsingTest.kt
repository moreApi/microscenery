package microscenery.unit.settings


import com.jogamp.common.util.ReflectionUtil.instanceOf
import fromScenery.Settings
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalArgumentException
import kotlin.test.assertEquals

class TypeParsingTest {

    @Test
    fun parseType()
    {
        //Ints
        assertEquals(1, Settings.parseType("1"))
        assertEquals(1L, Settings.parseType("1l"))

        //Float and double
        assertEquals(1.0f, Settings.parseType("1.0"))
        assertEquals(1.0f, Settings.parseType("1.0f"))
        assertEquals(1.0f, Settings.parseType("1f"))

        //String
        assertEquals("asd", Settings.parseType("asd"))
        assertInstanceOf(String::class.java, Settings.parseType("asd"))

        //Bools
        assertEquals(true, Settings.parseType("True"))
        assertEquals(true, Settings.parseType("tRuE"))
        assertEquals(false, Settings.parseType("false"))
        assertEquals(false, Settings.parseType("FalSE"))


        //Vectors
        val vec2 = Vector2f(1.0f, 1.0f)
        assertEquals(vec2, Settings.parseType("(1,1)"))
        assertEquals(vec2, Settings.parseType("(1.0f,1.0f)"))
        assertEquals(vec2, Settings.parseType("(1, 1.0f)"))
        assertEquals(vec2, Settings.parseType("(1.0, 1.0f)"))
        val vec3 = Vector3f(1.0f, 1.0f, 1.0f)
        assertEquals(vec3, Settings.parseType("(1,1,1)"))
        assertEquals(vec3, Settings.parseType("(1.0f,1.0f,1.0f)"))
        assertEquals(vec3, Settings.parseType("(1.0,1.0f,1)"))
        assertEquals(vec3, Settings.parseType("(1.0, 1.0f,1)"))
        val vec4 = Vector4f(1.0f, 1.0f, 1.0f, 1.0f)
        assertEquals(vec4, Settings.parseType("(1,1,1,1)"))
        assertEquals(vec4, Settings.parseType("(1.0f,1.0f,1.0f,1.0f)"))
        assertEquals(vec4, Settings.parseType("(1.0,1.0f,1,1.0)"))
        assertEquals(vec4, Settings.parseType("(1.0, 1.0f,1 ,1.0f)"))

        //Exceptions
        var formatException = assertThrows<NumberFormatException> {
            Settings.parseType("(g,1,1,1)")
        }
        assertEquals("Wrong type inserted at index 0", formatException.message)
        formatException = assertThrows {
            Settings.parseType("(1,g,1,1)")
        }
        assertEquals("Wrong type inserted at index 1", formatException.message)
        formatException = assertThrows {
            Settings.parseType("(1,1,g,1)")
        }
        assertEquals("Wrong type inserted at index 2", formatException.message)
        formatException = assertThrows {
            Settings.parseType("(1,1,1,g)")
        }
        assertEquals("Wrong type inserted at index 3", formatException.message)

        var argumentException = assertThrows<IllegalArgumentException> {
            Settings.parseType("(1)")
        }
        assertEquals("Too little or too many arguments!", argumentException.message)
        argumentException = assertThrows {
            Settings.parseType("(1,1,1,1,1)")
        }
        assertEquals("Too little or too many arguments!", argumentException.message)

    }


}