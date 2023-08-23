package no.nav.foreldrepenger.domene.modell;

import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

import java.time.LocalDate;
import java.util.Objects;


public class BeregningRefusjonPeriode {

    private InternArbeidsforholdRef arbeidsforholdRef;
    private LocalDate startdatoRefusjon;

    public BeregningRefusjonPeriode(InternArbeidsforholdRef arbeidsforholdRef, LocalDate startdatoRefusjon) {
        Objects.requireNonNull(startdatoRefusjon, "startdatorefusjon");
        this.arbeidsforholdRef = arbeidsforholdRef;
        this.startdatoRefusjon = startdatoRefusjon;
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        if (arbeidsforholdRef == null) {
            return InternArbeidsforholdRef.nullRef();
        }
        return arbeidsforholdRef;
    }

    public LocalDate getStartdatoRefusjon() {
        return startdatoRefusjon;
    }
}
