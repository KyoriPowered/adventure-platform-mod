package net.kyori.adventure.platform.fabric.dfu;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.examination.string.StringExaminer;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BinaryTagOpsTest {

  private <T> T valueOrThrow(DataResult<T> result) {
    result.error().ifPresent(err -> {
      throw new RuntimeException("Error with data result: " + err.message());
    });

    return result.result().orElseThrow(() -> new IllegalStateException("Neither result or error was present"));
  }

  @Test
  public void testStringCreation() {

  }

  @Test
  void testComplexObject() {
    final TestValue subject = new TestValue("meow", 28, new Identifier("adventure", "purr"));
    final DataResult<BinaryTag> tag = TestValue.CODEC.encode(subject, BinaryTagOps.UNCOMPRESSED, BinaryTagOps.UNCOMPRESSED.empty());
    final CompoundBinaryTag compound = (CompoundBinaryTag) valueOrThrow(tag);

    final TestValue reversed = valueOrThrow(TestValue.CODEC.decode(BinaryTagOps.UNCOMPRESSED, compound)).getFirst();
    assertEquals(subject, reversed);

    System.out.println(compound.examine(StringExaminer.simpleEscaping()));
  }

  @Test
  void testMergeListWithEmpty() {
    final BinaryTag tag = valueOrThrow(BinaryTagOps.UNCOMPRESSED.mergeToList(BinaryTagOps.UNCOMPRESSED.empty(), StringBinaryTag.of("test")));
    assertEquals(ListBinaryTag.builder().add(StringBinaryTag.of("test")).build(), tag);
  }

  @Test
  void testMergeWithCompound() {

  }

  @Test
  void testListAdd() {

  }

  @Test
  void testListAddToEmpty() {

  }

  static class TestValue {
    public static final Codec<TestValue> CODEC = RecordCodecBuilder.create(o -> o.group(
      Codec.STRING.fieldOf("name").forGetter(t -> t.name),
      Codec.INT.fieldOf("amount").forGetter(t -> t.amount),
      Identifier.CODEC.fieldOf("ident").forGetter(t -> t.ident)
    ).apply(o, TestValue::new));

    private final String name;
    private final int amount;
    private final Identifier ident;

    TestValue(final String name, final int amount, final Identifier ident) {
      this.name = name;
      this.amount = amount;
      this.ident = ident;
    }

    @Override
    public boolean equals(final Object o) {
      if(this == o) return true;
      if(!(o instanceof TestValue)) return false;

      final TestValue testValue = (TestValue) o;

      if(amount != testValue.amount) return false;
      if(!name.equals(testValue.name)) return false;
      return ident.equals(testValue.ident);
    }

    @Override
    public int hashCode() {
      int result = name.hashCode();
      result = 31 * result + amount;
      result = 31 * result + ident.hashCode();
      return result;
    }
  }


}
