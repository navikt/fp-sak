package no.nav.foreldrepenger.mottak.vedtak;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.vedtak.infotrygd.overlapp.SjekkOverlappForeldrepengerInfotrygdTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.KøKontroller;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderKonsekvensTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.konfig.Tid;

/**
Funksjonen sjekker om det finnes løpende saker for den personen det innvilges foreldrepenger eller svangerskapsper på.
Om det finnes løpende saker sjekkes det om ny sak overlapper med løpende sak. Det sjekkes både for mor, far og en eventuell medforelder på foreldrepenger.
Dersom det er overlapp opprettes en "vurder konsekvens for ytelse"-oppgave i Gosys, og en revurdering med egen årsak slik at saksbehandler kan
vurdere om opphør skal gjennomføres eller ikke. Saksbehandling må skje manuelt, og fritekstbrev må benyttes for opphør av løpende sak.
 */
@ApplicationScoped
public class VurderOpphørAvYtelser  {
    private static final Logger LOG = LoggerFactory.getLogger(VurderOpphørAvYtelser.class);

    private FagsakRepository fagsakRepository;
    private PersonopplysningRepository personopplysningRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private RevurderingTjeneste revurderingTjenesteFP;
    private RevurderingTjeneste revurderingTjenesteSVP;
    private ProsessTaskRepository prosessTaskRepository;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private SjekkOverlappForeldrepengerInfotrygdTjeneste sjekkOverlappInfortrygd;
    private KøKontroller køKontroller;
    private static final BigDecimal HUNDRE = new BigDecimal(100);

    public VurderOpphørAvYtelser() {
        //NoSonar
    }

    @Inject
    public VurderOpphørAvYtelser(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                 @FagsakYtelseTypeRef("FP") RevurderingTjeneste revurderingTjenesteFP,
                                 @FagsakYtelseTypeRef("SVP") RevurderingTjeneste revurderingTjenesteSVP,
                                 ProsessTaskRepository prosessTaskRepository,
                                 BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                 BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                 SjekkOverlappForeldrepengerInfotrygdTjeneste sjekkOverlappInfortrygd,
                                 KøKontroller køKontroller
                                 ) {
        this.fagsakRepository = behandlingRepositoryProvider.getFagsakRepository();
        this.personopplysningRepository = behandlingRepositoryProvider.getPersonopplysningRepository();
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = behandlingRepositoryProvider.getBehandlingsresultatRepository();
        this.beregningsresultatRepository = behandlingRepositoryProvider.getBeregningsresultatRepository();
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.revurderingTjenesteFP = revurderingTjenesteFP;
        this.revurderingTjenesteSVP = revurderingTjenesteSVP;
        this.sjekkOverlappInfortrygd = sjekkOverlappInfortrygd;
        this.køKontroller = køKontroller;
    }

