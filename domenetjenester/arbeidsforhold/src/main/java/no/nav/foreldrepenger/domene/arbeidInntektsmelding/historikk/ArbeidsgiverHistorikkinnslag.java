package no.nav.foreldrepenger.domene.arbeidInntektsmelding.historikk;

import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;

class ArbeidsgiverHistorikkinnslag {

    private ArbeidsgiverHistorikkinnslag() {
        // Skjuler default konstruktør
    }

    public static String lagArbeidsgiverHistorikkinnslagTekst(ArbeidsgiverOpplysninger arbeidsgiverOpplysninger, Optional<EksternArbeidsforholdRef> eksternArbeidsforholdRef) {
        Objects.requireNonNull(arbeidsgiverOpplysninger, "arbeidsgiverOpplysninger");
        var tekstForArbeidsgiver = lagTekstForArbeidsgiver(arbeidsgiverOpplysninger);
        return eksternArbeidsforholdRef.map(ref -> lagTekstMedArbeidsgiverOgArbeidforholdRef(tekstForArbeidsgiver, ref))
            .orElse(tekstForArbeidsgiver);
    }

    private static String lagTekstMedArbeidsgiverOgArbeidforholdRef(String tekstForArbeidsgiver, EksternArbeidsforholdRef eksternArbeidsforholdRef) {
        var sb = new StringBuilder(tekstForArbeidsgiver);
        var referanse = eksternArbeidsforholdRef.getReferanse();
        var sisteFireTegnIRef = referanse.substring(referanse.length() - 4);
        return sb.append(" ...").append(sisteFireTegnIRef).toString();
    }

    private static String lagTekstForArbeidsgiver(ArbeidsgiverOpplysninger opplysninger) {
        var sb = new StringBuilder();
        var arbeidsgiverNavn = opplysninger.getNavn();
        sb.append(arbeidsgiverNavn);

        if (opplysninger.getFødselsdato() != null) {
            sb.append(opplysninger.getFødselsdato().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        // Ved kunstig orgnr er det ikke noe orgnr å vise, da det ikke betyr noe for saksbehandler
        if (!OrgNummer.KUNSTIG_ORG.equals(opplysninger.getIdentifikator())) {
            sb.append(arbeidsgiverNavn)
                .append(" (")
                .append(opplysninger.getIdentifikator())
                .append(")");
        }
        return sb.toString();

    }
}
