package no.nav.foreldrepenger.domene.rest.dto;


import java.time.LocalDate;
import java.util.Objects;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.util.InputValideringRegex;

public class RedigerbarAndelDto {

    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long andelsnr;
    @Pattern(regexp = InputValideringRegex.ARBEIDSGIVER)
    private String arbeidsgiverId;
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsforholdId;
    @NotNull
    private Boolean nyAndel;
    private AktivitetStatus aktivitetStatus;
    private OpptjeningAktivitetType arbeidsforholdType;
    private Boolean lagtTilAvSaksbehandler;
    private LocalDate beregningsperiodeFom;
    private LocalDate beregningsperiodeTom;


    protected RedigerbarAndelDto() {
        // Jackson
    }

    public RedigerbarAndelDto(Boolean nyAndel,
                              String arbeidsgiverId, String internArbeidsforholdId,
                              Long andelsnr,
                              Boolean lagtTilAvSaksbehandler,
                              AktivitetStatus aktivitetStatus,
                              OpptjeningAktivitetType arbeidsforholdType) {
        Objects.requireNonNull(aktivitetStatus, "aktivitetStatus");
        Objects.requireNonNull(arbeidsforholdType, "arbeidsforholdType");
        this.nyAndel = nyAndel;
        this.arbeidsgiverId = arbeidsgiverId;
        this.arbeidsforholdId = internArbeidsforholdId;
        this.andelsnr = andelsnr;
        this.lagtTilAvSaksbehandler = lagtTilAvSaksbehandler;
        this.aktivitetStatus = aktivitetStatus;
        this.arbeidsforholdType = arbeidsforholdType;
    }

    public RedigerbarAndelDto(Boolean nyAndel,
                              String arbeidsgiverId, InternArbeidsforholdRef arbeidsforholdId,
                              Long andelsnr,
                              Boolean lagtTilAvSaksbehandler,
                              AktivitetStatus aktivitetStatus, OpptjeningAktivitetType arbeidsforholdType) {
        Objects.requireNonNull(aktivitetStatus, "aktivitetStatus");
        Objects.requireNonNull(arbeidsforholdType, "arbeidsforholdType");
        this.nyAndel = nyAndel;
        this.arbeidsgiverId = arbeidsgiverId;
        this.arbeidsforholdId = arbeidsforholdId == null ? null : arbeidsforholdId.getReferanse();
        this.andelsnr = andelsnr;
        this.lagtTilAvSaksbehandler = lagtTilAvSaksbehandler;
        this.aktivitetStatus = aktivitetStatus;
        this.arbeidsforholdType = arbeidsforholdType;
    }


    public RedigerbarAndelDto(Boolean nyAndel,
                              Long andelsnr,
                              Boolean lagtTilAvSaksbehandler,
                              AktivitetStatus aktivitetStatus, OpptjeningAktivitetType arbeidsforholdType) {
        this(nyAndel, null, (InternArbeidsforholdRef) null, andelsnr, lagtTilAvSaksbehandler, aktivitetStatus, arbeidsforholdType);
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public Long getAndelsnr() {
        return andelsnr;
    }

    public InternArbeidsforholdRef getArbeidsforholdId() {
        return InternArbeidsforholdRef.ref(arbeidsforholdId);
    }

    public String getArbeidsgiverId() {
        return arbeidsgiverId;
    }

    public OpptjeningAktivitetType getArbeidsforholdType() {
        return arbeidsforholdType;
    }

    public Boolean getNyAndel() {
        return nyAndel;
    }

    public Boolean getLagtTilAvSaksbehandler() {
        return lagtTilAvSaksbehandler;
    }

    public LocalDate getBeregningsperiodeFom() {
        return beregningsperiodeFom;
    }

    public LocalDate getBeregningsperiodeTom() {
        return beregningsperiodeTom;
    }
}
