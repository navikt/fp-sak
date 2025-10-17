package no.nav.foreldrepenger.web.app.tjenester.formidling;

import static no.nav.foreldrepenger.web.app.tjenester.formidling.BrevGrunnlagDto.Barn;
import static no.nav.foreldrepenger.web.app.tjenester.formidling.BrevGrunnlagDto.BehandlingÅrsakType;
import static no.nav.foreldrepenger.web.app.tjenester.formidling.BrevGrunnlagDto.FamilieHendelse;
import static no.nav.foreldrepenger.web.app.tjenester.formidling.BrevGrunnlagDto.InnsynBehandling;
import static no.nav.foreldrepenger.web.app.tjenester.formidling.BrevGrunnlagDto.Inntektsmelding;
import static no.nav.foreldrepenger.web.app.tjenester.formidling.BrevGrunnlagDto.KlageBehandling;
import static no.nav.foreldrepenger.web.app.tjenester.formidling.BrevGrunnlagDto.Rettigheter;
import static no.nav.foreldrepenger.web.app.tjenester.formidling.BrevGrunnlagDto.Svangerskapspenger;
import static no.nav.foreldrepenger.web.app.tjenester.formidling.BrevGrunnlagDto.Verge;
import static no.nav.foreldrepenger.web.app.tjenester.formidling.EnumMapper.mapBehandlingResultatType;
import static no.nav.foreldrepenger.web.app.tjenester.formidling.EnumMapper.mapBehandlingType;
import static no.nav.foreldrepenger.web.app.tjenester.formidling.EnumMapper.mapFagsakYtelseType;
import static no.nav.foreldrepenger.web.app.tjenester.formidling.EnumMapper.mapInnsynResultatType;
import static no.nav.foreldrepenger.web.app.tjenester.formidling.EnumMapper.mapPeriodeResultatType;
import static no.nav.foreldrepenger.web.app.tjenester.formidling.EnumMapper.mapRelasjonsRolleType;
import static no.nav.foreldrepenger.web.app.tjenester.formidling.EnumMapper.mapRettighetstype;
import static no.nav.foreldrepenger.web.app.tjenester.formidling.EnumMapper.mapSpråkkode;
import static no.nav.foreldrepenger.web.app.tjenester.formidling.EnumMapper.mapStønadskontoType;
import static no.nav.foreldrepenger.web.app.tjenester.formidling.EnumMapper.mapSvpUttakArbeidType;
import static no.nav.foreldrepenger.web.app.tjenester.formidling.EnumMapper.mapTrekkontoType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.klage.KlageVurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.Aktør;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.EngangsstønadBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.eøs.EøsUttakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.eøs.EøsUttaksperiodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageFormkravEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeOrganisasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.Rettighetstype;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.MorsStillingsprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatArbeidsforholdEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.TapteDagerFpffTjeneste;
import no.nav.foreldrepenger.domene.uttak.Uttak;
import no.nav.foreldrepenger.domene.uttak.UttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.UtregnetStønadskontoTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.kontrakter.fpsak.beregningsgrunnlag.v2.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.kontrakter.fpsak.inntektsmeldinger.ArbeidsforholdInntektsmeldingerDto;
import no.nav.foreldrepenger.kontrakter.fpsak.tilkjentytelse.TilkjentYtelseDagytelseDto;
import no.nav.foreldrepenger.kontrakter.fpsak.tilkjentytelse.TilkjentYtelseEngangsstønadDto;
import no.nav.foreldrepenger.produksjonsstyring.tilbakekreving.FptilbakeRestKlient;
import no.nav.foreldrepenger.produksjonsstyring.tilbakekreving.TilbakeBehandlingDto;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.UtsettelseCore2021;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.SaldoerDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.formidling.BrevGrunnlagDto.Foreldrepenger.Stønadskonto;
import no.nav.foreldrepenger.web.app.tjenester.formidling.arbeidsforholdInntektsmelding.ArbeidsforholdInntektsmeldingDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.formidling.beregningsgrunnlag.BeregningsgrunnlagFormidlingV2DtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.formidling.tilkjentytelse.TilkjentYtelseFormidlingDtoTjeneste;

@ApplicationScoped
class BrevGrunnlagTjeneste {

    private SøknadRepository søknadRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private UføretrygdRepository uføretrygdRepository;
    private BehandlingDokumentRepository behandlingDokumentRepository;
    private EøsUttakRepository eøsUttakRepository;
    private EngangsstønadBeregningRepository engangsstønadBeregningRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;
    private VergeRepository vergeRepository;
    private InnsynRepository innsynRepository;
    private SvangerskapspengerUttakResultatRepository svpRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private UttakTjeneste uttakTjeneste;
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;
    private MedlemTjeneste medlemTjeneste;
    private UtregnetStønadskontoTjeneste utregnetStønadskontoTjeneste;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private DekningsgradTjeneste dekningsgradTjeneste;
    private BeregningTjeneste beregningTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private KlageVurderingTjeneste klageVurderingTjeneste;
    private VedtakTjeneste vedtakTjeneste;
    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;
    private TapteDagerFpffTjeneste tapteDagerFpffTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private SaldoerDtoTjeneste saldoerDtoTjeneste;

    private FptilbakeRestKlient fptilbakeRestKlient;

