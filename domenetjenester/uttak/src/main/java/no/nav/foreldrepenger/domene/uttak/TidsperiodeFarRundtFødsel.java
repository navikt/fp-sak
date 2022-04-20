package no.nav.foreldrepenger.domene.uttak;

import java.util.Optional;

import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.DatoerGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.KontoerGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.SøknadGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.FarUttakRundtFødsel;
import no.nav.foreldrepenger.regler.uttak.konfig.StandardKonfigurasjon;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public final class TidsperiodeFarRundtFødsel {


    public static Optional<LocalDateInterval> intervallFarRundtFødsel(UttakInput uttakInput) {
        var datoer = DatoerGrunnlagBygger.byggForenkletGrunnlagKunFamiliehendelse(uttakInput);
        var type = SøknadGrunnlagBygger.type(uttakInput.getYtelsespesifiktGrunnlag());
        var kontoerKunMinsterett = KontoerGrunnlagBygger.byggKunRettighetFarUttakRundtFødsel(uttakInput.getBehandlingReferanse());
        return FarUttakRundtFødsel.utledFarsPeriodeRundtFødsel(datoer.build(), kontoerKunMinsterett.build(), type, StandardKonfigurasjon.KONFIGURASJON)
            .map(p -> new LocalDateInterval(p.getFom(), p.getTom()));

    }
}
