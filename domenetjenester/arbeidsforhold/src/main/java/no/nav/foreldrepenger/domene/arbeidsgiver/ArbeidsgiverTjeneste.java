package no.nav.foreldrepenger.domene.arbeidsgiver;


import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;

public interface ArbeidsgiverTjeneste {

    ArbeidsgiverOpplysninger hent(Arbeidsgiver arbeidsgiver);

    Virksomhet hentVirksomhet(String orgnr);

    Arbeidsgiver hentArbeidsgiver(String orgnr, String arbeidsgiverIdentifikator);
}
