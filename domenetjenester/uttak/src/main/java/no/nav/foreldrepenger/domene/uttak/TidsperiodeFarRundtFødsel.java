package no.nav.foreldrepenger.domene.uttak;

import java.util.Optional;

import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.FarUttakRundtFødsel;
import no.nav.foreldrepenger.regler.uttak.konfig.Konfigurasjon;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public final class TidsperiodeFarRundtFødsel {


    public static Optional<LocalDateInterval> intervallFarRundtFødsel(UttakInput uttakInput) {
        ForeldrepengerGrunnlag fpGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        return intervallFarRundtFødsel(fpGrunnlag.getFamilieHendelser(), uttakInput.getBehandlingReferanse().getSkjæringstidspunkt().utenMinsterett());
    }

    public static Optional<LocalDateInterval> intervallFarRundtFødsel(FamilieHendelser familieHendelser, boolean utenMinsterett) {
        var familiehendelse = familieHendelser.getGjeldendeFamilieHendelse();
        return FarUttakRundtFødsel.utledFarsPeriodeRundtFødsel(utenMinsterett, familieHendelser.gjelderTerminFødsel(),
                familiehendelse.getFamilieHendelseDato(), familiehendelse.getTermindato().orElse(null), Konfigurasjon.STANDARD)
            .map(p -> new LocalDateInterval(p.getFom(), p.getTom()));
    }
}
