package no.nav.foreldrepenger.behandlingslager.behandling.dokument;

import java.util.Objects;
import java.util.UUID;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

@Entity(name = "BehandlingDokumentBestilt")
@Table(name = "BEHANDLING_DOKUMENT_BESTILT")
public class BehandlingDokumentBestiltEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BEHANDLING_DOK_BESTILT")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "behandling_dokument_id", nullable = false, updatable = false)
    private BehandlingDokumentEntitet behandlingDokument;

    @Column(name = "dokument_mal_type", nullable = false)
    private String dokumentMalType;

    @Column(name = "bestilling_uuid")
    private UUID bestillingUuid;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "journalpostId", column = @Column(name = "journalpost_id")))
    private JournalpostId journalpostId;

    public BehandlingDokumentBestiltEntitet() {
        // for hibernate
    }

    public Long getId() {
        return id;
    }

    public BehandlingDokumentEntitet getBehandlingDokument() {
        return behandlingDokument;
    }

    public String getDokumentMalType() {
        return dokumentMalType;
    }

    public UUID getBestillingUuid() {
        return bestillingUuid;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
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
            Objects.equals(journalpostId, that.journalpostId)
            ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingDokument, dokumentMalType, bestillingUuid, journalpostId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            (id != null ? "id=" + id + ", " : "")
            + "behandlingDokument=" + behandlingDokument + ", "
            + "dokumentMalType=" + dokumentMalType + ", "
            + "bestillingUuid=" + bestillingUuid + ", "
            + "journalpostId=" + journalpostId
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

        public BehandlingDokumentBestiltEntitet.Builder medDokumentMalType(String dokumentMalType) {
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
