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

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.BrukerTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingOpprettingTjeneste;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveFeilmeldinger;
import no.nav.foreldrepenger.produksjonsstyring.opprettgsak.OpprettGSakTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(OpprettInformasjonsFagsakTask.TASKTYPE)
public class OpprettInformasjonsFagsakTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "opprettsak.informasjonssak";
    public static final String FH_DATO_KEY = "familieHendelseDato";
    public static final String OPPRETTET_DATO_KEY = "opprettetDato";
    public static final String BEH_ENHET_ID_KEY = "enhetId";
    public static final String BEH_ENHET_NAVN_KEY = "enhetNavn";
    public static final String BEHANDLING_AARSAK = "behandlingAarsak";
    public static final String FAGSAK_ID_MOR_KEY = "fagsakIdMor";

    private static final Logger log = LoggerFactory.getLogger(OpprettInformasjonsFagsakTask.class);
    private static final Period FH_DIFF_PERIODE = Period.parse("P4W");

    private BehandlingOpprettingTjeneste behandlingOpprettingTjeneste;
    private TpsTjeneste tpsTjeneste;
    private FagsakTjeneste fagsakTjeneste;
    private BrukerTjeneste brukerTjeneste;
    private OpprettGSakTjeneste opprettGSakTjeneste;
    private FagsakRepository fagsakRepository;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private FamilieHendelseRepository familieHendelseRepository;
    private BehandlingRepository behandlingRepository;

    OpprettInformasjonsFagsakTask() {
        // for CDI proxy
    }

    @Inject
    public OpprettInformasjonsFagsakTask(BehandlingRepositoryProvider repositoryProvider,
                                         BehandlingOpprettingTjeneste behandlingOpprettingTjeneste,
                                         TpsTjeneste tpsTjeneste,
                                         BrukerTjeneste brukerTjeneste,
                                         FagsakTjeneste fagsakTjeneste,
                                         OpprettGSakTjeneste opprettGSakTjeneste,
                                         FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.behandlingOpprettingTjeneste = behandlingOpprettingTjeneste;
        this.tpsTjeneste = tpsTjeneste;
        this.brukerTjeneste = brukerTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
        this.opprettGSakTjeneste = opprettGSakTjeneste;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
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
        kobleNyFagsakTilMors(Long.parseLong(prosessTaskData.getPropertyValue(FAGSAK_ID_MOR_KEY)), fagsak);
        Behandling behandling = opprettFørstegangsbehandlingInformasjonssak(fagsak, enhet, behandlingÅrsakType);
        behandlingOpprettingTjeneste.asynkStartBehandlingsprosess(behandling);
        log.info("Opprettet fagsak/informasjon {} med behandling {}", fagsak.getSaksnummer().getVerdi(), behandling.getId()); //NOSONAR
    }

    private Fagsak opprettNyFagsak(Personinfo bruker) {
        FagsakYtelseType ytelseType = FagsakYtelseType.FORELDREPENGER;
        NavBruker navBruker = brukerTjeneste.hentEllerOpprettFraAktorId(bruker);
        Fagsak fagsak = Fagsak.opprettNy(ytelseType, navBruker);
        fagsakTjeneste.opprettFagsak(fagsak);
        Saksnummer saksnummer = opprettGSakTjeneste.opprettArkivsak(bruker.getAktørId());
        fagsakTjeneste.oppdaterFagsakMedGsakSaksnummer(fagsak.getId(), saksnummer);
        return fagsakTjeneste.finnEksaktFagsak(fagsak.getId());
    }

    private void kobleNyFagsakTilMors(Long fagsakIdMor, Fagsak fagsak) {
        // Fagsakene må kobles da infobrevet på ny fagsak trenger informasjon fra uttaket på eksisterende fagsak
        Fagsak fagsakMor = fagsakRepository.finnEksaktFagsak(fagsakIdMor);
        Optional<Behandling> vedtakMor = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakIdMor);
        vedtakMor.ifPresent(behandling -> fagsakRelasjonTjeneste.kobleFagsaker(fagsakMor, fagsak, behandling));
    }

    private Behandling opprettFørstegangsbehandlingInformasjonssak(Fagsak fagsak, OrganisasjonsEnhet enhet, BehandlingÅrsakType behandlingÅrsakType) {
        return behandlingOpprettingTjeneste.opprettBehandling(fagsak, BehandlingType.FØRSTEGANGSSØKNAD, enhet, behandlingÅrsakType);
    }

    private List<Fagsak> hentBrukersRelevanteSaker(AktørId aktørId, LocalDate opprettetEtter, LocalDate familieHendelse) {
        return fagsakRepository.hentForBruker(aktørId).stream()
            .filter(sak -> FagsakYtelseType.FORELDREPENGER.equals(sak.getYtelseType()))
            .filter(sak -> sak.getOpprettetTidspunkt().toLocalDate().isAfter(opprettetEtter.minusDays(1)))
            .filter(Fagsak::erÅpen)
            .filter(sak -> erRelevantFagsak(sak, familieHendelse))
            .collect(Collectors.toList());
    }

    private boolean erRelevantFagsak(Fagsak fagsak, LocalDate fhDato) {
        if (erUkoblet(fagsak)) {
            Optional<LocalDate> fagsakFhDato = finnSakensFHDato(fagsak);
            return fagsakFhDato.isEmpty() || erSammeFH(fhDato, fagsakFhDato.get());
        }
        return false;
    }

    private boolean erUkoblet(Fagsak fagsak) {
        Optional<FagsakRelasjon> relasjon = fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak);
        return relasjon.isEmpty() || (relasjon.get().getFagsakNrEn().equals(fagsak) && relasjon.get().getFagsakNrTo().isEmpty());
    }

    private Optional<LocalDate> finnSakensFHDato(Fagsak fagsak) {
        Optional<Behandling> siste = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId());
        return siste.flatMap(behandling -> familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId()).map(FamilieHendelseGrunnlagEntitet::finnGjeldendeFødselsdato));
    }

    private boolean erSammeFH(LocalDate fhDato, LocalDate fagsakFhDato) {
        return fhDato.isAfter(fagsakFhDato.minus(FH_DIFF_PERIODE)) && fhDato.isBefore(fagsakFhDato.plus(FH_DIFF_PERIODE));
    }

    private Personinfo hentPersonInfo(AktørId aktørId) {
        return tpsTjeneste.hentBrukerForAktør(aktørId)
            .orElseThrow(() -> OppgaveFeilmeldinger.FACTORY.identIkkeFunnet(aktørId).toException());
    }
}
