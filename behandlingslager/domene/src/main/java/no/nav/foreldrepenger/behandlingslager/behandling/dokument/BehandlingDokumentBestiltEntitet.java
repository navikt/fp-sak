package no.nav.foreldrepenger.behandlingslager.behandling.dokument;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

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

    public BehandlingDokumentBestiltEntitet() {
        // for hibernate
    }

    public BehandlingDokumentEntitet getBehandlingDokument() {
        return behandlingDokument;
    }

    public String getDokumentMalType() {
        return dokumentMalType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BehandlingDokumentBestiltEntitet that = (BehandlingDokumentBestiltEntitet) o;
        return Objects.equals(behandlingDokument, that.behandlingDokument) &&
            Objects.equals(dokumentMalType, that.dokumentMalType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingDokument, dokumentMalType);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            (id != null ? "id=" + id + ", " : "")
            + "behandlingDokument=" + behandlingDokument + ", "
            + "dokumentMalType=" + dokumentMalType + ", "
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

        public BehandlingDokumentBestiltEntitet build() {
            verifyStateForBuild();
            return behandlingDokumentBestiltMal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(behandlingDokumentBestiltMal.behandlingDokument, "BehandlingDokumentEntitet må være satt");
        }
    }
}
