package no.nav.foreldrepenger.domene.uttak.fastsetteperioder;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering.OverstyrUttakResultatValidator;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.feil.FeilFactory;

@ApplicationScoped
public class FastsettePerioderTjeneste {

    private OverstyrUttakResultatValidator uttakResultatValidator;
    private FastsettePerioderRegelAdapter regelAdapter;
    private UttakRepository uttakRepository;
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private YtelsesFordelingRepository ytelsesFordelingRepository;

    FastsettePerioderTjeneste() {
        // For CDI
    }

    @Inject
    public FastsettePerioderTjeneste(UttakRepository uttakRepository,
                                     YtelsesFordelingRepository ytelsesfordelingRepository,
                                     OverstyrUttakResultatValidator uttakResultatValidator,
                                     FastsettePerioderRegelAdapter regelAdapter,
                                     ForeldrepengerUttakTjeneste uttakTjeneste) {
        this.uttakResultatValidator = uttakResultatValidator;
        this.regelAdapter = regelAdapter;
        this.uttakRepository = uttakRepository;
        this.ytelsesFordelingRepository = ytelsesfordelingRepository;
        this.uttakTjeneste = uttakTjeneste;
    }

    public void fastsettePerioder(UttakInput input) {
        UttakResultatPerioderEntitet resultat = regelAdapter.fastsettePerioder(input);
        uttakRepository.lagreOpprinneligUttakResultatPerioder(input.getBehandlingReferanse().getBehandlingId(), resultat);
    }

    public void manueltFastsettePerioder(UttakInput uttakInput, List<ForeldrepengerUttakPeriode> perioder) {
        valider(perioder, uttakInput);
        lagreManueltFastsatt(uttakInput, perioder);
    }

    private void valider(List<ForeldrepengerUttakPeriode> perioder, UttakInput uttakInput) {
        var behandlingId = uttakInput.getBehandlingReferanse().getBehandlingId();
        var opprinnelig = hentOpprinnelig(behandlingId);
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(behandlingId);
        uttakResultatValidator.valider(uttakInput, opprinnelig, perioder, ytelseFordelingAggregat.getGjeldendeEndringsdato());
    }

    private List<ForeldrepengerUttakPeriode> hentOpprinnelig(Long behandlingId) {
        return uttakTjeneste.hentUttak(behandlingId).getOpprinneligPerioder();
    }

    private void lagreManueltFastsatt(UttakInput uttakInput, List<ForeldrepengerUttakPeriode> perioder) {

        UttakResultatPerioderEntitet overstyrtEntitet = new UttakResultatPerioderEntitet();
        Long behandlingId = uttakInput.getBehandlingReferanse().getBehandlingId();
        UttakResultatEntitet opprinnelig = uttakRepository.hentUttakResultat(behandlingId);
        for (var periode : perioder) {
            UttakResultatPeriodeEntitet matchendeOpprinneligPeriode = matchendeOpprinneligPeriode(periode, opprinnelig.getOpprinneligPerioder());
            UttakResultatPeriodeEntitet periodeEntitet = map(matchendeOpprinneligPeriode, periode);
            overstyrtEntitet.leggTilPeriode(periodeEntitet);
        }

        uttakRepository.lagreOverstyrtUttakResultatPerioder(behandlingId, overstyrtEntitet);
    }

    private UttakResultatPeriodeEntitet matchendeOpprinneligPeriode(ForeldrepengerUttakPeriode periode,
                                                                    UttakResultatPerioderEntitet opprinnelig) {
        Optional<UttakResultatPeriodeEntitet> matchende = opprinnelig.getPerioder()
            .stream()
            .filter(oPeriode -> {
                LocalDateInterval tidsperiode = periode.getTidsperiode();
                return (tidsperiode.getFomDato().isEqual(oPeriode.getFom()) || tidsperiode.getFomDato().isAfter(oPeriode.getFom()))
                    && (tidsperiode.getTomDato().isEqual(oPeriode.getTom()) || tidsperiode.getTomDato().isBefore(oPeriode.getTom()));
            })
            .findFirst();

        return matchende.orElseThrow(()
            -> FeilFactory.create(FastsettePerioderFeil.class).manglendeOpprinneligPeriode(periode).toException());
    }