    void vurderOpphørAvYtelser(Long fagsakId, Long behandlingId) {

        Fagsak gjeldendeFagsak = fagsakRepository.finnEksaktFagsak(fagsakId);

        if (FagsakYtelseType.ENGANGSTØNAD.equals(gjeldendeFagsak.getYtelseType())) {
            return;
        }
        // Finner første fradato i vedtatt periode for iverksatt behandling
        LocalDate startDatoIVB = finnMinDato(behandlingId);
        if (Tid.TIDENES_ENDE.equals(startDatoIVB)) {
            return;
        }

        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(gjeldendeFagsak.getYtelseType())) {
            vurderOpphørAvYtelserForSVP(gjeldendeFagsak, startDatoIVB);
        } else {
            vurderOppørAvYtelserForFP(gjeldendeFagsak, behandlingId, startDatoIVB);
        }
    }

    void vurderOppørAvYtelserForFP(Fagsak gjeldendeFagsak, Long behandlingId, LocalDate startDatoIVB) {
        List<AktørId> aktørIdList = new ArrayList<>();

        aktørIdList.add(gjeldendeFagsak.getAktørId());

        List<AktørId> aktørIdListSjekkInfotrygd = new ArrayList<>();
        if (RelasjonsRolleType.erFar(gjeldendeFagsak.getRelasjonsRolleType())) {
            aktørIdListSjekkInfotrygd.add(gjeldendeFagsak.getAktørId());
        }

        if (RelasjonsRolleType.erMor(gjeldendeFagsak.getRelasjonsRolleType())) {
            Optional<AktørId> annenPartAktørId = hentAnnenPartAktørId(behandlingId);
            if (annenPartAktørId.isPresent()) {
                aktørIdList.add(annenPartAktørId.get());
                aktørIdListSjekkInfotrygd.add(annenPartAktørId.get());
            }
        }
        //Sjekker om det finnes overlapp på far og medforelder i Infotrygd
        aktørIdListSjekkInfotrygd.forEach(aktørId -> {
            boolean overlappInfotrygd = sjekkOverlappInfortrygd.harForeldrepengerInfotrygdSomOverlapper(aktørId, startDatoIVB);
            if (overlappInfotrygd) {
                håndtereOpphørInfotrygd(behandlingId, gjeldendeFagsak, aktørId);
            }
        });
        //Sjekker om det finnes overlapp i fpsak
        aktørIdList
            .forEach(aktørId -> løpendeSakerSomOverlapperUttakPåNySak(aktørId, gjeldendeFagsak.getSaksnummer(), startDatoIVB)
                .forEach(this::håndtereOpphør));
    }

    void vurderOpphørAvYtelserForSVP(Fagsak gjeldendeSVPsak, LocalDate startDatoIVB) {
        List<Fagsak> overlapper = løpendeSakerSomOverlapperUttakPåNySak(gjeldendeSVPsak.getAktørId(), gjeldendeSVPsak.getSaksnummer(), startDatoIVB);
        overlapper.forEach( fagsak -> {
            //Vi logger foreløpig når det er overlapp SVP-SVP
            if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(fagsak.getYtelseType())) {
                LOG.info("Overlapp SVP oppdaget for sak {} med løpende SVP-sak {}. Ingen revurdering opprettet", gjeldendeSVPsak.getSaksnummer(), fagsak.getSaksnummer());
            }
            if (FagsakYtelseType.FORELDREPENGER.equals(fagsak.getYtelseType())) {
                if (erFullUtbetalingSistePeriode(fagsak.getId())) {
//                    Midlertidig fix (TFP-3379) for å hindre at feilaktige revurderinger opprettes - må endre hvordan overlapp sjekkes for SVP
//                   håndtereOpphør(fagsak);
                    LOG.info("Overlapp SVP: SVP-sak {} overlapper med FP-sak {}. Mest sannsynlig ikke overlapp. Kode skal endres", gjeldendeSVPsak.getSaksnummer(), fagsak.getSaksnummer());
                } else {
                    LOG.info("Overlapp SVP: SVP-sak {} overlapper med gradert FP-sak {}. Ingen revurdering opprettet", gjeldendeSVPsak.getSaksnummer(), fagsak.getSaksnummer());
                }
            }
        });
    }

    private void håndtereOpphørInfotrygd(Long behandlingId, Fagsak gjeldendeFagsak, AktørId aktørId) {
        Behandling gjeldendeBehandling = behandlingRepository.hentBehandling(behandlingId);

        opprettTaskForÅVurdereKonsekvens(gjeldendeFagsak.getId(), gjeldendeBehandling.getBehandlendeEnhet(),
            "Nytt barn i VL: Vurder opphør av ytelse i Infotrygd", Optional.of(aktørId.getId()));

        LOG.info("Overlapp INFOTRYGD på aktør {} for vedtatt sak {}", aktørId, gjeldendeFagsak.getSaksnummer());
    }

    private void håndtereOpphør(Fagsak sakOpphør) {
        Optional<Behandling> sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(sakOpphør.getId());
        if (sisteBehandling.isPresent() && !sisteBehandling.get().harBehandlingÅrsak(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN)) {
            var behandlingId = sisteBehandling.get().getId();
            var lås = behandlingRepository.taSkriveLås(behandlingId);
            var behandling = behandlingRepository.hentBehandling(behandlingId);

            opprettTaskForÅVurdereKonsekvens(sakOpphør.getId(), behandling.getBehandlendeEnhet(),
                "Nytt barn: Vurder om ytelse skal opphøre", Optional.empty());
            var harÅpenOrdinærBehandling = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(sakOpphør.getId()).stream()
                .anyMatch(b -> !b.harBehandlingÅrsak(BehandlingÅrsakType.BERØRT_BEHANDLING));

            if (!harÅpenOrdinærBehandling) {
                var skalKøes = !behandling.erAvsluttet() && behandling.harBehandlingÅrsak(BehandlingÅrsakType.BERØRT_BEHANDLING);
                Behandling revurderingOpphør = opprettRevurdering(sakOpphør, BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN, skalKøes);
                if (revurderingOpphør != null) {
                    LOG.info("Overlapp FPSAK: Vurder opphør av ytelse har opprettet revurdering med behandlingId {} på sak med saksnummer {} pga behandlingId {}", revurderingOpphør.getId(), sakOpphør.getSaksnummer(), behandlingId);
                } else {
                    LOG.info("Overlapp FPSAK: Vurder opphør av ytelse kunne ikke opprette revurdering på sak med saksnummer {} pga behandlingId {}", sakOpphør.getSaksnummer(), behandlingId);
                }
            } else {
                oppdatereBehMedÅrsak(behandlingId, lås);
            }
        }
    }

    private List<Fagsak> løpendeSakerSomOverlapperUttakPåNySak(AktørId aktørId, Saksnummer saksnummer, LocalDate startDato) {
        return fagsakRepository.hentForBruker(aktørId).stream()
            .filter(f -> !FagsakYtelseType.ENGANGSTØNAD.equals(f.getYtelseType()))
            .filter(f -> !saksnummer.equals(f.getSaksnummer()))
            .filter(f -> erMaxDatoPåLøpendeSakEtterStartDatoNysak(f, startDato))
            .collect(Collectors.toList());
    }

    private boolean erMaxDatoPåLøpendeSakEtterStartDatoNysak(Fagsak fagsak, LocalDate startDato) {
        var behandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId());
        LocalDate maxDato = behandling.map(Behandling::getId).map(this::finnMaxDato).orElse(Tid.TIDENES_BEGYNNELSE);
        LocalDate maxUtbetaltDato = behandling.map(Behandling::getId).map(this::finnMaxDatoUtenAvslåtte).orElse(Tid.TIDENES_BEGYNNELSE);
        if (!maxDato.isBefore(startDato) && maxUtbetaltDato.isBefore(startDato))
            LOG.info("VurderOpphør - mulig overlapp ifm avslått periode for fagsak {} med maxDato {} og maxUtbetalt {}", fagsak.getSaksnummer(), maxDato, maxUtbetaltDato );

        return !maxUtbetaltDato.isBefore(startDato);
    }

    private LocalDate finnMinDato(Long behandlingId) {
        if (henlagtEllerOpphørFomFørsteUttak(behandlingId))
            return Tid.TIDENES_ENDE;

        Optional<BeregningsresultatEntitet> berResultat = beregningsresultatRepository.hentBeregningsresultat(behandlingId);
        Optional<LocalDate> minFom = berResultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
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
        Optional<BeregningsresultatEntitet> berResultat = beregningsresultatRepository.hentBeregningsresultat(behandlingId);
        Optional<LocalDate> maxTom = berResultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom)
            .max(Comparator.naturalOrder())
            .map(VirkedagUtil::tomVirkedag);
        return maxTom.orElse(Tid.TIDENES_BEGYNNELSE);
    }

    private LocalDate finnMaxDatoUtenAvslåtte(Long behandlingId) {
        Optional<BeregningsresultatEntitet> berResultat = beregningsresultatRepository.hentBeregningsresultat(behandlingId);
        Optional<LocalDate> maxTom = berResultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
            .filter(beregningsresultatPeriode -> beregningsresultatPeriode.getDagsats() > 0)
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom)
            .max(Comparator.naturalOrder())
            .map(VirkedagUtil::tomVirkedag);
        return maxTom.orElse(Tid.TIDENES_BEGYNNELSE);
    }

    private boolean erFullUtbetalingSistePeriode(Long fagsakId) {
        var behandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);
        Optional<BeregningsresultatEntitet> berResultat = beregningsresultatRepository.hentBeregningsresultat(behandling.map(Behandling::getId).orElse(null));

        return berResultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
            .max(Comparator.comparing(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom))
            .map(BeregningsresultatPeriode::getKalkulertUtbetalingsgrad)
            .map(ug -> ug.compareTo(HUNDRE) >= 0).orElse(false);

    }

    private Optional<AktørId> hentAnnenPartAktørId(long behId) {
        return personopplysningRepository.hentPersonopplysningerHvisEksisterer(behId)
            .flatMap(PersonopplysningGrunnlagEntitet::getOppgittAnnenPart)
            .map(OppgittAnnenPartEntitet::getAktørId);
    }

    private Behandling opprettRevurdering(Fagsak sakRevurdering, BehandlingÅrsakType behandlingÅrsakType, boolean skalKøes) {
        OrganisasjonsEnhet enhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(sakRevurdering);

        Behandling revurdering = getRevurderingTjeneste(sakRevurdering).opprettAutomatiskRevurdering(sakRevurdering, behandlingÅrsakType, enhet);

        if (skalKøes) {
            køKontroller.enkøBehandling(revurdering);
        } else {
            behandlingProsesseringTjeneste.opprettTasksForStartBehandling(revurdering);
        }

        return revurdering;
    }

    private RevurderingTjeneste getRevurderingTjeneste(Fagsak fagsak) {
        return FagsakYtelseType.SVANGERSKAPSPENGER.equals(fagsak.getYtelseType()) ? revurderingTjenesteSVP : revurderingTjenesteFP;
    }

    public void opprettTaskForÅVurdereKonsekvens(Long fagsakId, String behandlendeEnhetsId, String oppgaveBeskrivelse, Optional<String> gjeldendeAktørId) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(OpprettOppgaveVurderKonsekvensTask.TASKTYPE);
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BEHANDLENDE_ENHET, behandlendeEnhetsId);
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BESKRIVELSE, oppgaveBeskrivelse);
        gjeldendeAktørId.ifPresent(a-> prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_GJELDENDE_AKTØR_ID, a));
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_PRIORITET, OpprettOppgaveVurderKonsekvensTask.PRIORITET_HØY);
        prosessTaskData.setFagsakId(fagsakId);
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }

    private void oppdatereBehMedÅrsak(Long behandlingId, BehandlingLås lås) {
        behandlingRepository.verifiserBehandlingLås(lås);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        BehandlingÅrsak.builder(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN).buildFor(behandling);
        behandlingRepository.lagre(behandling, lås);
    }

}
