package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;

class EndringerBareEtterEndringsdatoValidering implements OverstyrUttakPerioderValidering {

    private final LocalDate endringsdato;
    private final List<ForeldrepengerUttakPeriode> opprinnelige;

    EndringerBareEtterEndringsdatoValidering(List<ForeldrepengerUttakPeriode> opprinnelige, LocalDate endringsdato) {
        this.endringsdato = Objects.requireNonNull(endringsdato);
        this.opprinnelige = opprinnelige;
    }

    @Override
    public void utfør(List<ForeldrepengerUttakPeriode> nyePerioder) {
        nyePerioder.forEach(p -> {
            if (p.getTom().isBefore(endringsdato) && harEndring(p)) {
                throw OverstyrUttakValideringFeil.perioderFørEndringsdatoKanIkkeEndres(p.getTidsperiode());
            }
        });
    }

    private boolean harEndring(ForeldrepengerUttakPeriode nyPeriode) {
        return opprinnelige.stream()
            .noneMatch(opprinneligPeriode -> opprinneligPeriode.erLikBortsettFraTrekkdager(nyPeriode));
    }
}
