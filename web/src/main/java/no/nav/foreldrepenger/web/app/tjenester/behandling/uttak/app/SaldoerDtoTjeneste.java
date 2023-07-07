package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import static no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering.SaldoValidering.round;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakUtsettelseType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.TapteDagerFpffTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakEnumMapper;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.StønadskontoRegelAdapter;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering.SaldoValidering;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AktivitetIdentifikator;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.ArbeidsgiverIdentifikator;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.FastsattUttakPeriode;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.FastsattUttakPeriodeAktivitet;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Stønadskontotype;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.saldo.SaldoUtregning;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AktivitetIdentifikatorDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AktivitetSaldoDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.KontoUtvidelser;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.SaldoerDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.StønadskontoDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeAktivitetLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeLagreDto;

@Dependent
public class SaldoerDtoTjeneste {
    private StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;
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
                              StønadskontoRegelAdapter stønadskontoRegelAdapter,
                              BehandlingRepositoryProvider repositoryProvider,
                              ForeldrepengerUttakTjeneste uttakTjeneste,
                              TapteDagerFpffTjeneste tapteDagerFpffTjeneste) {
        this.stønadskontoSaldoTjeneste = stønadskontoSaldoTjeneste;
        this.stønadskontoRegelAdapter = stønadskontoRegelAdapter;
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.uttakTjeneste = uttakTjeneste;
        this.tapteDagerFpffTjeneste = tapteDagerFpffTjeneste;
    }

    public SaldoerDto lagStønadskontoerDto(UttakInput input) {
        var saldoUtregning = stønadskontoSaldoTjeneste.finnSaldoUtregning(input);
        return lagStønadskontoDto(input, saldoUtregning);
    }

    public SaldoerDto lagStønadskontoerDto(UttakInput input, List<UttakResultatPeriodeLagreDto> perioder) {
        var saldoUtregning = stønadskontoSaldoTjeneste.finnSaldoUtregning(input, mapFromDto(perioder));
        return lagStønadskontoDto(input, saldoUtregning);
    }

    private List<FastsattUttakPeriode> mapFromDto(List<UttakResultatPeriodeLagreDto> perioder) {
        return perioder.stream().map(this::map).toList();
    }

    private SaldoerDto lagStønadskontoDto(UttakInput input, SaldoUtregning saldoUtregning) {
        ForeldrepengerGrunnlag fpGrunnlag = input.getYtelsespesifiktGrunnlag();
        var ref = input.getBehandlingReferanse();
        var annenpart = annenPartUttak(fpGrunnlag);
        Map<SaldoerDto.SaldoVisningStønadskontoType, StønadskontoDto> stønadskontoMap = new HashMap<>();
        var saldoValidering = new SaldoValidering(saldoUtregning, annenpart.isPresent(), fpGrunnlag.isBerørtBehandling());
        for (var stønadskontotype : saldoUtregning.stønadskontoer()) {
            List<AktivitetSaldoDto> aktivitetSaldoListe = new ArrayList<>();
            for (var aktivitet : saldoUtregning.aktiviteterForSøker()) {
                var saldo = saldoUtregning.saldo(stønadskontotype, aktivitet);
                var aktivitetIdentifikatorDto = mapToDto(aktivitet);
                aktivitetSaldoListe.add(new AktivitetSaldoDto(aktivitetIdentifikatorDto, saldo));
            }
            var kontoUtvidelser = finnKontoUtvidelser(ref, stønadskontotype, annenpart, fpGrunnlag);
            var saldoValideringResultat = saldoValidering.valider(stønadskontotype);
            var visningStønadskontoType = SaldoerDto.SaldoVisningStønadskontoType.fra(stønadskontotype);
            stønadskontoMap.put(visningStønadskontoType,
                new StønadskontoDto(visningStønadskontoType, saldoUtregning.getMaxDager(stønadskontotype), saldoUtregning.saldo(stønadskontotype),
                    aktivitetSaldoListe, saldoValideringResultat.isGyldig(), kontoUtvidelser.orElse(null)));
        }
        if (saldoUtregning.getMaxDagerFlerbarnsdager().merEnn0()) {
            var stønadskontoDto = foreldrepengerFlerbarnsdagerDto(saldoUtregning);
            stønadskontoMap.put(stønadskontoDto.stonadskontotype(), stønadskontoDto);
        }
        if (saldoUtregning.getMaxDagerUtenAktivitetskrav().merEnn0()) {
            var stønadskontoDto = foreldrepengerUtenAktKravDto(saldoUtregning);
            stønadskontoMap.put(stønadskontoDto.stonadskontotype(), stønadskontoDto);
        }
        if (saldoUtregning.getMaxDagerMinsterett().merEnn0()) {
            var stønadskontoDto = foreldrepengerMinsterettDto(saldoUtregning);
            stønadskontoMap.put(stønadskontoDto.stonadskontotype(), stønadskontoDto);
        }
        if (harMinsterettEtterNesteSak(saldoUtregning, fpGrunnlag, ref)) {
            var stønadskontoDto = foreldrepengerEtterNesteStønadsperiodeDto(saldoUtregning);
            stønadskontoMap.put(stønadskontoDto.stonadskontotype(), stønadskontoDto);
        }

        var tapteDagerFpff = finnTapteDagerFpff(input);
        return new SaldoerDto(stønadskontoMap, tapteDagerFpff);
    }

    private boolean harMinsterettEtterNesteSak(SaldoUtregning saldoUtregning, ForeldrepengerGrunnlag fpGrunnlag, BehandlingReferanse ref) {
        return saldoUtregning.getMaxDagerEtterNesteStønadsperiode().merEnn0() && fpGrunnlag.getNesteSakGrunnlag().isPresent()
            && harTrekkdagerEtterNesteSak(ref, fpGrunnlag.getNesteSakGrunnlag().orElseThrow());
    }

    private boolean harTrekkdagerEtterNesteSak(BehandlingReferanse ref, NesteSakGrunnlagEntitet nesteSakGrunnlag) {
        var nesteSakFom = nesteSakGrunnlag.getStartdato();
        return uttakTjeneste.hentUttakHvisEksisterer(ref.behandlingId()).map(u -> {
            //Bruker opprinnelige uttak fra reglene for at minsterett saldo ikke skal forsvinne hvis saksbehandler fjerner alle trekkdager etter ny sak i AP
            return u.getOpprinneligPerioder().stream().anyMatch(p -> p.harTrekkdager() && !p.getTom().isBefore(nesteSakFom));
        }).orElse(false);
    }

    private StønadskontoDto foreldrepengerFlerbarnsdagerDto(SaldoUtregning saldoUtregning) {
        var aktivitetSaldoList = saldoUtregning.aktiviteterForSøker().stream().map(a -> {
            var restSaldoFlerbarnsdager = saldoUtregning.restSaldoFlerbarnsdager(a);
            return new AktivitetSaldoDto(mapToDto(a), round(restSaldoFlerbarnsdager));
        }).toList();
        int restSaldoFlerbarnsdager = aktivitetSaldoList.stream()
            .map(AktivitetSaldoDto::saldo)
            .max(Comparator.comparing(integer -> integer))
            .orElse(0);
        var gyldigForbruk = restSaldoFlerbarnsdager >= 0;
        return new StønadskontoDto(SaldoerDto.SaldoVisningStønadskontoType.FLERBARNSDAGER,
            saldoUtregning.getMaxDagerFlerbarnsdager().rundOpp(), restSaldoFlerbarnsdager, aktivitetSaldoList, gyldigForbruk,
            null);
    }

    private StønadskontoDto foreldrepengerUtenAktKravDto(SaldoUtregning saldoUtregning) {
        var aktivitetSaldoList = saldoUtregning.aktiviteterForSøker().stream().map(a -> {
            var restSaldoDagerUtenAktivitetskrav = saldoUtregning.restSaldoDagerUtenAktivitetskrav(a);
            var totalSaldo = saldoUtregning.saldo(Stønadskontotype.FORELDREPENGER, a);
            return new AktivitetSaldoDto(mapToDto(a), Math.min(round(restSaldoDagerUtenAktivitetskrav), totalSaldo));
        }).toList();
        int restSaldoDagerUtenAktivitetskrav = aktivitetSaldoList.stream()
            .map(AktivitetSaldoDto::saldo)
            .max(Comparator.comparing(integer -> integer))
            .orElse(0);
        var gyldigForbruk = restSaldoDagerUtenAktivitetskrav >= 0;
        return new StønadskontoDto(SaldoerDto.SaldoVisningStønadskontoType.UTEN_AKTIVITETSKRAV,
            saldoUtregning.getMaxDagerUtenAktivitetskrav().rundOpp(), restSaldoDagerUtenAktivitetskrav, aktivitetSaldoList, gyldigForbruk,
            null);
    }

    private StønadskontoDto foreldrepengerMinsterettDto(SaldoUtregning saldoUtregning) {
        var aktivitetSaldoList = saldoUtregning.aktiviteterForSøker().stream().map(a -> {
            var restSaldoMinsterettDager = saldoUtregning.restSaldoMinsterett(a);
            var totalSaldo = saldoUtregning.saldo(Stønadskontotype.FORELDREPENGER, a);
            return new AktivitetSaldoDto(mapToDto(a), Math.min(round(restSaldoMinsterettDager), totalSaldo));
        }).toList();
        int restSaldoMinsterett = aktivitetSaldoList.stream()
            .map(AktivitetSaldoDto::saldo)
            .max(Comparator.comparing(integer -> integer))
            .orElse(0);
        var gyldigForbruk = restSaldoMinsterett >= 0;
        return new StønadskontoDto(SaldoerDto.SaldoVisningStønadskontoType.MINSTERETT,
            saldoUtregning.getMaxDagerMinsterett().rundOpp(), restSaldoMinsterett, aktivitetSaldoList, gyldigForbruk,
            null);
    }

    private StønadskontoDto foreldrepengerEtterNesteStønadsperiodeDto(SaldoUtregning saldoUtregning) {
        var aktivitetSaldoList = saldoUtregning.aktiviteterForSøker().stream().map(a -> {
            var restSaldo = saldoUtregning.restSaldoEtterNesteStønadsperiode(a);
            return new AktivitetSaldoDto(mapToDto(a), round(restSaldo));
        }).toList();
        int restSaldo = aktivitetSaldoList.stream()
            .map(AktivitetSaldoDto::saldo)
            .max(Comparator.comparing(integer -> integer))
            .orElse(0);
        var gyldigForbruk = restSaldo >= 0;
        return new StønadskontoDto(SaldoerDto.SaldoVisningStønadskontoType.MINSTERETT_NESTE_STØNADSPERIODE,
            saldoUtregning.getMaxDagerEtterNesteStønadsperiode().rundOpp(), restSaldo, aktivitetSaldoList, gyldigForbruk, null);
    }

    private int finnTapteDagerFpff(UttakInput input) {
        return tapteDagerFpffTjeneste.antallTapteDagerFpff(input);
    }

    private Optional<ForeldrepengerUttak> annenPartUttak(ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        var annenpart = foreldrepengerGrunnlag.getAnnenpart();
        if (annenpart.isPresent()) {
            return uttakTjeneste.hentUttakHvisEksisterer(annenpart.get().gjeldendeVedtakBehandlingId());
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
        var yfAggregat = ytelsesFordelingRepository.hentAggregat(ref.behandlingId());
        var fagsakRelasjon = fagsakRelasjonRepository.finnRelasjonFor(ref.saksnummer());
        var stønadskontoberegning = stønadskontoRegelAdapter.beregnKontoerMedResultat(ref, yfAggregat, fagsakRelasjon, annenpart, fpGrunnlag);
        int prematurdager = stønadskontoberegning.getAntallPrematurDager();
        int flerbarnsdager = stønadskontoberegning.getAntallFlerbarnsdager();

        if (prematurdager > 0 || flerbarnsdager > 0) {
            return Optional.of(new KontoUtvidelser(prematurdager, flerbarnsdager));
        }
        return Optional.empty();
    }

    private FastsattUttakPeriode map(UttakResultatPeriodeLagreDto dto) {
        return new FastsattUttakPeriode.Builder()
            .samtidigUttak(dto.isSamtidigUttak())
            .oppholdÅrsak(UttakEnumMapper.map(dto.getOppholdÅrsak()))
            .flerbarnsdager(dto.isFlerbarnsdager())
            .aktiviteter(map(dto.getAktiviteter()))
            .periodeResultatType(UttakEnumMapper.map(dto.getPeriodeResultatType()))
            .resultatÅrsak(UttakEnumMapper.mapTilFastsattPeriodeÅrsak(dto.getPeriodeResultatÅrsak()))
            .utsettelse(!UttakUtsettelseType.UDEFINERT.equals(dto.getUtsettelseType()))
            .tidsperiode(dto.getFom(), dto.getTom())
            .mottattDato(dto.getMottattDato())
            .build();
    }

    private List<FastsattUttakPeriodeAktivitet> map(List<UttakResultatPeriodeAktivitetLagreDto> aktiviteter) {
        return aktiviteter.stream().map(this::map).toList();
    }

    private FastsattUttakPeriodeAktivitet map(UttakResultatPeriodeAktivitetLagreDto dto) {
        return new FastsattUttakPeriodeAktivitet(UttakEnumMapper.map(dto.getTrekkdagerDesimaler()), UttakEnumMapper.map(dto.getStønadskontoType()),
            UttakEnumMapper.map(dto.getUttakArbeidType(), dto.getArbeidsgiver(), dto.getArbeidsforholdId()));
    }

    private AktivitetIdentifikatorDto mapToDto(AktivitetIdentifikator aktivitet) {
        return new AktivitetIdentifikatorDto(UttakEnumMapper.map(aktivitet.getAktivitetType()),
            Optional.ofNullable(aktivitet.getArbeidsgiverIdentifikator()).map(ArbeidsgiverIdentifikator::value).orElse(null), aktivitet.getArbeidsforholdId());
    }

}
