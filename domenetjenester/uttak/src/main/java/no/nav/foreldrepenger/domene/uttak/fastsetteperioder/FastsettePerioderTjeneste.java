package no.nav.foreldrepenger.domene.uttak.fastsetteperioder;

import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering.OverstyrUttakResultatValidator;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

@ApplicationScoped
public class FastsettePerioderTjeneste {

    private OverstyrUttakResultatValidator uttakResultatValidator;
    private FastsettePerioderRegelAdapter regelAdapter;
    private FpUttakRepository fpUttakRepository;
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private YtelsesFordelingRepository ytelsesFordelingRepository;

    FastsettePerioderTjeneste() {
        // For CDI
    }

    @Inject
    public FastsettePerioderTjeneste(FpUttakRepository fpUttakRepository,
                                     YtelsesFordelingRepository ytelsesfordelingRepository,
                                     OverstyrUttakResultatValidator uttakResultatValidator,
                                     FastsettePerioderRegelAdapter regelAdapter,
                                     ForeldrepengerUttakTjeneste uttakTjeneste) {
        this.uttakResultatValidator = uttakResultatValidator;
        this.regelAdapter = regelAdapter;
        this.fpUttakRepository = fpUttakRepository;
        this.ytelsesFordelingRepository = ytelsesfordelingRepository;
        this.uttakTjeneste = uttakTjeneste;
    }

    public void fastsettePerioder(UttakInput input, Stønadskontoberegning stønadskontoberegning) {
        var resultat = regelAdapter.fastsettePerioder(input, stønadskontoberegning);
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(input.getBehandlingReferanse().behandlingId(),
            stønadskontoberegning, resultat);
    }

    public void manueltFastsettePerioder(UttakInput uttakInput, List<ForeldrepengerUttakPeriode> perioder) {
        valider(perioder, uttakInput);
        lagreManueltFastsatt(uttakInput, perioder);
    }

    private void valider(List<ForeldrepengerUttakPeriode> perioder, UttakInput uttakInput) {
        var behandlingId = uttakInput.getBehandlingReferanse().behandlingId();
        var opprinnelig = hentOpprinnelig(behandlingId);
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(behandlingId);
        uttakResultatValidator.valider(uttakInput, opprinnelig, perioder,
            ytelseFordelingAggregat.getGjeldendeEndringsdato());
    }

    private List<ForeldrepengerUttakPeriode> hentOpprinnelig(Long behandlingId) {
        return uttakTjeneste.hentUttak(behandlingId).getOpprinneligPerioder();
    }

    private void lagreManueltFastsatt(UttakInput uttakInput, List<ForeldrepengerUttakPeriode> perioder) {

        var overstyrtEntitet = new UttakResultatPerioderEntitet();
        var behandlingId = uttakInput.getBehandlingReferanse().behandlingId();
        var opprinnelig = fpUttakRepository.hentUttakResultat(behandlingId);
        for (var periode : perioder) {
            var matchendeOpprinneligPeriode = matchendeOpprinneligPeriode(periode,
                opprinnelig.getOpprinneligPerioder());
            var periodeEntitet = map(matchendeOpprinneligPeriode, periode);
            overstyrtEntitet.leggTilPeriode(periodeEntitet);
        }

        fpUttakRepository.lagreOverstyrtUttakResultatPerioder(behandlingId, overstyrtEntitet);
    }

    private UttakResultatPeriodeEntitet matchendeOpprinneligPeriode(ForeldrepengerUttakPeriode periode,
                                                                    UttakResultatPerioderEntitet opprinnelig) {
        return opprinnelig.getPerioder().stream().filter(oPeriode -> {
            var tidsperiode = periode.getTidsperiode();
            return (tidsperiode.getFomDato().isEqual(oPeriode.getFom()) || tidsperiode.getFomDato()
                .isAfter(oPeriode.getFom())) && (tidsperiode.getTomDato().isEqual(oPeriode.getTom())
                || tidsperiode.getTomDato().isBefore(oPeriode.getTom()));
        }).findFirst().orElseThrow(() -> FastsettePerioderFeil.manglendeOpprinneligPeriode(periode));
    }

    private UttakResultatPeriodeEntitet map(UttakResultatPeriodeEntitet opprinneligPeriode,
                                            ForeldrepengerUttakPeriode nyPeriode) {
        var builder = new UttakResultatPeriodeEntitet.Builder(nyPeriode.getTidsperiode().getFomDato(),
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
        var periodeEntitet = builder.build();

        for (var nyAktivitet : nyPeriode.getAktiviteter()) {
            var matchendeOpprinneligAktivitet = matchendeOpprinneligAktivitet(opprinneligPeriode,
                nyAktivitet.getUttakAktivitet());
            var periodeAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periodeEntitet,
                matchendeOpprinneligAktivitet.getUttakAktivitet()).medTrekkonto(nyAktivitet.getTrekkonto())
                .medTrekkdager(nyAktivitet.getTrekkdager())
                .medArbeidsprosent(nyAktivitet.getArbeidsprosent())
                .medUtbetalingsgrad(nyAktivitet.getUtbetalingsgrad())
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
            .filter(opprinneligAktivitet -> gjelderSammeAktivitet(uttakAktivitet, opprinneligAktivitet))
            .findFirst()
            .orElseThrow(() -> FastsettePerioderFeil.manglendeOpprinneligAktivitet(uttakAktivitet,
                opprinneligPeriode.getAktiviteter()));
    }

    private boolean gjelderSammeAktivitet(ForeldrepengerUttakAktivitet uttakAktivitet,
                                          UttakResultatPeriodeAktivitetEntitet opprinneligAktivitet) {
        return Objects.equals(opprinneligAktivitet.getUttakArbeidType(), uttakAktivitet.getUttakArbeidType())
            && Objects.equals(opprinneligAktivitet.getArbeidsforholdRef(), uttakAktivitet.getArbeidsforholdRef())
            && Objects.equals(opprinneligAktivitet.getUttakAktivitet().getArbeidsgiver().orElse(null),
            uttakAktivitet.getArbeidsgiver().orElse(null));
    }
}
