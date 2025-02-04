package no.nav.foreldrepenger.økonomistøtte.oppdrag.tjeneste;

import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomioppdragRepository;
import no.nav.foreldrepenger.økonomistøtte.OppdragskontrollPostConditionCheck;
import no.nav.foreldrepenger.økonomistøtte.OppdragskontrollTjeneste;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper.LagOppdragTjeneste;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper.OppdragInput;

@Dependent
public class OppdragskontrollTjenesteImpl implements OppdragskontrollTjeneste {

    private ØkonomioppdragRepository økonomioppdragRepository;

    OppdragskontrollTjenesteImpl() {
        //for cdi proxy
    }

    @Inject
    public OppdragskontrollTjenesteImpl(ØkonomioppdragRepository økonomioppdragRepository) {
        this.økonomioppdragRepository = økonomioppdragRepository;
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
        var oppdragskontroll = LagOppdragTjeneste.lagOppdrag(input, brukFellesEndringstidspunkt, oppdragFraFør.orElse(null));
        oppdragskontroll.ifPresent(OppdragskontrollPostConditionCheck::valider);
        return oppdragskontroll;
    }
}