    @Inject
    BrevGrunnlagTjeneste(BehandlingRepositoryProvider repositoryProvider,
                         InnsynRepository innsynRepository,
                         FamilieHendelseTjeneste familieHendelseTjeneste,
                         UttakTjeneste uttakTjeneste,
                         MedlemTjeneste medlemTjeneste,
                         EøsUttakRepository eøsUttakRepository,
                         UtregnetStønadskontoTjeneste utregnetStønadskontoTjeneste,
                         YtelseFordelingTjeneste ytelseFordelingTjeneste,
                         UføretrygdRepository uføretrygdRepository,
                         BehandlingDokumentRepository behandlingDokumentRepository,
                         ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste,
                         SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                         DekningsgradTjeneste dekningsgradTjeneste,
                         EngangsstønadBeregningRepository engangsstønadBeregningRepository,
                         BeregningTjeneste beregningTjeneste,
                         InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                         ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste,
                         InntektsmeldingTjeneste inntektsmeldingTjeneste,
                         VergeRepository vergeRepository,
                         KlageVurderingTjeneste klageVurderingTjeneste,
                         FptilbakeRestKlient fptilbakeRestKlient,
                         VedtakTjeneste vedtakTjeneste,
                         RelatertBehandlingTjeneste relatertBehandlingTjeneste,
                         TapteDagerFpffTjeneste tapteDagerFpffTjeneste,
                         UttakInputTjeneste uttakInputTjeneste,
                         SaldoerDtoTjeneste saldoerDtoTjeneste) {
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.innsynRepository = innsynRepository;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.uttakTjeneste = uttakTjeneste;
        this.medlemTjeneste = medlemTjeneste;
        this.eøsUttakRepository = eøsUttakRepository;
        this.utregnetStønadskontoTjeneste = utregnetStønadskontoTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.uføretrygdRepository = uføretrygdRepository;
        this.behandlingDokumentRepository = behandlingDokumentRepository;
        this.foreldrepengerUttakTjeneste = foreldrepengerUttakTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.dekningsgradTjeneste = dekningsgradTjeneste;
        this.engangsstønadBeregningRepository = engangsstønadBeregningRepository;
        this.beregningTjeneste = beregningTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.arbeidsforholdInntektsmeldingMangelTjeneste = arbeidsforholdInntektsmeldingMangelTjeneste;
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.mottatteDokumentRepository = repositoryProvider.getMottatteDokumentRepository();
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.uttaksperiodegrenseRepository = repositoryProvider.getUttaksperiodegrenseRepository();
        this.vergeRepository = vergeRepository;
        this.klageVurderingTjeneste = klageVurderingTjeneste;
        this.fptilbakeRestKlient = fptilbakeRestKlient;
        this.vedtakTjeneste = vedtakTjeneste;
        this.svpRepository = repositoryProvider.getSvangerskapspengerUttakResultatRepository();
        this.relatertBehandlingTjeneste = relatertBehandlingTjeneste;
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.tapteDagerFpffTjeneste = tapteDagerFpffTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.saldoerDtoTjeneste = saldoerDtoTjeneste;
    }

    BrevGrunnlagTjeneste() {
        //CDI
    }

    BrevGrunnlagDto lagGrunnlagDto(Behandling behandling) {
        var saksnummer = behandling.getFagsak().getSaksnummer().getVerdi();
        var ytelseType = mapFagsakYtelseType(behandling.getFagsak().getYtelseType());
        var relasjonsRolleType = mapRelasjonsRolleType(behandling.getRelasjonsRolleType());
        var aktørId = behandling.getAktørId().getId();
        var behandlingType = mapBehandlingType(behandling.getType());

        var opprettet = behandling.getOpprettetDato();
        var avsluttet = behandling.getAvsluttetDato();
        var behandlendeEnhet = behandling.getBehandlendeEnhet();
        var språkkode = finnSpråkkode(behandling);
        var automatiskBehandlet = !behandling.isToTrinnsBehandling() || behandlingType == BrevGrunnlagDto.BehandlingType.KLAGE;
        var familieHendelse = finnFamilieHendelse(behandling.getId()).orElse(null);
        var originalBehandling = behandling.getOriginalBehandlingId().map(this::finnOriginalBehandling).orElse(null);
        var behandlingsresultat = finnBehandlingsresultat(behandling).orElse(null);
        var behandlingÅrsakTyper = finnBehandlingÅrsakTyper(behandling);
        var førsteSøknadMottattDato = finnFørsteSøknadMottattDato(behandling.getFagsak()).orElse(null);
        var sisteSøknadMottattDato = finnSisteSøknadMottattDato(behandling.getFagsak()).orElse(null);
        var ref = BehandlingReferanse.fra(behandling);
        var beregningsgrunnlag = finnBeregningsgrunnlag(behandling).orElse(null);
        var søknadMottattDato = finnSøknadMottattDato(behandling).orElse(null);
        var verge = finnVerge(behandling).orElse(null);
        var klageBehandling = finnKlageBehandling(behandling).orElse(null);
        var innsynBehandling = finnInnsynBehandling(behandling).orElse(null);

        var stp = finnSkjæringstidspunktForBehandling(behandling);
        var tilkjentYtelse = stp.flatMap(_ -> finnTilkjentYtelse(behandling)).orElse(null);
        var inntektsmeldingerStatus = stp.flatMap(skjæringstidspunkt -> finnInntektsmeldingerStatus(ref, skjæringstidspunkt)).orElse(null);
        var inntektsmeldinger = stp.map(s -> finnInntektsmeldinger(behandling, s)).orElse(List.of());
        var svangerskapspengerUttak = stp.flatMap(_ -> finnSvangerskapspengerUttak(behandling)).orElse(null);
        var foreldrepenger = stp.flatMap(_ -> finnForeldrepengerUttak(behandling)).orElse(null);

        return new BrevGrunnlagDto(behandling.getUuid(), saksnummer, ytelseType, relasjonsRolleType, aktørId, behandlingType, opprettet, avsluttet,
            behandlendeEnhet, språkkode, automatiskBehandlet, familieHendelse, originalBehandling, behandlingsresultat, behandlingÅrsakTyper,
            tilkjentYtelse, beregningsgrunnlag, inntektsmeldingerStatus, førsteSøknadMottattDato, sisteSøknadMottattDato, søknadMottattDato,
            inntektsmeldinger, verge, klageBehandling, innsynBehandling, svangerskapspengerUttak, foreldrepenger);
    }

