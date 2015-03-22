// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: protos-repo/mcs.proto
package org.microg.gms.gcm.mcs;

import com.squareup.wire.Message;
import com.squareup.wire.ProtoField;

import static com.squareup.wire.Message.Datatype.INT32;
import static com.squareup.wire.Message.Datatype.STRING;
import static com.squareup.wire.Message.Label.REQUIRED;

public final class ErrorInfo extends Message {

  public static final Integer DEFAULT_CODE = 0;
  public static final String DEFAULT_MESSAGE = "";
  public static final String DEFAULT_TYPE = "";

  @ProtoField(tag = 1, type = INT32, label = REQUIRED)
  public final Integer code;

  @ProtoField(tag = 2, type = STRING)
  public final String message;

  @ProtoField(tag = 3, type = STRING)
  public final String type;

  @ProtoField(tag = 4)
  public final Extension extension;

  public ErrorInfo(Integer code, String message, String type, Extension extension) {
    this.code = code;
    this.message = message;
    this.type = type;
    this.extension = extension;
  }

  private ErrorInfo(Builder builder) {
    this(builder.code, builder.message, builder.type, builder.extension);
    setBuilder(builder);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof ErrorInfo)) return false;
    ErrorInfo o = (ErrorInfo) other;
    return equals(code, o.code)
        && equals(message, o.message)
        && equals(type, o.type)
        && equals(extension, o.extension);
  }

  @Override
  public int hashCode() {
    int result = hashCode;
    if (result == 0) {
      result = code != null ? code.hashCode() : 0;
      result = result * 37 + (message != null ? message.hashCode() : 0);
      result = result * 37 + (type != null ? type.hashCode() : 0);
      result = result * 37 + (extension != null ? extension.hashCode() : 0);
      hashCode = result;
    }
    return result;
  }

  public static final class Builder extends Message.Builder<ErrorInfo> {

    public Integer code;
    public String message;
    public String type;
    public Extension extension;

    public Builder() {
    }

    public Builder(ErrorInfo message) {
      super(message);
      if (message == null) return;
      this.code = message.code;
      this.message = message.message;
      this.type = message.type;
      this.extension = message.extension;
    }

    public Builder code(Integer code) {
      this.code = code;
      return this;
    }

    public Builder message(String message) {
      this.message = message;
      return this;
    }

    public Builder type(String type) {
      this.type = type;
      return this;
    }

    public Builder extension(Extension extension) {
      this.extension = extension;
      return this;
    }

    @Override
    public ErrorInfo build() {
      checkRequiredFields();
      return new ErrorInfo(this);
    }
  }
}
