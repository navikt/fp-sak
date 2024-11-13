package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstBuilderFormater;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public class HistorikkinnslagV2 {
    private Long fagsakId;
    private Long behandlingId;
    private HistorikkAktør aktør;
    private SkjermlenkeType skjermlenke;
    private List<Tekstlinje> linjer;


    @Override
    public String toString() {
        return "HistorikkinnslagV2{" + "fagsakId=" + fagsakId + ", behandlingId=" + behandlingId + ", aktør=" + aktør + ", skjermlenke=" + skjermlenke
            + ", linjer=" + linjer + '}';
    }

    public static class Builder {

        private HistorikkinnslagV2 kladd = new HistorikkinnslagV2();

        public Builder medFagsakId(Long fagsakId) {
            kladd.fagsakId = fagsakId;
            return this;
        }

        public Builder medBehandlingId(Long behandlingId) {
            kladd.behandlingId = behandlingId;
            return this;
        }

        public Builder medAktør(HistorikkAktør aktør) {
            kladd.aktør = aktør;
            return this;
        }

        public Builder medTittel(SkjermlenkeType skjermlenke) {
            kladd.skjermlenke = skjermlenke;
            return this;
        }

        public Builder medTekstlinjer(List<Tekstlinje> linjer) {
            kladd.linjer = new ArrayList<>(linjer);
            return this;
        }

        public HistorikkinnslagV2 build() {
            var t = kladd;
            kladd = null;
            return t;
        }
    }

    public Long getFagsakId() {
        return fagsakId;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public HistorikkAktør getAktør() {
        return aktør;
    }

    public SkjermlenkeType getSkjermlenke() {
        return skjermlenke;
    }

    public List<Tekstlinje> getLinjer() {
        return linjer;
    }

    public static class Tekstlinje {

        private final StringBuilder stringBuilder = new StringBuilder();

        public Tekstlinje b(String b) {
            stringBuilder.append(" __").append(b).append("__");
            return this;
        }

        public Tekstlinje t(String t) {
            stringBuilder.append(" ").append(t);
            return this;
        }

        public Tekstlinje t(LocalDate dato) {
            return t(HistorikkinnslagTekstBuilderFormater.formatDate(dato));
        }

        public Tekstlinje fraTil(String hva, String fra, String til) {
            if (Objects.equals(fra, til)) {
                throw new IllegalArgumentException("Like verdier " + fra);
            }
            if (fra == null) {
                return b(hva).t("er satt til").b(til);
            }
            if (til == null) {
                //TODO tekst for at noe er fjernet. Trenger vi?
                return b(hva).t("er fjernet");
            }
            return b(hva).t("er endret fra").t(fra).t("til").b(til);
        }

        public Tekstlinje fraTil(String hva, Kodeverdi fra, Kodeverdi til) {
            return fraTil(hva, fra == null ? null : fra.getNavn(), til.getNavn());
        }

        public Tekstlinje fraTil(String hva, Boolean fra, boolean til) {
            var fraTekst = fra == null ? null : fra ? "Ja" : "Nei";
            var tilTekst = til ? "Ja" : "Nei";
            return fraTil(hva, fraTekst, tilTekst);
        }

        public Tekstlinje fraTil(String hva, BigDecimal fra, BigDecimal til) {
            var fraTekst = fra == null ? null : fra.toString();
            var tilTekst = til == null ? null : til.toString();
            return fraTil(hva, fraTekst, tilTekst);
        }

        public Tekstlinje p() {
            stringBuilder.append(".");
            return this;
        }

        public String asString() {
            return stringBuilder.delete(0, 1).toString();
        }

        @Override
        public String toString() {
            return "Tekstlinje{" + "tekst='" + "***" + '\'' + '}';
        }
    }
}
