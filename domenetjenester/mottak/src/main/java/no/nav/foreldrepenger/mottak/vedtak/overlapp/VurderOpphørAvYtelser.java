package no.nav.foreldrepenger.mottak.vedtak.overlapp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderKonsekvensTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.konfig.Tid;

/**
 * Tjenesten sjekker om det finnes løpende saker for den personen det innvilges foreldrepenger eller svangerskapsper på.
 * Om det finnes løpende saker sjekkes det om ny sak overlapper med løpende sak. Det sjekkes både for mor, far og en
 * eventuell medforelder på foreldrepenger.
 * Dersom det er overlapp opprettes en prosesstask for å håndtere overlappet videre.
 */
@ApplicationScoped
public class VurderOpphørAvYtelser  {
    private static final Logger LOG = LoggerFactory.getLogger(VurderOpphørAvYtelser.class);

    private static final Set<FagsakYtelseType> VURDER_OVERLAPP = Set.of(FagsakYtelseType.FORELDREPENGER, FagsakYtelseType.SVANGERSKAPSPENGER);

    private FagsakRepository fagsakRepository;
    private PersonopplysningRepository personopplysningRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private ProsessTaskRepository prosessTaskRepository;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private OverlappFPInfotrygdTjeneste sjekkOverlappInfortrygd;
    private FamilieHendelseRepository familieHendelseRepository;
    private static final BigDecimal HUNDRE = new BigDecimal(100);

    private EntityManager entityManager;

    public VurderOpphørAvYtelser() {
        //NoSonar
    }

    @Inject
    public VurderOpphørAvYtelser(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                 @FagsakYtelseTypeRef("FP") RevurderingTjeneste revurderingTjenesteFP,
                                 @FagsakYtelseTypeRef("SVP") RevurderingTjeneste revurderingTjenesteSVP,
                                 ProsessTaskRepository prosessTaskRepository,
                                 BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                 OverlappFPInfotrygdTjeneste sjekkOverlappInfortrygd,
                                 EntityManager entityManager) {
        this.fagsakRepository = behandlingRepositoryProvider.getFagsakRepository();
        this.personopplysningRepository = behandlingRepositoryProvider.getPersonopplysningRepository();
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = behandlingRepositoryProvider.getBehandlingsresultatRepository();
        this.beregningsresultatRepository = behandlingRepositoryProvider.getBeregningsresultatRepository();
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.sjekkOverlappInfortrygd = sjekkOverlappInfortrygd;
        this.familieHendelseRepository = behandlingRepositoryProvider.getFamilieHendelseRepository();
        this.entityManager = entityManager;
    }

