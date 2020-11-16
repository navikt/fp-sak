package no.nav.foreldrepenger.behandlingslager.aktør;

import java.util.Objects;

public class Adresseinfo {

    private AdresseType gjeldendePostadresseType;
    private String adresselinje1;
    private String adresselinje2;
    private String adresselinje3;
    private String adresselinje4;
    private String postNr;
    private String poststed;
    private String land;

    private Adresseinfo() {
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

    public static class Builder {
        private final AdresseType gjeldendePostadresseType;
        private String adresselinje1;
        private String adresselinje2;
        private String adresselinje3;
        private String adresselinje4;
        private String postNr;
        private String poststed;
        private String land;

        public Builder(AdresseType gjeldende) {
            this.gjeldendePostadresseType = gjeldende;
        }
        public Builder medAdresselinje1(String adresselinje1) {
            this.adresselinje1 = adresselinje1;
            return this;
        }

        public Builder medAdresselinje2(String adresselinje2) {
            this.adresselinje2 = adresselinje2;
            return this;
        }

        public Builder medAdresselinje3(String adresselinje3) {
            this.adresselinje3 = adresselinje3;
            return this;
        }

        public Builder medAdresselinje4(String adresselinje4) {
            this.adresselinje4 = adresselinje4;
            return this;
        }

        public Builder medPostNr(String postNr) {
            this.postNr = postNr;
            return this;
        }

        public Builder medPoststed(String poststed) {
            this.poststed = poststed;
            return this;
        }

        public Builder medLand(String land) {
            this.land = land;
            return this;
        }

        public Adresseinfo build() {
            verifyStateForBuild();
            Adresseinfo adresseinfo = new Adresseinfo();
            adresseinfo.gjeldendePostadresseType = this.gjeldendePostadresseType;
            adresseinfo.adresselinje1 = this.adresselinje1;
            adresseinfo.adresselinje2 = this.adresselinje2;
            adresseinfo.adresselinje3 = this.adresselinje3;
            adresseinfo.adresselinje4 = this.adresselinje4;
            adresseinfo.postNr = this.postNr;
            adresseinfo.poststed = this.poststed;
            adresseinfo.land = this.land;
            return adresseinfo;
        }

        private void verifyStateForBuild() {
            Objects.requireNonNull(gjeldendePostadresseType, "gjeldendePostadresseType");
        }
    }
}
