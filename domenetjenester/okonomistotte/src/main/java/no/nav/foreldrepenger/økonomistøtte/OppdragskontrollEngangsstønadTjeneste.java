package no.nav.foreldrepenger.økonomistøtte;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.es.UtledVedtakResultatTypeES;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.OppdragKvittering;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.kontantytelse.es.OppdragskontrollEngangsstønad;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.Input;

@Dependent
@FagsakYtelseTypeRef("ES")
@Named("oppdragEngangstønadTjeneste")
public class    OppdragskontrollEngangsstønadTjeneste implements OppdragskontrollTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(OppdragskontrollEngangsstønadTjeneste.class);
    private static final Long DUMMY_PT_ID_SIMULERING = -1L;

    private ØkonomioppdragRepository økonomioppdragRepository;
    private BehandlingRepository behandlingRepository;
    private OppdragskontrollEngangsstønad oppdragskontrollEngangsstønad;
    private RevurderingEndring revurderingEndring;

    OppdragskontrollEngangsstønadTjeneste() {
        // For CDI
    }

    @Inject
    public OppdragskontrollEngangsstønadTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                                 ØkonomioppdragRepository økonomioppdragRepository,
                                                 OppdragskontrollEngangsstønad oppdragskontrollEngangsstønad,
                                                 @FagsakYtelseTypeRef("ES") RevurderingEndring revurderingEndring) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.økonomioppdragRepository = økonomioppdragRepository;
        this.oppdragskontrollEngangsstønad = oppdragskontrollEngangsstønad;
        this.revurderingEndring = revurderingEndring;
    }

    @Override
    public final Optional<Oppdragskontroll> opprettOppdrag(Long behandlingId, Long prosessTaskId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Saksnummer saksnummer = behandling.getFagsak().getSaksnummer();

        List<Oppdragskontroll> tidligereOppdragListe = økonomioppdragRepository.finnAlleOppdragForSak(saksnummer);

        Oppdragskontroll oppdragskontroll = FastsettOppdragskontroll.finnEllerOpprett(tidligereOppdragListe, behandlingId, prosessTaskId, saksnummer);

        if (skalSendeOppdrag(behandling)) {
            Oppdragskontroll oppdrag = oppdragskontrollEngangsstønad.opprettØkonomiOppdrag(behandling, oppdragskontroll);
            OppdragskontrollPostConditionCheck.valider(oppdrag);
            return Optional.of(oppdrag);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Oppdragskontroll> simulerOppdrag(Long behandlingId) {
        return opprettOppdrag(behandlingId, DUMMY_PT_ID_SIMULERING);
    }

    @Override
    public Optional<Oppdragskontroll> opprettOppdrag(Long behandlingId, Long prosessTaskId, boolean brukFellesEndringstidspunkt) {
        return opprettOppdrag(behandlingId, DUMMY_PT_ID_SIMULERING);
    }

    @Override
    public Optional<Oppdragskontroll> opprettOppdrag(Input input) {
        throw new IllegalStateException("Støttes ikke ennå.");
    }

    @Override
    public Optional<Oppdragskontroll> simulerOppdrag(Input input) {
        throw new IllegalStateException("Støttes ikke ennå.");
    }

    @Override
    public Optional<Oppdragskontroll> opprettOppdrag(Input input, boolean brukFellesEndringstidspunkt) {
        throw new IllegalStateException("Støttes ikke ennå.");
    }

    @Override
    public void lagre(Oppdragskontroll oppdragskontroll) {
        økonomioppdragRepository.lagre(oppdragskontroll);
    }

    private boolean skalSendeOppdrag(Behandling behandling) {
        boolean erBeslutningsvedtak = revurderingEndring.erRevurderingMedUendretUtfall(behandling);
        if (behandling.erRevurdering()) {
            return !erBeslutningsvedtak;
        }
        Behandlingsresultat behandlingsresultat = behandling.getBehandlingsresultat();
        VedtakResultatType vedtakResultatType = UtledVedtakResultatTypeES.utled(behandling.getType(), behandlingsresultat.getBehandlingResultatType());
        return !erAvslagPåGrunnAvTidligereUtbetaltEngangsstønad(behandlingsresultat, vedtakResultatType)
            && erInnvilgetVedtak(vedtakResultatType, erBeslutningsvedtak);
    }

    private boolean erAvslagPåGrunnAvTidligereUtbetaltEngangsstønad(Behandlingsresultat behandlingsresultat, VedtakResultatType vedtakResultatType) {
        if (VedtakResultatType.AVSLAG.equals(vedtakResultatType)) {
            return Optional.ofNullable(behandlingsresultat.getAvslagsårsak())
                .map(Avslagsårsak::erAlleredeUtbetaltEngangsstønad)
                .orElse(Boolean.FALSE);
        }
        return false;
    }

    private boolean erInnvilgetVedtak(VedtakResultatType vedtakResultatType, boolean erBeslutningsvedtak) {
        return !erBeslutningsvedtak && VedtakResultatType.INNVILGET.equals(vedtakResultatType);
    }
}