    void vurderOpphørAvYtelser(Long fagsakId, Long behandlingId) {

        var gjeldendeFagsak = fagsakRepository.finnEksaktFagsak(fagsakId);

        if (!VURDER_OVERLAPP.contains(gjeldendeFagsak.getYtelseType())) {
            return;
        }
        // Finner første fradato i utbetalingsperioden for iverksatt behandling
        var startDatoIVB = finnMinDato(behandlingId);
        if (Tid.TIDENES_ENDE.equals(startDatoIVB)) {
            return;
        }

        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(gjeldendeFagsak.getYtelseType())) {
            vurderOpphørAvYtelserForSVP(gjeldendeFagsak, startDatoIVB, behandlingId);
        } else {
            vurderOppørAvYtelserForFP(gjeldendeFagsak, behandlingId, startDatoIVB);
        }
    }

    private void vurderOppørAvYtelserForFP(Fagsak gjeldendeFagsak, Long behandlingId, LocalDate startDatoIVB) {
        List<AktørId> aktørIdList = new ArrayList<>();

        aktørIdList.add(gjeldendeFagsak.getAktørId());

        List<AktørId> aktørIdListSjekkInfotrygd = new ArrayList<>();
        if (RelasjonsRolleType.erFar(gjeldendeFagsak.getRelasjonsRolleType())) {
            aktørIdListSjekkInfotrygd.add(gjeldendeFagsak.getAktørId());
        }

        if (RelasjonsRolleType.erMor(gjeldendeFagsak.getRelasjonsRolleType())) {
            var annenPartAktørId = hentAnnenPartAktørId(behandlingId);
            if (annenPartAktørId.isPresent()) {
                aktørIdList.add(annenPartAktørId.get());
                aktørIdListSjekkInfotrygd.add(annenPartAktørId.get());
            }
        }
        // Sjekker om det finnes overlapp på far og medforelder i Infotrygd
        aktørIdListSjekkInfotrygd.forEach(aktørId -> {
            var overlappInfotrygd = sjekkOverlappInfortrygd.harForeldrepengerInfotrygdSomOverlapper(aktørId, startDatoIVB);
            if (overlappInfotrygd) {
                håndtereOpphørInfotrygd(behandlingId, gjeldendeFagsak, aktørId);
            }
        });
        // Sjekker om det finnes overlapp i fpsak
        aktørIdList
            .forEach(aktørId -> løpendeSakerSomOverlapperUttakPåNySak(aktørId, gjeldendeFagsak, startDatoIVB)
                .forEach(this::opprettTaskForÅHåndtereOpphør));
    }

    private void vurderOpphørAvYtelserForSVP(Fagsak gjeldendeSVPsak, LocalDate startDatoIVB, Long behandlingId) {
        var sisteDatoIVB = finnMaxDatoUtenAvslåtte(behandlingId);
        var overlapper = løpendeSakerSomOverlapperUttakNySakSVP(gjeldendeSVPsak.getAktørId(), gjeldendeSVPsak.getSaksnummer(), startDatoIVB, sisteDatoIVB);
        overlapper.forEach( fagsak -> {
            // Overlapp SVP-SVP - logger foreløpig
            if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(fagsak.getYtelseType())) {
                LOG.info("Overlapp SVP oppdaget for sak {} med løpende SVP-sak {}. Ingen revurdering opprettet", gjeldendeSVPsak.getSaksnummer(), fagsak.getSaksnummer());
            }
            if (FagsakYtelseType.FORELDREPENGER.equals(fagsak.getYtelseType())) {
                behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()).ifPresent(overlappFPBeh -> {
                    var startDatoverlappBeh = finnMinDato(overlappFPBeh.getId());

                    if (startDatoIVB.isBefore(startDatoverlappBeh)) {
                        // Overlapp med løpende foreldrepenger på samme barn - opprettes revurdering på innvilget svp behandling
                        opprettTaskForÅHåndtereOpphør(gjeldendeSVPsak);
                        LOG.info("Overlapp SVP: SVP-sak {} overlapper med FP-sak på samme barn {}", gjeldendeSVPsak.getSaksnummer(), fagsak.getSaksnummer());

                    } else if (erFullUtbetalingSistePeriode(fagsak.getId())) {
                        // Overlapp med løpende foreldrepenger og svp for nytt barn - opprettes revurdering på løpende foreldrepenger-sak
                        opprettTaskForÅHåndtereOpphør(fagsak);
                        LOG.info("Overlapp SVP: SVP-sak {} overlapper med FP-sak {}", gjeldendeSVPsak.getSaksnummer(), fagsak.getSaksnummer());
                    } else {
                        // Overlapp med løpenge graderte foreldrepenger -  kan være tillatt så derfor logger vi foreløpig
                        LOG.info("Overlapp SVP: SVP-sak {} overlapper med gradert FP-sak {}. Ingen revurdering opprettet", gjeldendeSVPsak.getSaksnummer(), fagsak.getSaksnummer());
                    }
                });
            }
        });
    }

    private void håndtereOpphørInfotrygd(Long behandlingId, Fagsak gjeldendeFagsak, AktørId aktørId) {
        var gjeldendeBehandling = behandlingRepository.hentBehandling(behandlingId);

        var enhet = utledEnhetFraBehandling(gjeldendeBehandling);

        opprettTaskForÅVurdereKonsekvens(gjeldendeFagsak.getId(), enhet.enhetId(),
            "Nytt barn i VL: Vurder opphør av ytelse i Infotrygd", Optional.of(aktørId.getId()));

        LOG.info("Overlapp INFOTRYGD på aktør {} for vedtatt sak {}", aktørId, gjeldendeFagsak.getSaksnummer());
    }

    private void opprettTaskForÅHåndtereOpphør(Fagsak sakOpphør) {
        var beskrivelse = String.format("Overlapp identifisert: Vurder saksnr %s", sakOpphør.getSaksnummer());
        var prosessTaskData = new ProsessTaskData(HåndterOpphørAvYtelserTask.TASKTYPE);
        prosessTaskData.setFagsakId(sakOpphør.getId());
        prosessTaskData.setProperty(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY, beskrivelse);
        prosessTaskData.setProperty(HåndterOpphørAvYtelserTask.BEHANDLING_ÅRSAK_KEY, BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN.getKode());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }

    private List<Fagsak> løpendeSakerSomOverlapperUttakPåNySak(AktørId aktørId, Fagsak fagsakIVB, LocalDate startDato) {
        return fagsakRepository.hentForBruker(aktørId).stream()
            .filter(f -> VURDER_OVERLAPP.contains(f.getYtelseType()))
            .filter(f -> !fagsakIVB.getSaksnummer().equals(f.getSaksnummer()))
            .filter(f -> erMaxDatoPåLøpendeSakEtterStartDatoNysak(f, startDato))
            .filter(f-> !gjelderAdopsjonSammeBarn(f, fagsakIVB))
            .collect(Collectors.toList());
    }

    private List<Fagsak> løpendeSakerSomOverlapperUttakNySakSVP(AktørId aktørId, Saksnummer saksnummer, LocalDate startDato, LocalDate sisteDato) {
        return fagsakRepository.hentForBruker(aktørId).stream()
            .filter(f -> VURDER_OVERLAPP.contains(f.getYtelseType()))
            .filter(f -> !saksnummer.equals(f.getSaksnummer()))
            .filter(f -> f.getYtelseType().equals(FagsakYtelseType.SVANGERSKAPSPENGER) ? erMaxDatoPåLøpendeSakEtterStartDatoNysak(f, startDato) : erOverlappMedløpendeFp(f, startDato, sisteDato))
            .collect(Collectors.toList());
    }

    private boolean gjelderAdopsjonSammeBarn(Fagsak fagsakLop, Fagsak fagsakIVB) {
        var fhGrunnlagLop = hendelseGrunnlagEntitet(fagsakLop.getId());
        var fhGrunnlagIVB = hendelseGrunnlagEntitet(fagsakIVB.getId());
        var erAdopsjonLop = fhGrunnlagLop.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon).map(FamilieHendelseEntitet::getType).map(FamilieHendelseType::gjelderAdopsjon).orElse(false);
        var erAdopsjonIVB = fhGrunnlagIVB.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon).map(FamilieHendelseEntitet::getType).map(FamilieHendelseType::gjelderAdopsjon).orElse(false);
        if (!erAdopsjonLop || !erAdopsjonIVB) {
            return false;
        }
        return  Objects.equals(fhGrunnlagLop.flatMap(FamilieHendelseGrunnlagEntitet::getGjeldendeAdopsjon).map(AdopsjonEntitet::getOmsorgsovertakelseDato),
            fhGrunnlagIVB.flatMap(FamilieHendelseGrunnlagEntitet::getGjeldendeAdopsjon).map(AdopsjonEntitet::getOmsorgsovertakelseDato));
    }

    private Optional<FamilieHendelseGrunnlagEntitet> hendelseGrunnlagEntitet(Long fagsakId) {
        var behandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);

        return behandling.flatMap(b-> familieHendelseRepository.hentAggregatHvisEksisterer(b.getId()));
    }

    private boolean erMaxDatoPåLøpendeSakEtterStartDatoNysak(Fagsak fagsak, LocalDate startDato) {
        var behandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId());
        var maxDato = behandling.map(Behandling::getId).map(this::finnMaxDato).orElse(Tid.TIDENES_BEGYNNELSE);
        var maxUtbetaltDato = behandling.map(Behandling::getId).map(this::finnMaxDatoUtenAvslåtte).orElse(Tid.TIDENES_BEGYNNELSE);
        if (!maxDato.isBefore(startDato) && maxUtbetaltDato.isBefore(startDato))
            LOG.info("VurderOpphør - mulig overlapp ifm avslått periode for fagsak {} med maxDato {} og maxUtbetalt {}", fagsak.getSaksnummer(), maxDato, maxUtbetaltDato );

        return !maxUtbetaltDato.isBefore(startDato);
    }

    private boolean erOverlappMedløpendeFp(Fagsak fagsak, LocalDate startDatoIVB, LocalDate sisteDatoIVB) {
        var behandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId());
        var startDatoLøpFpSak = behandling.map(Behandling::getId).map(this::finnMinDato).orElse(Tid.TIDENES_ENDE);

        //Dersom det er SVP og FP for samme barn må overlapp sjekkes mot maxdato på innvilget SVP sak
        if (startDatoIVB.isBefore(startDatoLøpFpSak)) {
            return !sisteDatoIVB.isBefore(startDatoLøpFpSak);
        }
        return erMaxDatoPåLøpendeSakEtterStartDatoNysak(fagsak, startDatoIVB);
    }

    private LocalDate finnMinDato(Long behandlingId) {
        if (henlagtEllerOpphørFomFørsteUttak(behandlingId))
            return Tid.TIDENES_ENDE;

        var minFom = beregningsresultatRepository.hentUtbetBeregningsresultat(behandlingId)
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
            .min(Comparator.naturalOrder())
            .map(VirkedagUtil::fomVirkedag);
        return minFom.orElse(Tid.TIDENES_ENDE);
    }

    private boolean henlagtEllerOpphørFomFørsteUttak(Long behandlingId) {
        var resultat = behandlingsresultatRepository.hentHvisEksisterer(behandlingId).map(Behandlingsresultat::getBehandlingResultatType).orElse(BehandlingResultatType.INNVILGET);
        if (resultat.erHenlagt())
            return true;
        // Aktuelt for revurderinger med Opphør fom start. Enkelte har opphør fom senere dato.
        return Set.of(BehandlingResultatType.OPPHØR, BehandlingResultatType.AVSLÅTT).contains(resultat)
            && Tid.TIDENES_BEGYNNELSE.equals(finnMaxDatoUtenAvslåtte(behandlingId));
    }

    private LocalDate finnMaxDato(Long behandlingId) {
        var maxTom = beregningsresultatRepository.hentUtbetBeregningsresultat(behandlingId)
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom)
            .max(Comparator.naturalOrder())
            .map(VirkedagUtil::tomVirkedag);
        return maxTom.orElse(Tid.TIDENES_BEGYNNELSE);
    }

    private LocalDate finnMaxDatoUtenAvslåtte(Long behandlingId) {
        var maxTom = beregningsresultatRepository.hentUtbetBeregningsresultat(behandlingId)
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
            .filter(beregningsresultatPeriode -> beregningsresultatPeriode.getDagsats() > 0)
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom)
            .max(Comparator.naturalOrder())
            .map(VirkedagUtil::tomVirkedag);
        return maxTom.orElse(Tid.TIDENES_BEGYNNELSE);
    }

    private boolean erFullUtbetalingSistePeriode(Long fagsakId) {
        var behandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);
        var berResultat = beregningsresultatRepository.hentUtbetBeregningsresultat(behandling.map(Behandling::getId).orElse(null));

        return berResultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
            .filter(beregningsresultatPeriode -> beregningsresultatPeriode.getDagsats() > 0)
            .max(Comparator.comparing(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom))
            .map(BeregningsresultatPeriode::getKalkulertUtbetalingsgrad)
            .map(ug -> ug.compareTo(HUNDRE) >= 0).orElse(false);
    }

    private Optional<AktørId> hentAnnenPartAktørId(long behId) {
        return personopplysningRepository.hentOppgittAnnenPartHvisEksisterer(behId)
            .map(OppgittAnnenPartEntitet::getAktørId);
    }

    void opprettTaskForÅVurdereKonsekvens(Long fagsakId, String behandlendeEnhetsId, String oppgaveBeskrivelse, Optional<String> gjeldendeAktørId) {
        var prosessTaskData = new ProsessTaskData(OpprettOppgaveVurderKonsekvensTask.TASKTYPE);
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BEHANDLENDE_ENHET, behandlendeEnhetsId);
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BESKRIVELSE, oppgaveBeskrivelse);
        gjeldendeAktørId.ifPresent(a-> prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_GJELDENDE_AKTØR_ID, a));
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_PRIORITET, OpprettOppgaveVurderKonsekvensTask.PRIORITET_HØY);
        prosessTaskData.setFagsakId(fagsakId);
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }

    private OrganisasjonsEnhet utledEnhetFraBehandling(Behandling behandling) {
        return behandlendeEnhetTjeneste.gyldigEnhetNfpNk(behandling.getBehandlendeEnhet()) ?
            behandling.getBehandlendeOrganisasjonsEnhet() : behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(behandling.getFagsak());
    }
}
