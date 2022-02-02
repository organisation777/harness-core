/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.variable;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.EntitySubtype;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

@OwnedBy(PL)
public enum VariableType implements EntitySubtype {
    @JsonProperty("String") STRING("String"),
    @JsonProperty("Number") NUMBER("Number"),
    @JsonProperty("Boolean") BOOELAN("Boolean"),
    @JsonProperty("Secret") SECCRET("Secret");
    private final String displayName;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static VariableType getVariableType(@JsonProperty("type") String displayName) {
        for (VariableType VariableType : VariableType.values()) {
            if (VariableType.displayName.equalsIgnoreCase(displayName)) {
                return VariableType;
            }
        }
        throw new IllegalArgumentException("Invalid value: " + displayName);
    }

    VariableType(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static VariableType fromString(final String s) {
        return VariableType.getVariableType(s);
    }
}
