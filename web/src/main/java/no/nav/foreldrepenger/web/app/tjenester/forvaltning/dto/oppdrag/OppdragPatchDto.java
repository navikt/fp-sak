package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.oppdrag;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

@JsonInclude(value = JsonInclude.Include.NON_ABSENT, content = JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class OppdragPatchDto implements AbacDto {

    @Min(0)
    @Max(Long.MAX_VALUE)
    @NotNull
    @JsonProperty("behandingId")
    private Long behandlingId;

    @NotNull
    @JsonProperty("brukerErMottaker")
    private Boolean brukerErMottaker;

    @Size(min = 9, max = 9)
    @Pattern(regexp = "^\\d*$")
    @JsonProperty("arbeidsgiverOrgnr")
    private String arbeidsgiverOrgNr;

    @NotNull
    @Pattern(regexp = "^UEND|ENDR|NY$")
    @JsonProperty("kodeEndring")
    private String kodeEndring;

    @NotNull
    @Min(10000000000L)
    @Max(300000000000L)
    @JsonProperty("fagsystemId")
    private Long fagsystemId;

    @Valid
    @NotNull
    @Size(min = 1, max = 30)
    @JsonProperty("oppdragslinjer")
    private List<OppdragslinjePatchDto> oppdragslinjer;

    @AssertTrue
    boolean isEntenBrukerMottakerEllerArbeidsgiverOppgitt() {
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
