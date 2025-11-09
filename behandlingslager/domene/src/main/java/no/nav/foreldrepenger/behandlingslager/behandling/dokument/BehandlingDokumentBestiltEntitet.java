package no.nav.foreldrepenger.behandlingslager.behandling.dokument;

import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

@Entity(name = "BehandlingDokumentBestilt")
@Table(name = "BEHANDLING_DOKUMENT_BESTILT")
public class BehandlingDokumentBestiltEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BEHANDLING_DOK_BESTILT")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "BEHANDLING_DOKUMENT_ID", nullable = false, updatable = false)
    private BehandlingDokumentEntitet behandlingDokument;

    @Convert(converter = DokumentMalType.KodeverdiConverter.class)
    @Column(name = "DOKUMENT_MAL_TYPE", nullable = false)
    private DokumentMalType dokumentMalType;

    @Column(name = "BESTILLING_UUID")
    private UUID bestillingUuid;

    @Embedded
    // trenges til å kunne sette journalpost fra kvittering, siden JournalpostId har satt updatable = false
    @AttributeOverride(name = "journalpostId", column = @Column(name = "JOURNALPOST_ID"))
    private JournalpostId journalpostId;

    @Convert(converter = DokumentMalType.KodeverdiConverter.class)
    @Column(name = "OPPRINNELIG_DOKUMENT_MAL")
    private DokumentMalType opprineligDokumentMal;

    public BehandlingDokumentBestiltEntitet() {
        // for hibernate
    }

    public Long getId() {
        return id;
    }

    public BehandlingDokumentEntitet getBehandlingDokument() {
        return behandlingDokument;
    }

    public DokumentMalType getDokumentMalType() {
        return dokumentMalType;
    }

    public UUID getBestillingUuid() {
        return bestillingUuid;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public DokumentMalType getOpprineligDokumentMal() {
        return opprineligDokumentMal;
    }

    public void setJournalpostId(JournalpostId journalpostId) {
        this.journalpostId = journalpostId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (BehandlingDokumentBestiltEntitet) o;
        return Objects.equals(behandlingDokument, that.behandlingDokument) &&
            Objects.equals(dokumentMalType, that.dokumentMalType) &&
            Objects.equals(bestillingUuid, that.bestillingUuid) &&
            Objects.equals(journalpostId, that.journalpostId) &&
            Objects.equals(opprineligDokumentMal, that.opprineligDokumentMal)
            ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingDokument, dokumentMalType, bestillingUuid, journalpostId, opprineligDokumentMal);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            (id != null ? "id=" + id + ", " : "")
            + "behandlingDokument=" + behandlingDokument + ", "
            + "dokumentMalType=" + dokumentMalType + ", "
            + "bestillingUuid=" + bestillingUuid + ", "
            + "journalpostId=" + journalpostId + ", "
            + "opprineligDokumentMal=" + opprineligDokumentMal
            + ">";
    }

    public static class Builder {
        private BehandlingDokumentBestiltEntitet behandlingDokumentBestiltMal;

        public Builder() {
            behandlingDokumentBestiltMal = new BehandlingDokumentBestiltEntitet();
        }

        public BehandlingDokumentBestiltEntitet.Builder medBehandlingDokument(BehandlingDokumentEntitet behandlingDokument) {
            behandlingDokumentBestiltMal.behandlingDokument = behandlingDokument;
            return this;
        }

        public BehandlingDokumentBestiltEntitet.Builder medDokumentMalType(DokumentMalType dokumentMalType) {
            behandlingDokumentBestiltMal.dokumentMalType = dokumentMalType;
            return this;
        }

        public BehandlingDokumentBestiltEntitet.Builder medBestillingUuid(UUID bestillingUuid) {
            behandlingDokumentBestiltMal.bestillingUuid = bestillingUuid;
            return this;
        }

        public BehandlingDokumentBestiltEntitet.Builder medJournalpostId(JournalpostId journalpostId) {
            behandlingDokumentBestiltMal.journalpostId = journalpostId;
            return this;
        }

        public BehandlingDokumentBestiltEntitet.Builder medOpprinneligDokumentMal(DokumentMalType opprineligDokumentMal) {
            behandlingDokumentBestiltMal.opprineligDokumentMal = opprineligDokumentMal;
            return this;
        }

        public BehandlingDokumentBestiltEntitet build() {
            verifyStateForBuild();
            return behandlingDokumentBestiltMal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(behandlingDokumentBestiltMal.behandlingDokument, "BehandlingDokumentEntitet må være satt");
            Objects.requireNonNull(behandlingDokumentBestiltMal.dokumentMalType, "DokumentMalType må være satt");
            Objects.requireNonNull(behandlingDokumentBestiltMal.bestillingUuid, "BestillingUuid må være satt");
        }
    }
}
