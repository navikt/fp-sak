package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.oppdrag;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class OppdragPatchDto implements AbacDto {

    @Min(0)
    @Max(Long.MAX_VALUE)
    @NotNull
    private Long behandlingId;

    @NotNull
    private Boolean brukerErMottaker;

    @Size(min = 9, max = 9)
    @Pattern(regexp = "^\\d*$")
    private String arbeidsgiverOrgNr;

    @NotNull
    @Pattern(regexp = "^UEND|ENDR|NY$")
    private String kodeEndring;

    @NotNull
    @DecimalMin("100000000000")
    @DecimalMax("300000000000")
    private Long fagsystemId;


    @Valid
    @NotNull
    @Size(min = 1, max = 30)
    private List<OppdragslinjePatchDto> oppdragslinjer;

    @AssertTrue
    public boolean isEntenBrukerMottakerEllerArbeidsgiverOppgitt() {
        boolean arbeidsgiverOppgitt = arbeidsgiverOrgNr != null;
        return brukerErMottaker != arbeidsgiverOppgitt;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public boolean erBrukerMottaker() {
        return brukerErMottaker;
    }

    public long getFagsystemId() {
        return fagsystemId;
    }

    public String getArbeidsgiverOrgNr() {
        return arbeidsgiverOrgNr;
    }

    public String getKodeEndring() {
        return kodeEndring;
    }

    public List<OppdragslinjePatchDto> getOppdragslinjer() {
        return oppdragslinjer;
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett()
            .leggTil(AppAbacAttributtType.BEHANDLING_ID, behandlingId);
    }
}
