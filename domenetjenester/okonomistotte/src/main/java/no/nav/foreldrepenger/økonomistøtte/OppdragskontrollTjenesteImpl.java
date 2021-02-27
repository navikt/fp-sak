package no.nav.foreldrepenger.økonomistøtte;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.SjekkOmDetFinnesTilkjentYtelse;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.adapter.BehandlingTilOppdragMapperTjeneste;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.endring.OppdragskontrollEndring;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.fp.OppdragInput;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.førstegangsoppdrag.OppdragskontrollFørstegang;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.opphør.OppdragskontrollOpphør;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.Input;

@Dependent
@FagsakYtelseTypeRef("FP")
@FagsakYtelseTypeRef("SVP")
@Named("oppdragTjeneste")
public class OppdragskontrollTjenesteImpl implements OppdragskontrollTjeneste {

    private static final Logger log = LoggerFactory.getLogger(OppdragskontrollTjenesteImpl.class);
    private static final Long DUMMY_PT_ID_SIMULERING = -1L;

    private ØkonomioppdragRepository økonomioppdragRepository;
    private BehandlingRepository behandlingRepository;
    private OppdragskontrollFørstegang oppdragskontrollFørstegang;
    private OppdragskontrollEndring oppdragskontrollEndring;
    private OppdragskontrollOpphør oppdragskontrollOpphør;
    private SjekkOmDetFinnesTilkjentYtelse sjekkOmDetFinnesTilkjentYtelse;
    private BehandlingTilOppdragMapperTjeneste behandlingTilOppdragMapperTjeneste;

    OppdragskontrollTjenesteImpl() {
        // For CDI
    }

    @Inject
    public OppdragskontrollTjenesteImpl(BehandlingRepositoryProvider repositoryProvider,
                                        ØkonomioppdragRepository økonomioppdragRepository,
                                        OppdragskontrollFørstegang oppdragskontrollFørstegang,
                                        OppdragskontrollEndring oppdragskontrollEndring,
                                        OppdragskontrollOpphør oppdragskontrollOpphør,
                                        SjekkOmDetFinnesTilkjentYtelse sjekkOmDetFinnesTilkjentYtelse,
                                        BehandlingTilOppdragMapperTjeneste behandlingTilOppdragMapperTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.økonomioppdragRepository = økonomioppdragRepository;
        this.oppdragskontrollFørstegang = oppdragskontrollFørstegang;
        this.oppdragskontrollEndring = oppdragskontrollEndring;
        this.oppdragskontrollOpphør = oppdragskontrollOpphør;
        this.sjekkOmDetFinnesTilkjentYtelse = sjekkOmDetFinnesTilkjentYtelse;
        this.behandlingTilOppdragMapperTjeneste = behandlingTilOppdragMapperTjeneste;
    }

    @Override
    public final Optional<Oppdragskontroll> opprettOppdrag(Long behandlingId, Long prosessTaskId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Saksnummer saksnummer = behandling.getFagsak().getSaksnummer();

        List<Oppdragskontroll> tidligereOppdragListe = økonomioppdragRepository.finnAlleOppdragForSak(saksnummer);

        boolean tidligereOppdragFinnes = !tidligereOppdragListe.isEmpty();

        Oppdragskontroll oppdragskontroll = FastsettOppdragskontroll.finnEllerOpprett(tidligereOppdragListe, behandlingId, prosessTaskId, saksnummer);

        var oppdragManager = hentTjeneste(behandling, tidligereOppdragFinnes);

        if (oppdragManager.isPresent()) {
            OppdragInput oppdragInput = behandlingTilOppdragMapperTjeneste.map(behandling);
            Oppdragskontroll oppdrag = oppdragManager.get().opprettØkonomiOppdrag(oppdragInput, oppdragskontroll);
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
        return opprettOppdrag(behandlingId, prosessTaskId);
    }

    @Override
    public Optional<Oppdragskontroll> opprettOppdrag(Input input) {
        throw new IllegalStateException("Ikke støttet ennå.");
    }

    @Override
    public Optional<Oppdragskontroll> simulerOppdrag(Input input) {
        throw new IllegalStateException("Ikke støttet ennå.");
    }

    @Override
    public Optional<Oppdragskontroll> opprettOppdrag(Input input, boolean brukFellesEndringstidspunkt) {
        throw new IllegalStateException("Ikke støttet ennå.");
    }

    @Override
    public void lagre(Oppdragskontroll oppdragskontroll) {
        økonomioppdragRepository.lagre(oppdragskontroll);
    }

    private Optional<OppdragskontrollManager> hentTjeneste(Behandling behandling, boolean finnesOppdragFraFør) {
        var diff = sjekkOmDetFinnesTilkjentYtelse.tilkjentYtelseDiffMotForrige(behandling);
        return switch (diff) {
            case INGEN_ENDRING -> Optional.empty();
            case ENDRET_TIL_TOM -> Optional.of(oppdragskontrollOpphør);
            case ENDRET_FRA_TOM -> Optional.of(finnesOppdragFraFør ? oppdragskontrollEndring : oppdragskontrollFørstegang);
            case ANNEN_ENDRING -> Optional.of(oppdragskontrollEndring);
            default -> throw new IllegalArgumentException("Ikke-støttet YtelseDiff: " + diff);
        };
    }
}
