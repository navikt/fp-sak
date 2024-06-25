package no.nav.foreldrepenger.datavarehus.v2;

import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.AnnenForelder;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.Beregning;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.Builder;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.FamilieHendelse;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.ForeldrepengerRettigheter;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.HendelseType;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.LovVersjon;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.LovVersjon.FORELDREPENGER_MINSTERETT_2022_08_02;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.LovVersjon.FORELDREPENGER_MINSTERETT_2024_08_02;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.LovVersjon.FORELDREPENGER_UTJEVNE80_2024_07_01;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.RettighetType;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.UtlandsTilsnitt;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.VedtakResultat;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.YtelseType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.datavarehus.domene.VilkårIkkeOppfylt;
import no.nav.foreldrepenger.datavarehus.tjeneste.BehandlingVedtakDvhMapper;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.UtregnetStønadskontoTjeneste;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Stønadskontotype;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.saldo.SaldoUtregning;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@ApplicationScoped
public class StønadsstatistikkTjeneste {

    private static final Period INTERVALL_SAMME_BARN = Period.ofWeeks(6);
    private static final LocalDateTime VEDTAK_MED_TIDSPUNKT = LocalDateTime.of(2019, 6, 27, 11, 45,0);
    private static final LocalDateTime SVP_ALLTID_100_PROSENT = LocalDate.of(2023, 3, 24).atStartOfDay();

    private BehandlingRepository behandlingRepository;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private FagsakEgenskapRepository fagsakEgenskapRepository;
    private FagsakRepository fagsakRepository;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;
    private LegacyESBeregningRepository legacyESBeregningRepository;
    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private SvangerskapspengerUttakResultatRepository svangerskapspengerUttakResultatRepository;
    private SøknadRepository søknadRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private UtregnetStønadskontoTjeneste utregnetStønadskontoTjeneste;