    private Optional<BrevGrunnlagDto.TilkjentYtelse> finnTilkjentYtelse(Behandling behandling) {
        var engangsstønad = finnTilkjentYtelseEngangsstønad(behandling).orElse(null);
        var originalBehandlingEngangsstønad = behandling.getOriginalBehandlingId().flatMap(b -> {
            var originalBehandling = behandlingRepository.hentBehandling(b);
            return finnTilkjentYtelseEngangsstønad(originalBehandling);
        }).orElse(null);
        var dagytelse = finnTilkjentYtelseDagytelse(behandling).orElse(null);
        if (engangsstønad == null && originalBehandlingEngangsstønad == null && dagytelse == null) {
            return Optional.empty();
        }
        return Optional.of(new BrevGrunnlagDto.TilkjentYtelse(engangsstønad, originalBehandlingEngangsstønad, dagytelse));
    }

    private Optional<BrevGrunnlagDto.Foreldrepenger> finnForeldrepengerUttak(Behandling behandling) {
        if (behandling.getFagsakYtelseType() != FagsakYtelseType.FORELDREPENGER || harUttakTilManuellBehandling(behandling)) {
            return Optional.empty();
        }
        var uttakInput = uttakInputTjeneste.lagInput(behandling);
        var perioderSøker = finnUttakResultatPerioderSøker(behandling.getId());
        var annenpartUttaksperioder = annenpartBehandling(behandling).flatMap(ab -> foreldrepengerUttakTjeneste.hentHvisEksisterer(ab.getId()))
            .map(BrevGrunnlagTjeneste::finnUttakResultatPerioder)
            .orElse(List.of());
        var dekningsgrad = finnDekningsgrad(behandling);
        var rettigheter = utledRettigheter(behandling);
        var nyStartDatoVedUtsattOppstart = finnNyStartDatoVedUtsattOppstart(behandling);
        if (perioderSøker.isEmpty() && annenpartUttaksperioder.isEmpty() && dekningsgrad.isEmpty() && rettigheter.isEmpty()
            && nyStartDatoVedUtsattOppstart.isEmpty()) {
            return Optional.empty();
        }
        var stønadskontoer = finnStønadskontoer(uttakInput);
        var tapteDagerFpff = finnTapteDagerFpff(uttakInput, stønadskontoer);
        var ønskerJustertUttakVedFødsel = ytelseFordelingTjeneste.hentAggregat(behandling.getId()).getGjeldendeFordeling().ønskerJustertVedFødsel();
        return Optional.of(
            new BrevGrunnlagDto.Foreldrepenger(dekningsgrad.orElse(null), rettigheter.orElse(null), stønadskontoer, tapteDagerFpff,
                perioderSøker, annenpartUttaksperioder, ønskerJustertUttakVedFødsel, nyStartDatoVedUtsattOppstart.orElse(null)));
    }

    private boolean harUttakTilManuellBehandling(Behandling behandling) {
        return foreldrepengerUttakTjeneste.hentHvisEksisterer(behandling.getId())
            .map(ForeldrepengerUttak::getGjeldendePerioder)
            .orElse(List.of())
            .stream()
            .anyMatch(p -> p.getResultatType() == PeriodeResultatType.MANUELL_BEHANDLING);
    }

    private List<BrevGrunnlagDto.Foreldrepenger.Stønadskonto> finnStønadskontoer(UttakInput input) {
        var saldo = saldoerDtoTjeneste.lagStønadskontoerDto(input);
        return saldo.stonadskontoer().values().stream().map(k -> {
            var kontoUtvidelser = Optional.ofNullable(k.kontoUtvidelser())
                .map(u -> new BrevGrunnlagDto.Foreldrepenger.KontoUtvidelser(u.prematurdager(), u.flerbarnsdager()))
                .orElse(null);
            return new Stønadskonto(mapStønadskontoType(k.stonadskontotype()), k.maxDager(), k.saldo(), kontoUtvidelser);
        }).toList();
    }

    private int finnTapteDagerFpff(UttakInput input, List<Stønadskonto> konti) {
        if (konti.stream().noneMatch(k -> k.stønadskontotype() == BrevGrunnlagDto.Foreldrepenger.Stønadskonto.Type.FORELDREPENGER_FØR_FØDSEL)) {
            return 0;
        }
        var saldoFpff = konti.stream()
            .filter(k -> k.stønadskontotype() == Stønadskonto.Type.FORELDREPENGER_FØR_FØDSEL)
            .map(BrevGrunnlagDto.Foreldrepenger.Stønadskonto::saldo)
            .findAny()
            .orElseThrow();
        return tapteDagerFpffTjeneste.antallTapteDagerFpff(input, saldoFpff);
    }

    private Optional<Behandling> annenpartBehandling(Behandling søkersBehandling) {
        if (harVedtak(søkersBehandling)) {
            return relatertBehandlingTjeneste.hentAnnenPartsGjeldendeBehandlingPåVedtakstidspunkt(søkersBehandling);
        }
        return relatertBehandlingTjeneste.hentAnnenPartsGjeldendeVedtattBehandling(søkersBehandling.getSaksnummer());
    }

    private boolean harVedtak(Behandling søkersBehandling) {
        return behandlingVedtakRepository.hentForBehandlingHvisEksisterer(søkersBehandling.getId()).isPresent();
    }

    private List<BrevGrunnlagDto.Foreldrepenger.Uttaksperiode> finnUttakResultatPerioderSøker(Long behandlingId) {
        return foreldrepengerUttakTjeneste.hentHvisEksisterer(behandlingId).map(BrevGrunnlagTjeneste::finnUttakResultatPerioder).orElse(List.of());
    }

