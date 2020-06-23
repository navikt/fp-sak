package no.nav.foreldrepenger.mottak.hendelser.es;

import java.time.LocalDate;
import java.time.Period;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.domene.registerinnhenting.RegisterdataInnhenter;
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
    private RegisterdataInnhenter registerdataInnhenter;


    /**
     * @param tpsRegistreringsTidsrom - Periode før termin hvor dødfødsel kan være registrert i TPS
     */
    @Inject
    public FødselForretningshendelseHåndtererImpl(ForretningshendelseHåndtererFelles forretningshendelseHåndtererFelles,
                                                @KonfigVerdi(value = "etterkontroll.tpsregistrering.periode", defaultVerdi = "P11W") Period tpsRegistreringsTidsrom,
                                                SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                RegisterdataInnhenter registerdataInnhenter) {
        this.forretningshendelseHåndtererFelles = forretningshendelseHåndtererFelles;
        this.tpsRegistreringsTidsrom = tpsRegistreringsTidsrom;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.registerdataInnhenter = registerdataInnhenter;
    }

    @Override
    public void håndterÅpenBehandling(Behandling åpenBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        forretningshendelseHåndtererFelles.fellesHåndterÅpenBehandling(åpenBehandling, behandlingÅrsakType);
    }

    @Override
    public void håndterAvsluttetBehandling(Behandling avsluttetBehandling, ForretningshendelseType forretningshendelseType, BehandlingÅrsakType behandlingÅrsakType) {
        if (erRelevantForEngangsstønadSak(avsluttetBehandling)) {
            forretningshendelseHåndtererFelles.opprettRevurderingLagStartTask(avsluttetBehandling.getFagsak(), behandlingÅrsakType);
        } else { // TODO(pjv): Det er litt ugreit å oppdatere grunnlag i avsluttet behandling. Kan ikke fagsaken oppdateres?
            registerdataInnhenter.innhentPersonopplysninger(avsluttetBehandling);
        }
    }

    private boolean erRelevantForEngangsstønadSak(Behandling behandling) {
        LocalDate stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()).getUtledetSkjæringstidspunkt();
        LocalDate idag = LocalDate.now();
        // Gjelder terminsøknader pluss intervall. Øvrige tilfelle fanges opp i etterkontroll.
        if (idag.isBefore(stp.plus(tpsRegistreringsTidsrom))) {
            // Ønsker ikke revurderinger gjennom året, kun hvis sist vedtaksdato er året før fødsel er registrert
            return behandling.getOpprettetDato().getYear() != idag.getYear();
        }
        return false;
    }
}
