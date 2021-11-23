package se.eris.equals;

import org.jetbrains.annotations.NotNull;

public class TestEqualsImplicit {
    public String getSomething() {
        return "something";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
