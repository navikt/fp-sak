package no.nav.foreldrepenger.domene.rest.historikk;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.tilMaskertNummer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.util.InputValideringRegex;

public record ArbeidsgiverDto(@Pattern(regexp = InputValideringRegex.ARBEIDSGIVER) String arbeidsgiverOrgnr,
                              @Valid AktørId arbeidsgiverAktørId)  {



    public ArbeidsgiverDto {
        if (arbeidsgiverAktørId==null && arbeidsgiverOrgnr==null) {
            throw new IllegalArgumentException("Utvikler-feil: arbeidsgiver uten hverken orgnr eller aktørId");
        }
        if (arbeidsgiverAktørId!=null && arbeidsgiverOrgnr!=null) {
            throw new IllegalArgumentException("Utvikler-feil: arbeidsgiver med både orgnr og aktørId");
        }
    }

    public static ArbeidsgiverDto virksomhet(String arbeidsgiverOrgnr) {
        return new ArbeidsgiverDto(arbeidsgiverOrgnr, null);
    }

    public static ArbeidsgiverDto person(AktørId arbeidsgiverAktørId) {
        return new ArbeidsgiverDto(null, arbeidsgiverAktørId);
    }

    public String getIdentifikator() {
        if (arbeidsgiverAktørId != null) {
            return arbeidsgiverAktørId().getId();
        }
        return arbeidsgiverOrgnr();
    }

    public boolean getErVirksomhet() {
        return arbeidsgiverOrgnr() != null;
    }

    @Override
    public String toString() {
        return "Arbeidsgiver{" +
            "virksomhet=" + tilMaskertNummer(arbeidsgiverOrgnr()) +
            ", arbeidsgiverAktørId='" + arbeidsgiverAktørId() + '\'' +
            '}';
    }

}
