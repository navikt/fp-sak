package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.oppdrag;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OppdragslinjePatchDto {

    @NotNull
    @Pattern(regexp = "^NY|ENDR$")
    @JsonProperty("kodeEndring")
    private String kodeEndring;

    @JsonProperty("opphoerFom")
    private LocalDate opphørFom;

    @NotNull
    @Pattern(regexp = "^(FP(AD|SV)(ATORD|ATFRI|SND-OP|ATAL|ATSJO|SNDDM-OP|SNDJB-OP|SNDFI|REFAG-IOP|REFAGFER-IOP))|FPENFOD-OP|FPENAD-OP?|$")
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
    @Pattern(regexp = "^ENG|DAG$")
    @JsonProperty("satsType")
    private String satsType;

    @NotNull
    @Min(100000000000000L)
    @Max(300000000000000L)
    @JsonProperty("delytelseId")
    private Long delytelseId;

    @Min(100000000000000L)
    @Max(300000000000000L)
    @JsonProperty("refDelytelseId")
    private Long refDelytelseId;

    @Min(100000000000L)
    @Max(300000000000L)
    @JsonProperty("refFagsystemId")
    private Long refFagsystemId;

    @AssertTrue
    public boolean isBeggeEllerIngenRefSatt() {
        return (refDelytelseId == null) == (refFagsystemId == null);
    }

    public long getDelytelseId() {
        return delytelseId;
    }

    @AssertFalse
    public boolean isPeriodeUgyldig() {
        return tom.isBefore(fom);
    }

    @AssertTrue
    public boolean isPeriodeSannsynlig() {
        long antallDager = ChronoUnit.DAYS.between(fom, tom);
        switch (satsType) {
            case "ENG":
                return antallDager == 1;
            case "DAG":
                int maxSannsynligLengdeDager = 20 * 7;
                return antallDager < maxSannsynligLengdeDager;
            default:
                throw new IllegalArgumentException("Ikke-støttet satsType: " + satsType);
        }
    }

    @AssertTrue
    public boolean isSatsSannsynlig() {
        switch (satsType) {
            case "ENG":
                return sats < 100000;
            case "DAG":
                return sats < 3000;
            default:
                throw new IllegalArgumentException("Ikke-støttet satsType: " + satsType);
        }
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
