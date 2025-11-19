package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.oppdrag;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

@JsonInclude(value = JsonInclude.Include.NON_ABSENT, content = JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class OppdragPatchDto implements AbacDto {

    @Valid
    @NotNull
    @JsonProperty("behandingUuid")
    private UUID behandlingUuid;

    @NotNull
    @JsonProperty("brukerErMottaker")
    private Boolean brukerErMottaker;

    @Size(min = 9, max = 9)
    @Pattern(regexp = "^\\d*$")
    @JsonProperty("arbeidsgiverOrgnr")
    private String arbeidsgiverOrgNr;

    @NotNull
    @Pattern(regexp = "^UEND$|^ENDR$|^NY$")
    @JsonProperty("kodeEndring")
    private String kodeEndring;

    @NotNull
    @Min(10000000000L)
    @Max(300000000000L)
    @JsonProperty("fagsystemId")
    private Long fagsystemId;

    @NotNull
    @Size(min = 1, max = 20)
    @JsonProperty("oppdragslinjer")
    private List<@Valid OppdragslinjePatchDto> oppdragslinjer;

    @JsonProperty("bruk-ompostering116")
    private boolean brukOmpostering116 = false;

    @JsonProperty("omposter-fom")
    private LocalDate omposterFom;

    @AssertTrue
    boolean isEntenBrukerMottakerEllerArbeidsgiverOppgitt() {
        var arbeidsgiverOppgitt = arbeidsgiverOrgNr != null;
        return brukerErMottaker != arbeidsgiverOppgitt;
    }

    @AssertFalse(message = "må sette bruk-ompostering116 for å bruke omposter-fom")
    boolean isOmposteringFomSattUtenBrukOmpostering116Satt() {
        return omposterFom != null && !brukOmpostering116;
    }

    @AssertTrue
    boolean isAntallDagerSannsynlig() {
        long sum = 0;
        for (var dto : oppdragslinjer) {
            sum += ChronoUnit.DAYS.between(dto.getFom(), dto.getTom()) + 1;
        }
        return sum < 365;
    }

    @AssertTrue
    boolean isSumBeløpSannsynlig() {
        long estimertSum = 0;
        for (var dto : oppdragslinjer) {
            if ("ENG".equals(dto.getSatsType())) {
                estimertSum += dto.getSats();
            } else {
                var dager = ChronoUnit.DAYS.between(dto.getFom(), dto.getTom()) + 1;
                estimertSum += dager * dto.getSats();
            }
        }
        //beløp for FP kan være opptil 6G over ett år
        return estimertSum < 600000;
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
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

    public LocalDate getOmposterFom() {
        return omposterFom;
    }

    public boolean taMedOmpostering116() {
        return brukOmpostering116;
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett()
            .leggTil(AppAbacAttributtType.BEHANDLING_UUID, behandlingUuid);
    }
}
