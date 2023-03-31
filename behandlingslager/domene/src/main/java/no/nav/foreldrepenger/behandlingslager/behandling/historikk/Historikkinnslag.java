package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;

@Entity(name = "Historikkinnslag")
@Table(name = "HISTORIKKINNSLAG")
public class Historikkinnslag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_HISTORIKKINNSLAG")
    private Long id;

    @Column(name = "behandling_id", updatable = false)
    private Long behandlingId;

    @Column(name = "fagsak_id", nullable = false, updatable = false)
    private Long fagsakId;

    @Convert(converter = HistorikkAktør.KodeverdiConverter.class)
    @Column(name="historikk_aktoer_id", nullable = false)
    private HistorikkAktør aktør = HistorikkAktør.UDEFINERT;

    @Convert(converter = HistorikkinnslagType.KodeverdiConverter.class)
    @Column(name="historikkinnslag_type", nullable = false)
    private HistorikkinnslagType type = HistorikkinnslagType.UDEFINERT;

    @OneToMany(mappedBy = "historikkinnslag", cascade = CascadeType.ALL)
    private List<HistorikkinnslagDokumentLink> dokumentLinker = new ArrayList<>();

    @OneToMany(mappedBy = "historikkinnslag")
    private List<HistorikkinnslagDel> historikkinnslagDeler = new ArrayList<>();

    @Convert(converter = NavBrukerKjønn.KodeverdiConverter.class)
    @Column(name="bruker_kjoenn", nullable = false, updatable = false)
    private NavBrukerKjønn kjoenn = NavBrukerKjønn.UDEFINERT;

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public void setBehandling(Behandling behandling) {
        this.behandlingId = behandling.getId();
        this.fagsakId = behandling.getFagsakId();
    }

    public void setBehandlingId(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        this.behandlingId = behandlingId;
    }

    public HistorikkAktør getAktør() {
        return Objects.equals(HistorikkAktør.UDEFINERT, aktør) ? null : aktør;
    }

    public void setAktør(HistorikkAktør aktør) {
        this.aktør = aktør == null ? HistorikkAktør.UDEFINERT : aktør;
    }

    public HistorikkinnslagType getType() {
        return type;
    }

    public void setType(HistorikkinnslagType type) {
        this.type = type;
    }

    public List<HistorikkinnslagDokumentLink> getDokumentLinker() {
        return dokumentLinker;
    }

    public void setDokumentLinker(List<HistorikkinnslagDokumentLink> dokumentLinker) {
        this.dokumentLinker = dokumentLinker;
    }

    public NavBrukerKjønn getKjoenn() {
        return kjoenn;
    }

    public void setKjoenn(NavBrukerKjønn kjoenn) {
        this.kjoenn = kjoenn;
    }

    public Long getFagsakId() {
        return fagsakId;
    }

    public void setFagsakId(Long fagsakId) {
        this.fagsakId = fagsakId;
    }

    public List<HistorikkinnslagDel> getHistorikkinnslagDeler() {
        return historikkinnslagDeler;
    }

    public void setHistorikkinnslagDeler(List<HistorikkinnslagDel> historikkinnslagDeler) {
        historikkinnslagDeler.forEach(del -> HistorikkinnslagDel.builder(del).medHistorikkinnslag(this));
        this.historikkinnslagDeler = historikkinnslagDeler;
    }

    public static class Builder {
        private Historikkinnslag historikkinnslag;

        public Builder() {
            historikkinnslag = new Historikkinnslag();
        }

        public Builder medBehandlingId(Long behandlingId) {
            historikkinnslag.behandlingId = behandlingId;
            return this;
        }

        public Builder medFagsakId(Long fagsakId) {
            historikkinnslag.fagsakId = fagsakId;
            return this;
        }

        public Builder medAktør(HistorikkAktør historikkAktør) {
            historikkinnslag.aktør = historikkAktør;
            return this;
        }

        public Builder medType(HistorikkinnslagType type) {
            historikkinnslag.type = type;
            return this;
        }

        public Builder medKjoenn(NavBrukerKjønn kjoenn) {
            historikkinnslag.kjoenn = kjoenn;
            return this;
        }

        public Historikkinnslag build() {
            return historikkinnslag;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Historikkinnslag that)) {
            return false;
        }
        return Objects.equals(behandlingId, that.behandlingId) &&
            Objects.equals(fagsakId, that.fagsakId) &&
            Objects.equals(getAktør(), that.getAktør()) &&
            Objects.equals(type, that.type) &&
            Objects.equals(getDokumentLinker(), that.getDokumentLinker()) &&
            Objects.equals(kjoenn, that.kjoenn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingId, fagsakId, getAktør(), type, getDokumentLinker(), kjoenn);
    }
}