    private static List<BrevGrunnlagDto.Foreldrepenger.Uttaksperiode> finnUttakResultatPerioder(ForeldrepengerUttak uttakResultat) {
        return uttakResultat.getGjeldendePerioder()
            .stream()
            .map(BrevGrunnlagTjeneste::map)
            .sorted(Comparator.comparing(BrevGrunnlagDto.Foreldrepenger.Uttaksperiode::fom))
            .toList();
    }

    private static BrevGrunnlagDto.Foreldrepenger.Uttaksperiode map(ForeldrepengerUttakPeriode periode) {
        var periodeRsesultatType = mapPeriodeResultatType(periode.getResultatType());
        var aktiviteter = periode.getAktiviteter().stream().map(BrevGrunnlagTjeneste::map).toList();
        return new BrevGrunnlagDto.Foreldrepenger.Uttaksperiode(periode.getFom(), periode.getTom(), aktiviteter, periodeRsesultatType,
            periode.getResultatÅrsak().getKode(), periode.getGraderingAvslagÅrsak().getKode(), periode.getResultatÅrsak().getLovHjemmelData(),
            periode.getGraderingAvslagÅrsak().getLovHjemmelData(), periode.getTidligstMottatttDato(),
            utledOmUtbetalingErRedusertTilMorsStillingsprosent(periode));
    }

    private static boolean utledOmUtbetalingErRedusertTilMorsStillingsprosent(ForeldrepengerUttakPeriode periode) {
        if (periode.harRedusertUtbetaling() && MorsAktivitet.ARBEID.equals(periode.getMorsAktivitet()) && periode.getDokumentasjonVurdering()
            .isPresent()) {
            var morsStillingsprosent = periode.getDokumentasjonVurdering()
                .map(DokumentasjonVurdering::morsStillingsprosent)
                .map(MorsStillingsprosent::decimalValue)
                .orElse(BigDecimal.ZERO);

            var morsStillingsProsentErUnder75 =
                (morsStillingsprosent.compareTo(BigDecimal.ZERO) != 0) && morsStillingsprosent.compareTo(BigDecimal.valueOf(75)) < 0;
            var minstEnPeriodeHarUtbetalingLikMorsStillingsprosent = periode.getAktiviteter()
                .stream()
                .anyMatch(aktivitet -> aktivitet.getUtbetalingsgrad().decimalValue().compareTo(morsStillingsprosent) == 0);
            boolean morsAktivitetGodkjent = periode.getDokumentasjonVurdering()
                .map(dokvurdering -> dokvurdering.type().equals(DokumentasjonVurdering.Type.MORS_AKTIVITET_GODKJENT))
                .orElse(false);
            var graderingEllerSamtidigUttak = periode.isSamtidigUttak() || periode.isGraderingInnvilget();

            return morsStillingsProsentErUnder75 && minstEnPeriodeHarUtbetalingLikMorsStillingsprosent && morsAktivitetGodkjent
                && !graderingEllerSamtidigUttak;
        }
        return false;
    }

    private static BrevGrunnlagDto.Foreldrepenger.Aktivitet map(ForeldrepengerUttakPeriodeAktivitet aktivitet) {
        var stønadskontoType = mapTrekkontoType(aktivitet.getTrekkonto());
        var arbeidsgiverReferanse = aktivitet.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElse(null);
        var arbeidsforholdId = aktivitet.getArbeidsforholdRef() == null ? null : aktivitet.getArbeidsforholdRef().getReferanse();
        return new BrevGrunnlagDto.Foreldrepenger.Aktivitet(stønadskontoType, aktivitet.getTrekkdager().decimalValue(),
            aktivitet.getArbeidsprosent(), arbeidsgiverReferanse, arbeidsforholdId, aktivitet.getUtbetalingsgrad().decimalValue(),
            mapSvpUttakArbeidType(aktivitet.getUttakArbeidType()), aktivitet.isSøktGraderingForAktivitetIPeriode());
    }

    private Optional<Svangerskapspenger> finnSvangerskapspengerUttak(Behandling behandling) {
        if (behandling.getFagsakYtelseType() != FagsakYtelseType.SVANGERSKAPSPENGER) {
            return Optional.empty();
        }
        var optionalUttakResultat = svpRepository.hentHvisEksisterer(behandling.getId());
        if (optionalUttakResultat.isEmpty()) {
            return Optional.empty();
        }
        var uttakResultat = optionalUttakResultat.get();

        List<Svangerskapspenger.UttakArbeidsforhold> arbeidsforholdDtos = new ArrayList<>();
        for (var arbeidsforholdEntitet : uttakResultat.getUttaksResultatArbeidsforhold()) {

            var uttakResultatPeriodeDtos = arbeidsforholdEntitet.getPerioder().stream().map(this::mapSvpUttakResultatPeriodeDto).toList();
            arbeidsforholdDtos.add(
                mapSvpUttakResultatArbeidsforholdDto(arbeidsforholdEntitet, sortSvpUttakResultatPeriodeDtoer(uttakResultatPeriodeDtos)));
        }

        return Optional.of(new Svangerskapspenger(arbeidsforholdDtos));
    }

    private List<Svangerskapspenger.Uttaksperiode> sortSvpUttakResultatPeriodeDtoer(List<Svangerskapspenger.Uttaksperiode> uttakResultatPeriodeDtos) {
        return uttakResultatPeriodeDtos.stream().sorted(Comparator.comparing(Svangerskapspenger.Uttaksperiode::fom)).toList();
    }

