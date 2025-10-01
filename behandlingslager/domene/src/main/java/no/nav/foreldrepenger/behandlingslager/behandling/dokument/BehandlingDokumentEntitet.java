package no.nav.foreldrepenger.behandlingslager.behandling.dokument;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

@Entity(name = "BehandlingDokument")
@Table(name = "BEHANDLING_DOKUMENT")
public class BehandlingDokumentEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BEHANDLING_DOKUMENT")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @Column(name = "overstyrt_brev_overskrift")
    private String overstyrtBrevOverskrift;

    @Lob
    @Column(name = "overstyrt_brev_fritekst")
    private String overstyrtBrevFritekst;

    @Lob
    @Column(name = "overstyrt_brev_fritekst_html")
    private String overstyrtBrevFritekstHtml;

    @Lob
    @Column(name = "vedtak_fritekst") // bør hete utfyllende tekst
    private String vedtakFritekst;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "behandlingDokument", cascade = CascadeType.ALL)
    private List<BehandlingDokumentBestiltEntitet> bestilteDokumenter = new ArrayList<>();

    protected BehandlingDokumentEntitet() {
        // for hibernate
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public String getOverstyrtBrevOverskrift() {
        return overstyrtBrevOverskrift;
    }

    public String getOverstyrtBrevFritekst() {
        return overstyrtBrevFritekst;
    }

    public String getOverstyrtBrevFritekstHtml() {
        return overstyrtBrevFritekstHtml;
    }

    public String getVedtakFritekst() {
        return vedtakFritekst;
    }

    public List<BehandlingDokumentBestiltEntitet> getBestilteDokumenter() {
        return bestilteDokumenter;
    }

    public void leggTilBestiltDokument(BehandlingDokumentBestiltEntitet bestiltDokument) {
        Objects.requireNonNull(bestiltDokument, "bestiltDokument");
        bestilteDokumenter.add(bestiltDokument);
    }

    public boolean harFritekst() {
        return getOverstyrtBrevFritekstHtml() != null || getOverstyrtBrevFritekst() != null || getVedtakFritekst() != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (BehandlingDokumentEntitet) o;
        return Objects.equals(behandlingId, that.behandlingId) &&
            Objects.equals(overstyrtBrevOverskrift, that.overstyrtBrevOverskrift) &&
            Objects.equals(overstyrtBrevFritekst, that.overstyrtBrevFritekst) &&
            Objects.equals(vedtakFritekst, that.vedtakFritekst);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingId, overstyrtBrevOverskrift, overstyrtBrevFritekst, vedtakFritekst);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            (id != null ? "id=" + id + ", " : "")
            + "behandling=" + behandlingId + ", "
            + ">";
    }

    public static class Builder {
        private BehandlingDokumentEntitet behandlingDokumentMal;

        private Builder() {
            behandlingDokumentMal = new BehandlingDokumentEntitet();
        }

        public static BehandlingDokumentEntitet.Builder ny() {
            return new Builder();
        }

        public static BehandlingDokumentEntitet.Builder fraEksisterende(BehandlingDokumentEntitet behandlingDokument) {
            var builder = new Builder()
                .medBehandling(behandlingDokument.getBehandlingId())
                .medOverstyrtBrevOverskrift(behandlingDokument.getOverstyrtBrevOverskrift())
                .medOverstyrtBrevFritekst(behandlingDokument.getOverstyrtBrevFritekst())
                .medOverstyrtBrevFritekstHtml(behandlingDokument.getOverstyrtBrevFritekstHtml())
                .medUtfyllendeTekstAutomatiskVedtaksbrev(behandlingDokument.getVedtakFritekst())
                .medBestilteDokumenter(behandlingDokument.getBestilteDokumenter());
            builder.behandlingDokumentMal.id = behandlingDokument.id;
            return builder;
        }

        public BehandlingDokumentEntitet.Builder medBehandling(Long behandlingId) {
            behandlingDokumentMal.behandlingId = behandlingId;
            return this;
        }

        public BehandlingDokumentEntitet.Builder medOverstyrtBrevOverskrift(String overstyrtBrevOverskrift) {
            behandlingDokumentMal.overstyrtBrevOverskrift = overstyrtBrevOverskrift;
            return this;
        }

        public BehandlingDokumentEntitet.Builder medOverstyrtBrevFritekst(String overstyrtBrevFritekst) {
            behandlingDokumentMal.overstyrtBrevFritekst = overstyrtBrevFritekst;
            return this;
        }

        public BehandlingDokumentEntitet.Builder medOverstyrtBrevFritekstHtml(String overstyrtBrevFritekstHtml) {
            behandlingDokumentMal.overstyrtBrevFritekstHtml = overstyrtBrevFritekstHtml;
            return this;
        }

        public BehandlingDokumentEntitet.Builder medUtfyllendeTekstAutomatiskVedtaksbrev(String vedtakFritekst) {
            behandlingDokumentMal.vedtakFritekst = vedtakFritekst;
            return this;
        }

        public BehandlingDokumentEntitet.Builder medBestilteDokumenter(List<BehandlingDokumentBestiltEntitet> bestilteDokumenter) {
            behandlingDokumentMal.bestilteDokumenter = bestilteDokumenter;
            return this;
        }

        public BehandlingDokumentEntitet build() {
            verifyStateForBuild();
            return behandlingDokumentMal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(behandlingDokumentMal.behandlingId, "Behandling må være satt");
            if (behandlingDokumentMal.overstyrtBrevFritekst != null || behandlingDokumentMal.overstyrtBrevOverskrift != null) {
                Objects.requireNonNull(behandlingDokumentMal.overstyrtBrevFritekst, "overstyrtBrevFritekst må være satt");
                Objects.requireNonNull(behandlingDokumentMal.overstyrtBrevOverskrift, "overstyrtBrevOverskrift må være satt");
            }
        }
    }
}
