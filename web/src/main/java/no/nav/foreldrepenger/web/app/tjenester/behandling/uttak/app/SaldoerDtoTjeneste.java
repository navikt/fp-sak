package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.TapteDagerFpffTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakEnumMapper;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.StønadskontoRegelAdapter;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering.SaldoValidering;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.MaksDatoUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.regler.uttak.beregnkontoer.StønadskontoResultat;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AktivitetIdentifikator;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.FastsattUttakPeriode;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.FastsattUttakPeriodeAktivitet;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.saldo.SaldoUtregning;
import no.nav.foreldrepenger.regler.uttak.felles.grunnlag.Stønadskontotype;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AktivitetIdentifikatorDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AktivitetSaldoDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsgiverDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.KontoUtvidelser;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.SaldoerDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.StønadskontoDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeAktivitetLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeLagreDto;

@Dependent
public class SaldoerDtoTjeneste {
    private StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;
    private MaksDatoUttakTjeneste maksDatoUttakTjeneste;
    private ArbeidsgiverDtoTjeneste arbeidsgiverDtoTjeneste;
    private StønadskontoRegelAdapter stønadskontoRegelAdapter;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private TapteDagerFpffTjeneste tapteDagerFpffTjeneste;

    public SaldoerDtoTjeneste() {
        //For CDI
    }

    @Inject
    public SaldoerDtoTjeneste(StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste,
                              MaksDatoUttakTjeneste maksDatoUttakTjeneste,
                              ArbeidsgiverDtoTjeneste arbeidsgiverDtoTjeneste,
                              StønadskontoRegelAdapter stønadskontoRegelAdapter,
                              BehandlingRepositoryProvider repositoryProvider,
                              ForeldrepengerUttakTjeneste uttakTjeneste,
                              TapteDagerFpffTjeneste tapteDagerFpffTjeneste) {
        this.stønadskontoSaldoTjeneste = stønadskontoSaldoTjeneste;
        this.maksDatoUttakTjeneste = maksDatoUttakTjeneste;
        this.arbeidsgiverDtoTjeneste = arbeidsgiverDtoTjeneste;
        this.stønadskontoRegelAdapter = stønadskontoRegelAdapter;
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.uttakTjeneste = uttakTjeneste;
        this.tapteDagerFpffTjeneste = tapteDagerFpffTjeneste;
    }

    public SaldoerDto lagStønadskontoerDto(UttakInput input) {
        SaldoUtregning saldoUtregning = stønadskontoSaldoTjeneste.finnSaldoUtregning(input);
        return lagStønadskontoDto(input, saldoUtregning);
    }

    public SaldoerDto lagStønadskontoerDto(UttakInput input, List<UttakResultatPeriodeLagreDto> perioder) {
        SaldoUtregning saldoUtregning = stønadskontoSaldoTjeneste.finnSaldoUtregning(input, mapFromDto(perioder));
        return lagStønadskontoDto(input, saldoUtregning);
    }

    private List<FastsattUttakPeriode> mapFromDto(List<UttakResultatPeriodeLagreDto> perioder) {
        return perioder.stream().map(this::map).collect(Collectors.toList());
    }

    private SaldoerDto lagStønadskontoDto(UttakInput input, SaldoUtregning saldoUtregning) {
        ForeldrepengerGrunnlag fpGrunnlag = input.getYtelsespesifiktGrunnlag();
        var ref = input.getBehandlingReferanse();
        var annenpart = annenPartUttak(fpGrunnlag);
        Map<String, StønadskontoDto> stønadskontoMap = new HashMap<>();
        for (var stønadskontotype : saldoUtregning.stønadskontoer()) {
            List<AktivitetSaldoDto> aktivitetSaldoListe = new ArrayList<>();
            for (AktivitetIdentifikator aktivitet : saldoUtregning.aktiviteterForSøker()) {
                var saldo = saldoUtregning.saldo(stønadskontotype, aktivitet);
                var aktivitetIdentifikatorDto = mapToDto(aktivitet, input.getIayGrunnlag().getArbeidsforholdOverstyringer());
                aktivitetSaldoListe.add(new AktivitetSaldoDto(aktivitetIdentifikatorDto, saldo));
            }
            var saldoValidering = new SaldoValidering(saldoUtregning, annenpart.isPresent(), fpGrunnlag.isTapendeBehandling());
            var kontoUtvidelser = finnKontoUtvidelser(ref, stønadskontotype, annenpart, fpGrunnlag);
            var saldoValideringResultat = saldoValidering.valider(stønadskontotype);
            stønadskontoMap.put(stønadskontotype.name(), new StønadskontoDto(stønadskontotype.name(),
                saldoUtregning.getMaxDager(stønadskontotype), saldoUtregning.saldo(stønadskontotype), aktivitetSaldoListe,
                saldoValideringResultat.isGyldig(), kontoUtvidelser.orElse(null)));
        }
        var tapteDagerFpff = finnTapteDagerFpff(input);
        return new SaldoerDto(maksDatoUttakTjeneste.beregnMaksDatoUttak(input), stønadskontoMap, tapteDagerFpff);
    }