    private Svangerskapspenger.Uttaksperiode mapSvpUttakResultatPeriodeDto(SvangerskapspengerUttakResultatPeriodeEntitet svangerskapspengerUttakResultatPeriodeEntitet) {
        return new Svangerskapspenger.Uttaksperiode(svangerskapspengerUttakResultatPeriodeEntitet.getFom(),
            svangerskapspengerUttakResultatPeriodeEntitet.getTom(),
            mapPeriodeResultatType(svangerskapspengerUttakResultatPeriodeEntitet.getPeriodeResultatType()),
            svangerskapspengerUttakResultatPeriodeEntitet.getPeriodeIkkeOppfyltÅrsak().getKode());
    }

    private Svangerskapspenger.UttakArbeidsforhold mapSvpUttakResultatArbeidsforholdDto(SvangerskapspengerUttakResultatArbeidsforholdEntitet perArbeidsforhold,
                                                                                        List<Svangerskapspenger.Uttaksperiode> periodeDtoer) {
        var arbeidsforholdIkkeOppfyltÅrsak = perArbeidsforhold.getArbeidsforholdIkkeOppfyltÅrsak().getKode();
        var arbeidsgiverReferanse = perArbeidsforhold.getArbeidsgiver() != null ? perArbeidsforhold.getArbeidsgiver().getIdentifikator() : null;
        var uttakArbeidType = mapSvpUttakArbeidType(perArbeidsforhold.getUttakArbeidType());
        return new Svangerskapspenger.UttakArbeidsforhold(arbeidsforholdIkkeOppfyltÅrsak, arbeidsgiverReferanse, uttakArbeidType, periodeDtoer);
    }

    private Optional<InnsynBehandling> finnInnsynBehandling(Behandling behandling) {
        var lagreteVedtak = vedtakTjeneste.hentLagreteVedtakPåFagsak(behandling.getFagsakId());
        var innsynOpt = innsynRepository.hentForBehandling(behandling.getId());
        if (innsynOpt.isEmpty() || lagreteVedtak.isEmpty()) {
            return Optional.empty();
        }
        var resultat = mapInnsynResultatType(innsynOpt.get().getInnsynResultatType());
        var dokumenter = innsynOpt.get()
            .getInnsynDokumenter()
            .stream()
            .map(d -> new InnsynBehandling.InnsynDokument(d.getJournalpostId().getVerdi(), d.getDokumentId()))
            .toList();
        return Optional.of(new InnsynBehandling(resultat, dokumenter));
    }

    private Optional<KlageBehandling> finnKlageBehandling(Behandling behandling) {
        var klageResultat = klageVurderingTjeneste.hentKlageResultatHvisEksisterer(behandling);
        if (klageResultat.isEmpty()) {
            return Optional.empty();
        }
        var påklagdBehandling = klageResultat.get().getPåKlagdBehandlingId().map(behandlingRepository::hentBehandling);
        var nfpVurdering = klageVurderingTjeneste.hentKlageVurderingResultat(behandling, KlageVurdertAv.NFP)
            .map(BrevGrunnlagTjeneste::mapKlageVurderingResultatDto);
        var nkVurdering = klageVurderingTjeneste.hentKlageVurderingResultat(behandling, KlageVurdertAv.NK)
            .map(BrevGrunnlagTjeneste::mapKlageVurderingResultatDto);
        var nfpFormkravEntitet = klageVurderingTjeneste.hentKlageFormkrav(behandling, KlageVurdertAv.NFP);
        var klageMottattDato = klageVurderingTjeneste.getKlageMottattDato(behandling).orElse(null);
        var nfpFormkrav = nfpFormkravEntitet.map(fk -> mapKlageFormkravResultatDto(fk, påklagdBehandling, fptilbakeRestKlient));
        var kaFormkrav = klageVurderingTjeneste.hentKlageFormkrav(behandling, KlageVurdertAv.NK)
            .map(fk -> mapKlageFormkravResultatDto(fk, påklagdBehandling, fptilbakeRestKlient));

        return Optional.of(new KlageBehandling(nfpFormkrav.orElse(null), nfpVurdering.orElse(null), kaFormkrav.orElse(null), nkVurdering.orElse(null),
            klageMottattDato));
    }

    private static KlageBehandling.KlageFormkravResultat mapKlageFormkravResultatDto(KlageFormkravEntitet klageFormkrav,
                                                                                     Optional<Behandling> påklagdBehandling,
                                                                                     FptilbakeRestKlient fptilbakeRestKlient) {
        var paKlagdEksternBehandlingUuid = klageFormkrav.hentKlageResultat().getPåKlagdEksternBehandlingUuid();
        Optional<TilbakeBehandlingDto> tilbakekrevingVedtakDto = påklagdBehandling.isPresent() ? Optional.empty() : paKlagdEksternBehandlingUuid.flatMap(
            b -> hentPåklagdBehandlingIdForEksternApplikasjon(b, fptilbakeRestKlient));
        var behandlingType = påklagdBehandling.map(Behandling::getType)
            .orElseGet(() -> tilbakekrevingVedtakDto.map(TilbakeBehandlingDto::type).orElse(null));
        var avvistÅrsaker = klageFormkrav.hentAvvistÅrsaker().stream().map(EnumMapper::mapKlageAvvistÅrsak).toList();
        return new KlageBehandling.KlageFormkravResultat(mapBehandlingType(behandlingType), avvistÅrsaker);
    }

    private static KlageBehandling.KlageVurderingResultat mapKlageVurderingResultatDto(KlageVurderingResultat klageVurderingResultat) {
        return new KlageBehandling.KlageVurderingResultat(klageVurderingResultat.getFritekstTilBrev());
    }

    private static Optional<TilbakeBehandlingDto> hentPåklagdBehandlingIdForEksternApplikasjon(UUID paKlagdEksternBehandlingUuid,
                                                                                               FptilbakeRestKlient fptilbakeRestKlient) {
        return Optional.ofNullable(fptilbakeRestKlient.hentBehandlingInfo(paKlagdEksternBehandlingUuid));
    }

