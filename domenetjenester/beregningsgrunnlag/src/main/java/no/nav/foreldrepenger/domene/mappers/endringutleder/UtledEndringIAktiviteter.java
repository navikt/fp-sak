package no.nav.foreldrepenger.domene.mappers.endringutleder;

import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetEntitet;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeregningAktivitetEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeregningAktivitetNøkkel;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeregningAktiviteterEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.DatoEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.ToggleEndring;
import no.nav.foreldrepenger.domene.rest.dto.AvklarteAktiviteterDto;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;

class UtledEndringIAktiviteter {

    static Optional<BeregningAktiviteterEndring> utedEndring(
        BekreftetAksjonspunktDto dto, BeregningAktivitetAggregatEntitet register,
        BeregningAktivitetAggregatEntitet gjeldende,
        Optional<BeregningAktivitetAggregatEntitet> forrigeRegister,
        Optional<BeregningAktivitetAggregatEntitet> forrigeGjeldende) {

        if (dto instanceof AvklarteAktiviteterDto) {

            var registerAktiviteter = register.getBeregningAktiviteter();
            var gjeldendeAktiviteter = gjeldende.getBeregningAktiviteter();

            var endringer = registerAktiviteter.stream()
                    .map(aktivitet -> utledEndring(aktivitet, gjeldendeAktiviteter, forrigeRegister, forrigeGjeldende))
                    .toList();
            return Optional.of(new BeregningAktiviteterEndring(endringer));
        }
        return Optional.empty();
    }

    private static BeregningAktivitetEndring utledEndring(BeregningAktivitetEntitet aktivitet, List<BeregningAktivitetEntitet> gjeldendeAktiviteter, Optional<BeregningAktivitetAggregatEntitet> forrigeRegister, Optional<BeregningAktivitetAggregatEntitet> forrigeGjeldende) {
        var gjeldendeAktivitet = finnKorresponderende(aktivitet, gjeldendeAktiviteter);
        var skalBrukesEndring = finnSkalBrukesEndring(aktivitet, forrigeRegister, forrigeGjeldende, gjeldendeAktivitet);
        var datoEndring = finnDatoEndring(aktivitet, gjeldendeAktivitet, forrigeGjeldende);
        return new BeregningAktivitetEndring(
                mapNøkkel(aktivitet),
                skalBrukesEndring,
                datoEndring.orElse(null)
        );
    }

    private static Optional<DatoEndring> finnDatoEndring(BeregningAktivitetEntitet aktivitet,
                                                         Optional<BeregningAktivitetEntitet> gjeldendeAktivitet,
                                                         Optional<BeregningAktivitetAggregatEntitet> forrigeGjeldende) {
        var forrigeKorresponderendeGjeldende = forrigeGjeldende.map(BeregningAktivitetAggregatEntitet::getBeregningAktiviteter)
                .flatMap(akts -> finnKorresponderende(aktivitet, akts));
        return gjeldendeAktivitet.map(a ->
                new DatoEndring(forrigeKorresponderendeGjeldende.map(BeregningAktivitetEntitet::getPeriode)
                    .map(ÅpenDatoIntervallEntitet::getTomDato).orElse(null),
                        a.getPeriode().getTomDato()));
    }

    private static ToggleEndring finnSkalBrukesEndring(BeregningAktivitetEntitet aktivitet, Optional<BeregningAktivitetAggregatEntitet> forrigeRegister, Optional<BeregningAktivitetAggregatEntitet> forrigeGjeldende, Optional<BeregningAktivitetEntitet> gjeldendeAktivitet) {
        boolean skalBrukesNyVerdi = gjeldendeAktivitet.isPresent();
        var skalBrukesForrige = finnSkalBrukesForrige(aktivitet, forrigeRegister, forrigeGjeldende);
        var skalBrukesEndring = new ToggleEndring(skalBrukesForrige.orElse(null), skalBrukesNyVerdi);
        return skalBrukesEndring;
    }

    private static BeregningAktivitetNøkkel mapNøkkel(BeregningAktivitetEntitet aktivitet) {
        return new BeregningAktivitetNøkkel(OpptjeningAktivitetType.fraKode(aktivitet.getOpptjeningAktivitetType().getKode()),
                aktivitet.getPeriode().getFomDato(),
                aktivitet.getArbeidsgiver(),
                aktivitet.getArbeidsforholdRef());
    }

    private static Optional<Boolean> finnSkalBrukesForrige(BeregningAktivitetEntitet aktivitet, Optional<BeregningAktivitetAggregatEntitet> forrigeRegister, Optional<BeregningAktivitetAggregatEntitet> forrigeGjeldende) {
        var forrigeKorresponderendeGjeldende = forrigeGjeldende.map(BeregningAktivitetAggregatEntitet::getBeregningAktiviteter)
                .flatMap(akts -> finnKorresponderende(aktivitet, akts));
        var forrigeKorresponderendeRegister = forrigeRegister.map(BeregningAktivitetAggregatEntitet::getBeregningAktiviteter)
                .flatMap(registerAktiviteter -> finnKorresponderende(aktivitet, registerAktiviteter));
        return forrigeKorresponderendeRegister.map(a -> forrigeKorresponderendeGjeldende.isPresent());
    }

    private static Optional<BeregningAktivitetEntitet> finnKorresponderende(BeregningAktivitetEntitet aktivitet, List<BeregningAktivitetEntitet> gjeldendeAktiviteter) {
        return gjeldendeAktiviteter.stream()
                .filter(a -> aktivitet.getNøkkel().equals(a.getNøkkel()))
                .findFirst();
    }

}