    @Inject
    public StønadsstatistikkTjeneste(BehandlingRepository behandlingRepository,
                                     FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                     BehandlingVedtakRepository behandlingVedtakRepository,
                                     FagsakEgenskapRepository fagsakEgenskapRepository,
                                     FagsakRepository fagsakRepository,
                                     FamilieHendelseTjeneste familieHendelseTjeneste,
                                     PersonopplysningTjeneste personopplysningTjeneste,
                                     ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste,
                                     BeregningsresultatRepository beregningsresultatRepository,
                                     YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                     UttakInputTjeneste uttakInputTjeneste,
                                     StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste,
                                     LegacyESBeregningRepository legacyESBeregningRepository,
                                     HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                     InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                     SvangerskapspengerUttakResultatRepository svangerskapspengerUttakResultatRepository,
                                     SøknadRepository søknadRepository,
                                     MottatteDokumentRepository mottatteDokumentRepository) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
        this.fagsakRepository = fagsakRepository;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.foreldrepengerUttakTjeneste = foreldrepengerUttakTjeneste;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.stønadskontoSaldoTjeneste = stønadskontoSaldoTjeneste;
        this.legacyESBeregningRepository = legacyESBeregningRepository;
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.svangerskapspengerUttakResultatRepository = svangerskapspengerUttakResultatRepository;
        this.søknadRepository = søknadRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.utregnetStønadskontoTjeneste = new UtregnetStønadskontoTjeneste(fagsakRelasjonTjeneste, foreldrepengerUttakTjeneste);
    }

    StønadsstatistikkTjeneste() {
        //CDI
    }

    public StønadsstatistikkVedtak genererVedtak(BehandlingReferanse behandlingReferanse) {
        var behandlingId = behandlingReferanse.behandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var vedtak = behandlingVedtakRepository.hentForBehandling(behandlingId);
        var stp = behandlingReferanse.getSkjæringstidspunkt();
        var forrigeBehandlingUuid = behandling.getOriginalBehandlingId().map(id -> behandlingRepository.hentBehandling(id)).map(Behandling::getUuid);
        var utlandMarkering = fagsakEgenskapRepository.finnFagsakMarkering(behandling.getFagsakId()).orElse(FagsakMarkering.NASJONAL);
        var familiehendelse = familieHendelseTjeneste.finnAggregat(behandlingId)
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon).orElse(null);
        var vedtakstidspunkt = vedtak.getVedtakstidspunkt().isBefore(VEDTAK_MED_TIDSPUNKT) ? vedtak.getOpprettetTidspunkt() : vedtak.getVedtakstidspunkt();

        var fagsak = behandling.getFagsak();
        var ytelseType = mapYtelseType(fagsak.getYtelseType());
        var lovVersjon = utledLovVersjon(stp, ytelseType, vedtakstidspunkt);
        var saksnummer = mapSaksnummer(fagsak.getSaksnummer());
        var søker = mapAktørId(fagsak.getAktørId());
        var søkersRolle = mapBrukerRolle(fagsak.getRelasjonsRolleType());
        var søknadsdato = finnSøknadsdato(behandlingReferanse).orElse(behandling.getOpprettetDato().toLocalDate());

        var builder = new Builder()
            .medLovVersjon(lovVersjon)
            .medSak(saksnummer, fagsak.getId())
            .medSøker(søker)
            .medSøkersRolle(søkersRolle)
            .medYtelseType(ytelseType)
            .medBehandlingUuid(behandlingReferanse.behandlingUuid())
            .medForrigeBehandlingUuid(forrigeBehandlingUuid.orElse(null))
            .medRevurderingÅrsak(utledRevurderingÅrsak(behandling, utlandMarkering))
            .medSøknadsdato(søknadsdato)
            .medSkjæringstidspunkt(stp.getSkjæringstidspunktHvisUtledet().orElse(null))
            .medVedtakstidspunkt(vedtakstidspunkt)
            .medVedtaksresultat(mapVedtaksresultat(vedtak))
            .medVilkårIkkeOppfylt(utledVilkårIkkeOppfylt(vedtak, behandling))
            .medUtlandsTilsnitt(utledUtlandsTilsnitt(utlandMarkering))
            .medAnnenForelder(utledAnnenForelder(behandling, familiehendelse))
            .medFamilieHendelse(mapFamilieHendelse(behandlingReferanse, familiehendelse))
            .medUtbetalingsreferanse(String.valueOf(behandlingReferanse.behandlingId()))
            .medBehandlingId(behandlingId);

        if (FagsakYtelseType.FORELDREPENGER.equals(fagsak.getYtelseType())) {
            var rettigheter = utledRettigheter(behandling, familiehendelse, vedtakstidspunkt);
            rettigheter.ifPresent(f -> {
                var foreldrepengerUttaksperioder = mapForeldrepengerUttaksperioder(behandling, f.rettighetType());
                builder.medUttakssperioder(foreldrepengerUttaksperioder).medForeldrepengerRettigheter(f);
            });
        }
        if (FagsakYtelseType.ENGANGSTØNAD.equals(fagsak.getYtelseType())) {
            builder.medEngangsstønadInnvilget(utledTilkjentEngangsstønad(behandlingId));
        } else {
            var utbetalingssperioder = mapUtbetalingssperioder(behandling);
            builder.medUtbetalingssperioder(utbetalingssperioder).medBeregning(utledBeregning(behandling));
        }
        return builder.build();
    }

    private Optional<LocalDate> finnSøknadsdato(BehandlingReferanse behandlingReferanse) {
        return Optional.ofNullable(søknadRepository.hentSøknad(behandlingReferanse.behandlingId()))
            .map(SøknadEntitet::getSøknadsdato)
            .or(() -> mottatteDokumentRepository.hentMottatteDokument(behandlingReferanse.behandlingId()).stream()
                .filter(MottattDokument::erSøknadsDokument)
                .map(MottattDokument::getMottattTidspunkt)
                .map(LocalDateTime::toLocalDate)
                .min(Comparator.naturalOrder()))
            .or(() -> mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandlingReferanse.fagsakId()).stream()
                .filter(MottattDokument::erSøknadsDokument)
                .map(MottattDokument::getMottattTidspunkt)
                .map(LocalDateTime::toLocalDate)
                .min(Comparator.naturalOrder()));
    }

    private StønadsstatistikkVedtak.Saksrolle mapBrukerRolle(RelasjonsRolleType relasjonsRolleType) {
        return switch (relasjonsRolleType) {
            case FARA -> StønadsstatistikkVedtak.Saksrolle.FAR;
            case MORA -> StønadsstatistikkVedtak.Saksrolle.MOR;
            case MEDMOR -> StønadsstatistikkVedtak.Saksrolle.MEDMOR;
            case UDEFINERT -> StønadsstatistikkVedtak.Saksrolle.UKJENT;
            case EKTE, REGISTRERT_PARTNER, BARN, ANNEN_PART_FRA_SØKNAD -> throw new IllegalStateException("Unexpected value: " + relasjonsRolleType.getKode());
        };
    }

    private Beregning utledBeregning(Behandling behandling) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            return null;
        }
        var beregningsgrunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetForBehandling(behandling.getId()).orElse(null);
        if (beregningsgrunnlag == null) {
            return null;
        }
        var iayGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(behandling.getId()).orElse(null);
        return StønadsstatistikkBeregningMapper.mapBeregning(behandling, beregningsgrunnlag, iayGrunnlag);
    }

    private Long utledTilkjentEngangsstønad(Long behandlingId) {
        return legacyESBeregningRepository.getSisteBeregning(behandlingId).map(LegacyESBeregning::getBeregnetTilkjentYtelse).orElse(null);
    }

    private List<StønadsstatistikkUtbetalingPeriode> mapUtbetalingssperioder(Behandling behandling) {
        var perioder = beregningsresultatRepository.hentUtbetBeregningsresultat(behandling.getId())
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder)
            .orElse(List.of());

        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(behandling.getFagsakYtelseType()) && trengerSvpLegacyUtregning(perioder)) {
            var uttak = svangerskapspengerUttakResultatRepository.hentHvisEksisterer(behandling.getId())
                .orElseThrow();
            return StønadsstatistikkUtbetalingSVPLegacyMapper.mapTilkjent(perioder, uttak);
        }
        return StønadsstatistikkUtbetalingPeriodeMapper.mapTilkjent(perioder);
    }

    private boolean trengerSvpLegacyUtregning(List<BeregningsresultatPeriode> perioder) {
        return perioder.stream().anyMatch(periode -> SVP_ALLTID_100_PROSENT.isAfter(periode.getOpprettetTidspunkt()));
    }

    private List<StønadsstatistikkUttakPeriode> mapForeldrepengerUttaksperioder(Behandling behandling, RettighetType rettighetType) {
        var logContext = String.format("saksnummer %s behandling %s", behandling.getFagsak().getSaksnummer().getVerdi(), behandling.getUuid().toString());
        return foreldrepengerUttakTjeneste.hentUttakHvisEksisterer(behandling.getId(), true)
            .map(u -> StønadsstatistikkUttakPeriodeMapper.mapUttak(behandling.getRelasjonsRolleType(), rettighetType, u.getGjeldendePerioder(), logContext))
            .orElse(List.of());
    }

    private FamilieHendelse mapFamilieHendelse(BehandlingReferanse behandling, FamilieHendelseEntitet familiehendelse) {
        if (familiehendelse == null) {
            return null;
        }
        var termindato = familiehendelse.getTermindato().orElse(null);
        var adopsjonsdato = familiehendelse.getGjelderAdopsjon() ? familiehendelse.getAdopsjon()
            .map(AdopsjonEntitet::getOmsorgsovertakelseDato)
            .orElse(null) : null;
        var antallBarn = familiehendelse.getAntallBarn();
        var identifiserteBarn = personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(behandling)
            .map(PersonopplysningerAggregat::getBarna)
            .orElse(List.of());
        var barn = identifiserteBarn.size() == familiehendelse.getBarna().size() ? hentIdentifiserteBarn(identifiserteBarn) : hentBarn(
            familiehendelse.getBarna());
        var hendelseType = utledFamilieHendelseType(familiehendelse);

        return new FamilieHendelse(termindato, adopsjonsdato, antallBarn, barn, hendelseType);
    }

    private static HendelseType utledFamilieHendelseType(FamilieHendelseEntitet familiehendelse) {
        if (familiehendelse.getGjelderFødsel()) {
            return HendelseType.FØDSEL;
        }
        if (FamilieHendelseType.OMSORG.equals(familiehendelse.getType())) {
            return HendelseType.OMSORGSOVERTAKELSE;
        }
        var stebarnsAdopsjon = familiehendelse.getAdopsjon().filter(a -> Objects.equals(a.getErEktefellesBarn(), Boolean.TRUE)).isPresent();
        return stebarnsAdopsjon ? HendelseType.STEBARNSADOPSJON : HendelseType.ADOPSJON;
    }

    private static List<FamilieHendelse.Barn> hentBarn(List<UidentifisertBarn> barna) {
        return barna.stream().map(b -> new FamilieHendelse.Barn(null, b.getFødselsdato(), b.getDødsdato().orElse(null))).toList();
    }

    private static List<FamilieHendelse.Barn> hentIdentifiserteBarn(List<PersonopplysningEntitet> identifiserteBarn) {
        return identifiserteBarn.stream()
            .map(b -> new FamilieHendelse.Barn(mapAktørId(b.getAktørId()), b.getFødselsdato(), b.getDødsdato()))
            .toList();
    }

    private AnnenForelder utledAnnenForelder(Behandling behandling, FamilieHendelseEntitet familiehendelse) {
        var fagsakRelasjon = fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(behandling.getFagsak());
        return fagsakRelasjon.flatMap(fr -> fr.getRelatertFagsak(behandling.getFagsak()))
            .or(() -> personopplysningTjeneste.hentOppgittAnnenPartAktørId(behandling.getId())
                .flatMap(apaid -> finnEngangsstønadSak(apaid, familiehendelse)))
            .map(relatert -> new AnnenForelder(mapAktørId(relatert.getAktørId()), mapSaksnummer(relatert.getSaksnummer()),
                mapYtelseType(relatert.getYtelseType()), mapBrukerRolle(relatert.getRelasjonsRolleType())))
            // Vi har ikke annenpart med sak. Lag AnnenForelder med oppgitt aktør id, uten saksinfo
            .orElseGet(() -> personopplysningTjeneste.hentOppgittAnnenPartAktørId(behandling.getId())
                .map(a -> new AnnenForelder(mapAktørId(a), null, null, null))
                .orElse(null));
    }

    private Optional<Fagsak> finnEngangsstønadSak(AktørId aktørId, FamilieHendelseEntitet familieHendelse) {
        return fagsakRepository.hentForBruker(aktørId)
            .stream()
            .filter(f -> FagsakYtelseType.ENGANGSTØNAD.equals(f.getYtelseType()))
            .filter(f -> behandlingVedtakRepository.hentGjeldendeVedtak(f).isPresent())
            .flatMap(f -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(f.getId()).stream())
            .filter(b -> matcherFamiliehendelseMedSak(familieHendelse, b))
            .findFirst()
            .map(Behandling::getFagsak);
    }

    private boolean matcherFamiliehendelseMedSak(FamilieHendelseEntitet familieHendelse, Behandling behandling) {
        if (familieHendelse == null || familieHendelse.getSkjæringstidspunkt() == null) {
            return false;
        }
        var fhDato = familieHendelse.getSkjæringstidspunkt();
        var egetIntervall = new LocalDateInterval(fhDato.minus(INTERVALL_SAMME_BARN), fhDato.plus(INTERVALL_SAMME_BARN));
        var annenpartIntervall = familieHendelseTjeneste.finnAggregat(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getSkjæringstidspunkt)
            .map(d -> new LocalDateInterval(d.minus(INTERVALL_SAMME_BARN), d.plus(INTERVALL_SAMME_BARN)));

        return annenpartIntervall.filter(i -> i.overlaps(egetIntervall)).isPresent();
    }

    private static UtlandsTilsnitt utledUtlandsTilsnitt(FagsakMarkering fagsakMarkering) {
        return switch (fagsakMarkering) {
            case NASJONAL, SAMMENSATT_KONTROLL, DØD_DØDFØDSEL, SELVSTENDIG_NÆRING, PRAKSIS_UTSETTELSE -> UtlandsTilsnitt.NASJONAL;
            case EØS_BOSATT_NORGE -> UtlandsTilsnitt.EØS_BOSATT_NORGE;
            case BOSATT_UTLAND -> UtlandsTilsnitt.BOSATT_UTLAND;
        };
    }

    private static VilkårIkkeOppfylt utledVilkårIkkeOppfylt(BehandlingVedtak vedtak, Behandling behandling) {
        var vilkårIkkeOppfylt = Optional.ofNullable(vedtak.getBehandlingsresultat().getVilkårResultat())
            .map(VilkårResultat::getVilkårene)
            .orElse(List.of())
            .stream()
            .filter(v -> VilkårUtfallType.IKKE_OPPFYLT.equals(v.getGjeldendeVilkårUtfall()))
            .map(Vilkår::getVilkårType)
            .collect(Collectors.toSet());
        return BehandlingVedtakDvhMapper.mapVilkårIkkeOppfylt(vedtak.getVedtakResultatType(), behandling.getFagsakYtelseType(), vilkårIkkeOppfylt);
    }

    private static VedtakResultat mapVedtaksresultat(BehandlingVedtak vedtak) {
        return switch (vedtak.getVedtakResultatType()) {

            case INNVILGET -> VedtakResultat.INNVILGET;
            case AVSLAG -> VedtakResultat.AVSLAG;
            case OPPHØR -> VedtakResultat.OPPHØR;
            case VEDTAK_I_KLAGEBEHANDLING, VEDTAK_I_ANKEBEHANDLING, VEDTAK_I_INNSYNBEHANDLING, UDEFINERT ->
                throw new IllegalStateException("Unexpected value: " + vedtak.getVedtakResultatType());
        };
    }

    private static LovVersjon utledLovVersjon(Skjæringstidspunkt stp, YtelseType ytelseType, LocalDateTime vedtakstidspunkt) {
        return switch (ytelseType) {
            case FORELDREPENGER -> utledLovVersjonFp(stp, vedtakstidspunkt);
            case SVANGERSKAPSPENGER -> LovVersjon.SVANGERSKAPSPENGER_2019_01_01;
            case ENGANGSSTØNAD -> LovVersjon.ENGANGSSTØNAD_2019_01_01;
        };
    }

    private static LovVersjon utledLovVersjonFp(Skjæringstidspunkt stp, LocalDateTime vedtakstidspunkt) {
        if (stp == null || stp.getSkjæringstidspunktHvisUtledet().isEmpty()) {
            var vedtaksdatoPlus1 = vedtakstidspunkt.toLocalDate().plusDays(1);
            return Arrays.stream(LovVersjon.values())
                .filter(v -> YtelseType.FORELDREPENGER.equals(v.getYtelseType()))
                .filter(v -> vedtaksdatoPlus1.isAfter(v.getDatoFom()))
                .max(Comparator.comparing(LovVersjon::getDatoFom)).orElseThrow();
        }
        if (stp.utenMinsterett()) {
            return stp.kreverSammenhengendeUttak() ? LovVersjon.FORELDREPENGER_2019_01_01 : LovVersjon.FORELDREPENGER_FRI_2021_10_01;
        }
        var familieHendelseDato = stp.getFamiliehendelsedato();
        if (LocalDate.now().isBefore(FORELDREPENGER_UTJEVNE80_2024_07_01.getDatoFom())) {
            return FORELDREPENGER_MINSTERETT_2022_08_02;
        } else if (familieHendelseDato != null && familieHendelseDato.isBefore(FORELDREPENGER_UTJEVNE80_2024_07_01.getDatoFom())) {
            return FORELDREPENGER_MINSTERETT_2022_08_02;
        } if (LocalDate.now().isBefore(FORELDREPENGER_MINSTERETT_2024_08_02.getDatoFom())) {
            return FORELDREPENGER_UTJEVNE80_2024_07_01;
        } else if (familieHendelseDato != null && familieHendelseDato.isBefore(FORELDREPENGER_MINSTERETT_2024_08_02.getDatoFom())) {
            return FORELDREPENGER_UTJEVNE80_2024_07_01;
        } else {
            return FORELDREPENGER_MINSTERETT_2024_08_02;
        }

    }

    private Optional<ForeldrepengerRettigheter> utledRettigheter(Behandling behandling,
                                                                 FamilieHendelseEntitet familiehendelse,
                                                                 LocalDateTime vedtakstidspunkt) {
        if (familiehendelse == null) { //Avslått papirsøknad der fagsakrel er opprettet i senere behandlinger skaper trøbbel
            return Optional.empty();
        }
        var fr = fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(behandling.getFagsakId(), vedtakstidspunkt);
        return fr.map(fagsakRelasjon -> {
            var gjeldendeStønadskontoberegning = utregnetStønadskontoTjeneste.gjeldendeKontoutregning(behandling.getId(), fagsakRelasjon);
            var uttakInput = uttakInputTjeneste.lagInput(behandling);
            var saldoUtregning = stønadskontoSaldoTjeneste.finnSaldoUtregning(uttakInput);
            var konti = gjeldendeStønadskontoberegning.entrySet().stream()
                .filter(sk -> sk.getKey().erStønadsdager())
                .map(k -> map(k.getKey(), k.getValue(), saldoUtregning))
                .collect(Collectors.toSet());


            var yfa = ytelseFordelingTjeneste.hentAggregat(behandling.getId());
            var rettighetType = utledRettighetType(behandling.getRelasjonsRolleType(), yfa, konti);
            var ekstradager = new HashSet<ForeldrepengerRettigheter.Stønadsutvidelse>();
            var flerbarnsdager = gjeldendeStønadskontoberegning.getOrDefault(StønadskontoType.TILLEGG_FLERBARN, 0);
            if (flerbarnsdager > 0) {
                ekstradager.add(new ForeldrepengerRettigheter.Stønadsutvidelse(StønadsstatistikkVedtak.StønadUtvidetType.FLERBARNSDAGER, new ForeldrepengerRettigheter.Trekkdager(flerbarnsdager)));
            }
            var prematurdager = gjeldendeStønadskontoberegning.getOrDefault(StønadskontoType.TILLEGG_PREMATUR, 0);
            if (prematurdager > 0) {
                ekstradager.add(new ForeldrepengerRettigheter.Stønadsutvidelse(StønadsstatistikkVedtak.StønadUtvidetType.PREMATURDAGER, new ForeldrepengerRettigheter.Trekkdager(prematurdager)));
            }

            return new ForeldrepengerRettigheter(yfa.getGjeldendeDekningsgrad().getVerdi(), rettighetType, konti, ekstradager);
        });
    }

    private static RettighetType utledRettighetType(RelasjonsRolleType relasjonsRolleType, YtelseFordelingAggregat yfa, Set<ForeldrepengerRettigheter.Stønadskonto> konti) {
        if (konti.stream().anyMatch(k -> k.type().equals(StønadsstatistikkVedtak.StønadskontoType.FORELDREPENGER))) {
            return yfa.robustHarAleneomsorg(relasjonsRolleType) ? RettighetType.ALENEOMSORG : RettighetType.BARE_SØKER_RETT;
        }
        return yfa.avklartAnnenForelderHarRettEØS() ? RettighetType.BEGGE_RETT_EØS : RettighetType.BEGGE_RETT;
    }

    private static ForeldrepengerRettigheter.Stønadskonto map(StønadskontoType stønadskonto, Integer maxDager, SaldoUtregning saldoUtregning) {
        var minsterett = StønadskontoType.FORELDREPENGER.equals(stønadskonto) ? saldoUtregning.getMaxDagerMinsterett()
            .add(saldoUtregning.getMaxDagerUtenAktivitetskrav())
            .rundOpp() : 0;
        var stønadskontoType = map(stønadskonto);
        var maksdager = map(maxDager);
        var restdager = saldoUtregning.saldoITrekkdager(switch (stønadskonto) {
            case FELLESPERIODE -> Stønadskontotype.FELLESPERIODE;
            case MØDREKVOTE -> Stønadskontotype.MØDREKVOTE;
            case FEDREKVOTE -> Stønadskontotype.FEDREKVOTE;
            case FORELDREPENGER -> Stønadskontotype.FORELDREPENGER;
            case FORELDREPENGER_FØR_FØDSEL -> Stønadskontotype.FORELDREPENGER_FØR_FØDSEL;
            default -> throw new IllegalStateException("Ukjent " + stønadskonto);
        });
        //Kan være trukket i minus
        var restdagerDto = new ForeldrepengerRettigheter.Trekkdager(restdager.mindreEnn0() ? BigDecimal.ZERO : restdager.decimalValue());
        return new ForeldrepengerRettigheter.Stønadskonto(stønadskontoType, maksdager, restdagerDto,
            new ForeldrepengerRettigheter.Trekkdager(minsterett));
    }

    private static ForeldrepengerRettigheter.Trekkdager map(int maxDager) {
        return new ForeldrepengerRettigheter.Trekkdager(maxDager);
    }

    private static StønadsstatistikkVedtak.StønadskontoType map(StønadskontoType stønadskontoType) {
        return switch (stønadskontoType) {
            case FELLESPERIODE -> StønadsstatistikkVedtak.StønadskontoType.FELLESPERIODE;
            case MØDREKVOTE -> StønadsstatistikkVedtak.StønadskontoType.MØDREKVOTE;
            case FEDREKVOTE -> StønadsstatistikkVedtak.StønadskontoType.FEDREKVOTE;
            case FORELDREPENGER -> StønadsstatistikkVedtak.StønadskontoType.FORELDREPENGER;
            case FORELDREPENGER_FØR_FØDSEL -> StønadsstatistikkVedtak.StønadskontoType.FORELDREPENGER_FØR_FØDSEL;
            default -> throw new IllegalStateException("Unexpected value: " + stønadskontoType);
        };
    }

    private static StønadsstatistikkVedtak.AktørId mapAktørId(AktørId aktørId) {
        return new StønadsstatistikkVedtak.AktørId(aktørId.getId());
    }

    private static YtelseType mapYtelseType(FagsakYtelseType ytelseType) {
        return switch (ytelseType) {
            case ENGANGSTØNAD -> YtelseType.ENGANGSSTØNAD;
            case FORELDREPENGER -> YtelseType.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> YtelseType.SVANGERSKAPSPENGER;
            case UDEFINERT -> throw new IllegalStateException("Unexpected value: " + ytelseType);
        };
    }

    private static StønadsstatistikkVedtak.Saksnummer mapSaksnummer(Saksnummer saksnummer) {
        return new StønadsstatistikkVedtak.Saksnummer(saksnummer.getVerdi());
    }

    private static StønadsstatistikkVedtak.RevurderingÅrsak utledRevurderingÅrsak(Behandling behandling, FagsakMarkering fagsakMarkering) {
        if (!behandling.erRevurdering()) {
            return null;
        }
        // MIdlertidig
        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.FEIL_PRAKSIS_UTSETTELSE) || FagsakMarkering.PRAKSIS_UTSETTELSE.equals(fagsakMarkering)) {
            return StønadsstatistikkVedtak.RevurderingÅrsak.PRAKSIS_UTSETTELSE;
        }
        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)) {
            return StønadsstatistikkVedtak.RevurderingÅrsak.SØKNAD;
        }
        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_UTSATT_START)) {
            return StønadsstatistikkVedtak.RevurderingÅrsak.SØKNAD;
        }
        if (behandling.harNoenBehandlingÅrsaker(BehandlingÅrsakType.årsakerEtterKlageBehandling())) {
            return StønadsstatistikkVedtak.RevurderingÅrsak.KLAGE;
        }
        if (behandling.harNoenBehandlingÅrsaker(BehandlingÅrsakType.årsakerForEtterkontroll())) {
            return StønadsstatistikkVedtak.RevurderingÅrsak.ETTERKONTROLL;
        }
        if (behandling.erManueltOpprettet() && behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FORDELING)) {
            return StønadsstatistikkVedtak.RevurderingÅrsak.UTTAKMANUELL;
        }
        if (behandling.erManueltOpprettet()) {
            return StønadsstatistikkVedtak.RevurderingÅrsak.MANUELL;
        }
        if (behandling.harNoenBehandlingÅrsaker(BehandlingÅrsakType.årsakerForRelatertVedtak())) {
            return StønadsstatistikkVedtak.RevurderingÅrsak.ANNENFORELDER;
        }
        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN)) {
            return StønadsstatistikkVedtak.RevurderingÅrsak.NYSAK;
        }
        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_VEDTAK_PLEIEPENGER)) {
            return StønadsstatistikkVedtak.RevurderingÅrsak.PLEIEPENGER;
        }
        if (behandling.harNoenBehandlingÅrsaker(BehandlingÅrsakType.årsakerRelatertTilPdl())) {
            return StønadsstatistikkVedtak.RevurderingÅrsak.FOLKEREGISTER;
        }
        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING)) {
            return StønadsstatistikkVedtak.RevurderingÅrsak.INNTEKTSMELDING;
        }
        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_SATS_REGULERING)) {
            return StønadsstatistikkVedtak.RevurderingÅrsak.REGULERING;
        }
        return StønadsstatistikkVedtak.RevurderingÅrsak.MANUELL;
    }

}
