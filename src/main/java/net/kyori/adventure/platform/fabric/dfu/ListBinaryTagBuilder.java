package net.kyori.adventure.platform.fabric.dfu;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.ListBuilder;
import java.util.function.UnaryOperator;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ListBinaryTagBuilder implements ListBuilder<BinaryTag> {
  private final BinaryTagOps ops;
  private final ListBinaryTag.Builder<BinaryTag> builder = ListBinaryTag.builder();

  private @Nullable DataResult<?> error = null;

  public ListBinaryTagBuilder(final BinaryTagOps ops) {
    this.ops = ops;
  }

  @Override
  public DynamicOps<BinaryTag> ops() {
    return this.ops;
  }

  @Override
  public DataResult<BinaryTag> build(final BinaryTag prefix) {
    if(this.error != null) {
      return this.error.map(x -> (BinaryTag) null);
    }

    if(prefix.equals(this.ops.empty())) {
      return DataResult.success(this.builder.build());
    } else if(prefix instanceof ListBinaryTag) {
      for(BinaryTag tag : (ListBinaryTag) prefix) {
        builder.add(tag);
      }
    }
    return DataResult.success(builder.build(), Lifecycle.stable());
  }

  @Override
  public ListBuilder<BinaryTag> add(final BinaryTag value) {
    this.builder.add(value);
    return this;
  }

  @Override
  public ListBuilder<BinaryTag> add(final DataResult<BinaryTag> value) {
    if(value.error().isPresent()) {
      return withErrorsFrom(value);
    } else {
      add(value.result().get());
    }
    return this;
  }

  @Override
  public ListBuilder<BinaryTag> withErrorsFrom(final DataResult<?> result) {
    if(this.error != null) {
      this.error = this.error.flatMap(x -> result);
    } else {
      this.error = result;
    }
    return this;
  }

  @Override
  public ListBuilder<BinaryTag> mapError(final UnaryOperator<String> onError) {
    if(this.error != null) {
      this.error = this.error.mapError(onError);
    }
    return this;
  }
}
