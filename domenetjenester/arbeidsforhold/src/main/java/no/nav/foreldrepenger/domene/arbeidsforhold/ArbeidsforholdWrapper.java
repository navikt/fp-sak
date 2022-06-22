package no.nav.foreldrepenger.domene.arbeidsforhold;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

//NOSONAR
public class ArbeidsforholdWrapper {

    private String arbeidsgiverReferanse;
    private String arbeidsforholdId;
    private String eksternArbeidsforholdId;
    private LocalDate fomDato = LocalDate.now();
    private LocalDate tomDato;
    private BigDecimal stillingsprosent;

    public String getArbeidsgiverReferanse() {
        return arbeidsgiverReferanse;
    }

    public void setArbeidsgiverReferanse(String arbeidsgiverReferanse) {
        this.arbeidsgiverReferanse = arbeidsgiverReferanse;
    }

    public LocalDate getFomDato() {
        return fomDato;
    }

    public void setFomDato(LocalDate fomDato) {
        if (fomDato != null) {
            this.fomDato = fomDato;
        }
    }

    public LocalDate getTomDato() {
        return tomDato;
    }

    public void setTomDato(LocalDate tomDato) {
        this.tomDato = tomDato;
    }

    public BigDecimal getStillingsprosent() {
        return stillingsprosent;
    }

    public void setStillingsprosent(BigDecimal stillingsprosent) {
        this.stillingsprosent = stillingsprosent;
    }

    public String getArbeidsforholdId() {
        return arbeidsforholdId;
    }

    public void setArbeidsforholdId(String arbeidsforholdId) {
        this.arbeidsforholdId = arbeidsforholdId;
    }

    public String getEksternArbeidsforholdId() {
        return eksternArbeidsforholdId;
    }

    public void setEksternArbeidsforholdId(String eksternArbeidsforholdId) {
        this.eksternArbeidsforholdId = eksternArbeidsforholdId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        var that = (ArbeidsforholdWrapper) o;
        return Objects.equals(arbeidsgiverReferanse, that.arbeidsgiverReferanse) &&
                InternArbeidsforholdRef.ref(arbeidsforholdId).gjelderFor(InternArbeidsforholdRef.ref(that.arbeidsforholdId));
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsgiverReferanse, arbeidsforholdId);
    }

}