    private int finnTapteDagerFpff(UttakInput input) {
        return tapteDagerFpffTjeneste.antallTapteDagerFpff(input);
    }

    private Optional<ForeldrepengerUttak> annenPartUttak(ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        var annenpart = foreldrepengerGrunnlag.getAnnenpart();
        if (annenpart.isPresent()) {
            return uttakTjeneste.hentUttakHvisEksisterer(annenpart.get().getGjeldendeVedtakBehandlingId());
        }
        return Optional.empty();
    }

    private Optional<KontoUtvidelser> finnKontoUtvidelser(BehandlingReferanse ref,
                                                          Stønadskontotype stønadskonto,
                                                          Optional<ForeldrepengerUttak> annenpart,
                                                          ForeldrepengerGrunnlag fpGrunnlag) {
        if (!Stønadskontotype.FELLESPERIODE.equals(stønadskonto) && !Stønadskontotype.FORELDREPENGER.equals(stønadskonto)) {
            return Optional.empty();
        }
        YtelseFordelingAggregat yfAggregat = ytelsesFordelingRepository.hentAggregat(ref.getBehandlingId());
        FagsakRelasjon fagsakRelasjon = fagsakRelasjonRepository.finnRelasjonFor(ref.getSaksnummer());
        StønadskontoResultat stønadskontoberegning = stønadskontoRegelAdapter.beregnKontoerMedResultat(ref, yfAggregat, fagsakRelasjon, annenpart, fpGrunnlag);
        int prematurdager = stønadskontoberegning.getAntallPrematurDager();
        int flerbarnsdager = stønadskontoberegning.getAntallFlerbarnsdager();

        if (prematurdager > 0 || flerbarnsdager > 0) {
            return Optional.of(new KontoUtvidelser(prematurdager, flerbarnsdager));
        }
        return Optional.empty();
    }

    private FastsattUttakPeriode map(UttakResultatPeriodeLagreDto dto) {
        return new FastsattUttakPeriode.Builder()
            .medSamtidigUttak(dto.isSamtidigUttak())
            .medOppholdÅrsak(UttakEnumMapper.map(dto.getOppholdÅrsak()))
            .medFlerbarnsdager(dto.isFlerbarnsdager())
            .medAktiviteter(map(dto.getAktiviteter()))
            .medPeriodeResultatType(UttakEnumMapper.map(dto.getPeriodeResultatType()))
            .medTidsperiode(dto.getFom(), dto.getTom())
            .build();
    }

    private List<FastsattUttakPeriodeAktivitet> map(List<UttakResultatPeriodeAktivitetLagreDto> aktiviteter) {
        return aktiviteter.stream().map(this::map).collect(Collectors.toList());
    }

    private FastsattUttakPeriodeAktivitet map(UttakResultatPeriodeAktivitetLagreDto dto) {
        return new FastsattUttakPeriodeAktivitet(UttakEnumMapper.map(dto.getTrekkdagerDesimaler()), UttakEnumMapper.map(dto.getStønadskontoType()),
            UttakEnumMapper.map(dto.getUttakArbeidType(), dto.getArbeidsgiver(), dto.getArbeidsforholdRef()));
    }

    private AktivitetIdentifikatorDto mapToDto(AktivitetIdentifikator aktivitet, List<ArbeidsforholdOverstyring> overstyringer) {
        var arbeidsgiverDto = mapToArbeidsgiverDto(aktivitet.getArbeidsgiverType(), aktivitet.getArbeidsgiverIdentifikator(),
            overstyringer);
        return new AktivitetIdentifikatorDto(
            UttakEnumMapper.map(aktivitet.getAktivitetType()),
            arbeidsgiverDto.orElse(null),
            aktivitet.getArbeidsforholdId());
    }

    private Optional<ArbeidsgiverDto> mapToArbeidsgiverDto(AktivitetIdentifikator.ArbeidsgiverType arbeidsgiverType,
                                                           String arbeidsgiverIdentifikator,
                                                           List<ArbeidsforholdOverstyring> overstyringer) {
        if (arbeidsgiverIdentifikator == null) {
            return Optional.empty();
        }
        var arbeidsgiver = arbeidsgiverType.equals(AktivitetIdentifikator.ArbeidsgiverType.PERSON) ?
            Arbeidsgiver.person(new AktørId(arbeidsgiverIdentifikator)) : Arbeidsgiver.virksomhet(arbeidsgiverIdentifikator);
        return Optional.ofNullable(arbeidsgiverDtoTjeneste.mapFra(arbeidsgiver, overstyringer));
    }
}
