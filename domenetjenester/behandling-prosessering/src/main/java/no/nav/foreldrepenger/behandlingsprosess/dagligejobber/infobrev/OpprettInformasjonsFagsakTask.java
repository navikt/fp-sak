package no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.BrukerTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.StartBehandlingTask;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveFeilmeldinger;
import no.nav.foreldrepenger.produksjonsstyring.opprettgsak.OpprettGSakTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.util.FPDateUtil;

@ApplicationScoped
@ProsessTask(OpprettInformasjonsFagsakTask.TASKTYPE)
public class OpprettInformasjonsFagsakTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "opprettsak.informasjonssak";
    public static final String FH_DATO_KEY = "familieHendelseDato";
    public static final String OPPRETTET_DATO_KEY = "opprettetDato";
    public static final String BEH_ENHET_ID_KEY = "enhetId";
    public static final String BEH_ENHET_NAVN_KEY = "enhetNavn";
    public static final String BEHANDLING_AARSAK = "behandlingAarsak";

    private static final Logger log = LoggerFactory.getLogger(OpprettInformasjonsFagsakTask.class);
    private static final Period FH_DIFF_PERIODE = Period.parse("P4W");

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private TpsTjeneste tpsTjeneste;
    private FagsakTjeneste fagsakTjeneste;
    private BrukerTjeneste brukerTjeneste;
    private OpprettGSakTjeneste opprettGSakTjeneste;
    private FagsakRepository fagsakRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskRepository prosessTaskRepository;

    OpprettInformasjonsFagsakTask() {
        // for CDI proxy
    }

    @Inject
    public OpprettInformasjonsFagsakTask(BehandlingRepositoryProvider repositoryProvider,
                                         ProsessTaskRepository prosessTaskRepository,
                                         BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                         TpsTjeneste tpsTjeneste,
                                         BrukerTjeneste brukerTjeneste,
                                         FagsakTjeneste fagsakTjeneste,
                                         OpprettGSakTjeneste opprettGSakTjeneste) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.tpsTjeneste = tpsTjeneste;
        this.brukerTjeneste = brukerTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
        this.opprettGSakTjeneste = opprettGSakTjeneste;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
        this.prosessTaskRepository = prosessTaskRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        AktørId aktørId = new AktørId(prosessTaskData.getAktørId());
        // Sjekk at ikke finnes relaterte saker: Saker opprettet etter mors søknad, minus saker andre barn.
        LocalDate morsFHDato = LocalDate.parse(prosessTaskData.getPropertyValue(FH_DATO_KEY));
        LocalDate morsOpprettetDato = LocalDate.parse(prosessTaskData.getPropertyValue(OPPRETTET_DATO_KEY));
        BehandlingÅrsakType behandlingÅrsakType = BehandlingÅrsakType.fraKode(prosessTaskData.getPropertyValue(BEHANDLING_AARSAK));

        List<Fagsak> brukersSaker = hentBrukersRelevanteSaker(aktørId, morsOpprettetDato, morsFHDato);
        if (!brukersSaker.isEmpty()) {
            return;
        }

        OrganisasjonsEnhet enhet = new OrganisasjonsEnhet(prosessTaskData.getPropertyValue(BEH_ENHET_ID_KEY), prosessTaskData.getPropertyValue(BEH_ENHET_NAVN_KEY));
        Personinfo bruker = hentPersonInfo(aktørId);
        if (bruker.getDødsdato() != null) {
            return; // Unngå brev til død annen part
        }

        Fagsak fagsak = opprettNyFagsak(bruker);
        Behandling behandling = opprettFørstegangsbehandlingInformasjonssak(fagsak, enhet, behandlingÅrsakType);
        opprettTaskForÅStarteBehandling(behandling);
        log.info("Opprettet fagsak/informasjon {} med behandling {}", fagsak.getSaksnummer().getVerdi(), behandling.getId()); //NOSONAR
    }

    private Fagsak opprettNyFagsak(Personinfo bruker) {
        FagsakYtelseType ytelseType = FagsakYtelseType.FORELDREPENGER;
        NavBruker navBruker = brukerTjeneste.hentEllerOpprettFraAktorId(bruker);
        Fagsak fagsak = Fagsak.opprettNy(ytelseType, navBruker);
        fagsakTjeneste.opprettFagsak(fagsak);
        Saksnummer saksnummer = opprettGSakTjeneste.opprettEllerFinnGsak(fagsak.getId(), bruker);
        fagsakTjeneste.oppdaterFagsakMedGsakSaksnummer(fagsak.getId(), saksnummer);
        return fagsakTjeneste.finnEksaktFagsak(fagsak.getId());
    }

    private Behandling opprettFørstegangsbehandlingInformasjonssak(Fagsak fagsak, OrganisasjonsEnhet enhet, BehandlingÅrsakType behandlingÅrsakType) {
        BehandlingType behandlingType = BehandlingType.FØRSTEGANGSSØKNAD;
        return behandlingskontrollTjeneste.opprettNyBehandling(fagsak, behandlingType, (beh) -> {
            BehandlingÅrsak.builder(behandlingÅrsakType).buildFor(beh);
            beh.setBehandlingstidFrist(FPDateUtil.iDag().plusWeeks(behandlingType.getBehandlingstidFristUker()));
            beh.setBehandlendeEnhet(enhet);
        }); // NOSONAR
    }

    void opprettTaskForÅStarteBehandling(Behandling behandling) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(StartBehandlingTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }

    private List<Fagsak> hentBrukersRelevanteSaker(AktørId aktørId, LocalDate opprettetEtter, LocalDate familieHendelse) {
        return fagsakRepository.hentForBruker(aktørId).stream()
            .filter(sak -> FagsakYtelseType.FORELDREPENGER.equals(sak.getYtelseType()))
            .filter(sak -> sak.getOpprettetTidspunkt().toLocalDate().isAfter(opprettetEtter))
            .filter(Fagsak::erÅpen) // TODO: Bør man ha med denne???
            .filter(sak -> erRelevantFagsak(sak, familieHendelse))
            .collect(Collectors.toList());
    }

    private boolean erRelevantFagsak(Fagsak fagsak, LocalDate fhDato) {
        if (erUkoblet(fagsak)) {
            Optional<LocalDate> fagsakFhDato = finnSakensFHDato(fagsak);
            return !fagsakFhDato.isPresent() || erSammeFH(fhDato, fagsakFhDato.get());
        }
        return false;
    }

    private boolean erUkoblet(Fagsak fagsak) {
        Optional<FagsakRelasjon> relasjon = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(fagsak);
        return !relasjon.isPresent() || (relasjon.get().getFagsakNrEn().equals(fagsak) && !relasjon.get().getFagsakNrTo().isPresent());
    }

    private Optional<LocalDate> finnSakensFHDato(Fagsak fagsak) {
        Optional<Behandling> siste = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (!siste.isPresent()) {
            return Optional.empty();
        }
        return familieHendelseRepository.hentAggregatHvisEksisterer(siste.get().getId()).map(FamilieHendelseGrunnlagEntitet::finnGjeldendeFødselsdato);
    }

    private boolean erSammeFH(LocalDate fhDato, LocalDate fagsakFhDato) {
        return fhDato.isAfter(fagsakFhDato.minus(FH_DIFF_PERIODE)) && fhDato.isBefore(fagsakFhDato.plus(FH_DIFF_PERIODE));
    }

    private Personinfo hentPersonInfo(AktørId aktørId) {
        return tpsTjeneste.hentBrukerForAktør(aktørId)
            .orElseThrow(() -> OppgaveFeilmeldinger.FACTORY.identIkkeFunnet(aktørId).toException());
    }
}
