package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Organisasjonstype;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;

@ApplicationScoped
public class ArbeidsgiverHistorikkinnslag {
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    public ArbeidsgiverHistorikkinnslag() {
        // CDI
    }

    @Inject
    public ArbeidsgiverHistorikkinnslag(ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }

    String lagArbeidsgiverHistorikkinnslagTekst(Arbeidsgiver arbeidsgiver, List<ArbeidsforholdOverstyring> overstyringer) {
        return lagTekstMedArbeidsgiver(arbeidsgiver, overstyringer);
    }

    private String lagTekstMedArbeidsgiver(Arbeidsgiver arbeidsgiver, List<ArbeidsforholdOverstyring> overstyringer) {
        Objects.requireNonNull(arbeidsgiver, "arbeidsgiver");
        return lagTekstForArbeidsgiver(arbeidsgiver, overstyringer);
    }

    private String lagTekstForArbeidsgiver(Arbeidsgiver arbeidsgiver, List<ArbeidsforholdOverstyring> overstyringer) {
        var opplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
        var sb = new StringBuilder();
        var arbeidsgiverNavn = opplysninger.getNavn();
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
        return overstyringer
            .stream()
            .findFirst()
            .map(ArbeidsforholdOverstyring::getArbeidsgiverNavn)
            .orElseThrow(() -> new IllegalStateException("Utvikler feil: Kaller denne uten overstyring "));
    }
}
