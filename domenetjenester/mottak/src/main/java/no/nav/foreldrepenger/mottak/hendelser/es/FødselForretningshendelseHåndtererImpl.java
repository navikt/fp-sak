package no.nav.foreldrepenger.mottak.hendelser.es;

import java.time.LocalDate;
import java.time.Period;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelseHåndterer;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelsestypeRef;
import no.nav.foreldrepenger.mottak.hendelser.håndterer.ForretningshendelseHåndtererFelles;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
@ForretningshendelsestypeRef(ForretningshendelseType.FØDSEL)
@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
public class FødselForretningshendelseHåndtererImpl implements ForretningshendelseHåndterer {

    private ForretningshendelseHåndtererFelles forretningshendelseHåndtererFelles;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private Period tpsRegistreringsTidsrom;
    private LegacyESBeregningRepository legacyESBeregningRepository;


    /**
     * @param tpsRegistreringsTidsrom - Periode før termin hvor dødfødsel kan være registrert i TPS
     */
    @Inject
    public FødselForretningshendelseHåndtererImpl(ForretningshendelseHåndtererFelles forretningshendelseHåndtererFelles,
                                                  @KonfigVerdi(value = "etterkontroll.tid.tilbake", defaultVerdi = "P60D") Period tpsRegistreringsTidsrom,
                                                  SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                  LegacyESBeregningRepository legacyESBeregningRepository) {
        this.forretningshendelseHåndtererFelles = forretningshendelseHåndtererFelles;
        this.tpsRegistreringsTidsrom = tpsRegistreringsTidsrom;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.legacyESBeregningRepository = legacyESBeregningRepository;
    }

    @Override
    public void håndterÅpenBehandling(Behandling åpenBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        forretningshendelseHåndtererFelles.håndterÅpenBehandling(åpenBehandling, behandlingÅrsakType);
    }

    @Override
    public void håndterAvsluttetBehandling(Behandling avsluttetBehandling, ForretningshendelseType forretningshendelseType, BehandlingÅrsakType behandlingÅrsakType) {
        if (erRelevantForEngangsstønadSak(avsluttetBehandling)) {
            forretningshendelseHåndtererFelles.opprettRevurderingLagStartTask(avsluttetBehandling.getFagsak(), behandlingÅrsakType);
        }
    }

    private boolean erRelevantForEngangsstønadSak(Behandling behandling) {
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()).getUtledetSkjæringstidspunkt();
        var idag = LocalDate.now().minusDays(1);
        // Gjelder terminsøknader pluss intervall. Øvrige tilfelle fanges opp i etterkontroll.
        return idag.isBefore(stp.plus(tpsRegistreringsTidsrom)) && legacyESBeregningRepository.skalReberegne(behandling.getId(), idag);
    }
}
