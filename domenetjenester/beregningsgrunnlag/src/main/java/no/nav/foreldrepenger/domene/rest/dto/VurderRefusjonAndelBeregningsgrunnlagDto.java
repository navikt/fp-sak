package no.nav.foreldrepenger.domene.rest.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import no.nav.vedtak.util.InputValideringRegex;

public class VurderRefusjonAndelBeregningsgrunnlagDto {

    @Valid
    @Pattern(regexp = InputValideringRegex.ARBEIDSGIVER)
    private String arbeidsgiverOrgnr;

    @Valid
    @Pattern(regexp = InputValideringRegex.ARBEIDSGIVER)
    private String arbeidsgiverAktoerId;

    private UUID internArbeidsforholdRef;

    @Valid
    @NotNull
    private LocalDate fastsattRefusjonFom;

    @Valid
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer delvisRefusjonPrMndFørStart;

    VurderRefusjonAndelBeregningsgrunnlagDto() {
        // Jackson
    }

    public VurderRefusjonAndelBeregningsgrunnlagDto(@Valid String arbeidsgiverOrgnr,
                                                    @Valid String arbeidsgiverAktoerId,
                                                    @Valid UUID internArbeidsforholdRef,
                                                    @Valid @NotNull LocalDate fastsattRefusjonFom,
                                                    @Valid Integer delvisRefusjonPrMndFørStart) {
        this.arbeidsgiverOrgnr = arbeidsgiverOrgnr;
        this.arbeidsgiverAktoerId = arbeidsgiverAktoerId;
        this.internArbeidsforholdRef = internArbeidsforholdRef;
        this.fastsattRefusjonFom = fastsattRefusjonFom;
        this.delvisRefusjonPrMndFørStart = delvisRefusjonPrMndFørStart;
    }

    public String getArbeidsgiverOrgnr() {
        return arbeidsgiverOrgnr;
    }

    public String getArbeidsgiverAktoerId() {
        return arbeidsgiverAktoerId;
    }

    public UUID getInternArbeidsforholdRef() {
        return internArbeidsforholdRef;
    }

    public LocalDate getFastsattRefusjonFom() {
        return fastsattRefusjonFom;
    }

    public Integer getDelvisRefusjonPrMndFørStart() {
        return delvisRefusjonPrMndFørStart;
    }
}
