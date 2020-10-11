package no.nav.foreldrepenger.behandling.revurdering.etterkontroll.task;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingHistorikk;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.Etterkontroll;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.KontrollType;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.tjeneste.EtterkontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.FagsakProsessTask;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderKonsekvensTask;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
@ProsessTask(AutomatiskEtterkontrollTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class AutomatiskEtterkontrollTask extends FagsakProsessTask {

    public static final String TASKTYPE = "behandlingsprosess.etterkontroll";
    public static final String OPTIONS_KEY = "ekoptions";
    public static final String OPTIONS_OPPRETT_EK = "ekopprett";
    public static final String OPTIONS_MANUELL_EK = "ekmanuell";
    public static final String OPTIONS_REBEREGN_ES = "esrebergn";

    private static final Logger LOG = LoggerFactory.getLogger(AutomatiskEtterkontrollTask.class);

    private PersoninfoAdapter personinfoAdapter;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskRepository prosessTaskRepository;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private EtterkontrollRepository etterkontrollRepository;
    private RevurderingHistorikk revurderingHistorikk;



    AutomatiskEtterkontrollTask() {
        // for CDI proxy
    }

    @Inject
    public AutomatiskEtterkontrollTask(BehandlingRepositoryProvider repositoryProvider,// NOSONAR
                                       EtterkontrollRepository etterkontrollRepository,
                                       HistorikkRepository historikkRepository,
                                       FamilieHendelseTjeneste familieHendelseTjeneste,
                                       PersoninfoAdapter personinfoAdapter,
                                       ProsessTaskRepository prosessTaskRepository,
                                       BehandlendeEnhetTjeneste behandlendeEnhetTjeneste) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.personinfoAdapter = personinfoAdapter;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.prosessTaskRepository = prosessTaskRepository;
        this.revurderingHistorikk = new RevurderingHistorikk(historikkRepository);
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.etterkontrollRepository = etterkontrollRepository;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        LOG.info("Etterkontrollerer fagsak med fagsakId = {}", fagsakId);

        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        String options = prosessTaskData.getPropertyValue(OPTIONS_KEY);

        etterkontrollRepository.avflaggDersomEksisterer(fagsakId, KontrollType.MANGLENDE_FØDSEL);

        if (behandlingRepository.harÅpenOrdinærYtelseBehandlingerForFagsakId(fagsakId)) {
            if (!(OPTIONS_OPPRETT_EK.equals(options) || OPTIONS_MANUELL_EK.equals(options) || OPTIONS_REBEREGN_ES.equals(options))) {
                opprettTaskForÅVurdereKonsekvens(fagsakId, behandling.getBehandlendeEnhet());
                return;
            }
        }

        List<FødtBarnInfo> barnFødtIPeriode = new ArrayList<>();
        final FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = familieHendelseTjeneste.finnAggregat(behandling.getId()).orElse(null);
        if (familieHendelseGrunnlag != null) {
            var intervaller = familieHendelseTjeneste.forventetFødselsIntervaller(BehandlingReferanse.fra(behandling));
            barnFødtIPeriode.addAll(personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(behandling.getAktørId(), intervaller));
            if (!barnFødtIPeriode.isEmpty()) {
                if (!(OPTIONS_OPPRETT_EK.equals(options) || OPTIONS_MANUELL_EK.equals(options) || OPTIONS_REBEREGN_ES.equals(options))) {
                    revurderingHistorikk.opprettHistorikkinnslagForFødsler(behandling, barnFødtIPeriode);
                }
            }
        }

        EtterkontrollTjeneste automatiskEtterkontrollTjeneste = FagsakYtelseTypeRef.Lookup.find(EtterkontrollTjeneste.class, behandling.getFagsak().getYtelseType()).orElseThrow();
        Optional<BehandlingÅrsakType> revurderingsÅrsak = automatiskEtterkontrollTjeneste.utledRevurderingÅrsak(behandling, familieHendelseGrunnlag, barnFødtIPeriode);

        if (OPTIONS_OPPRETT_EK.equals(options) || OPTIONS_MANUELL_EK.equals(options) || OPTIONS_REBEREGN_ES.equals(options)) {
            revurderingsÅrsak.ifPresent(årsakType -> LOG.info("Etterkontroll Restanse sak {} ville gitt årsak {}", behandling.getFagsak().getSaksnummer().getVerdi(), årsakType.getKode()));
            if (!OPTIONS_REBEREGN_ES.equals(options) &&
                !Set.of(BehandlingÅrsakType.RE_MANGLER_FØDSEL, BehandlingÅrsakType.RE_MANGLER_FØDSEL_I_PERIODE, BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN)
                    .contains(revurderingsÅrsak.orElse(BehandlingÅrsakType.RE_MANGLER_FØDSEL))) {
                opprettEtterkontroll(behandling);
                etterkontrollRepository.avflaggDersomEksisterer(fagsakId, KontrollType.MANGLENDE_FØDSEL);
            }
            return;
        }

        revurderingsÅrsak.ifPresent(årsak -> {
            OrganisasjonsEnhet enhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(behandling.getFagsak());

            automatiskEtterkontrollTjeneste.opprettRevurdering(behandling, årsak, enhet);
        });
    }

    private void opprettEtterkontroll(Behandling behandling) {
        LocalDate ekDato = familieHendelseTjeneste.forventetFødselsIntervaller(BehandlingReferanse.fra(behandling)).stream()
            .map(LocalDateInterval::getTomDato)
            .max(Comparator.naturalOrder()).orElseGet(LocalDate::now);
        var ekTid = ekDato.plusDays(30).atStartOfDay();
        List<Etterkontroll> ekListe = etterkontrollRepository.finnEtterkontrollForFagsak(behandling.getFagsakId(), KontrollType.MANGLENDE_FØDSEL);
        if (ekListe.isEmpty()) {
            Etterkontroll etterkontroll = new Etterkontroll.Builder(behandling.getFagsakId())
                .medKontrollType(KontrollType.MANGLENDE_FØDSEL)
                .medErBehandlet(false)
                .medKontrollTidspunkt(ekTid)
                .build();
            etterkontrollRepository.lagre(etterkontroll);
        } else {
            ekListe.forEach(ek -> {
                ek.setKontrollTidspunktt(ekTid);
                ek.setErBehandlet(false);
                etterkontrollRepository.lagre(ek);
            });
        }
    }

    private void opprettTaskForÅVurdereKonsekvens(Long fagsakId, String behandlendeEnhetsId) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(OpprettOppgaveVurderKonsekvensTask.TASKTYPE);
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BEHANDLENDE_ENHET, behandlendeEnhetsId);
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BESKRIVELSE, "Kontroller manglende fødselsregistrering");
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_PRIORITET, OpprettOppgaveVurderKonsekvensTask.PRIORITET_NORM);
        prosessTaskData.setFagsakId(fagsakId);
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }
}
