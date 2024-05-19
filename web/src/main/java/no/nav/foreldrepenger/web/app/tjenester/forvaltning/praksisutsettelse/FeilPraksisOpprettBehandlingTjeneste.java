package no.nav.foreldrepenger.web.app.tjenester.forvaltning.praksisutsettelse;

import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoBasis;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.GraderingAvslagÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;

/**
 *  Opprette revurderinger der det er oppdaget feil praksis
 */
@ApplicationScoped
public class FeilPraksisOpprettBehandlingTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(FeilPraksisOpprettBehandlingTjeneste.class);

    private static final Set<PeriodeResultatÅrsak> FEIL_UTSETTELSE = Set.of(PeriodeResultatÅrsak.HULL_MELLOM_FORELDRENES_PERIODER,
        PeriodeResultatÅrsak.AVSLAG_UTSETTELSE_PGA_FERIE_TILBAKE_I_TID, PeriodeResultatÅrsak.AVSLAG_UTSETTELSE_PGA_ARBEID_TILBAKE_I_TID);

    private BehandlingRepository behandlingRepository;
    private FagsakLåsRepository fagsakLåsRepository;
    private RevurderingTjeneste revurderingTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private PersoninfoAdapter personinfoAdapter;
    private PersonopplysningRepository personopplysningRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private FpUttakRepository fpUttakRepository;

    @Inject
    public FeilPraksisOpprettBehandlingTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                                @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) RevurderingTjeneste revurderingTjeneste,
                                                BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                                BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                                PersoninfoAdapter personinfoAdapter,
                                                PersonopplysningRepository personopplysningRepository,
                                                FamilieHendelseRepository familieHendelseRepository,
                                                FpUttakRepository fpUttakRepository) {
        this.fagsakLåsRepository = behandlingRepositoryProvider.getFagsakLåsRepository();
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.revurderingTjeneste = revurderingTjeneste;
        this.personinfoAdapter = personinfoAdapter;
        this.personopplysningRepository = personopplysningRepository;
        this.familieHendelseRepository = familieHendelseRepository;
        this.fpUttakRepository = fpUttakRepository;
    }

    FeilPraksisOpprettBehandlingTjeneste() {
        // CDI
    }

    public void opprettBehandling(Fagsak fagsak) {
        if (harÅpenBehandling(fagsak)) {
            LOG.info("FeilPraksisUtsettelse: Har åpen behandling saksnummer {}", fagsak.getSaksnummer());
            return;
        }

        var sisteVedtatte = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()).orElse(null);
        if (sisteVedtatte == null) {
            LOG.info("FeilPraksisUtsettelse: Fant ingen vedtatte behandlinger saksnummer {}", fagsak.getSaksnummer());
            return;
        }
        if (harDødsfall(sisteVedtatte)) {
            LOG.info("FeilPraksisUtsettelse: Sak med dødsfall saksnummer {}", fagsak.getSaksnummer());
            return;
        }
        var uttak = fpUttakRepository.hentUttakResultat(sisteVedtatte.getId());
        var fortsattRammet = uttak.getGjeldendePerioder().getPerioder().stream().anyMatch(FeilPraksisOpprettBehandlingTjeneste::feilPraksis);
        if (!fortsattRammet) {
            LOG.info("FeilPraksisUtsettelse: Sak ikke utslag feil praksis saksnummer {}", fagsak.getSaksnummer());
            return;
        }

        var enhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFra(sisteVedtatte);
        fagsakLåsRepository.taLås(fagsak.getId());
        var revurdering = revurderingTjeneste.opprettAutomatiskRevurdering(fagsak, BehandlingÅrsakType.FEIL_PRAKSIS_UTSETTELSE, enhet);
        behandlingProsesseringTjeneste.opprettTasksForStartBehandling(revurdering);
        LOG.info("FeilPraksisUtsettelse: Opprettet revurdering med behandlingId {} saksnummer {}", revurdering.getId(), fagsak.getSaksnummer());

    }

    private static boolean feilPraksis(UttakResultatPeriodeEntitet periode) {
        return feilUtsettelseTrekkUtenUtbetaling(periode) || feilPraksisGraderingTrekkUtenUtbetaling(periode);
    }


    private static boolean feilUtsettelseTrekkUtenUtbetaling(UttakResultatPeriodeEntitet periode) {
        return FEIL_UTSETTELSE.contains(periode.getResultatÅrsak()) && periode.getAktiviteter().stream()
            .anyMatch(a -> a.getTrekkdager().merEnn0() && !a.getUtbetalingsgrad().harUtbetaling());
    }

    private static boolean feilPraksisGraderingTrekkUtenUtbetaling(UttakResultatPeriodeEntitet periode) {
        return GraderingAvslagÅrsak.FOR_SEN_SØKNAD.equals(periode.getGraderingAvslagÅrsak()) && periode.getSamtidigUttaksprosent() == null &&
            periode.getAktiviteter().stream().anyMatch(a -> a.getUtbetalingsgrad().compareTo(Utbetalingsgrad.HUNDRED) < 0);
    }

    private boolean harÅpenBehandling(Fagsak fagsak) {
        return !behandlingRepository.hentÅpneBehandlingerIdForFagsakId(fagsak.getId()).isEmpty();
    }

    private boolean harDødsfall(Behandling behandling) {
        var barnDødsdato = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getBarna).orElse(List.of())
            .stream().anyMatch(b -> b.getDødsdato().isPresent());
        var personDødsdato = personopplysningRepository.hentPersonopplysningerHvisEksisterer(behandling.getId())
            .map(PersonopplysningGrunnlagEntitet::getGjeldendeVersjon)
            .map(PersonInformasjonEntitet::getPersonopplysninger).orElse(List.of())
            .stream().anyMatch(p -> p.getDødsdato() != null || harDødsdato(p.getAktørId()));

        return barnDødsdato || personDødsdato;
    }

    private boolean harDødsdato(AktørId aktørId) {
        return personinfoAdapter.hentBrukerBasisForAktør(FagsakYtelseType.FORELDREPENGER, aktørId)
            .map(PersoninfoBasis::dødsdato).isPresent();
    }

}
