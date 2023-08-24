package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;

import java.util.List;
import java.util.stream.Collectors;

import static no.nav.foreldrepenger.domene.uttak.UttakEnumMapper.mapTilYf;

class AvslagØverføringValidering implements OverstyrUttakPerioderValidering {

    @Override
    public void utfør(List<ForeldrepengerUttakPeriode> nyePerioder) {
        for (var p : nyePerioder) {
            if (p.isOverføringAvslått() && trekkerDagerFraSøktKonto(p)) {
                throw OverstyrUttakValideringFeil.trekkerDagerFraSøktKontoVedAvslagAvOverføring(p);
            }
        }
    }

    private boolean trekkerDagerFraSøktKonto(ForeldrepengerUttakPeriode p) {
        var søktKonto = p.getSøktKonto();
        var kontoerSomTrekkesDager = p.getAktiviteter()
            .stream()
            .filter(a -> a.getTrekkdager().merEnn0())
            .map(a -> mapTilYf(a.getTrekkonto()))
            .collect(Collectors.toSet());
        return kontoerSomTrekkesDager.contains(søktKonto);
    }
}
