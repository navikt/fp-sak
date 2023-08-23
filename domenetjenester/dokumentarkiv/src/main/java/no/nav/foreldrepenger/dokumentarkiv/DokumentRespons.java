package no.nav.foreldrepenger.dokumentarkiv;

import java.util.Arrays;
import java.util.Objects;

public record DokumentRespons(byte[] innhold, String contentType, String contentDisp) {

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DokumentRespons that = (DokumentRespons) o;
        return Arrays.equals(innhold, that.innhold) && Objects.equals(contentType, that.contentType) && Objects.equals(contentDisp, that.contentDisp);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(contentType, contentDisp);
        result = 31 * result + Arrays.hashCode(innhold);
        return result;
    }

    @Override
    public String toString() {
        return "DokumentRespons{" +
                "contentType='" + contentType + '\'' +
                ", contentDisp='" + contentDisp + '\'' +
                '}';
    }


}
