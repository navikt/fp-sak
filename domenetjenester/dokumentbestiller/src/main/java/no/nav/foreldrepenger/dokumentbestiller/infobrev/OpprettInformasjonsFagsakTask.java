package no.nav.foreldrepenger.dokumentbestiller.infobrev;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoBasis;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingOpprettingTjeneste;
import no.nav.foreldrepenger.domene.bruker.NavBrukerTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "opprettsak.informasjonssak", prioritet = 3)
public class OpprettInformasjonsFagsakTask implements ProsessTaskHandler {

    public static final String FH_DATO_KEY = "familieHendelseDato";
    public static final String OPPRETTET_DATO_KEY = "opprettetDato";
    public static final String BEH_ENHET_ID_KEY = "enhetId";
    public static final String BEH_ENHET_NAVN_KEY = "enhetNavn";
    public static final String BEHANDLING_AARSAK = "behandlingAarsak";
    public static final String FAGSAK_ID_MOR_KEY = "fagsakIdMor";

    private static final Logger LOG = LoggerFactory.getLogger(OpprettInformasjonsFagsakTask.class);
    private static final Period FH_DIFF_PERIODE = Period.parse("P6W");

    private BehandlingOpprettingTjeneste behandlingOpprettingTjeneste;
    private PersoninfoAdapter personinfoAdapter;
    private FagsakTjeneste fagsakTjeneste;
    private NavBrukerTjeneste brukerTjeneste;
    private FagsakRepository fagsakRepository;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private FamilieHendelseRepository familieHendelseRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    OpprettInformasjonsFagsakTask() {
        // for CDI proxy
    }

    @Inject
    public OpprettInformasjonsFagsakTask(BehandlingRepositoryProvider repositoryProvider,
            BehandlingOpprettingTjeneste behandlingOpprettingTjeneste,
            PersoninfoAdapter personinfoAdapter,
            NavBrukerTjeneste brukerTjeneste,
            FagsakTjeneste fagsakTjeneste,
            BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
            FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.behandlingOpprettingTjeneste = behandlingOpprettingTjeneste;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.personinfoAdapter = personinfoAdapter;
        this.brukerTjeneste = brukerTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var aktørId = new AktørId(prosessTaskData.getAktørId());
        // Sjekk at ikke finnes relaterte saker: Saker opprettet etter mors søknad,
        // minus saker andre barn.
        var morsFHDato = LocalDate.parse(prosessTaskData.getPropertyValue(FH_DATO_KEY));
        var morsOpprettetDato = LocalDate.parse(prosessTaskData.getPropertyValue(OPPRETTET_DATO_KEY));
        var behandlingÅrsakType = BehandlingÅrsakType.fraKode(prosessTaskData.getPropertyValue(BEHANDLING_AARSAK));

        var brukersSaker = hentBrukersRelevanteSaker(aktørId, morsOpprettetDato, morsFHDato);
        if (!brukersSaker.isEmpty()) {
            return;
        }

        var fagsakMor = fagsakRepository.finnEksaktFagsakReadOnly(Long.parseLong(prosessTaskData.getPropertyValue(FAGSAK_ID_MOR_KEY)));
        var enhet = prosessTaskData.getPropertyValue(BEH_ENHET_ID_KEY);
        var brukEnhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetForUkoblet(fagsakMor, enhet);
        var bruker = hentPersonInfo(aktørId);
        if (bruker.dødsdato() != null) {
            return; // Unngå brev til død annen part
        }
        var fagsak = opprettNyFagsak(aktørId);
        kobleNyFagsakTilMors(fagsakMor, fagsak);
        var behandling = opprettFørstegangsbehandlingInformasjonssak(fagsak, brukEnhet, behandlingÅrsakType);
        behandlingOpprettingTjeneste.asynkStartBehandlingsprosess(behandling);
        LOG.info("Opprettet fagsak/informasjon {} med behandling {}", fagsak.getSaksnummer().getVerdi(), behandling.getId());
    }

    private Fagsak opprettNyFagsak(AktørId aktørId) {
        var ytelseType = FagsakYtelseType.FORELDREPENGER;
        var navBruker = brukerTjeneste.hentEllerOpprettFraAktørId(aktørId);
        return fagsakTjeneste.opprettFagsak(ytelseType, navBruker);
    }

    private void kobleNyFagsakTilMors(Fagsak fagsakMor, Fagsak fagsak) {
        // Fagsakene må kobles da infobrevet på ny fagsak trenger informasjon fra
        // uttaket på eksisterende fagsak
        var vedtakMor = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakMor.getId());
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
                .toList();
    }

    private boolean erRelevantFagsak(Fagsak fagsak, LocalDate fhDato) {
        if (erUkoblet(fagsak)) {
            var fagsakFhDato = finnSakensFHDato(fagsak);
            return fagsakFhDato.isEmpty() || erSammeFH(fhDato, fagsakFhDato.get());
        }
        return false;
    }

    private boolean erUkoblet(Fagsak fagsak) {
        var relasjon = fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak);
        return relasjon.isEmpty() || relasjon.get().getFagsakNrEn().equals(fagsak) && relasjon.get().getFagsakNrTo().isEmpty();
    }

    private Optional<LocalDate> finnSakensFHDato(Fagsak fagsak) {
        var siste = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId());
        return siste.flatMap(behandling -> familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())
                .map(FamilieHendelseGrunnlagEntitet::finnGjeldendeFødselsdato));
    }

    private boolean erSammeFH(LocalDate fhDato, LocalDate fagsakFhDato) {
        return fhDato.isAfter(fagsakFhDato.minus(FH_DIFF_PERIODE)) && fhDato.isBefore(fagsakFhDato.plus(FH_DIFF_PERIODE));
    }

    private PersoninfoBasis hentPersonInfo(AktørId aktørId) {
        return personinfoAdapter.hentBrukerBasisForAktør(FagsakYtelseType.FORELDREPENGER, aktørId)
                .orElseThrow(() -> new TekniskException("FP-442142", String.format("Fant ingen ident for aktør %s.", aktørId)));
    }

}
