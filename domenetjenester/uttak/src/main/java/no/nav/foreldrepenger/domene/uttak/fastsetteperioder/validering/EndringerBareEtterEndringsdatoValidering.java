package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.vedtak.feil.FeilFactory;

class EndringerBareEtterEndringsdatoValidering implements OverstyrUttakPerioderValidering {

    private final LocalDate endringsdato;

    EndringerBareEtterEndringsdatoValidering(LocalDate endringsdato) {
        this.endringsdato = Objects.requireNonNull(endringsdato);
    }

    @Override
    public void utfør(List<ForeldrepengerUttakPeriode> nyePerioder) {
        nyePerioder.forEach(p -> {
            if (endringsdato.isAfter(p.getTidsperiode().getFomDato())) {
                throw FeilFactory.create(OverstyrUttakValideringFeil.class).perioderFørEndringsdatoKanIkkeEndres().toException();
            }
        });
    }
}
