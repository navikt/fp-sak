package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import javax.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.AksjonspunktÅrsak;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public record ArbeidsforholdMangel(@NotNull Arbeidsgiver arbeidsgiver, @NotNull InternArbeidsforholdRef ref, @NotNull AksjonspunktÅrsak årsak){
    @Override
    public String toString() {
        return "ArbeidsforholdMangel{" + "arbeidsgiver=" + arbeidsgiver + ", ref=" + ref + ", årsak=" + årsak + '}';
    }
}