    private UttakResultatPeriodeEntitet map(UttakResultatPeriodeEntitet opprinneligPeriode,
                                            ForeldrepengerUttakPeriode nyPeriode) {
        UttakResultatPeriodeEntitet.Builder builder = new UttakResultatPeriodeEntitet.Builder(nyPeriode.getTidsperiode().getFomDato(),
            nyPeriode.getTidsperiode().getTomDato())
            .medPeriodeSoknad(opprinneligPeriode.getPeriodeSøknad().orElse(null))
            .medResultatType(nyPeriode.getResultatType(), nyPeriode.getResultatÅrsak())
            .medBegrunnelse(nyPeriode.getBegrunnelse())
            .medGraderingInnvilget(nyPeriode.isGraderingInnvilget())
            .medGraderingAvslagÅrsak(nyPeriode.getGraderingAvslagÅrsak())
            .medUtsettelseType(opprinneligPeriode.getUtsettelseType())
            .medOppholdÅrsak(nyPeriode.getOppholdÅrsak())
            .medOverføringÅrsak(opprinneligPeriode.getOverføringÅrsak())
            .medSamtidigUttak(nyPeriode.isSamtidigUttak())
            .medSamtidigUttaksprosent(nyPeriode.getSamtidigUttaksprosent())
            .medFlerbarnsdager(nyPeriode.isFlerbarnsdager())
            .medManueltBehandlet(nyPeriode.getBegrunnelse() != null);
        UttakResultatPeriodeEntitet periodeEntitet = builder.build();

        for (ForeldrepengerUttakPeriodeAktivitet nyAktivitet : nyPeriode.getAktiviteter()) {
            UttakResultatPeriodeAktivitetEntitet matchendeOpprinneligAktivitet = matchendeOpprinneligAktivitet(opprinneligPeriode, nyAktivitet.getUttakAktivitet());
            UttakResultatPeriodeAktivitetEntitet periodeAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periodeEntitet, matchendeOpprinneligAktivitet.getUttakAktivitet())
                .medTrekkonto(nyAktivitet.getTrekkonto())
                .medTrekkdager(nyAktivitet.getTrekkdager())
                .medArbeidsprosent(nyAktivitet.getArbeidsprosent())
                .medUtbetalingsprosent(nyAktivitet.getUtbetalingsgrad())
                .medErSøktGradering(matchendeOpprinneligAktivitet.isSøktGradering())
                .build();
            periodeEntitet.leggTilAktivitet(periodeAktivitet);
        }
        return periodeEntitet;
    }

    private UttakResultatPeriodeAktivitetEntitet matchendeOpprinneligAktivitet(UttakResultatPeriodeEntitet opprinneligPeriode,
                                                                               ForeldrepengerUttakAktivitet uttakAktivitet) {
        return opprinneligPeriode.getAktiviteter()
            .stream()
            .filter(opprinneligAktivitet -> Objects.equals(opprinneligAktivitet.getUttakArbeidType(), uttakAktivitet.getUttakArbeidType()) &&
                Objects.equals(opprinneligAktivitet.getArbeidsforholdRef(), uttakAktivitet.getArbeidsforholdRef()) &&
                Objects.equals(opprinneligAktivitet.getUttakAktivitet().getArbeidsgiver().orElse(null), uttakAktivitet.getArbeidsgiver().orElse(null)))
            .findFirst().orElseThrow(() -> FeilFactory.create(FastsettePerioderFeil.class)
                .manglendeOpprinneligAktivitet(uttakAktivitet, opprinneligPeriode.getAktiviteter()).toException());
    }
}
