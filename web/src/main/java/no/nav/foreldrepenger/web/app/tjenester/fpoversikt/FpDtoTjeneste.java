package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import static no.nav.foreldrepenger.web.app.tjenester.fpoversikt.DtoTjenesteFelles.statusForSøknad;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.eøs.EøsUttakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.eøs.EøsUttaksperiodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.GraderingAvslagÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Virkedager;

@ApplicationScoped
public class FpDtoTjeneste {

    private static final String UNEXPECTED_VALUE = "Unexpected value: ";
    private BehandlingRepository behandlingRepository;
    private DekningsgradTjeneste dekningsgradTjeneste;
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private UføretrygdRepository uføretrygdRepository;
    private DtoTjenesteFelles felles;
    private SøknadRepository søknadRepository;
    private EøsUttakRepository eøsUttakRepository;
    private BeregningOversiktDtoTjeneste beregningOversiktDtoTjeneste;

    @Inject
    public FpDtoTjeneste(BehandlingRepositoryProvider repositoryProvider,
                         ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste,
                         PersonopplysningTjeneste personopplysningTjeneste,
                         YtelseFordelingTjeneste ytelseFordelingTjeneste,
                         UføretrygdRepository uføretrygdRepository,
                         DtoTjenesteFelles felles,
                         DekningsgradTjeneste dekningsgradTjeneste, EøsUttakRepository eøsUttakRepository,
                         BeregningOversiktDtoTjeneste beregningOversiktDtoTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.dekningsgradTjeneste = dekningsgradTjeneste;
        this.foreldrepengerUttakTjeneste = foreldrepengerUttakTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.uføretrygdRepository = uføretrygdRepository;
        this.felles = felles;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.eøsUttakRepository = eøsUttakRepository;
        this.beregningOversiktDtoTjeneste = beregningOversiktDtoTjeneste;
    }

    FpDtoTjeneste() {
        //CDI
    }

    public Sak hentSak(Fagsak fagsak) {
        if (fagsak.getYtelseType() != FagsakYtelseType.FORELDREPENGER) {
            throw new IllegalArgumentException("Forventer bare fp fagsaker");
        }
        var saksnummer = fagsak.getSaksnummer().getVerdi();
        var aktørId = fagsak.getAktørId().getId();
        var gjeldendeVedtak = felles.finnGjeldendeVedtak(fagsak);
        var åpenYtelseBehandling = felles.hentÅpenBehandling(fagsak);
        var familieHendelse = felles.finnFamilieHendelse(fagsak, gjeldendeVedtak, åpenYtelseBehandling);
        var erSakAvsluttet = felles.erAvsluttet(fagsak);
        var ikkeHenlagteBehandlinger = felles.finnIkkeHenlagteBehandlinger(fagsak);
        var aksjonspunkt = felles.finnAksjonspunkt(ikkeHenlagteBehandlinger);
        var mottatteSøknader = felles.finnRelevanteSøknadsdokumenter(fagsak);
        var alleVedtak = felles.finnVedtakForFagsak(fagsak);
        var vedtak = finnFpVedtak(alleVedtak);
        var oppgittAnnenPart = oppgittAnnenPart(fagsak).map(AktørId::getId).orElse(null);
        var søknader = finnFpSøknader(åpenYtelseBehandling, mottatteSøknader);
        var gjeldendeBehandling = finnGjeldendeBehandling(fagsak, gjeldendeVedtak, åpenYtelseBehandling);
        var fødteBarn = gjeldendeBehandling.map(this::finnFødteBarn).orElse(Set.of());
        var rettigheter = finnRettigheter(fagsak, gjeldendeVedtak, åpenYtelseBehandling);
        var ønskerJustertUttakVedFødsel = gjeldendeBehandling.flatMap(b -> ytelseFordelingTjeneste.hentAggregatHvisEksisterer(b.getId()))
            .map(yfa -> yfa.getGjeldendeFordeling().ønskerJustertVedFødsel())
            .orElse(false);
        var brukerRolle = finnBrukerRolle(fagsak);
        return new FpSak(saksnummer, aktørId, familieHendelse, erSakAvsluttet, vedtak, oppgittAnnenPart, aksjonspunkt, søknader, brukerRolle,
            fødteBarn, rettigheter.orElse(null), ønskerJustertUttakVedFødsel);
    }

