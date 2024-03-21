package no.nav.foreldrepenger.domene.iay.modell;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.abakus.ArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;

public record ArbeidsforholdMedPermisjon(Arbeidsgiver arbeidsgiver,
                                         ArbeidType arbeidType,
                                         EksternArbeidsforholdRef arbeidsforholdId,
                                         List<ArbeidsforholdTjeneste.AktivitetAvtale> aktivitetsavtaler,
                                         List<ArbeidsforholdTjeneste.Permisjon> permisjoner) {
}
