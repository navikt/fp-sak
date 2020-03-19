package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.domene.uttak.UttakEnumMapper;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.FastsettePerioderRevurderingUtil;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.FastsattUttakPeriode;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.FastsattUttakPeriodeAktivitet;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Revurdering;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Vedtak;

@ApplicationScoped
public class RevurderingGrunnlagBygger {

    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private UttakRepository uttakRepository;

    RevurderingGrunnlagBygger() {
        // CDI
    }

    @Inject
    public RevurderingGrunnlagBygger(YtelsesFordelingRepository ytelsesFordelingRepository,
                                     UttakRepository uttakRepository) {
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
        this.uttakRepository = uttakRepository;
    }

    public Optional<Revurdering.Builder> byggGrunnlag(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        if (!ref.erRevurdering()) {
            return Optional.empty();
        }
        var endringsdato = endringsdato(input);
        return Optional.of(new Revurdering.Builder()
            .medEndringsdato(endringsdato)
            .medGjeldendeVedtak(vedtak(input, endringsdato)));
    }

    private Vedtak.Builder vedtak(UttakInput input, LocalDate endringsdato) {
        var uttaksperioder = finnGjeldendeUttaksperioderFørEndringsdato(input, endringsdato);
        var builder = new Vedtak.Builder();
        for (var periode : uttaksperioder) {
            builder.leggTilPeriode(map(periode));
        }
        return builder;
    }

    private List<UttakResultatPeriodeEntitet> finnGjeldendeUttaksperioderFørEndringsdato(UttakInput input, LocalDate endringsdato) {
        ForeldrepengerGrunnlag fpGrunnlag = input.getYtelsespesifiktGrunnlag();
        var originalBehandling = fpGrunnlag.getOriginalBehandling().orElseThrow();
        var gjeldendeUttak = uttakRepository.hentUttakResultatHvisEksisterer(originalBehandling.getId());

        if (gjeldendeUttak.isEmpty()) {
            return List.of();
        }
        return FastsettePerioderRevurderingUtil.perioderFørDato(gjeldendeUttak.get(), endringsdato);
    }

    private FastsattUttakPeriode map(UttakResultatPeriodeEntitet periode) {
        return new FastsattUttakPeriode.Builder()
            .medAktiviteter(map(periode.getAktiviteter()))
            .medFlerbarnsdager(periode.isFlerbarnsdager())
            .medSamtidigUttak(periode.isSamtidigUttak())
            .medTidsperiode(periode.getFom(), periode.getTom())
            .medOppholdÅrsak(UttakEnumMapper.map(periode.getOppholdÅrsak()))
            .medPeriodeResultatType(UttakEnumMapper.map(periode.getPeriodeResultatType()))
            .build();
    }

    private List<FastsattUttakPeriodeAktivitet> map(List<UttakResultatPeriodeAktivitetEntitet> aktiviteter) {
        return aktiviteter.stream().map(a -> map(a)).collect(Collectors.toList());
    }

    private FastsattUttakPeriodeAktivitet map(UttakResultatPeriodeAktivitetEntitet aktivitet) {
        return new FastsattUttakPeriodeAktivitet(UttakEnumMapper.map(aktivitet.getTrekkdager()), UttakEnumMapper.map(aktivitet.getTrekkonto()),
            UttakEnumMapper.map(aktivitet.getUttakAktivitet()));
    }

    private LocalDate endringsdato(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        return ytelsesFordelingRepository.hentAggregat(ref.getBehandlingId()).getGjeldendeEndringsdato();
    }
}
