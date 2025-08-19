package no.nav.foreldrepenger.dokumentbestiller;

import java.util.Objects;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public record DokumentForhandsvisning(UUID behandlingUuid,
                                       Saksnummer saksnummer,
                                       DokumentMalType dokumentMal,
                                       String fritekst,
                                       String tittel,
                                       RevurderingVarslingÅrsak revurderingÅrsak,
                                       DokumentType dokumentType) {

    public enum DokumentType {
        AUTOMATISK,
        OVERSTYRT
    }

     public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID behandlingUuid;
        private Saksnummer saksnummer;
        private DokumentMalType dokumentMal;
        private RevurderingVarslingÅrsak revurderingÅrsak;
        private String fritekst;
        private String tittel;
        private DokumentType dokumentType;

        public Builder medBehandlingUuid(UUID behandlingUuid) {
            this.behandlingUuid = behandlingUuid;
            return this;
        }

        public Builder medSaksnummer(Saksnummer saksnummer) {
            this.saksnummer = saksnummer;
            return this;
        }

        public Builder medDokumentMal(DokumentMalType dokumentMal) {
            this.dokumentMal = dokumentMal;
            return this;
        }

        public Builder medRevurderingÅrsak(RevurderingVarslingÅrsak revurderingÅrsak) {
            this.revurderingÅrsak = revurderingÅrsak;
            return this;
        }

        public Builder medFritekst(String fritekst) {
            this.fritekst = fritekst;
            return this;
        }

        public Builder medTittel(String tittel) {
            this.tittel = tittel;
            return this;
        }

        public Builder medDokumentType(DokumentType dokumentType) {
            this.dokumentType = dokumentType;
            return this;
        }

        public DokumentForhandsvisning build() {
            valider();
            return new DokumentForhandsvisning(behandlingUuid, saksnummer, dokumentMal, fritekst, tittel, revurderingÅrsak, dokumentType);
        }

        private void valider() {
            Objects.requireNonNull(behandlingUuid, "Behandling UUID må være satt");
            Objects.requireNonNull(saksnummer, "Saksnummer må være satt");
            Objects.requireNonNull(dokumentType, "Dokument type må være satt");
            if (DokumentMalType.FRITEKSTBREV.equals(dokumentMal)) {
                Objects.requireNonNull(fritekst, "Fritekst må være satt for fritekstbrev");
                Objects.requireNonNull(tittel, "Tittel må være satt for fritekstbrev.");
            } else if (DokumentMalType.VEDTAKSBREV_FRITEKST_HTML.equals(dokumentMal)) {
                Objects.requireNonNull(fritekst, "Fritekst må være satt for fritekstbrev");
            } else if (DokumentMalType.INNHENTE_OPPLYSNINGER.equals(dokumentMal)) {
                Objects.requireNonNull(fritekst, "Fritekst må være satt for revurdering årsak Annet.");
            } else if (DokumentMalType.VARSEL_OM_REVURDERING.equals(dokumentMal)) {
                Objects.requireNonNull(revurderingÅrsak, "Revurdering årsak må være satt.");
                if (RevurderingVarslingÅrsak.ANNET.equals(revurderingÅrsak)) {
                    Objects.requireNonNull(fritekst, "Fritekst må være satt for revurdering årsak Annet.");
                }
            }
        }
    }
}
