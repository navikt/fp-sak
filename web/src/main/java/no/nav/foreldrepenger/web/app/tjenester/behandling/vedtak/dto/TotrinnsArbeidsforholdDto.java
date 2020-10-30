package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto;

import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;

public class TotrinnsArbeidsforholdDto {

    private String arbeidsforholdId;
    private ArbeidsforholdHandlingType arbeidsforholdHandlingType;
    private Boolean brukPermisjon;
    private String arbeidsgiverReferanse;
    private String navn;
    private String organisasjonsnummer;


    public TotrinnsArbeidsforholdDto(String arbeidsgiverReferanse,
                                     String navn,
                                     String organisasjonsnummer,
                                     String arbeidsforholdId,
                                     ArbeidsforholdHandlingType handling,
                                     Boolean brukPermisjon) {
        this.arbeidsgiverReferanse = arbeidsgiverReferanse;
        this.navn = navn;
        this.organisasjonsnummer = organisasjonsnummer;
        this.arbeidsforholdId = arbeidsforholdId;
        this.arbeidsforholdHandlingType = handling;
        this.brukPermisjon = brukPermisjon;
    }

    public String getArbeidsgiverReferanse() {
        return arbeidsgiverReferanse;
    }

    public String getNavn() {
        return navn;
    }

    public String getOrganisasjonsnummer() {
        return organisasjonsnummer;
    }

    public String getArbeidsforholdId() {
        return arbeidsforholdId;
    }

    public ArbeidsforholdHandlingType getArbeidsforholdHandlingType() {
        return arbeidsforholdHandlingType;
    }

    public Boolean getBrukPermisjon() {
        return brukPermisjon;
    }

}
