package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

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

    VurderRefusjonAndelBeregningsgrunnlagDto() { // NOSONAR
        // Jackson
    }

    public VurderRefusjonAndelBeregningsgrunnlagDto(@Valid String arbeidsgiverOrgnr,
                                                    @Valid String arbeidsgiverAktoerId,
                                                    @Valid String internArbeidsforholdRef,
                                                    @Valid @NotNull LocalDate fastsattRefusjonFom) {
        this.arbeidsgiverOrgnr = arbeidsgiverOrgnr;
        this.arbeidsgiverAktoerId = arbeidsgiverAktoerId;
        this.internArbeidsforholdRef = internArbeidsforholdRef;
        this.fastsattRefusjonFom = fastsattRefusjonFom;
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
}
