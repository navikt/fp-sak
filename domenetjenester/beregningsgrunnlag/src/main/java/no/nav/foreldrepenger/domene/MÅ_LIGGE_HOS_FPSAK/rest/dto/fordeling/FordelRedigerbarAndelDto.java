package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.fordeling;


import java.time.LocalDate;
import java.util.Objects;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus;
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
    private AktivitetStatus aktivitetStatus;
    private OpptjeningAktivitetType arbeidsforholdType;
    private Boolean lagtTilAvSaksbehandler;
    private LocalDate beregningsperiodeFom;
    private LocalDate beregningsperiodeTom;


    protected FordelRedigerbarAndelDto() { // NOSONAR
        // Jackson
    }

    public FordelRedigerbarAndelDto(Boolean nyAndel,
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

    public FordelRedigerbarAndelDto(Boolean nyAndel,
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


    public FordelRedigerbarAndelDto(Boolean nyAndel,
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
