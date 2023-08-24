package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.oppdrag;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_ABSENT, content = JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class OppdragslinjePatchDto {

    @NotNull
    @Pattern(regexp = "^(NY|ENDR)$")
    @JsonProperty("kodeEndring")
    private String kodeEndring;

    @JsonProperty("opphoerFom")
    private LocalDate opphørFom;

    @NotNull
    @Pattern(regexp = "^((FP(AD|SV)?(ATORD|ATFRI|SND-OP|ATAL|ATSJO|SNDDM-OP|SNDJB-OP|SNDFI|REFAG-IOP|REFAGFER-IOP))|FPATFER|FPENFOD-OP|FPENAD-OP)$")
    @JsonProperty("kodeKlassifik")
    private String kodeKlassifik;

    @NotNull
    @JsonProperty("fom")
    private LocalDate fom;

    @NotNull
    @JsonProperty("tom")
    private LocalDate tom;

    @NotNull
    @Min(1)
    @Max(100000)
    @JsonProperty("sats")
    private Long sats;

    @NotNull
    @Pattern(regexp = "^(ENG|DAG)$")
    @JsonProperty("satsType")
    private String satsType;

    @NotNull
    @Min(10000000000000L)
    @Max(300000000000000L)
    @JsonProperty("delytelseId")
    private Long delytelseId;

    @Min(10000000000000L)
    @Max(300000000000000L)
    @JsonProperty("refDelytelseId")
    private Long refDelytelseId;

    @Min(10000000000L)
    @Max(300000000000L)
    @JsonProperty("refFagsystemId")
    private Long refFagsystemId;

    @AssertTrue
    private boolean isBeggeEllerIngenRefSatt() {
        return (refDelytelseId == null) == (refFagsystemId == null);
    }

    @AssertFalse
    boolean isPeriodeUgyldig() {
        return tom.isBefore(fom);
    }

    @AssertTrue
    boolean isPeriodeSannsynlig() {
        var antallDager = ChronoUnit.DAYS.between(fom, tom) + 1;
        switch (satsType) {
            case "ENG":
                return antallDager <= 31; //for feriepenger brukes 1 måned, for ES brukes 1 dag
            case "DAG":
                //vanligvis begrenset til 17-18 uker, men kan være lenger ved gradert uttak
                var maxSannsynligLengdeDager = 38 * 7;
                return antallDager < maxSannsynligLengdeDager;
            default:
                return false;
        }
    }

    @AssertTrue
    boolean isSatsSannsynlig() {
        return switch (satsType) {
            case "ENG" -> sats < 100000;
            case "DAG" -> sats < 3000;
            default -> false;
        };
    }

    public long getDelytelseId() {
        return delytelseId;
    }

    public Long getRefDelytelseId() {
        return refDelytelseId;
    }

    public Long getRefFagsystemId() {
        return refFagsystemId;
    }

    public String getKodeEndring() {
        return kodeEndring;
    }

    public LocalDate getOpphørFom() {
        return opphørFom;
    }

    public String getKodeKlassifik() {
        return kodeKlassifik;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public Long getSats() {
        return sats;
    }

    public String getSatsType() {
        return satsType;
    }


}
