package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import java.time.LocalDateTime;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

public class FagsakBackendDto {
    private Long saksnummer;
    private FagsakYtelseType sakstype;
    private RelasjonsRolleType relasjonsRolleType;
    private FagsakStatus status;
    private LocalDateTime opprettet;
    private LocalDateTime endret;
    private Integer dekningsgrad;
    private String aktoerId;


    public FagsakBackendDto() {
        // Injiseres i test
    }

    public FagsakBackendDto(Fagsak fagsak,
                            Integer dekningsgrad) {
        this.saksnummer = Long.parseLong(fagsak.getSaksnummer().getVerdi());
        this.sakstype = fagsak.getYtelseType();
        this.status = fagsak.getStatus();
        this.relasjonsRolleType = fagsak.getRelasjonsRolleType();
        this.opprettet = fagsak.getOpprettetTidspunkt();
        this.endret = fagsak.getEndretTidspunkt();
        this.dekningsgrad = dekningsgrad;
        this.aktoerId = fagsak.getAktørId().getId();
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

    public RelasjonsRolleType getRelasjonsRolleType() {
        return relasjonsRolleType;
    }

    public LocalDateTime getOpprettet() {
        return opprettet;
    }

    public LocalDateTime getEndret() {
        return endret;
    }

    public Integer getDekningsgrad() {
        return dekningsgrad;
    }

    public String getAktoerId() {
        return aktoerId;
    }
}