    private Optional<FpSak.Rettigheter> finnRettigheter(Fagsak fagsak, Optional<BehandlingVedtak> gjeldendeVedtak, Optional<Behandling> åpenYtelseBehandling) {
        if (gjeldendeVedtak.isPresent()) {
            return finnGjeldendeRettigheter(gjeldendeVedtak.get().getBehandlingsresultat().getBehandlingId());
        }
        if (åpenYtelseBehandling.isPresent()) {
            return finnOppgitteRettigheter(åpenYtelseBehandling.get().getId());
        }
        var sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakIdReadOnly(fagsak.getId());
        return sisteBehandling.flatMap(b -> finnOppgitteRettigheter(b.getId()));
    }

    private Optional<FpSak.Rettigheter> finnOppgitteRettigheter(Long behandlingId) {
        return ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandlingId).map(ytelseFordelingAggregat -> {
            var oppgittRettighet = ytelseFordelingAggregat.getOppgittRettighet();
            var aleneomsorg = oppgittRettighet.getHarAleneomsorgForBarnet();
            var annenForelderRettEØS = oppgittRettighet.getAnnenForelderRettEØS();
            var morUføretrygd = oppgittRettighet.getMorMottarUføretrygd();
            return new FpSak.Rettigheter(aleneomsorg, morUføretrygd, annenForelderRettEØS);
        });
    }

    private Optional<FpSak.Rettigheter> finnGjeldendeRettigheter(Long behandlingId) {
        return ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandlingId).map(ytelseFordelingAggregat -> {
            var aleneomsorg = ytelseFordelingAggregat.harAleneomsorg();
            var annenForelderRettEØS = ytelseFordelingAggregat.avklartAnnenForelderHarRettEØS();
            var uføretrygdGrunnlagEntitet = uføretrygdRepository.hentGrunnlag(behandlingId);
            var morUføretrygd = ytelseFordelingAggregat.morMottarUføretrygd(uføretrygdGrunnlagEntitet.orElse(null));
            return new FpSak.Rettigheter(aleneomsorg, morUføretrygd, annenForelderRettEØS);
        });
    }

    private Optional<Behandling> finnGjeldendeBehandling(Fagsak fagsak, Optional<BehandlingVedtak> gjeldendeVedtak, Optional<Behandling> åpenYtelseBehandling) {
        final Optional<Behandling> behandling;
        if (gjeldendeVedtak.isPresent()) {
            var behandlingId = gjeldendeVedtak.get().getBehandlingsresultat().getBehandlingId();
            behandling = Optional.of(behandlingRepository.hentBehandling(behandlingId));
        } else if (åpenYtelseBehandling.isPresent()) {
            behandling = åpenYtelseBehandling;
        } else {
            behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakIdReadOnly(fagsak.getId());
        }
        return behandling;
    }

    private Set<String> finnFødteBarn(Behandling behandling) {
        var ref = BehandlingReferanse.fra(behandling);
        return personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(ref)
            .map(pi -> finnFødteBarn(behandling.getRelasjonsRolleType(), behandling.getAktørId(), pi))
            .orElse(Set.of());
    }

    private static Set<String> finnFødteBarn(RelasjonsRolleType relasjonsRolleType, AktørId søker, PersonopplysningerAggregat pi) {
        return pi.getBarna()
            .stream()
            .filter(barn -> riktigRelasjon(søker, pi, barn, relasjonsRolleType))
            .map(barn -> barn.getAktørId().getId())
            .collect(Collectors.toSet());
    }

    private static boolean riktigRelasjon(AktørId søker,
                                          PersonopplysningerAggregat pi,
                                          PersonopplysningEntitet barn,
                                          RelasjonsRolleType relasjonsRolleType) {
        return relasjonsRolleType == RelasjonsRolleType.UDEFINERT || pi.finnRelasjon(barn.getAktørId(), søker)
            .stream()
            .anyMatch(r -> r.getRelasjonsrolle() == relasjonsRolleType);
    }

    private FpSak.BrukerRolle finnBrukerRolle(Fagsak fagsak) {
        return switch (fagsak.getRelasjonsRolleType()) {
            case FARA -> FpSak.BrukerRolle.FAR;
            case MORA -> FpSak.BrukerRolle.MOR;
            case MEDMOR -> FpSak.BrukerRolle.MEDMOR;
            case UDEFINERT -> FpSak.BrukerRolle.UKJENT;
            case EKTE, REGISTRERT_PARTNER, BARN, ANNEN_PART_FRA_SØKNAD -> throw new IllegalStateException(UNEXPECTED_VALUE + fagsak.getRelasjonsRolleType());
        };
    }

    private Set<FpSak.Søknad> finnFpSøknader(Optional<Behandling> åpenYtelseBehandling, List<MottattDokument> mottatteSøknader) {
        return mottatteSøknader.stream().map(md -> {
                var status = statusForSøknad(åpenYtelseBehandling, md.getBehandlingId());
                var perioder = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(md.getBehandlingId()).map(ytelseFordelingAggregat -> {
                    var oppgittFordeling = ytelseFordelingAggregat.getOppgittFordeling();
                    return oppgittFordeling.getPerioder().stream().map(FpDtoTjeneste::tilDto).collect(Collectors.toSet());
                }).orElse(Set.of());
                var behandling = behandlingRepository.hentBehandling(md.getBehandlingId());
                var oppgittDekningsgrad = dekningsgradTjeneste.finnOppgittDekningsgrad(BehandlingReferanse.fra(behandling));
                var morArbeidUtenDok = morArbeidUtenDok(behandling);
                return new FpSak.Søknad(status, md.getMottattTidspunkt(), perioder,
                    oppgittDekningsgrad.map(FpDtoTjeneste::tilDekningsgradDto).orElse(null), morArbeidUtenDok);
            }).filter(s -> !s.perioder().isEmpty()) //Filtrerer ut søknaden som ikke er registert i YF. Feks behandling står i papir punching
            .collect(Collectors.toSet());
    }

    private boolean morArbeidUtenDok(Behandling behandling) {
        if (!behandling.getFagsak().getRelasjonsRolleType().erFarEllerMedMor()) {
            return false;
        }
        var søknad = søknadRepository.hentSøknadFraGrunnlag(behandling.getId())
            .filter(SøknadEntitet::getElektroniskRegistrert);
        if (søknad.isEmpty() || mottattEllerVenterDokumentasjonPåAtMorErIArbeid(søknad.get())) {
            return false;
        }
        var søknadsperioder = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandling.getId())
            .map(yfa -> yfa.getOppgittFordeling().getPerioder())
            .orElse(List.of());
        return søknadsperioder.stream().anyMatch(OppgittPeriodeEntitet::erAktivitetskravMedMorArbeid);
    }

    private boolean mottattEllerVenterDokumentasjonPåAtMorErIArbeid(SøknadEntitet søknad) {
        var vedleggMorsArbeid = søknad.getSøknadVedlegg()
            .stream()
            .filter(v -> Set.of(DokumentTypeId.MOR_ARBEID.getKode(), DokumentTypeId.DOK_MORS_UTDANNING_ARBEID_SYKDOM.getKode())
                .contains(v.getSkjemanummer())) //Sjekker gammelt skjemanummer her for historiske behandlinger
            .toList();
        return !vedleggMorsArbeid.isEmpty();
    }

    private static FpSak.Dekningsgrad tilDekningsgradDto(Dekningsgrad dekningsgrad) {
        return dekningsgrad.isÅtti() ? FpSak.Dekningsgrad.ÅTTI : FpSak.Dekningsgrad.HUNDRE;
    }

    private static FpSak.Søknad.Periode tilDto(OppgittPeriodeEntitet periode) {
        var konto = map(periode.getPeriodeType());
        var utsettelseÅrsak = finnUtsettelseÅrsak(periode.getÅrsak());
        var oppholdÅrsak = finnOppholdÅrsak(periode.getÅrsak());
        var overføringÅrsak = finnOverføringÅrsak(periode.getÅrsak());
        var morsAktivitet = map(periode.getMorsAktivitet());
        var gradering = mapGradering(periode);
        var samtidigUttak = periode.getSamtidigUttaksprosent() == null ? null : periode.getSamtidigUttaksprosent().decimalValue();
        return new FpSak.Søknad.Periode(periode.getFom(), periode.getTom(), konto, utsettelseÅrsak, oppholdÅrsak, overføringÅrsak,
            gradering, samtidigUttak, periode.isFlerbarnsdager(), morsAktivitet);
    }

    private static MorsAktivitet map(no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet morsAktivitet) {
        return morsAktivitet == null ? null : switch (morsAktivitet) {
            case UDEFINERT -> null;
            case ARBEID -> MorsAktivitet.ARBEID;
            case UTDANNING -> MorsAktivitet.UTDANNING;
            case KVALPROG -> MorsAktivitet.KVALPROG;
            case INTROPROG -> MorsAktivitet.INTROPROG;
            case TRENGER_HJELP -> MorsAktivitet.TRENGER_HJELP;
            case INNLAGT -> MorsAktivitet.INNLAGT;
            case ARBEID_OG_UTDANNING -> MorsAktivitet.ARBEID_OG_UTDANNING;
            case UFØRE -> MorsAktivitet.UFØRE;
            case IKKE_OPPGITT -> MorsAktivitet.IKKE_OPPGITT;
        };
    }

    private static Gradering mapGradering(OppgittPeriodeEntitet periode) {
        if (!periode.isGradert()) {
            return null;
        }
        var type = switch (periode.getGraderingAktivitetType()) {
            case ARBEID -> UttakAktivitet.Type.ORDINÆRT_ARBEID;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> UttakAktivitet.Type.SELVSTENDIG_NÆRINGSDRIVENDE;
            case FRILANS -> UttakAktivitet.Type.FRILANS;
        };
        var arbeidsgiver = periode.getArbeidsgiver() == null ? null : new Arbeidsgiver(periode.getArbeidsgiver().getIdentifikator());
        var aktivitet = new UttakAktivitet(type, arbeidsgiver, null);
        return new Gradering(periode.getArbeidsprosent(), aktivitet);
    }

    private static OverføringÅrsak finnOverføringÅrsak(Årsak årsak) {
        if (årsak instanceof no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak overføringÅrsak) {
            return switch (overføringÅrsak) {
                case INSTITUSJONSOPPHOLD_ANNEN_FORELDER -> OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER;
                case SYKDOM_ANNEN_FORELDER -> OverføringÅrsak.SYKDOM_ANNEN_FORELDER;
                case IKKE_RETT_ANNEN_FORELDER -> OverføringÅrsak.IKKE_RETT_ANNEN_FORELDER;
                case ALENEOMSORG -> OverføringÅrsak.ALENEOMSORG;
                case UDEFINERT -> throw new IllegalStateException(UNEXPECTED_VALUE + årsak);
            };
        }
        return null;
    }

    private static OppholdÅrsak finnOppholdÅrsak(Årsak årsak) {
        if (årsak instanceof no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak oppholdÅrsak) {
            return switch (oppholdÅrsak) {
                case MØDREKVOTE_ANNEN_FORELDER -> OppholdÅrsak.MØDREKVOTE_ANNEN_FORELDER;
                case FEDREKVOTE_ANNEN_FORELDER -> OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER;
                case KVOTE_FELLESPERIODE_ANNEN_FORELDER -> OppholdÅrsak.FELLESPERIODE_ANNEN_FORELDER;
                case KVOTE_FORELDREPENGER_ANNEN_FORELDER -> OppholdÅrsak.FORELDREPENGER_ANNEN_FORELDER;
                case UDEFINERT -> throw new IllegalStateException(UNEXPECTED_VALUE + årsak);
            };
        }
        return null;
    }

    private static no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak finnUtsettelseÅrsak(Årsak årsak) {
        if (årsak instanceof no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak utsettelseÅrsak) {
            return switch (utsettelseÅrsak) {
                case ARBEID -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.ARBEID;
                case FERIE -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.LOVBESTEMT_FERIE;
                case SYKDOM -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.SØKER_SYKDOM;
                case INSTITUSJON_SØKER -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.SØKER_INNLAGT;
                case INSTITUSJON_BARN -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.BARN_INNLAGT;
                case HV_OVELSE -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.HV_ØVELSE;
                case NAV_TILTAK -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.NAV_TILTAK;
                case FRI -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.FRI;
                case UDEFINERT -> throw new IllegalStateException(UNEXPECTED_VALUE + årsak);
            };
        }
        return null;
    }

    private Optional<AktørId> oppgittAnnenPart(Fagsak fagsak) {
        var førstegangsbehandling = behandlingRepository.finnSisteIkkeHenlagteBehandlingavAvBehandlingTypeFor(fagsak.getId(),
                BehandlingType.FØRSTEGANGSSØKNAD)
            .orElseGet(() -> behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsak.getId())
                .stream()
                .max(Comparator.comparing(Behandling::getOpprettetDato))
                .orElseThrow());
        return personopplysningTjeneste.hentOppgittAnnenPartAktørId(BehandlingReferanse.fra(førstegangsbehandling));
    }

    private Set<FpSak.Vedtak> finnFpVedtak(Stream<BehandlingVedtak> vedtak) {
        return vedtak.map(this::tilDto).collect(Collectors.toSet());
    }

    private FpSak.Vedtak tilDto(BehandlingVedtak vedtak) {
        var behandlingId = vedtak.getBehandlingsresultat().getBehandlingId();
        var uttaksperioder = finnUttaksperioder(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);
        var dekningsgrad = dekningsgradTjeneste.finnGjeldendeDekningsgradHvisEksisterer(ref);
        var annenpartEøsUttaksperioder = eøsUttakRepository.hentGrunnlag(behandlingId)
            .stream()
            .flatMap(g -> g.getSaksbehandlerPerioderEntitet().getPerioder().stream())
            .map(this::map)
            .toList();
        var beregningsgrunnlag = beregningOversiktDtoTjeneste.lagDtoForBehandling(ref);
        var tilkjentYtelse = beregningOversiktDtoTjeneste.lagDtoForTilkjentYtelse(ref);

        return new FpSak.Vedtak(vedtak.getVedtakstidspunkt(), uttaksperioder, dekningsgrad.map(FpDtoTjeneste::tilDekningsgradDto).orElse(null),
            annenpartEøsUttaksperioder, beregningsgrunnlag.orElse(null), tilkjentYtelse);
    }

    private FpSak.Vedtak.EøsUttaksperiode map(EøsUttaksperiodeEntitet p) {
        return new FpSak.Vedtak.EøsUttaksperiode(p.getPeriode().getFomDato(), p.getPeriode().getTomDato(), p.getTrekkdager().decimalValue(),
            map(p.getTrekkonto()));
    }

    private List<FpSak.Uttaksperiode> finnUttaksperioder(Long behandlingId) {
        return foreldrepengerUttakTjeneste.hentHvisEksisterer(behandlingId).map(uttak -> tilDto(uttak.getGjeldendePerioder())).orElse(List.of());
    }

    private List<FpSak.Uttaksperiode> tilDto(List<ForeldrepengerUttakPeriode> gjeldendePerioder) {
        return gjeldendePerioder.stream().map(this::tilDto).toList();
    }

    private FpSak.Uttaksperiode tilDto(ForeldrepengerUttakPeriode periode) {
        var type = utledResultatType(periode);
        var aktiviteter = periode.getAktiviteter().stream().map(a -> {
            var aktivitetType = switch (a.getUttakArbeidType()) {
                case ORDINÆRT_ARBEID -> UttakAktivitet.Type.ORDINÆRT_ARBEID;
                case SELVSTENDIG_NÆRINGSDRIVENDE -> UttakAktivitet.Type.SELVSTENDIG_NÆRINGSDRIVENDE;
                case FRILANS -> UttakAktivitet.Type.FRILANS;
                case ANNET -> UttakAktivitet.Type.ANNET;
            };
            var arbeidsgiver = a.getArbeidsgiver().map(arb -> new Arbeidsgiver(arb.getIdentifikator())).orElse(null);
            var arbeidsforholdId = Optional.ofNullable(a.getArbeidsforholdRef()).map(InternArbeidsforholdRef::getReferanse).orElse(null);
            var trekkdager = a.getTrekkdager().decimalValue();
            var konto = map(a.getTrekkonto());
            var arbeidstidsprosent = a.isSøktGraderingForAktivitetIPeriode() ? a.getArbeidsprosent() : BigDecimal.ZERO;
            return new FpSak.Uttaksperiode.UttaksperiodeAktivitet(new UttakAktivitet(aktivitetType, arbeidsgiver, arbeidsforholdId), konto,
                trekkdager, arbeidstidsprosent);
        }).collect(Collectors.toSet());
        var årsak = tilResultatÅrsak(periode);
        var trekkerMinsterett = trekkerMinsterett(periode);
        var resultat = new FpSak.Uttaksperiode.Resultat(type, årsak, aktiviteter, trekkerMinsterett);
        var utsettelseÅrsak = switch (periode.getUtsettelseType()) {
            case ARBEID -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.ARBEID;
            case FERIE -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.LOVBESTEMT_FERIE;
            case SYKDOM_SKADE -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.SØKER_SYKDOM;
            case SØKER_INNLAGT -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.SØKER_INNLAGT;
            case BARN_INNLAGT -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.BARN_INNLAGT;
            case HV_OVELSE -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.HV_ØVELSE;
            case NAV_TILTAK -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.NAV_TILTAK;
            case FRI -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.FRI;
            case UDEFINERT -> null;
        };
        var oppholdÅrsak = switch (periode.getOppholdÅrsak()) {
            case UDEFINERT -> null;
            case MØDREKVOTE_ANNEN_FORELDER -> OppholdÅrsak.MØDREKVOTE_ANNEN_FORELDER;
            case FEDREKVOTE_ANNEN_FORELDER -> OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER;
            case KVOTE_FELLESPERIODE_ANNEN_FORELDER -> OppholdÅrsak.FELLESPERIODE_ANNEN_FORELDER;
            case KVOTE_FORELDREPENGER_ANNEN_FORELDER -> OppholdÅrsak.FORELDREPENGER_ANNEN_FORELDER;
        };
        var overføringÅrsak = periode.isSøktOverføring() ? switch (periode.getOverføringÅrsak()) {
            case INSTITUSJONSOPPHOLD_ANNEN_FORELDER -> OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER;
            case SYKDOM_ANNEN_FORELDER -> OverføringÅrsak.SYKDOM_ANNEN_FORELDER;
            case IKKE_RETT_ANNEN_FORELDER -> OverføringÅrsak.IKKE_RETT_ANNEN_FORELDER;
            case ALENEOMSORG -> OverføringÅrsak.ALENEOMSORG;
            case UDEFINERT -> null;
        } : null;
        var samtidigUttaksprosent = periode.getSamtidigUttaksprosent();
        return new FpSak.Uttaksperiode(periode.getFom(), periode.getTom(), utsettelseÅrsak, oppholdÅrsak, overføringÅrsak,
            samtidigUttaksprosent == null ? null : samtidigUttaksprosent.decimalValue(), periode.isFlerbarnsdager(),
            map(periode.getMorsAktivitet()), resultat);
    }

    private static Konto map(UttakPeriodeType trekkonto) {
        return switch (trekkonto) {
            case FELLESPERIODE -> Konto.FELLESPERIODE;
            case MØDREKVOTE -> Konto.MØDREKVOTE;
            case FEDREKVOTE -> Konto.FEDREKVOTE;
            case FORELDREPENGER -> Konto.FORELDREPENGER;
            case FORELDREPENGER_FØR_FØDSEL -> Konto.FORELDREPENGER_FØR_FØDSEL;
            case UDEFINERT -> null;
        };
    }

    private static FpSak.Uttaksperiode.Resultat.Årsak tilResultatÅrsak(ForeldrepengerUttakPeriode periode) {
        if (periode.getGraderingAvslagÅrsak().equals(GraderingAvslagÅrsak.FOR_SEN_SØKNAD) && periode.harTrekkdager() &&
            periode.harRedusertUtbetaling() && harAktivitetMerTrekkEnnUtbetalingTilsier(periode)) {
            return FpSak.Uttaksperiode.Resultat.Årsak.INNVILGET_UTTAK_AVSLÅTT_GRADERING_TILBAKE_I_TID;
        }

        return switch (periode.getResultatÅrsak()) {
            case HULL_MELLOM_FORELDRENES_PERIODER, BARE_FAR_RETT_IKKE_SØKT -> FpSak.Uttaksperiode.Resultat.Årsak.AVSLAG_HULL_I_UTTAKSPLAN;
            case AVSLAG_UTSETTELSE_PGA_ARBEID_TILBAKE_I_TID, AVSLAG_UTSETTELSE_PGA_FERIE_TILBAKE_I_TID -> FpSak.Uttaksperiode.Resultat.Årsak.AVSLAG_UTSETTELSE_TILBAKE_I_TID;
            case FRATREKK_PLEIEPENGER -> FpSak.Uttaksperiode.Resultat.Årsak.AVSLAG_FRATREKK_PLEIEPENGER;
            default -> FpSak.Uttaksperiode.Resultat.Årsak.ANNET;
        };
    }

    private boolean trekkerMinsterett(ForeldrepengerUttakPeriode periode) {
        return periode.harTrekkdager() && !Set.of(PeriodeResultatÅrsak.FORELDREPENGER_KUN_FAR_HAR_RETT,
            PeriodeResultatÅrsak.GRADERING_FORELDREPENGER_KUN_FAR_HAR_RETT).contains(periode.getResultatÅrsak());
    }

    private static boolean harAktivitetMerTrekkEnnUtbetalingTilsier(ForeldrepengerUttakPeriode periode) {
        return periode.getAktiviteter().stream().anyMatch(a -> a.getTrekkdager().compareTo(graderingForventetTrekkdager(periode, a)) > 0);
    }

    private static Trekkdager graderingForventetTrekkdager(ForeldrepengerUttakPeriode periode, ForeldrepengerUttakPeriodeAktivitet aktivitet) {
        var virkedager = Virkedager.beregnAntallVirkedager(periode.getFom(), periode.getTom());
        var forventetTrekkdager = new BigDecimal(virkedager).multiply(aktivitet.getUtbetalingsgrad().decimalValue()).divide(BigDecimal.valueOf(100L), 1, RoundingMode.DOWN);
        return new Trekkdager(forventetTrekkdager);
    }

    private static FpSak.Uttaksperiode.Resultat.Type utledResultatType(ForeldrepengerUttakPeriode periode) {
        if (periode.isInnvilget() && periode.isGraderingInnvilget()) {
            return FpSak.Uttaksperiode.Resultat.Type.INNVILGET_GRADERING;
        }
        return switch (periode.getResultatType()) {
            case INNVILGET -> FpSak.Uttaksperiode.Resultat.Type.INNVILGET;
            case AVSLÅTT -> FpSak.Uttaksperiode.Resultat.Type.AVSLÅTT;
            case MANUELL_BEHANDLING -> throw new IllegalStateException("Forventer ikke perioder under manuell behandling");
        };
    }
}

