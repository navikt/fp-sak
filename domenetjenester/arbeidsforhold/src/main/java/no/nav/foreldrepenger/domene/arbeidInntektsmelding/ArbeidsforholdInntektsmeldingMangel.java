package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.AksjonspunktÅrsak;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

import javax.validation.constraints.NotNull;

public record ArbeidsforholdInntektsmeldingMangel(@NotNull Arbeidsgiver arbeidsgiver, @NotNull InternArbeidsforholdRef ref, @NotNull AksjonspunktÅrsak årsak){};
