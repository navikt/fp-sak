package no.nav.foreldrepenger.dokumentbestiller;

import java.util.Objects;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;

public record BrevBestilling(UUID behandlingUuid,
                             DokumentMalType dokumentMal,
                             String fritekst,
                             RevurderingVarslingÅrsak revurderingÅrsak,
                             DokumentMalType journalførSom,
                             UUID bestillingUuid) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID behandlingUuid;
        private DokumentMalType dokumentMal;
        private RevurderingVarslingÅrsak revurderingÅrsak;
        private String fritekst;
        private DokumentMalType journalførSom;
        private UUID bestillingUuid;

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

        public Builder medJournalførSom(DokumentMalType journalførSom) {
            this.journalførSom = journalførSom;
            return this;
        }

        public BrevBestilling build() {
            valider();
            return new BrevBestilling(behandlingUuid, dokumentMal, fritekst, revurderingÅrsak, journalførSom, UUID.randomUUID());
        }

        private void valider() {
            Objects.requireNonNull(behandlingUuid, "Behandling UUID må være satt");
            Objects.requireNonNull(dokumentMal, "Dokument mal må være satt");

            if (DokumentMalType.FRITEKSTBREV.equals(dokumentMal)) {
                Objects.requireNonNull(journalførSom, "journalførSom dokument mal må være satt for fritekst vedtak er valgt.");
            }
            if (DokumentMalType.INNHENTE_OPPLYSNINGER.equals(dokumentMal)) {
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
