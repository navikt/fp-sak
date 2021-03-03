package no.nav.foreldrepenger.økonomistøtte.ny.tjeneste;

import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.OppdragKvittering;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.økonomistøtte.OppdragskontrollPostConditionCheck;
import no.nav.foreldrepenger.økonomistøtte.OppdragskontrollTjeneste;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.Input;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.LagOppdragTjeneste;
import no.nav.foreldrepenger.økonomistøtte.ØkonomioppdragRepository;

@Dependent
@Named("nyOppdragTjeneste")
public class NyOppdragskontrollTjenesteImpl implements OppdragskontrollTjeneste {

    private ØkonomioppdragRepository økonomioppdragRepository;
    private LagOppdragTjeneste lagOppdragTjeneste;

    NyOppdragskontrollTjenesteImpl() {
        //for cdi proxy
    }

    @Inject
    public NyOppdragskontrollTjenesteImpl(LagOppdragTjeneste lagOppdragTjeneste,
                                          ØkonomioppdragRepository økonomioppdragRepository) {
        this.økonomioppdragRepository = økonomioppdragRepository;
        this.lagOppdragTjeneste = lagOppdragTjeneste;
    }

    /**
     * Brukes ved iverksettelse. Sender over kun nødvendige endringer til oppdragssystemet.
     */
    @Deprecated
    public Optional<Oppdragskontroll> opprettOppdrag(Long behandlingId, Long prosessTaskId) {
        return Optional.empty();
    }

    @Override
    public Optional<Oppdragskontroll> simulerOppdrag(Long behandlingId) {
        return Optional.empty();
    }

    /**
     * Brukes ved iverksettelse. Sender over kun nødvendige endringer til oppdragssystemet.
     */
    @Override
    public Optional<Oppdragskontroll> opprettOppdrag(Input input) {
        return opprettOppdrag(input, false);
    }

    @Override
    public Optional<Oppdragskontroll> simulerOppdrag(Input input) {
        return opprettOppdrag(input, true);
    }

    /**
     * Brukes ved simulering. Finner tidligste endringstidspunkt på tvers av mottakere, og sender alt for alle mottakere f.o.m. det felles endringstidspunktet.
     * Det gjør at simuleringsvisningen får data for alle mottakere og inntektskategorier, og ikke bare for de som er endret.
     */
    @Override
    public Optional<Oppdragskontroll> opprettOppdrag(Input input, boolean brukFellesEndringstidspunkt) {
        Oppdragskontroll oppdragskontroll = lagOppdragTjeneste.lagOppdrag(input, brukFellesEndringstidspunkt);
        if (oppdragskontroll != null) {
            OppdragskontrollPostConditionCheck.valider(oppdragskontroll);
            return Optional.of(oppdragskontroll);
        }
        return Optional.empty();
    }

    @Deprecated
    public Optional<Oppdragskontroll> opprettOppdrag(Long behandlingId, Long prosessTaskId, boolean brukFellesEndringstidspunkt) {
        return Optional.empty();
    }

    @Override
    public void lagre(Oppdragskontroll oppdragskontroll) {
        økonomioppdragRepository.lagre(oppdragskontroll);
    }
}
