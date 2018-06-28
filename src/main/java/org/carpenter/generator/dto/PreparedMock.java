package org.carpenter.generator.dto;

import org.carpenter.generator.dto.source.MethodLine;

import java.util.Objects;

public class PreparedMock {
    private MethodLine mock;
    private MethodLine verify;

    public PreparedMock(MethodLine mock, MethodLine verify) {
        this.mock = mock;
        this.verify = verify;
    }

    public MethodLine getMock() {
        return mock;
    }

    public void setMock(MethodLine mock) {
        this.mock = mock;
    }

    public MethodLine getVerify() {
        return verify;
    }

    public void setVerify(MethodLine verify) {
        this.verify = verify;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PreparedMock that = (PreparedMock) o;
        return Objects.equals(mock, that.mock) &&
                Objects.equals(verify, that.verify);
    }

    @Override
    public int hashCode() {

        return Objects.hash(mock, verify);
    }

    @Override
    public String toString() {
        return "PreparedMock{" +
                "mock=" + mock +
                ", verify=" + verify +
                '}';
    }
}
