package no.nav.foreldrepenger.web.app.tjenester.registrering.svp;

public class SvpTilretteleggingPrivatArbeidsgiverDto extends SvpTilretteleggingArbeidsforholdDto {

    private String arbeidsgiverIdentifikator;

    public String getArbeidsgiverIdentifikator() {
        return arbeidsgiverIdentifikator;
    }

    public void setArbeidsgiverIdentifikator(String arbeidsgiverIdentifikator) {
        this.arbeidsgiverIdentifikator = arbeidsgiverIdentifikator;
    }
}
