package no.nav.foreldrepenger.domene.rest.dto;

import java.time.LocalDate;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class VurderRefusjonAndelBeregningsgrunnlagDto {

    @Valid
    private String arbeidsgiverOrgnr;

    @Valid
    private String arbeidsgiverAktoerId;

    @Valid
    private String internArbeidsforholdRef;

    @Valid
    @NotNull
    private LocalDate fastsattRefusjonFom;

    @Valid
    private Integer delvisRefusjonPrMndFørStart;

    VurderRefusjonAndelBeregningsgrunnlagDto() {
        // Jackson
    }

    public VurderRefusjonAndelBeregningsgrunnlagDto(@Valid String arbeidsgiverOrgnr,
                                                    @Valid String arbeidsgiverAktoerId,
                                                    @Valid String internArbeidsforholdRef,
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

    public String getInternArbeidsforholdRef() {
        return internArbeidsforholdRef;
    }

    public LocalDate getFastsattRefusjonFom() {
        return fastsattRefusjonFom;
    }

    public Integer getDelvisRefusjonPrMndFørStart() {
        return delvisRefusjonPrMndFørStart;
    }
}
