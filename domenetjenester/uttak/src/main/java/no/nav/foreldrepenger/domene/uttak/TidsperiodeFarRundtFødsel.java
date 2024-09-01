package no.nav.foreldrepenger.domene.uttak;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.UttakParametre;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public final class TidsperiodeFarRundtFødsel {

    private TidsperiodeFarRundtFødsel() {
    }

    public static Optional<LocalDateInterval> intervallFarRundtFødsel(UttakInput uttakInput) {
        ForeldrepengerGrunnlag fpGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        var utenMinsterett = uttakInput.getSkjæringstidspunkt().filter(Skjæringstidspunkt::utenMinsterett).isPresent();
        return intervallFarRundtFødsel(fpGrunnlag.getFamilieHendelser(), utenMinsterett);
    }

    public static Optional<LocalDateInterval> intervallFarRundtFødsel(FamilieHendelser familieHendelser, boolean utenMinsterett) {
        var familiehendelse = familieHendelser.getGjeldendeFamilieHendelse();
        return intervallFarRundtFødsel(familiehendelse, utenMinsterett);
    }

    public static Optional<LocalDateInterval> intervallFarRundtFødsel(FamilieHendelse familieHendelse, boolean utenMinsterett) {
        return intervallFarRundtFødsel(utenMinsterett, familieHendelse.gjelderFødsel(),
                familieHendelse.getFamilieHendelseDato(), familieHendelse.getTermindato().orElse(null));
    }

    public static Optional<LocalDateInterval> intervallFarRundtFødsel(boolean utenMinsterett,
                                                                      boolean gjelderFødsel,
                                                                      LocalDate familiehendelseDato,
                                                                      LocalDate termindato) {
        return UttakParametre.utledFarsPeriodeRundtFødsel(utenMinsterett, gjelderFødsel, familiehendelseDato, termindato)
            .map(p -> new LocalDateInterval(p.getFom(), p.getTom()));
    }
}
