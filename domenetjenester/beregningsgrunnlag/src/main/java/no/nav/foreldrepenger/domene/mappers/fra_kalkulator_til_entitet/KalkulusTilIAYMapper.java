package no.nav.foreldrepenger.domene.mappers.fra_kalkulator_til_entitet;

import no.nav.folketrygdloven.kalkulator.modell.typer.InternArbeidsforholdRefDto;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;


public final class KalkulusTilIAYMapper {
    private KalkulusTilIAYMapper() {
    }

    public static InternArbeidsforholdRef mapArbeidsforholdRef(InternArbeidsforholdRefDto arbeidsforholdRef) {
        return InternArbeidsforholdRef.ref(arbeidsforholdRef.getReferanse());
    }

    public static Arbeidsgiver mapArbeidsgiver(no.nav.folketrygdloven.kalkulator.modell.typer.Arbeidsgiver arbeidsgiver) {
        return arbeidsgiver.getErVirksomhet() ? Arbeidsgiver.virksomhet(arbeidsgiver.getOrgnr()) :
            Arbeidsgiver.fra(new AktørId(arbeidsgiver.getAktørId().getId()));
    }
}
