package no.nav.foreldrepenger.dokumentbestiller;

import java.util.Objects;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;

 public record BrevForhandsvisning(UUID behandlingUuid,
                                   DokumentMalType dokumentMal,
                                   String fritekst,
                                   String tittel,
                                   RevurderingVarslingÅrsak revurderingÅrsak,
                                   BrevType brevType) {

    public enum BrevType {
        AUTOMATISK,
        OVERSTYRT
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID behandlingUuid;
        private DokumentMalType dokumentMal;
        private RevurderingVarslingÅrsak revurderingÅrsak;
        private String fritekst;
        private String tittel;
        private BrevType brevType;

        public Builder medBehandlingUuid(UUID behandlingUuid) {
            this.behandlingUuid = behandlingUuid;
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

        public Builder medBrevType(BrevType brevType) {
            this.brevType = brevType;
            return this;
        }

        public BrevForhandsvisning build() {
            valider();
            return new BrevForhandsvisning(behandlingUuid, dokumentMal, fritekst, tittel, revurderingÅrsak, brevType);
        }

        private void valider() {
            Objects.requireNonNull(behandlingUuid, "Behandling UUID må være satt");
            Objects.requireNonNull(brevType, "BrevType må være satt");

            if (DokumentMalType.FRITEKSTBREV.equals(dokumentMal)) {
                Objects.requireNonNull(fritekst, "Fritekst må være satt for fritekstbrev");
                Objects.requireNonNull(tittel, "Tittel må være satt for fritekstbrev.");
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
