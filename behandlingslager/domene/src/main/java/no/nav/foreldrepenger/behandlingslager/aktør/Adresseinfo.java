package no.nav.foreldrepenger.behandlingslager.aktør;

import java.util.Objects;
import java.util.Optional;

public class Adresseinfo {

    private AdresseType gjeldendePostadresseType;
    private String matrikkelId;
    private String adresselinje1;
    private String adresselinje2;
    private String adresselinje3;
    private String adresselinje4;
    private String postNr;
    private String poststed;
    private String land;

    private Adresseinfo() {
    }

    public String getMatrikkelId() {
        return matrikkelId;
    }

    public String getAdresselinje1() {
        return adresselinje1;
    }

    public String getAdresselinje2() {
        return adresselinje2;
    }

    public String getAdresselinje3() {
        return adresselinje3;
    }

    public String getAdresselinje4() {
        return adresselinje4;
    }

    public String getPostNr() {
        return postNr;
    }

    public String getPoststed() {
        return poststed;
    }

    public String getLand() {
        return land;
    }

    public AdresseType getGjeldendePostadresseType() {
        return gjeldendePostadresseType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (Adresseinfo) o;
        return gjeldendePostadresseType == that.gjeldendePostadresseType && Objects.equals(matrikkelId, that.matrikkelId) && Objects.equals(
            adresselinje1, that.adresselinje1) && Objects.equals(adresselinje2, that.adresselinje2) && Objects.equals(adresselinje3,
            that.adresselinje3) && Objects.equals(adresselinje4, that.adresselinje4) && Objects.equals(postNr, that.postNr) && Objects.equals(
            poststed, that.poststed) && Objects.equals(land, that.land);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gjeldendePostadresseType, matrikkelId, adresselinje1, adresselinje2, adresselinje3, adresselinje4, postNr, poststed,
            land);
    }

    public static boolean likeAdresser(Adresseinfo a1, Adresseinfo a2) {
        if (a1 == null && a2 == null) {
            return true;
        }
        if (a1 == null || a2 == null) {
            return false;
        }
        if (a1.matrikkelId != null || a2.matrikkelId != null) {
            return Objects.equals(a1.matrikkelId, a2.matrikkelId);
        }
        return likeAdresselinjer(a1, a2) && Objects.equals(a1.postNr, a2.postNr) && Objects.equals(a1.land, a2.land);
    }

    private static boolean likeAdresselinjer(Adresseinfo a1, Adresseinfo a2) {
        var a1l1 = kompaktAdresseline(a1.adresselinje1);
        var a2l1 = kompaktAdresseline(a2.adresselinje1);
        return Objects.equals(a1l1, a2l1) || Objects.equals(a1l1, kompaktAdresseline(a2.adresselinje2)) || Objects.equals(
            kompaktAdresseline(a1.adresselinje2), a2l1);
    }

    private static String kompaktAdresseline(String adresselinje) {
        return Optional.ofNullable(adresselinje).map(a -> a.replaceAll("\\s", "")).orElse(null);
    }

    public static Builder builder(AdresseType gjeldende) {
        return new Builder(gjeldende);
    }

    public static class Builder {
        private Adresseinfo kladd;

        public Builder(AdresseType gjeldende) {
            this.kladd = new Adresseinfo();
            this.kladd.gjeldendePostadresseType = gjeldende;
        }

        public Builder medMatrikkelId(String matrikkelId) {
            this.kladd.matrikkelId = matrikkelId;
            return this;
        }

        public Builder medAdresselinje1(String adresselinje1) {
            this.kladd.adresselinje1 = adresselinje1;
            return this;
        }

        public Builder medAdresselinje2(String adresselinje2) {
            this.kladd.adresselinje2 = adresselinje2;
            return this;
        }

        public Builder medAdresselinje3(String adresselinje3) {
            this.kladd.adresselinje3 = adresselinje3;
            return this;
        }

        public Builder medAdresselinje4(String adresselinje4) {
            this.kladd.adresselinje4 = adresselinje4;
            return this;
        }

        public Builder medPostNr(String postNr) {
            this.kladd.postNr = postNr;
            return this;
        }

        public Builder medPoststed(String poststed) {
            this.kladd.poststed = poststed;
            return this;
        }

        public Builder medLand(String land) {
            this.kladd.land = land;
            return this;
        }

        public Adresseinfo build() {
            verifyStateForBuild();
            return kladd;
        }

        private void verifyStateForBuild() {
            Objects.requireNonNull(kladd.gjeldendePostadresseType, "gjeldendePostadresseType");
        }
    }
}
