package no.nav.foreldrepenger.behandling.revurdering.etterkontroll.task;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
import no.nav.foreldrepenger.domene.person.tps.TpsFamilieTjeneste;
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

    private static final Logger LOG = LoggerFactory.getLogger(AutomatiskEtterkontrollTask.class);

    private TpsFamilieTjeneste tpsFamilieTjeneste;
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
                                       TpsFamilieTjeneste tpsFamilieTjeneste,
                                       ProsessTaskRepository prosessTaskRepository,
                                       BehandlendeEnhetTjeneste behandlendeEnhetTjeneste) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.tpsFamilieTjeneste = tpsFamilieTjeneste;
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
        if (OPTIONS_OPPRETT_EK.equals(options)) {
            opprettEtterkontroll(behandling);
        }

        etterkontrollRepository.avflaggDersomEksisterer(fagsakId, KontrollType.MANGLENDE_FØDSEL);

        if (behandlingRepository.harÅpenOrdinærYtelseBehandlingerForFagsakId(fagsakId)) {
            opprettTaskForÅVurdereKonsekvens(fagsakId, behandling.getBehandlendeEnhet());
            return;
        }

        List<FødtBarnInfo> barnFødtIPeriode = new ArrayList<>();
        final FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = familieHendelseTjeneste.finnAggregat(behandling.getId()).orElse(null);
        if (familieHendelseGrunnlag != null) {
            var intervaller = familieHendelseTjeneste.forventetFødselsIntervaller(BehandlingReferanse.fra(behandling));
            barnFødtIPeriode.addAll(tpsFamilieTjeneste.getFødslerRelatertTilBehandling(behandling.getAktørId(), intervaller));
            if (!barnFødtIPeriode.isEmpty()) {
                revurderingHistorikk.opprettHistorikkinnslagForFødsler(behandling, barnFødtIPeriode);
            }
        }

        EtterkontrollTjeneste automatiskEtterkontrollTjeneste = FagsakYtelseTypeRef.Lookup.find(EtterkontrollTjeneste.class, behandling.getFagsak().getYtelseType()).orElseThrow();
        Optional<BehandlingÅrsakType> revurderingsÅrsak = automatiskEtterkontrollTjeneste.utledRevurderingÅrsak(behandling, familieHendelseGrunnlag, barnFødtIPeriode);

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
