package no.nav.foreldrepenger.økonomistøtte.ny.tjeneste;

import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.økonomistøtte.OppdragskontrollPostConditionCheck;
import no.nav.foreldrepenger.økonomistøtte.OppdragskontrollTjeneste;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.LagOppdragTjeneste;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.OppdragInput;
import no.nav.foreldrepenger.økonomistøtte.ØkonomioppdragRepository;

@Dependent
public class OppdragskontrollTjenesteImpl implements OppdragskontrollTjeneste {

    private ØkonomioppdragRepository økonomioppdragRepository;
    private LagOppdragTjeneste lagOppdragTjeneste;

    OppdragskontrollTjenesteImpl() {
        //for cdi proxy
    }

    @Inject
    public OppdragskontrollTjenesteImpl(LagOppdragTjeneste lagOppdragTjeneste,
                                        ØkonomioppdragRepository økonomioppdragRepository) {
        this.økonomioppdragRepository = økonomioppdragRepository;
        this.lagOppdragTjeneste = lagOppdragTjeneste;
    }

    /**
     * Brukes ved iverksettelse. Sender over kun nødvendige endringer til oppdragssystemet.
     */
    @Override
    public Optional<Oppdragskontroll> opprettOppdrag(OppdragInput input) {
        return opprettOppdrag(input, false);
    }

    /**
     * Brukes ved simulering. Finner tidligste endringstidspunkt på tvers av mottakere, og sender alt for alle mottakere f.o.m. det felles endringstidspunktet.
     * Det gjør at simuleringsvisningen får data for alle mottakere og inntektskategorier, og ikke bare for de som er endret.
     */
    @Override
    public Optional<Oppdragskontroll> simulerOppdrag(OppdragInput input) {
        return opprettOppdrag(input, true);
    }

    @Override
    public void lagre(Oppdragskontroll oppdragskontroll) {
        økonomioppdragRepository.lagre(oppdragskontroll);
    }

    private Optional<Oppdragskontroll> opprettOppdrag(OppdragInput input, boolean brukFellesEndringstidspunkt) {
        var oppdragFraFør = økonomioppdragRepository.finnOppdragForBehandling(input.getBehandlingId());
        var oppdragskontroll = lagOppdragTjeneste.lagOppdrag(input, brukFellesEndringstidspunkt, oppdragFraFør.orElse(null));
        oppdragskontroll.ifPresent(OppdragskontrollPostConditionCheck::valider);
        return oppdragskontroll;
    }
}
