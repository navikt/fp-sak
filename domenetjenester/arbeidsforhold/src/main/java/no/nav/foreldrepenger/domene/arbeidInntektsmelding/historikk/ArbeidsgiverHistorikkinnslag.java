package no.nav.foreldrepenger.domene.arbeidInntektsmelding.historikk;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.format;

import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;

public class ArbeidsgiverHistorikkinnslag {

    private ArbeidsgiverHistorikkinnslag() {
    }

    public static String lagArbeidsgiverHistorikkinnslagTekst(ArbeidsgiverOpplysninger arbeidsgiverOpplysninger,
                                                              Optional<EksternArbeidsforholdRef> eksternRef) {
        Objects.requireNonNull(arbeidsgiverOpplysninger, "arbeidsgiverOpplysninger");
        var tekst = lagTekstForArbeidsgiver(arbeidsgiverOpplysninger);
        return eksternRef.map(ref -> tekst + " ..." + sisteFireTegn(ref.getReferanse())).orElse(tekst);
    }

    private static String sisteFireTegn(String referanse) {
        return referanse.length() < 4 ? referanse : referanse.substring(referanse.length() - 4);
    }

    private static String lagTekstForArbeidsgiver(ArbeidsgiverOpplysninger opplysninger) {
        if (OrgNummer.KUNSTIG_ORG.equals(opplysninger.getIdentifikator())) {
            return opplysninger.getNavn();
        }
        var identifikator = opplysninger.getFødselsdato() != null ? format(opplysninger.getFødselsdato()) : opplysninger.getIdentifikator();
        return opplysninger.getNavn() + " (" + identifikator + ")";
    }
}
