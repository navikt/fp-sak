package no.nav.foreldrepenger.mottak.hendelser.es;

import java.time.LocalDate;
import java.time.Period;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.EngangsstønadBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelseHåndterer;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelsestypeRef;
import no.nav.foreldrepenger.mottak.hendelser.håndterer.ForretningshendelseHåndtererFelles;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
@ForretningshendelsestypeRef(ForretningshendelseType.FØDSEL)
@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
public class FødselForretningshendelseHåndtererImpl implements ForretningshendelseHåndterer {

    private static final Period REGULERINGSVINDU = Period.ofDays(60); // Se etterkontrollvindu - EK vil fange opp øvrige tilfelle

    private final ForretningshendelseHåndtererFelles forretningshendelseHåndtererFelles;
    private final SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private final EngangsstønadBeregningRepository engangsstønadBeregningRepository;


    @Inject
    public FødselForretningshendelseHåndtererImpl(ForretningshendelseHåndtererFelles forretningshendelseHåndtererFelles,
                                                  SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                  EngangsstønadBeregningRepository engangsstønadBeregningRepository) {
        this.forretningshendelseHåndtererFelles = forretningshendelseHåndtererFelles;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.engangsstønadBeregningRepository = engangsstønadBeregningRepository;
    }

    @Override
    public void håndterÅpenBehandling(Behandling åpenBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        if (!forretningshendelseHåndtererFelles.barnFødselogDødAlleredeRegistrert(åpenBehandling)) {
            forretningshendelseHåndtererFelles.håndterÅpenBehandling(åpenBehandling, behandlingÅrsakType);
        }
    }

    @Override
    public void håndterAvsluttetBehandling(Behandling avsluttetBehandling, ForretningshendelseType forretningshendelseType, BehandlingÅrsakType behandlingÅrsakType) {
        if (!forretningshendelseHåndtererFelles.barnFødselogDødAlleredeRegistrert(avsluttetBehandling) && erRelevantForEngangsstønadSak(avsluttetBehandling)) {
            forretningshendelseHåndtererFelles.opprettRevurderingLagStartTask(avsluttetBehandling.getFagsak(), behandlingÅrsakType);
        }
    }

    private boolean erRelevantForEngangsstønadSak(Behandling behandling) {
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()).getUtledetSkjæringstidspunkt();
        var idag = LocalDate.now().minusDays(1);
        // Gjelder terminsøknader pluss intervall. Øvrige tilfelle fanges opp i etterkontroll.
        return idag.isBefore(stp.plus(REGULERINGSVINDU)) && engangsstønadBeregningRepository.skalReberegne(behandling.getId(), idag);
    }
}
