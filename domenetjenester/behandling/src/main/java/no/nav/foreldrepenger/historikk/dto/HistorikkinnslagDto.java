package no.nav.foreldrepenger.historikk.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;

public class HistorikkinnslagDto implements Comparable<HistorikkinnslagDto> {
    private Long behandlingId;
    private UUID behandlingUuid;
    private HistorikkinnslagType type;
    private HistorikkAktør aktoer;
    private NavBrukerKjønn kjoenn;
    private String opprettetAv;
    private LocalDateTime opprettetTidspunkt;
    private List<HistorikkInnslagDokumentLinkDto> dokumentLinks;
    private List<HistorikkinnslagDelDto> historikkinnslagDeler;

    public Long getBehandlingId() {
        return behandlingId;
    }

    public void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public void setBehandlingUuid(UUID behandlingUuid) {
        this.behandlingUuid = behandlingUuid;
    }

    public List<HistorikkInnslagDokumentLinkDto> getDokumentLinks() {
        return dokumentLinks;
    }

    public void setDokumentLinks(List<HistorikkInnslagDokumentLinkDto> dokumentLinks) {
        this.dokumentLinks = dokumentLinks;
    }

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public void setOpprettetAv(String opprettetAv) {
        this.opprettetAv = opprettetAv;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    public void setOpprettetTidspunkt(LocalDateTime opprettetTidspunkt) {
        this.opprettetTidspunkt = opprettetTidspunkt;
    }

    public HistorikkinnslagType getType() {
        return type;
    }

    public void setType(HistorikkinnslagType type) {
        this.type = type;
    }

    public HistorikkAktør getAktoer() {
        return aktoer;
    }

    public void setAktoer(HistorikkAktør aktoer) {
        this.aktoer = aktoer;
    }

    public NavBrukerKjønn getKjoenn() {
        return kjoenn;
    }

    public void setKjoenn(NavBrukerKjønn kjoenn) {
        this.kjoenn = kjoenn;
    }

    public List<HistorikkinnslagDelDto> getHistorikkinnslagDeler() {
        return historikkinnslagDeler;
    }

    public void setHistorikkinnslagDeler(List<HistorikkinnslagDelDto> historikkinnslagDeler) {
        this.historikkinnslagDeler = historikkinnslagDeler;
    }

    @Override
    public int compareTo(HistorikkinnslagDto that) {
        var comparatorValue = that.getOpprettetTidspunkt().compareTo(this.getOpprettetTidspunkt());
        if ((comparatorValue == 0) && that.getType().equals(HistorikkinnslagType.REVURD_OPPR)) {
            return -1;
        }
        return comparatorValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HistorikkinnslagDto that)) {
            return false;
        }
        return Objects.equals(getBehandlingId(), that.getBehandlingId()) &&
                Objects.equals(getBehandlingUuid(), that.getBehandlingUuid()) &&
                Objects.equals(getType(), that.getType()) &&
                Objects.equals(getAktoer(), that.getAktoer()) &&
                Objects.equals(getKjoenn(), that.getKjoenn()) &&
                Objects.equals(getOpprettetAv(), that.getOpprettetAv()) &&
                Objects.equals(getOpprettetTidspunkt(), that.getOpprettetTidspunkt()) &&
                Objects.equals(getDokumentLinks(), that.getDokumentLinks());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBehandlingId(), getBehandlingUuid(), getType(), getAktoer(),
            getKjoenn(), getOpprettetAv(), getOpprettetTidspunkt(), getDokumentLinks());
    }
}
