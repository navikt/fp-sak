package no.nav.foreldrepenger.domene.medlem.impl.rest;

import java.time.LocalDate;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Medlemskapsunntak {

    @JsonProperty("unntakId")
    private Long unntakId;
    @JsonProperty("fraOgMed")
    private LocalDate fraOgMed;
    @JsonProperty("tilOgMed")
    private LocalDate tilOgMed;
    @JsonProperty("dekning")
    private String dekning;
    @JsonProperty("grunnlag")
    private String grunnlag;
    @JsonProperty("lovvalg")
    private String lovvalg;
    @JsonProperty("lovvalgsland")
    private String lovvalgsland;
    @JsonProperty("helsedel")
    private Boolean helsedel;
    @JsonProperty("medlem")
    private Boolean medlem;
    @JsonProperty("sporingsinformasjon")
    private Sporingsinformasjon sporingsinformasjon;
    @JsonProperty("studieinformasjon")
    private Studieinformasjon studieinformasjon;

    @JsonCreator
    public Medlemskapsunntak(@JsonProperty("unntakId") Long unntakId,
                             @JsonProperty("fraOgMed") LocalDate fraOgMed,
                             @JsonProperty("tilOgMed") LocalDate tilOgMed,
                             @JsonProperty("dekning") String dekning,
                             @JsonProperty("grunnlag") String grunnlag,
                             @JsonProperty("lovvalg") String lovvalg,
                             @JsonProperty("lovvalgsland") String lovvalgsland,
                             @JsonProperty("helsedel") Boolean helsedel,
                             @JsonProperty("medlem") Boolean medlem,
                             @JsonProperty("sporingsinformasjon") Sporingsinformasjon sporingsinformasjon,
                             @JsonProperty("studieinformasjon") Studieinformasjon studieinformasjon) {
        this.unntakId = unntakId;
        this.fraOgMed = fraOgMed;
        this.tilOgMed = tilOgMed;
        this.dekning = dekning;
        this.grunnlag = grunnlag;
        this.lovvalg = lovvalg;
        this.lovvalgsland = lovvalgsland;
        this.helsedel = helsedel;
        this.medlem = medlem;
        this.sporingsinformasjon = sporingsinformasjon;
        this.studieinformasjon = studieinformasjon;
    }

    public Long getUnntakId() {
        return unntakId;
    }

    public LocalDate getFraOgMed() {
        return fraOgMed;
    }

    public LocalDate getTilOgMed() {
        return tilOgMed;
    }

    public String getDekning() {
        return dekning;
    }

    public String getGrunnlag() {
        return grunnlag;
    }

    public String getLovvalg() {
        return lovvalg;
    }

    public String getLovvalgsland() {
        return lovvalgsland;
    }

    public Boolean isHelsedel() {
        return helsedel;
    }

    public Boolean isMedlem() {
        return medlem;
    }

    public Sporingsinformasjon getSporingsinformasjon() {
        return sporingsinformasjon;
    }

    public Studieinformasjon getStudieinformasjon() {
        return studieinformasjon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Medlemskapsunntak that = (Medlemskapsunntak) o;
        return Objects.equals(unntakId, that.unntakId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(unntakId);
    }

    @Override
    public String toString() {
        return "Medlemskapsunntak{" +
            "unntakId=" + unntakId +
            ", fraOgMed=" + fraOgMed +
            ", tilOgMed=" + tilOgMed +
            ", dekning='" + dekning + '\'' +
            ", grunnlag='" + grunnlag + '\'' +
            ", lovvalg='" + lovvalg + '\'' +
            ", lovvalgsland='" + lovvalgsland + '\'' +
            ", helsedel=" + helsedel +
            ", medlem=" + medlem +
            ", sporingsinformasjon=" + sporingsinformasjon +
            ", studieinformasjon=" + studieinformasjon +
            '}';
    }

    public static class Sporingsinformasjon {

        @JsonProperty("besluttet")
        private LocalDate besluttet;
        @JsonProperty("kilde")
        private String kilde;

        @JsonCreator
        public Sporingsinformasjon(@JsonProperty("besluttet") LocalDate besluttet,
                                   @JsonProperty("kilde") String kilde) {
            this.besluttet = besluttet;
            this.kilde = kilde;
        }

        public LocalDate getBesluttet() {
            return besluttet;
        }

        public String getKilde() {
            return kilde;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Sporingsinformasjon that = (Sporingsinformasjon) o;
            return Objects.equals(besluttet, that.besluttet) &&
                Objects.equals(kilde, that.kilde);
        }

        @Override
        public int hashCode() {
            return Objects.hash(besluttet, kilde);
        }

        @Override
        public String toString() {
            return "Sporingsinformasjon{" +
                "besluttet=" + besluttet +
                ", kilde='" + kilde + '\'' +
                '}';
        }
    }

    public static class Studieinformasjon {

        @JsonProperty("studieland")
        private String studieland;

        @JsonCreator
        public Studieinformasjon(@JsonProperty("studieland") String studieland) {
            this.studieland = studieland;

        }

        public String getStudieland() {
            return studieland;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Studieinformasjon that = (Studieinformasjon) o;
            return Objects.equals(studieland, that.studieland);
        }

        @Override
        public int hashCode() {
            return Objects.hash(studieland);
        }

        @Override
        public String toString() {
            return "Studieinformasjon{" +
                "studieland='" + studieland + '\'' +
                '}';
        }
    }

}