    private Optional<BrevGrunnlagDto.Dekningsgrad> finnDekningsgrad(Behandling behandling) {
        return dekningsgradTjeneste.finnGjeldendeDekningsgradHvisEksisterer(BehandlingReferanse.fra(behandling)).map(this::mapDekningsgrad);
    }

    private BrevGrunnlagDto.Dekningsgrad mapDekningsgrad(Dekningsgrad dekningsgrad) {
        return dekningsgrad.isÅtti() ? BrevGrunnlagDto.Dekningsgrad.ÅTTI : BrevGrunnlagDto.Dekningsgrad.HUNDRE;
    }

    private Optional<Verge> finnVerge(Behandling behandling) {
        return vergeRepository.hentAggregat(behandling.getId()).flatMap(vergeAggregat -> vergeAggregat.getVerge().map(verge -> {
            var aktørId = verge.getBruker().map(Aktør::getAktørId).map(AktørId::getId).orElse(null);
            return new Verge(aktørId, verge.getVergeOrganisasjon().map(VergeOrganisasjonEntitet::getNavn).orElse(null),
                verge.getVergeOrganisasjon().map(VergeOrganisasjonEntitet::getOrganisasjonsnummer).orElse(null), verge.getGyldigFom(),
                verge.getGyldigTom());
        }));
    }

