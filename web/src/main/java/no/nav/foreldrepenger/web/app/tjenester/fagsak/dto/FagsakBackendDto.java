package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

public class FagsakBackendDto {
    private String saksnummer;
    private String saksnummerString;
    private FagsakYtelseType sakstype;
    private FagsakYtelseType fagsakYtelseType;
    private RelasjonsRolleType relasjonsRolleType;
    private FagsakStatus status;
    private Integer dekningsgrad;
    private String aktoerId;


    public FagsakBackendDto() {
        // Injiseres i test
    }

    public FagsakBackendDto(Fagsak fagsak,
                            Integer dekningsgrad) {
        this.saksnummer = fagsak.getSaksnummer().getVerdi();
        this.saksnummerString = fagsak.getSaksnummer().getVerdi();
        this.sakstype = fagsak.getYtelseType();
        this.fagsakYtelseType = fagsak.getYtelseType();
        this.status = fagsak.getStatus();
        this.relasjonsRolleType = fagsak.getRelasjonsRolleType();
        this.dekningsgrad = dekningsgrad;
        this.aktoerId = fagsak.getAktørId().getId();
    }

    public String  getSaksnummer() {
        return saksnummer;
    }

    public String getSaksnummerString() {
        return saksnummerString;
    }

    public FagsakYtelseType getSakstype() {
        return sakstype;
    }

    public FagsakYtelseType getFagsakYtelseType() {
        return fagsakYtelseType;
    }

    public FagsakStatus getStatus() {
        return status;
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
}
