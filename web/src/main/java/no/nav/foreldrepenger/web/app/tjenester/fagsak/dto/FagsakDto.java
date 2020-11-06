package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.web.app.rest.ResourceLink;

public class FagsakDto {
    private Long saksnummer;
    private FagsakYtelseType sakstype;
    private RelasjonsRolleType relasjonsRolleType;
    private FagsakStatus status;
    private LocalDate barnFodt;
    private LocalDateTime opprettet;
    private LocalDateTime endret;
    private Integer antallBarn;
    private Boolean kanRevurderingOpprettes;
    private Boolean skalBehandlesAvInfotrygd;
    private Integer dekningsgrad;
    private String aktoerId;

    private List<ResourceLink> links = new ArrayList<>();
    private List<ResourceLink> onceLinks = new ArrayList<>();

    public FagsakDto() {
        // Injiseres i test
    }

    public FagsakDto(Fagsak fagsak,
                     LocalDate barnFodt,
                     Integer antallBarn,
                     Boolean kanRevurderingOpprettes,
                     Boolean skalBehandlesAvInfotrygd,
                     RelasjonsRolleType relasjonsRolleType,
                     Integer dekningsgrad,
                     List<ResourceLink> links,
                     List<ResourceLink> linksOnce) {
        this.saksnummer = Long.parseLong(fagsak.getSaksnummer().getVerdi());
        this.aktoerId = fagsak.getAktørId().getId();
        this.sakstype = fagsak.getYtelseType();
        this.status = fagsak.getStatus();
        this.opprettet = fagsak.getOpprettetTidspunkt();
        this.endret = fagsak.getEndretTidspunkt();
        this.barnFodt = barnFodt;
        this.antallBarn = antallBarn;
        this.kanRevurderingOpprettes = kanRevurderingOpprettes;
        this.skalBehandlesAvInfotrygd = skalBehandlesAvInfotrygd;
        this.relasjonsRolleType = relasjonsRolleType;
        this.dekningsgrad = dekningsgrad;
        this.links = links;
        this.onceLinks = linksOnce;
    }

    public Long getSaksnummer() {
        return saksnummer;
    }

    public FagsakYtelseType getSakstype() {
        return sakstype;
    }

    public FagsakStatus getStatus() {
        return status;
    }

    public LocalDate getBarnFodt() {
        return barnFodt;
    }

    public Integer getAntallBarn() {
        return antallBarn;
    }

    public LocalDateTime getOpprettet() {
        return opprettet;
    }

    public LocalDateTime getEndret() {
        return endret;
    }

    public Boolean getKanRevurderingOpprettes() {
        return kanRevurderingOpprettes;
    }

    public Boolean getSkalBehandlesAvInfotrygd() {
        return skalBehandlesAvInfotrygd;
    }

    public RelasjonsRolleType getRelasjonsRolleType() {
        return relasjonsRolleType;
    }

    public Integer getDekningsgrad() {
        return dekningsgrad;
    }

    public String getAktoerId() {
        return aktoerId;
    }

    public List<ResourceLink> getLinks() {
        return links;
    }

    public List<ResourceLink> getOnceLinks() {
        return onceLinks;
    }

    @Override
    public String toString() {
        return "<saksnummer=" + saksnummer + //$NON-NLS-1$
            ", sakstype=" + sakstype + //$NON-NLS-1$
            ", status=" + status + //$NON-NLS-1$
            ", barnFodt=" + barnFodt + //$NON-NLS-1$
            ", antallBarn=" + antallBarn + //$NON-NLS-1$
            ", opprettet=" + opprettet + //$NON-NLS-1$
            ", endret=" + endret + //$NON-NLS-1$
            ", relasjonsrolle=" + relasjonsRolleType + //$NON-NLS-1$
            ", dekningsgrad=" + dekningsgrad + //$NON-NLS-1$
            ">";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FagsakDto)) return false;

        FagsakDto fagsakDto = (FagsakDto) o;

        if (!saksnummer.equals(fagsakDto.saksnummer)) return false;
        if (!sakstype.equals(fagsakDto.sakstype)) return false;
        if (!status.equals(fagsakDto.status)) return false;
        if (barnFodt != null ? !barnFodt.equals(fagsakDto.barnFodt) : fagsakDto.barnFodt != null) return false;
        if (opprettet != null ? !opprettet.equals(fagsakDto.opprettet) : fagsakDto.opprettet != null) return false;
        if (!relasjonsRolleType.equals(fagsakDto.relasjonsRolleType)) return false;
        if (!dekningsgrad.equals(fagsakDto.dekningsgrad)) return false;
        return endret != null ? endret.equals(fagsakDto.endret) : fagsakDto.endret == null;
    }

    @Override
    public int hashCode() {
        int result = saksnummer.hashCode();
        result = 31 * result + sakstype.hashCode();
        result = 31 * result + status.hashCode();
        result = 31 * result + (barnFodt != null ? barnFodt.hashCode() : 0);
        result = 31 * result + relasjonsRolleType.hashCode();
        result = 31 * result + dekningsgrad.hashCode();
        result = 31 * result + (opprettet != null ? opprettet.hashCode() : 0);
        result = 31 * result + (endret != null ? endret.hashCode() : 0);
        return result;
    }

}