    private Optional<LocalDate> finnNyStartDatoVedUtsattOppstart(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())
            .filter(br -> BehandlingResultatType.FORELDREPENGER_SENERE.equals(br.getBehandlingResultatType()))
            .flatMap(br -> ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandling.getId()))
            .map(yfa -> Optional.ofNullable(yfa.getOppgittFordeling()))
            .flatMap(UtsettelseCore2021::finnFørsteDatoFraSøknad);
    }

    private Optional<LocalDate> finnSøknadMottattDato(Behandling behandling) {
        return uttaksperiodegrenseRepository.hentHvisEksisterer(behandling.getId()).map(Uttaksperiodegrense::getMottattDato);
    }

    private List<Inntektsmelding> finnInntektsmeldinger(Behandling behandling, Skjæringstidspunkt skjæringstidspunkt) {
        var ref = BehandlingReferanse.fra(behandling);
        return inntektArbeidYtelseTjeneste.finnGrunnlag(behandling.getId()).map(g -> {
            var dato = skjæringstidspunkt.getUtledetSkjæringstidspunkt();
            var inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(ref, dato, g,
                skjæringstidspunkt.getFørsteUttaksdatoSøknad().isPresent());
            return inntektsmeldinger.stream()
                .map(inntektsmelding -> new Inntektsmelding(inntektsmelding.getArbeidsgiver().getIdentifikator(),
                    inntektsmelding.getInnsendingstidspunkt()))
                .toList();
        }).orElseGet(List::of);
    }

    private Optional<LocalDate> finnFørsteSøknadMottattDato(Fagsak fagsak) {
        var søknader = finnSøknadsdokumenter(fagsak);
        return søknader.stream().map(MottattDokument::getMottattDato).min(LocalDate::compareTo);
    }

    private Optional<LocalDate> finnSisteSøknadMottattDato(Fagsak fagsak) {
        var søknader = finnSøknadsdokumenter(fagsak);
        return søknader.stream().map(MottattDokument::getMottattDato).max(LocalDate::compareTo);
    }

    private List<MottattDokument> finnSøknadsdokumenter(Fagsak fagsak) {
        return mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(fagsak.getId())
            .stream()
            .filter(md -> md.getDokumentKategori().equals(DokumentKategori.SØKNAD))
            .toList();
    }

    private Optional<ArbeidsforholdInntektsmeldingerDto> finnInntektsmeldingerStatus(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        if (ref.fagsakYtelseType() == FagsakYtelseType.ENGANGSTØNAD) {
            return Optional.empty();
        }
        var alleYrkesaktiviteter = inntektArbeidYtelseTjeneste.finnGrunnlag(ref.behandlingId())
            .flatMap(iay -> iay.getAktørArbeidFraRegister(ref.aktørId()))
            .map(AktørArbeid::hentAlleYrkesaktiviteter)
            .orElse(List.of());
        var arbeidsforholdInntektsmeldingStatuser = arbeidsforholdInntektsmeldingMangelTjeneste.finnStatusForInntektsmeldingArbeidsforhold(ref, stp);

        return Optional.of(
            ArbeidsforholdInntektsmeldingDtoTjeneste.mapInntektsmeldingStatus(arbeidsforholdInntektsmeldingStatuser, alleYrkesaktiviteter,
                stp.getUtledetSkjæringstidspunkt()));
    }

    private Optional<BeregningsgrunnlagDto> finnBeregningsgrunnlag(Behandling behandling) {
        return beregningTjeneste.hent(BehandlingReferanse.fra(behandling)).flatMap(bg -> new BeregningsgrunnlagFormidlingV2DtoTjeneste(bg).map());
    }

    private Optional<TilkjentYtelseDagytelseDto> finnTilkjentYtelseDagytelse(Behandling behandling) {
        return beregningsresultatRepository.hentUtbetBeregningsresultat(behandling.getId()).map(TilkjentYtelseFormidlingDtoTjeneste::mapDagytelse);
    }

    private Optional<TilkjentYtelseEngangsstønadDto> finnTilkjentYtelseEngangsstønad(Behandling behandling) {
        if (behandling.getFagsakYtelseType() != FagsakYtelseType.ENGANGSTØNAD) {
            return Optional.empty();
        }
        return engangsstønadBeregningRepository.hentEngangsstønadBeregning(behandling.getId())
            .map(b -> new TilkjentYtelseEngangsstønadDto(b.getBeregnetTilkjentYtelse()));
    }

    private List<BehandlingÅrsakType> finnBehandlingÅrsakTyper(Behandling behandling) {
        return behandling.getBehandlingÅrsaker().stream().map(EnumMapper::mapBehandlingÅrsakType).toList();
    }

    private Optional<BrevGrunnlagDto.Behandlingsresultat> finnBehandlingsresultat(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).map(behandlingsresultat -> {
            var medlemskapOpphørsårsak = finnMedlemskapOpphørsÅrsak(behandling).orElse(null);
            var medlemskapFom = medlemTjeneste.hentMedlemFomDato(behandling.getId()).orElse(null);
            var behandlingResultatType = mapBehandlingResultatType(behandlingsresultat.getBehandlingResultatType());
            var avslagsårsak = behandlingsresultat.getAvslagsårsak() == null ? null : behandlingsresultat.getAvslagsårsak().getKode();
            var fritekst = finnFritekst(behandling).orElse(null);
            var stp = finnSkjæringstidspunktForBehandling(behandling).map(
                s -> new BrevGrunnlagDto.Behandlingsresultat.Skjæringstidspunkt(s.getUtledetSkjæringstidspunkt(), s.utenMinsterett())).orElse(null);
            var endretDekningsgrad = dekningsgradTjeneste.behandlingHarEndretDekningsgrad(BehandlingReferanse.fra(behandling));
            var opphørsdato = finnOpphørsdato(behandling).orElse(null);
            var konsekvenserForYtelsen = finnKonsekvenserForYtelsen(behandlingsresultat);
            var vilkårTyper = finnVilkårTyper(behandlingsresultat);
            return new BrevGrunnlagDto.Behandlingsresultat(medlemskapOpphørsårsak, medlemskapFom, behandlingResultatType, avslagsårsak, fritekst, stp,
                endretDekningsgrad, opphørsdato, konsekvenserForYtelsen, vilkårTyper);
        });
    }

    private static List<BrevGrunnlagDto.Behandlingsresultat.VilkårType> finnVilkårTyper(Behandlingsresultat behandlingsresultat) {
        return behandlingsresultat.getVilkårResultat().getVilkårTyper().stream().map(EnumMapper::mapVilkårType).toList();
    }

    private List<BrevGrunnlagDto.Behandlingsresultat.KonsekvensForYtelsen> finnKonsekvenserForYtelsen(Behandlingsresultat br) {
        return br.getKonsekvenserForYtelsen().stream().map(BrevGrunnlagTjeneste::mapKonsekvensForYtelsen).toList();
    }

    private static BrevGrunnlagDto.Behandlingsresultat.KonsekvensForYtelsen mapKonsekvensForYtelsen(KonsekvensForYtelsen konsekvensForYtelsen) {
        return switch (konsekvensForYtelsen) {
            case FORELDREPENGER_OPPHØRER -> BrevGrunnlagDto.Behandlingsresultat.KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER;
            case ENDRING_I_BEREGNING -> BrevGrunnlagDto.Behandlingsresultat.KonsekvensForYtelsen.ENDRING_I_BEREGNING;
            case ENDRING_I_UTTAK -> BrevGrunnlagDto.Behandlingsresultat.KonsekvensForYtelsen.ENDRING_I_UTTAK;
            case ENDRING_I_FORDELING_AV_YTELSEN -> BrevGrunnlagDto.Behandlingsresultat.KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN;
            case INGEN_ENDRING -> BrevGrunnlagDto.Behandlingsresultat.KonsekvensForYtelsen.INGEN_ENDRING;
            case UDEFINERT -> BrevGrunnlagDto.Behandlingsresultat.KonsekvensForYtelsen.UDEFINERT;
        };
    }

    private Optional<LocalDate> finnOpphørsdato(Behandling behandling) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            return Optional.empty();
        }
        return uttakTjeneste.hentHvisEksisterer(behandling.getId()).flatMap(Uttak::opphørsdato);
    }

    private Optional<Skjæringstidspunkt> finnSkjæringstidspunktForBehandling(Behandling behandling) {
        if (!behandling.erYtelseBehandling()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<BrevGrunnlagDto.Behandlingsresultat.Fritekst> finnFritekst(Behandling behandling) {
        return behandlingDokumentRepository.hentHvisEksisterer(behandling.getId())
            .filter(BehandlingDokumentEntitet::harFritekst)
            .map(behandlingDokument -> {
                var overskrift = behandlingDokument.getOverstyrtBrevOverskrift();
                var brødtekst = Optional.ofNullable(behandlingDokument.getOverstyrtBrevFritekstHtml())
                    .orElse(behandlingDokument.getOverstyrtBrevFritekst());
                var avslagsarsakFritekst = behandlingDokument.getVedtakFritekst();
                return new BrevGrunnlagDto.Behandlingsresultat.Fritekst(overskrift, brødtekst, avslagsarsakFritekst);
            });
    }

    private Optional<Rettigheter> utledRettigheter(Behandling behandling) {
        if (behandling.getFagsakYtelseType() != FagsakYtelseType.FORELDREPENGER || ytelseFordelingTjeneste.hentAggregatHvisEksisterer(
            behandling.getId()).isEmpty()) {
            return Optional.empty();
        }
        var opprinnelig = opprinneligRettighetstype(behandling);
        var gjeldende = gjeldendeRettighetstype(behandling);

        var eøsUttak = utledEøsUttak(behandling);
        return Optional.of(new Rettigheter(mapRettighetstype(opprinnelig), mapRettighetstype(gjeldende), eøsUttak.orElse(null)));
    }

    private Optional<Rettigheter.EøsUttak> utledEøsUttak(Behandling behandling) {
        return eøsUttakRepository.hentGrunnlag(behandling.getId()).flatMap(eøsUttak -> {
            var fom = eøsUttak.getFom();
            if (fom.isEmpty()) {
                return Optional.empty();
            }
            var tom = eøsUttak.getTom().orElseThrow();
            var forbruktFellesperiode = eøsUttak.getPerioder()
                .stream()
                .filter(p -> p.getTrekkonto() == UttakPeriodeType.FELLESPERIODE)
                .map(EøsUttaksperiodeEntitet::getTrekkdager)
                .reduce(Trekkdager::add)
                .orElse(Trekkdager.ZERO);
            var maksdagerFellesperiode = utregnetStønadskontoTjeneste.gjeldendeKontoutregning(BehandlingReferanse.fra(behandling))
                .get(StønadskontoType.FELLESPERIODE);
            var fellesperiodeINorge = new Trekkdager(maksdagerFellesperiode == null ? 0 : maksdagerFellesperiode).subtract(forbruktFellesperiode)
                .decimalValue()
                .setScale(0, RoundingMode.UP)
                .intValue();
            var forbruktFellesperiodeInt = forbruktFellesperiode.decimalValue().setScale(0, RoundingMode.DOWN).intValue();
            return Optional.of(new Rettigheter.EøsUttak(fom.get(), tom, forbruktFellesperiodeInt, Math.max(fellesperiodeINorge, 0)));
        });
    }

    private Rettighetstype opprinneligRettighetstype(Behandling behandling) {
        if (behandling.erRevurdering()) {
            var originalBehandlingId = behandling.getOriginalBehandlingId().orElseThrow();
            return gjeldendeRettighetstype(behandlingRepository.hentBehandling(originalBehandlingId));
        }
        var yfa = ytelseFordelingTjeneste.hentAggregat(behandling.getId());
        return yfa.getOppgittRettighet().rettighetstype(behandling.getRelasjonsRolleType());
    }

    private Rettighetstype gjeldendeRettighetstype(Behandling behandling) {
        var stønadskontoberegning = utregnetStønadskontoTjeneste.gjeldendeKontoutregning(BehandlingReferanse.fra(behandling));
        var yfa = ytelseFordelingTjeneste.hentAggregat(behandling.getId());
        var relasjonsRolleType = behandling.getRelasjonsRolleType();
        if (stønadskontoberegning.keySet().stream().anyMatch(stønadskontoType -> stønadskontoType.equals(StønadskontoType.FORELDREPENGER))) {
            if (yfa.robustHarAleneomsorg(relasjonsRolleType)) {
                return Rettighetstype.ALENEOMSORG;
            }
            if (relasjonsRolleType.erFarEllerMedMor()) {
                if (yfa.morMottarUføretrygd(uføretrygdRepository.hentGrunnlag(behandling.getId()).orElse(null))) {
                    return Rettighetstype.BARE_FAR_RETT_MOR_UFØR;
                }
                return Rettighetstype.BARE_FAR_RETT;
            }
            return Rettighetstype.BARE_MOR_RETT;
        }
        return yfa.avklartAnnenForelderHarRettEØS() ? Rettighetstype.BEGGE_RETT_EØS : Rettighetstype.BEGGE_RETT;
    }

    private Optional<String> finnMedlemskapOpphørsÅrsak(Behandling behandling) {
        if (behandling.getFagsakYtelseType() == FagsakYtelseType.ENGANGSTØNAD) {
            return Optional.empty();
        }
        return uttakTjeneste.hentHvisEksisterer(behandling.getId())
            .filter(Uttak::harAvslagPgaMedlemskap)
            .flatMap(u -> medlemTjeneste.hentAvslagsårsak(behandling.getId()))
            .map(Avslagsårsak::getKode);
    }

    private BrevGrunnlagDto.OriginalBehandling finnOriginalBehandling(Long behandlingId) {
        var familieHendelse = finnFamilieHendelse(behandlingId).orElseThrow();
        var behandlingsresultatType = mapBehandlingResultatType(behandlingsresultatRepository.hent(behandlingId).getBehandlingResultatType());
        var førsteDagMedUtbetaltForeldrepenger = foreldrepengerUttakTjeneste.hentHvisEksisterer(behandlingId)
            .map(ForeldrepengerUttak::getGjeldendePerioder)
            .orElse(List.of())
            .stream()
            .filter(ForeldrepengerUttakPeriode::harUtbetaling)
            .map(ForeldrepengerUttakPeriode::getFom)
            .min(LocalDate::compareTo)
            .orElse(null); //opphørsbrev søker død
        return new BrevGrunnlagDto.OriginalBehandling(familieHendelse, behandlingsresultatType, førsteDagMedUtbetaltForeldrepenger);
    }

    private Optional<FamilieHendelse> finnFamilieHendelse(Long behandlingId) {
        return familieHendelseTjeneste.finnAggregat(behandlingId).map(fh -> {
            var barn = fh.getGjeldendeBarna().stream().map(b -> new Barn(b.getFødselsdato(), b.getDødsdato().orElse(null))).toList();
            var termindato = fh.getGjeldendeTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato).orElse(null);
            var antallBarn = fh.getGjeldendeAntallBarn();
            return new FamilieHendelse(barn, termindato, antallBarn,
                fh.getGjeldendeAdopsjon().map(AdopsjonEntitet::getOmsorgsovertakelseDato).orElse(null));
        });
    }

    private BrevGrunnlagDto.Språkkode finnSpråkkode(Behandling behandling) {
        var språkkode = søknadRepository.hentSøknadHvisEksisterer(behandling.getId())
            .map(SøknadEntitet::getSpråkkode)
            .orElseGet(() -> behandling.getFagsak().getNavBruker().getSpråkkode());
        return mapSpråkkode(språkkode);
    }
}
