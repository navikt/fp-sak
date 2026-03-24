package no.nav.foreldrepenger.behandlingslager.behandling.dokument;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

@Entity(name = "BehandlingBrevMellomlagring")
@Table(name = "BEHANDLING_BREV_MELLOMLAGRING")
public class BehandlingBrevMellomlagringEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BREV_MELLOMLAGRING")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "BEHANDLING_DOKUMENT_ID", nullable = false, updatable = false)
    private BehandlingDokumentEntitet behandlingDokument;

    @Enumerated(EnumType.STRING)
    @Column(name = "DOKUMENT_MAL_TYPE", nullable = false, updatable = false)
    private DokumentMalType dokumentMalType;

    @Lob
    @Column(name = "FRITEKST_HTML")
    private String fritekstHtml;

    protected BehandlingBrevMellomlagringEntitet() {
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

    public String getFritekstHtml() {
        return fritekstHtml;
    }

    public void setFritekstHtml(String fritekstHtml) {
        this.fritekstHtml = fritekstHtml;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (BehandlingBrevMellomlagringEntitet) o;
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
            + "dokumentMalType=" + dokumentMalType
            + ">";
    }

    public static class Builder {
        private BehandlingBrevMellomlagringEntitet entitet;

        private Builder() {
            entitet = new BehandlingBrevMellomlagringEntitet();
        }

        public static Builder ny() {
            return new Builder();
        }

        public static Builder fraEksisterende(BehandlingBrevMellomlagringEntitet eksisterende) {
            var builder = new Builder();
            builder.entitet.id = eksisterende.id;
            builder.entitet.behandlingDokument = eksisterende.behandlingDokument;
            builder.entitet.dokumentMalType = eksisterende.dokumentMalType;
            builder.entitet.fritekstHtml = eksisterende.fritekstHtml;
            return builder;
        }

        public Builder medBehandlingDokument(BehandlingDokumentEntitet behandlingDokument) {
            entitet.behandlingDokument = behandlingDokument;
            return this;
        }

        public Builder medDokumentMalType(DokumentMalType dokumentMalType) {
            entitet.dokumentMalType = dokumentMalType;
            return this;
        }

        public Builder medFritekstHtml(String fritekstHtml) {
            entitet.fritekstHtml = fritekstHtml;
            return this;
        }

        public BehandlingBrevMellomlagringEntitet build() {
            Objects.requireNonNull(entitet.behandlingDokument, "BehandlingDokumentEntitet må være satt");
            Objects.requireNonNull(entitet.dokumentMalType, "DokumentMalType må være satt");
            return entitet;
        }
    }
}
