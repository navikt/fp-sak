package no.nav.foreldrepenger.domene.rest.dto.fordeling;


import java.time.LocalDate;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.AndelKilde;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class FordelRedigerbarAndelDto {

    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long andelsnr;
    @Pattern(regexp = "[\\d]{9}|[\\d]{13}")
    private String arbeidsgiverId;
    private String arbeidsforholdId;
    @NotNull
    private Boolean nyAndel;
    private AndelKilde kilde;
    private AktivitetStatus aktivitetStatus;
    private OpptjeningAktivitetType arbeidsforholdType;
    private Boolean lagtTilAvSaksbehandler;
    private LocalDate beregningsperiodeFom;
    private LocalDate beregningsperiodeTom;


    protected FordelRedigerbarAndelDto() { // NOSONAR
        // Jackson
    }

    public FordelRedigerbarAndelDto(@Min(0) @Max(Long.MAX_VALUE) Long andelsnr,
                                    @Pattern(regexp = "[\\d]{9}|[\\d]{13}") String arbeidsgiverId,
                                    String arbeidsforholdId, @NotNull Boolean nyAndel,
                                    AndelKilde kilde,
                                    AktivitetStatus aktivitetStatus,
                                    OpptjeningAktivitetType arbeidsforholdType,
                                    Boolean lagtTilAvSaksbehandler,
                                    LocalDate beregningsperiodeFom,
                                    LocalDate beregningsperiodeTom) {
        this.andelsnr = andelsnr;
        this.arbeidsgiverId = arbeidsgiverId;
        this.arbeidsforholdId = arbeidsforholdId;
        this.nyAndel = nyAndel;
        this.kilde = kilde;
        this.aktivitetStatus = aktivitetStatus;
        this.arbeidsforholdType = arbeidsforholdType;
        this.lagtTilAvSaksbehandler = lagtTilAvSaksbehandler;
        this.beregningsperiodeFom = beregningsperiodeFom;
        this.beregningsperiodeTom = beregningsperiodeTom;
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

    public AndelKilde getKilde() {
        return kilde;
    }
}
