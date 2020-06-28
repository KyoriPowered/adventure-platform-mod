package net.kyori.adventure.platform.fabric.dfu;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.RecordBuilder;
import java.util.Optional;
import java.util.function.UnaryOperator;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CompoundRecordBuilder implements RecordBuilder<BinaryTag> {
  private final BinaryTagOps ops;
  private final CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();
  private Lifecycle life = Lifecycle.experimental();
  private DataResult.@Nullable PartialResult<BinaryTag> error = null;

  public CompoundRecordBuilder(final BinaryTagOps ops) {
    this.ops = ops;
  }

  @Override
  public DynamicOps<BinaryTag> ops() {
    return this.ops;
  }

  @Override
  public RecordBuilder<BinaryTag> add(final BinaryTag key, final BinaryTag value) {
    builder.put(BinaryTagOps.unwrap(key), value);
    return this;
  }

  @Override
  public RecordBuilder<BinaryTag> add(final BinaryTag key, final DataResult<BinaryTag> value) {
    if(value.error().isPresent()) { // TODO: probably doing this wrong -- i won't preserve partials (but then partials don't really make sense?)
      this.error = value.error().get();
    } else {
      add(key, value.result().orElseThrow(() -> new IllegalStateException("Neither error or result was present")));
    }
    return this;
  }

  @Override
  public RecordBuilder<BinaryTag> add(final DataResult<BinaryTag> key, final DataResult<BinaryTag> value) {
    return null;
  }

  @Override
  public RecordBuilder<BinaryTag> withErrorsFrom(final DataResult<?> result) {
    if(result.error().isPresent()) {
      this.error = result.error().get().map(x -> this.ops.empty());
    }
    return this;
  }

  @Override
  public RecordBuilder<BinaryTag> setLifecycle(final Lifecycle lifecycle) {
    this.life = lifecycle;
    return this;
  }

  @Override
  public RecordBuilder<BinaryTag> mapError(final UnaryOperator<String> onError) {
    if(this.error != null) {
      this.error = new DataResult.PartialResult<>(onError.apply(this.error.message()), Optional.empty());
    }
    return this;
  }

  @Override
  public DataResult<BinaryTag> build(final BinaryTag prefix) {
    if(this.error != null) {
      return DataResult.error(this.error.message());
    } else {
      return DataResult.success(this.builder.build());
    }
  }
}
