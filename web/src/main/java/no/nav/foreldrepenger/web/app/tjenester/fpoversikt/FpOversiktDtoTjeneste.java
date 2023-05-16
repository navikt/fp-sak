package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakOmsorgUtil;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class FpOversiktDtoTjeneste {

    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository vedtakRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private FagsakRepository fagsakRepository;
    private MottatteDokumentRepository dokumentRepository;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private UføretrygdRepository uføretrygdRepository;

    @Inject
    public FpOversiktDtoTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                 ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste,
                                 PersonopplysningTjeneste personopplysningTjeneste,
                                 FamilieHendelseTjeneste familieHendelseTjeneste,
                                 YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                 SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                 UføretrygdRepository uføretrygdRepository) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.foreldrepengerUttakTjeneste = foreldrepengerUttakTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.dokumentRepository = repositoryProvider.getMottatteDokumentRepository();
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.uføretrygdRepository = uføretrygdRepository;
    }

    FpOversiktDtoTjeneste() {
        //CDI
    }

    public Sak hentSak(String saksnummer) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(saksnummer)).orElseThrow();
        var aktørId = fagsak.getAktørId().getId();

        var gjeldendeVedtak = vedtakRepository.hentGjeldendeVedtak(fagsak);
        var åpenYtelseBehandling = hentÅpenBehandling(fagsak);
        var familieHendelse = finnFamilieHendelse(fagsak, gjeldendeVedtak, åpenYtelseBehandling);
        var sakStatus = finnFagsakStatus(fagsak);
        var ikkeHenlagteBehandlinger = finnIkkeHenlagteBehandlinger(fagsak);
        var aksjonspunkt = finnAksjonspunkt(ikkeHenlagteBehandlinger);
        var mottatteSøknader = finnRelevanteSøknadsdokumenter(fagsak);
        return switch (fagsak.getYtelseType()) {
            case ENGANGSTØNAD -> new EsSak(saksnummer, aktørId, familieHendelse, sakStatus, aksjonspunkt, finnEsSøknader(åpenYtelseBehandling,
                mottatteSøknader));
            case FORELDREPENGER -> new FpSak(saksnummer, aktørId, familieHendelse, sakStatus, finnVedtakForForeldrepenger(fagsak),
                oppgittAnnenPart(fagsak).map(AktørId::getId).orElse(null), aksjonspunkt, finnFpSøknader(åpenYtelseBehandling, mottatteSøknader),
                finnBrukerRolle(fagsak), finnFødteBarn(fagsak, gjeldendeVedtak, åpenYtelseBehandling), finnRettigheter(fagsak, gjeldendeVedtak, åpenYtelseBehandling));
            case SVANGERSKAPSPENGER -> new SvpSak(saksnummer, aktørId, familieHendelse, sakStatus, aksjonspunkt, finnSvpSøknader(åpenYtelseBehandling,
                mottatteSøknader));
            case UDEFINERT -> throw new IllegalStateException("Unexpected value: " + fagsak.getYtelseType());
        };
    }

    private FpSak.Rettigheter finnRettigheter(Fagsak fagsak, Optional<BehandlingVedtak> gjeldendeVedtak, Optional<Behandling> åpenYtelseBehandling) {
        if (gjeldendeVedtak.isPresent()) {
            return finnGjeldendeRettigheter(gjeldendeVedtak.get().getBehandlingsresultat().getBehandlingId());
        }
        if (åpenYtelseBehandling.isPresent()) {
            return finnOppgitteRettigheter(åpenYtelseBehandling.get().getId()).orElse(null);
        }
        var sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakIdReadOnly(fagsak.getId());
        return sisteBehandling.flatMap(b -> finnOppgitteRettigheter(b.getId())).orElse(null);
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

    private FpSak.Rettigheter finnGjeldendeRettigheter(Long behandlingId) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandlingId);
        var aleneomsorg = UttakOmsorgUtil.harAleneomsorg(ytelseFordelingAggregat);
        var annenForelderRettEØS = UttakOmsorgUtil.avklartAnnenForelderHarRettEØS(ytelseFordelingAggregat);
        var uføretrygdGrunnlagEntitet = uføretrygdRepository.hentGrunnlag(behandlingId);
        var morUføretrygd = UttakOmsorgUtil.morMottarUføretrygd(ytelseFordelingAggregat, uføretrygdGrunnlagEntitet.orElse(null));
        return new FpSak.Rettigheter(aleneomsorg, morUføretrygd, annenForelderRettEØS);
    }

    private Set<String> finnFødteBarn(Fagsak fagsak,
                                      Optional<BehandlingVedtak> gjeldendeVedtak,
                                      Optional<Behandling> åpenYtelseBehandling) {
        final Optional<Behandling> behandling;
        if (gjeldendeVedtak.isPresent()) {
            var behandlingId = gjeldendeVedtak.get().getBehandlingsresultat().getBehandlingId();
            behandling = Optional.of(behandlingRepository.hentBehandling(behandlingId));
        } else if (åpenYtelseBehandling.isPresent()) {
            behandling = åpenYtelseBehandling;
        } else {
            behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakIdReadOnly(fagsak.getId());
        }
        return behandling.map(this::finnFødteBarn).orElse(Set.of());
    }

    private Set<String> finnFødteBarn(Behandling behandling) {
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        return personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(ref)
            .map(pi -> pi.getBarna().stream().map(barn -> barn.getAktørId().getId()).collect(Collectors.toSet()))
            .orElse(Set.of());
    }

    private FpSak.BrukerRolle finnBrukerRolle(Fagsak fagsak) {
        return switch (fagsak.getRelasjonsRolleType()) {
            case FARA -> FpSak.BrukerRolle.FAR;
            case MORA -> FpSak.BrukerRolle.MOR;
            case MEDMOR -> FpSak.BrukerRolle.MEDMOR;
            case EKTE, REGISTRERT_PARTNER, BARN, ANNEN_PART_FRA_SØKNAD, UDEFINERT -> throw new IllegalStateException("Unexpected value: " + fagsak.getRelasjonsRolleType());
        };
    }

    private List<MottattDokument> finnRelevanteSøknadsdokumenter(Fagsak fagsak) {
        return dokumentRepository.hentMottatteDokumentMedFagsakId(fagsak.getId())
            .stream()
            .filter(md -> md.erSøknadsDokument())
            .filter(md -> md.getJournalpostId() != null)
            .filter(md -> md.getMottattTidspunkt() != null)
            .toList();
    }

    private Set<FpSak.Søknad> finnFpSøknader(Optional<Behandling> åpenYtelseBehandling, List<MottattDokument> mottatteSøknader) {
        return mottatteSøknader.stream()
            .map(md -> {
                var status = statusForSøknad(åpenYtelseBehandling, md);
                var perioder = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(md.getBehandlingId()).map(ytelseFordelingAggregat -> {
                    var oppgittFordeling = ytelseFordelingAggregat.getOppgittFordeling();
                    return oppgittFordeling.getPerioder().stream().map(p -> tilDto(p)).collect(Collectors.toSet());
                }).orElse(Set.of());
                return new FpSak.Søknad(status, md.getMottattTidspunkt(), perioder);
            })
            .filter(s -> !s.perioder().isEmpty()) //Filtrerer ut søknaden som ikke er registert i YF. Feks behandling står i papir punching
            .collect(Collectors.toSet());
    }

    private static SøknadStatus statusForSøknad(Optional<Behandling> åpenYtelseBehandling, MottattDokument md) {
        return åpenYtelseBehandling.filter(b -> b.getId().equals(md.getBehandlingId())).map(b -> SøknadStatus.MOTTATT).orElse(SøknadStatus.BEHANDLET);
    }

    private static FpSak.Søknad.Periode tilDto(OppgittPeriodeEntitet periode) {
        return new FpSak.Søknad.Periode(periode.getFom(), periode.getTom(), switch (periode.getPeriodeType()) {
            case FELLESPERIODE -> Konto.FELLESPERIODE;
            case MØDREKVOTE -> Konto.MØDREKVOTE;
            case FEDREKVOTE -> Konto.FEDREKVOTE;
            case FORELDREPENGER -> Konto.FORELDREPENGER;
            case FORELDREPENGER_FØR_FØDSEL -> Konto.FORELDREPENGER_FØR_FØDSEL;
            case ANNET, UDEFINERT -> null;
        });
    }

    private static Set<SvpSak.Søknad> finnSvpSøknader(Optional<Behandling> åpenYtelseBehandling, List<MottattDokument> mottatteSøknader) {
        return mottatteSøknader.stream()
            .map(md -> {
                var status = statusForSøknad(åpenYtelseBehandling, md);
                return new SvpSak.Søknad(status, md.getMottattTidspunkt());
            })
            .collect(Collectors.toSet());
    }

    private static Set<EsSak.Søknad> finnEsSøknader(Optional<Behandling> åpenYtelseBehandling, List<MottattDokument> mottatteSøknader) {
        return mottatteSøknader.stream()
            .map(md -> {
                var status = statusForSøknad(åpenYtelseBehandling, md);
                return new EsSak.Søknad(status, md.getMottattTidspunkt());
            })
            .collect(Collectors.toSet());
    }

    private List<Behandling> finnIkkeHenlagteBehandlinger(Fagsak fagsak) {
        return behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsak.getId());
    }

    private Set<Sak.Aksjonspunkt> finnAksjonspunkt(List<Behandling> ikkeHenlagteBehandlinger) {
        return ikkeHenlagteBehandlinger.stream().flatMap(b -> b.getAksjonspunkter().stream())
            .filter(a -> a.erOpprettet() || a.erUtført())
            .map(a -> {
                var status = a.erOpprettet() ? Sak.Aksjonspunkt.Status.OPPRETTET : Sak.Aksjonspunkt.Status.UTFØRT;
                var venteÅrsak = Venteårsak.UDEFINERT.equals(a.getVenteårsak()) ? null : a.getVenteårsak().getKode();
                return new Sak.Aksjonspunkt(a.getAksjonspunktDefinisjon().getKode(), status, venteÅrsak, a.getOpprettetTidspunkt());
            }).collect(Collectors.toSet());
    }

    private Sak.Status finnFagsakStatus(Fagsak fagsak) {
        return switch (fagsak.getStatus()) {
            case OPPRETTET -> Sak.Status.OPPRETTET;
            case UNDER_BEHANDLING -> Sak.Status.UNDER_BEHANDLING;
            case LØPENDE -> Sak.Status.LØPENDE;
            case AVSLUTTET -> Sak.Status.AVSLUTTET;
        };
    }

    private Sak.FamilieHendelse finnFamilieHendelse(Fagsak fagsak,
                                                    Optional<BehandlingVedtak> gjeldendeVedtak,
                                                    Optional<Behandling> åpenYtelseBehandling) {
        if (gjeldendeVedtak.isPresent()) {
            return finnGjeldendeFamilieHendelse(gjeldendeVedtak.get().getBehandlingsresultat().getBehandlingId());
        }
        if (åpenYtelseBehandling.isPresent()) {
            return finnOppgittFamilieHendelse(åpenYtelseBehandling.get().getId()).orElse(null);
        }
        var sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakIdReadOnly(fagsak.getId());
        return sisteBehandling.flatMap(b -> finnOppgittFamilieHendelse(b.getId())).orElse(null);
    }

    private Optional<Behandling> hentÅpenBehandling(Fagsak fagsak) {
        return behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsak.getId())
            .stream()
            .filter(b -> b.erYtelseBehandling())
            .max(Comparator.comparing(Behandling::getOpprettetTidspunkt));
    }

    private Optional<Sak.FamilieHendelse> finnOppgittFamilieHendelse(Long behandling) {
        return familieHendelseTjeneste.finnAggregat(behandling).map(agg -> tilDto(agg.getSøknadVersjon()));
    }

    private Sak.FamilieHendelse finnGjeldendeFamilieHendelse(Long behandling) {
        return familieHendelseTjeneste.finnAggregat(behandling)
            .map(fhg -> tilDto(fhg.getGjeldendeVersjon()))
            .orElse(null);
    }

    private static Sak.FamilieHendelse tilDto(FamilieHendelseEntitet familieHendelse) {
        return new Sak.FamilieHendelse(familieHendelse.getFødselsdato().orElse(null),
            familieHendelse.getTerminbekreftelse().map(tb -> tb.getTermindato()).orElse(null),
            familieHendelse.getAntallBarn() == null ? 0 : familieHendelse.getAntallBarn(),
            familieHendelse.getAdopsjon().map(a -> a.getOmsorgsovertakelseDato()).orElse(null));
    }

    private Optional<AktørId> oppgittAnnenPart(Fagsak fagsak) {
        var førstegangsbehandling = behandlingRepository.finnSisteIkkeHenlagteBehandlingavAvBehandlingTypeFor(fagsak.getId(),
                BehandlingType.FØRSTEGANGSSØKNAD)
            .orElseGet(() -> behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsak.getId())
                .stream()
                .max(Comparator.comparing(Behandling::getOpprettetDato))
                .orElseThrow());
        return personopplysningTjeneste.hentOppgittAnnenPartAktørId(førstegangsbehandling.getId());
    }

    private Set<FpSak.Vedtak> finnVedtakForForeldrepenger(Fagsak fagsak) {
        var behandlingerMedVedtak = behandlingRepository.finnAlleAvsluttedeIkkeHenlagteBehandlinger(fagsak.getId());
        return behandlingerMedVedtak.stream()
            .map(b -> vedtakRepository.hentForBehandlingHvisEksisterer(b.getId()))
            .filter(Optional::isPresent)
            .map(v -> v.get())
            .map(vedtak -> tilDto(vedtak, fagsak))
            .collect(Collectors.toSet());
    }

    private FpSak.Vedtak tilDto(BehandlingVedtak vedtak, Fagsak fagsak) {
        var dekningsgrad = finnDekningsgrad(fagsak);
        var uttaksperioder = finnUttaksperioder(vedtak.getBehandlingsresultat().getBehandlingId());
        return new FpSak.Vedtak(vedtak.getVedtakstidspunkt(), uttaksperioder, dekningsgrad);
    }

    private List<FpSak.Uttaksperiode> finnUttaksperioder(Long behandlingId) {
        return foreldrepengerUttakTjeneste.hentUttakHvisEksisterer(behandlingId).map(uttak -> tilDto(uttak.getGjeldendePerioder())).orElse(List.of());
    }

    private List<FpSak.Uttaksperiode> tilDto(List<ForeldrepengerUttakPeriode> gjeldendePerioder) {
        return gjeldendePerioder.stream().map(this::tilDto).toList();
    }

    private FpSak.Uttaksperiode tilDto(ForeldrepengerUttakPeriode periode) {
        var type = switch (periode.getResultatType()) {
            case INNVILGET -> FpSak.Uttaksperiode.Resultat.Type.INNVILGET;
            case AVSLÅTT -> FpSak.Uttaksperiode.Resultat.Type.AVSLÅTT;
            case MANUELL_BEHANDLING -> throw new IllegalStateException("Forventer ikke perioder under manuell behandling");
        };
        var aktiviteter = periode.getAktiviteter().stream().map(a -> {
            var aktivitetType = switch (a.getUttakArbeidType()) {
                case ORDINÆRT_ARBEID -> FpSak.Uttaksperiode.UttakAktivitet.Type.ORDINÆRT_ARBEID;
                case SELVSTENDIG_NÆRINGSDRIVENDE -> FpSak.Uttaksperiode.UttakAktivitet.Type.SELVSTENDIG_NÆRINGSDRIVENDE;
                case FRILANS -> FpSak.Uttaksperiode.UttakAktivitet.Type.FRILANS;
                case ANNET -> FpSak.Uttaksperiode.UttakAktivitet.Type.ANNET;
            };
            var arbeidsgiver = a.getArbeidsgiver().map(arb -> new FpSak.Uttaksperiode.UttakAktivitet.Arbeidsgiver(arb.getIdentifikator())).orElse(null);
            var arbeidsforholdId = Optional.ofNullable(a.getArbeidsforholdRef()).map(ref -> ref.getReferanse()).orElse(null);
            var trekkdager = a.getTrekkdager().decimalValue();
            var konto = switch (a.getTrekkonto()) {
                case FELLESPERIODE -> Konto.FELLESPERIODE;
                case MØDREKVOTE -> Konto.MØDREKVOTE;
                case FEDREKVOTE -> Konto.FEDREKVOTE;
                case FORELDREPENGER -> Konto.FORELDREPENGER;
                case FORELDREPENGER_FØR_FØDSEL -> Konto.FORELDREPENGER_FØR_FØDSEL;
                case UDEFINERT, FLERBARNSDAGER -> null;
            };
            return new FpSak.Uttaksperiode.UttaksperiodeAktivitet(new FpSak.Uttaksperiode.UttakAktivitet(aktivitetType, arbeidsgiver, arbeidsforholdId),
                konto, trekkdager, a.getArbeidsprosent());
        }).collect(Collectors.toSet());
        var resultat = new FpSak.Uttaksperiode.Resultat(type, aktiviteter);
        return new FpSak.Uttaksperiode(periode.getFom(), periode.getTom(), resultat);
    }

    private FpSak.Vedtak.Dekningsgrad finnDekningsgrad(Fagsak fagsak) {
        var dekningsgrad = fagsakRelasjonRepository.finnRelasjonFor(fagsak).getGjeldendeDekningsgrad();
        return dekningsgrad.isÅtti() ? FpSak.Vedtak.Dekningsgrad.ÅTTI : FpSak.Vedtak.Dekningsgrad.HUNDRE;
    }
}
