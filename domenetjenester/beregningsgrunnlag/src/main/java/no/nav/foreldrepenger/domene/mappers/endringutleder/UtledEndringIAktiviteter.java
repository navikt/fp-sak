package no.nav.foreldrepenger.domene.mappers.endringutleder;

import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktKode;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitet;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetAggregat;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeregningAktivitetEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeregningAktivitetNøkkel;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeregningAktiviteterEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.DatoEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.ToggleEndring;
import no.nav.foreldrepenger.domene.rest.dto.AvklarteAktiviteterDto;
import no.nav.foreldrepenger.domene.rest.dto.OverstyrBeregningsaktiviteterDto;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;

public class UtledEndringIAktiviteter {

    public static Optional<BeregningAktiviteterEndring> utedEndring(AksjonspunktKode dto,
                                                                    BeregningAktivitetAggregat register,
                                                                    BeregningAktivitetAggregat gjeldende,
                                                                    Optional<BeregningAktivitetAggregat> forrigeRegister,
                                                                    Optional<BeregningAktivitetAggregat> forrigeGjeldende) {

        if (dto instanceof AvklarteAktiviteterDto || dto instanceof OverstyrBeregningsaktiviteterDto) {

            var registerAktiviteter = register.getBeregningAktiviteter();
            var gjeldendeAktiviteter = gjeldende.getBeregningAktiviteter();

            var endringer = registerAktiviteter.stream()
                .map(aktivitet -> utledEndring(aktivitet, gjeldendeAktiviteter, forrigeRegister, forrigeGjeldende))
                .toList();
            return Optional.of(new BeregningAktiviteterEndring(endringer));
        }
        return Optional.empty();
    }

    private static BeregningAktivitetEndring utledEndring(BeregningAktivitet aktivitet,
                                                          List<BeregningAktivitet> gjeldendeAktiviteter,
                                                          Optional<BeregningAktivitetAggregat> forrigeRegister,
                                                          Optional<BeregningAktivitetAggregat> forrigeGjeldende) {
        var gjeldendeAktivitet = finnKorresponderende(aktivitet, gjeldendeAktiviteter);
        var skalBrukesEndring = finnSkalBrukesEndring(aktivitet, forrigeRegister, forrigeGjeldende, gjeldendeAktivitet);
        var datoEndring = finnDatoEndring(aktivitet, gjeldendeAktivitet, forrigeGjeldende);
        return new BeregningAktivitetEndring(mapNøkkel(aktivitet), skalBrukesEndring, datoEndring.orElse(null));
    }

    private static Optional<DatoEndring> finnDatoEndring(BeregningAktivitet aktivitet,
                                                         Optional<BeregningAktivitet> gjeldendeAktivitet,
                                                         Optional<BeregningAktivitetAggregat> forrigeGjeldende) {
        var forrigeKorresponderendeGjeldende = forrigeGjeldende.map(BeregningAktivitetAggregat::getBeregningAktiviteter)
            .flatMap(akts -> finnKorresponderende(aktivitet, akts));
        return gjeldendeAktivitet.map(a -> new DatoEndring(
            forrigeKorresponderendeGjeldende.map(BeregningAktivitet::getPeriode).map(ÅpenDatoIntervallEntitet::getTomDato).orElse(null),
            a.getPeriode().getTomDato()));
    }

    private static ToggleEndring finnSkalBrukesEndring(BeregningAktivitet aktivitet,
                                                       Optional<BeregningAktivitetAggregat> forrigeRegister,
                                                       Optional<BeregningAktivitetAggregat> forrigeGjeldende,
                                                       Optional<BeregningAktivitet> gjeldendeAktivitet) {
        boolean skalBrukesNyVerdi = gjeldendeAktivitet.isPresent();
        var skalBrukesForrige = finnSkalBrukesForrige(aktivitet, forrigeRegister, forrigeGjeldende);
        var skalBrukesEndring = new ToggleEndring(skalBrukesForrige.orElse(null), skalBrukesNyVerdi);
        return skalBrukesEndring;
    }

    private static BeregningAktivitetNøkkel mapNøkkel(BeregningAktivitet aktivitet) {
        return new BeregningAktivitetNøkkel(OpptjeningAktivitetType.fraKode(aktivitet.getOpptjeningAktivitetType().getKode()),
            aktivitet.getPeriode().getFomDato(), aktivitet.getArbeidsgiver(), aktivitet.getArbeidsforholdRef());
    }

    private static Optional<Boolean> finnSkalBrukesForrige(BeregningAktivitet aktivitet,
                                                           Optional<BeregningAktivitetAggregat> forrigeRegister,
                                                           Optional<BeregningAktivitetAggregat> forrigeGjeldende) {
        var forrigeKorresponderendeGjeldende = forrigeGjeldende.map(BeregningAktivitetAggregat::getBeregningAktiviteter)
            .flatMap(akts -> finnKorresponderende(aktivitet, akts));
        var forrigeKorresponderendeRegister = forrigeRegister.map(BeregningAktivitetAggregat::getBeregningAktiviteter)
            .flatMap(registerAktiviteter -> finnKorresponderende(aktivitet, registerAktiviteter));
        return forrigeKorresponderendeRegister.map(a -> forrigeKorresponderendeGjeldende.isPresent());
    }

    private static Optional<BeregningAktivitet> finnKorresponderende(BeregningAktivitet aktivitet, List<BeregningAktivitet> gjeldendeAktiviteter) {
        return gjeldendeAktiviteter.stream().filter(a -> aktivitet.getNøkkel().equals(a.getNøkkel())).findFirst();
    }

}
