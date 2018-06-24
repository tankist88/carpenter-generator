package org.carpenter.generator.dto;

import java.util.Objects;

public class PreparedMock {
    private String mock;
    private String verify;

    public PreparedMock(String mock, String verify) {
        this.mock = mock;
        this.verify = verify;
    }

    public String getMock() {
        return mock;
    }

    public void setMock(String mock) {
        this.mock = mock;
    }

    public String getVerify() {
        return verify;
    }

    public void setVerify(String verify) {
        this.verify = verify;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PreparedMock)) return false;
        PreparedMock that = (PreparedMock) o;
        return Objects.equals(getMock(), that.getMock()) &&
                Objects.equals(getVerify(), that.getVerify());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMock(), getVerify());
    }

    @Override
    public String toString() {
        return "PreparedMock{" +
                "mock='" + mock + '\'' +
                ", verify='" + verify + '\'' +
                '}';
    }
}
