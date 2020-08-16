package no.nav.foreldrepenger.mottak.hendelser.es;

import java.time.LocalDate;
import java.time.Period;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.KontrollType;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelseHåndterer;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelsestypeRef;
import no.nav.foreldrepenger.mottak.hendelser.håndterer.ForretningshendelseHåndtererFelles;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
@ForretningshendelsestypeRef(ForretningshendelsestypeRef.FØDSEL_HENDELSE)
@FagsakYtelseTypeRef("ES")
public class FødselForretningshendelseHåndtererImpl implements ForretningshendelseHåndterer {

    private ForretningshendelseHåndtererFelles forretningshendelseHåndtererFelles;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private Period tpsRegistreringsTidsrom;
    private EtterkontrollRepository etterkontrollRepository;
    private LegacyESBeregningRepository legacyESBeregningRepository;


    /**
     * @param tpsRegistreringsTidsrom - Periode før termin hvor dødfødsel kan være registrert i TPS
     */
    @Inject
    public FødselForretningshendelseHåndtererImpl(ForretningshendelseHåndtererFelles forretningshendelseHåndtererFelles,
                                                  @KonfigVerdi(value = "etterkontroll.tpsregistrering.periode", defaultVerdi = "P11W") Period tpsRegistreringsTidsrom,
                                                  SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                  EtterkontrollRepository etterkontrollRepository,
                                                  LegacyESBeregningRepository legacyESBeregningRepository) {
        this.forretningshendelseHåndtererFelles = forretningshendelseHåndtererFelles;
        this.tpsRegistreringsTidsrom = tpsRegistreringsTidsrom;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.etterkontrollRepository = etterkontrollRepository;
        this.legacyESBeregningRepository = legacyESBeregningRepository;
    }

    @Override
    public void håndterÅpenBehandling(Behandling åpenBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        forretningshendelseHåndtererFelles.fellesHåndterÅpenBehandling(åpenBehandling, behandlingÅrsakType);
    }

    @Override
    public void håndterAvsluttetBehandling(Behandling avsluttetBehandling, ForretningshendelseType forretningshendelseType, BehandlingÅrsakType behandlingÅrsakType) {
        if (erRelevantForEngangsstønadSak(avsluttetBehandling)) {
            forretningshendelseHåndtererFelles.opprettRevurderingLagStartTask(avsluttetBehandling.getFagsak(), behandlingÅrsakType);
        } else {
            etterkontrollRepository.avflaggDersomEksisterer(avsluttetBehandling.getFagsakId(), KontrollType.MANGLENDE_FØDSEL);
        }
    }

    private boolean erRelevantForEngangsstønadSak(Behandling behandling) {
        LocalDate stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()).getUtledetSkjæringstidspunkt();
        LocalDate idag = LocalDate.now();
        // Gjelder terminsøknader pluss intervall. Øvrige tilfelle fanges opp i etterkontroll.
        if (idag.isBefore(stp.plus(tpsRegistreringsTidsrom))) {
            var vedtakSats = legacyESBeregningRepository.getSisteBeregning(behandling.getId()).map(LegacyESBeregning::getSatsVerdi).orElse(0L);
            return vedtakSats != legacyESBeregningRepository.finnEksaktSats(BeregningSatsType.ENGANG, idag).getVerdi();
        }
        return false;
    }
}
