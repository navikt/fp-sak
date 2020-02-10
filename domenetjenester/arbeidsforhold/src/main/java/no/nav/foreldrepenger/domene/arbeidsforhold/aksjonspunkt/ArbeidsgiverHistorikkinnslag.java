package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Organisasjonstype;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.FinnNavnForManueltLagtTilArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@ApplicationScoped
class ArbeidsgiverHistorikkinnslag {
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    ArbeidsgiverHistorikkinnslag() {
        // CDI
    }

    @Inject
    ArbeidsgiverHistorikkinnslag(ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }

    String lagArbeidsgiverHistorikkinnslagTekst(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef, List<ArbeidsforholdOverstyring> overstyringer) {
        if (arbeidsgiver != null && arbeidsforholdRef != null && arbeidsforholdRef.gjelderForSpesifiktArbeidsforhold()) {
            return lagTekstMedArbeidsgiverOgArbeidforholdRef(arbeidsgiver, arbeidsforholdRef, overstyringer);
        } else if (arbeidsgiver != null) {
            return lagTekstMedArbeidsgiver(arbeidsgiver, overstyringer);
        }
        throw new IllegalStateException("Klarte ikke lage historikkinnslagstekst for arbeidsgiver");
    }

    String lagArbeidsgiverHistorikkinnslagTekst(Arbeidsgiver arbeidsgiver, List<ArbeidsforholdOverstyring> overstyringer) {
        return lagTekstMedArbeidsgiver(arbeidsgiver, overstyringer);
    }

    private String lagTekstMedArbeidsgiver(Arbeidsgiver arbeidsgiver, List<ArbeidsforholdOverstyring> overstyringer) {
        Objects.requireNonNull(arbeidsgiver, "arbeidsgiver");
        return lagTekstForArbeidsgiver(arbeidsgiver, overstyringer);
    }

    private String lagTekstMedArbeidsgiverOgArbeidforholdRef(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef, List<ArbeidsforholdOverstyring> overstyringer) {
        StringBuilder sb = new StringBuilder();
        sb.append(lagTekstMedArbeidsgiver(arbeidsgiver, overstyringer));
        sb.append(lagTekstMedArbeidsforholdref(arbeidsforholdRef));
        return sb.toString();
    }

    private String lagTekstMedArbeidsforholdref(InternArbeidsforholdRef arbeidsforholdRef) {
        String referanse = arbeidsforholdRef.getReferanse();
        String sisteFireTegnIRef = referanse.substring(referanse.length() - 4);
        StringBuilder sb = new StringBuilder();
        sb.append(" ...")
            .append(sisteFireTegnIRef);
        return sb.toString();

    }

    private String lagTekstForArbeidsgiver(Arbeidsgiver arbeidsgiver, List<ArbeidsforholdOverstyring> overstyringer) {
        ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
        StringBuilder sb = new StringBuilder();
        String arbeidsgiverNavn = opplysninger.getNavn();
        if (arbeidsgiver.getErVirksomhet() && Organisasjonstype.erKunstig(arbeidsgiver.getOrgnr())) {
            arbeidsgiverNavn = hentNavnTilManueltArbeidsforhold(overstyringer);
        }
        sb.append(arbeidsgiverNavn)
            .append(" (")
            .append(opplysninger.getIdentifikator())
            .append(")");
        return sb.toString();
    }

    private String hentNavnTilManueltArbeidsforhold(List<ArbeidsforholdOverstyring> overstyringer) {
        return FinnNavnForManueltLagtTilArbeidsforholdTjeneste.finnNavnTilManueltLagtTilArbeidsforhold(overstyringer)
            .map(ArbeidsgiverOpplysninger::getNavn)
            .orElseThrow(() -> new IllegalStateException("Fant ikke forventet informasjon om manuelt arbeidsforhold"));
    }
}
