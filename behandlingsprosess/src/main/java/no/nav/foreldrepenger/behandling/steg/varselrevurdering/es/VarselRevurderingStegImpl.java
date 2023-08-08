package no.nav.foreldrepenger.behandling.steg.varselrevurdering.es;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_HENDELSE_FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_MANGLER_FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_MANGLER_FØDSEL_I_PERIODE;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_SATS_REGULERING;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VARSEL_REVURDERING_MANUELL;

import java.time.LocalDateTime;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.varselrevurdering.VarselRevurderingSteg;
import no.nav.foreldrepenger.behandling.steg.varselrevurdering.VarselRevurderingStegFeil;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@BehandlingStegRef(BehandlingStegType.VARSEL_REVURDERING)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
@ApplicationScoped
public class VarselRevurderingStegImpl implements VarselRevurderingSteg {

    private BehandlingRepository behandlingRepository;

    @Inject
    public VarselRevurderingStegImpl(BehandlingRepository behandlingRepository) {
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {

        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        if (harUtførtVentRevurdering(behandling)) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        if (behandling.getBehandlingÅrsaker().isEmpty()) {
            throw VarselRevurderingStegFeil.manglerBehandlingsårsakPåRevurdering();
        }

        if (behandling.harBehandlingÅrsak(RE_AVVIK_ANTALL_BARN) || behandling.harBehandlingÅrsak(RE_MANGLER_FØDSEL_I_PERIODE)) {
            // Svært få tilfelle. Slipper videre til SJEKK MANGLENDE FØDSEL så kan man
            // sjekke ettersendt dokumentasjon før man sender brev
            // Det skal ikke sendes automatisk brev i disse tilfellene
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        if (behandling.harBehandlingÅrsak(RE_MANGLER_FØDSEL)) {
            // Se autopunktobserver for brev
            var resultat = AksjonspunktResultat.opprettForAksjonspunktMedFrist(AUTO_SATT_PÅ_VENT_REVURDERING, Venteårsak.AVV_DOK,
                    LocalDateTime.now().plus(AUTO_SATT_PÅ_VENT_REVURDERING.getFristPeriod()));
            return BehandleStegResultat.utførtMedAksjonspunktResultat(resultat);
        }

        // Kunne sjekket på om behandling.erManueltOpprettet() er false - men se an tilfelle av automatisering av ES
        if (behandling.harBehandlingÅrsak(RE_HENDELSE_FØDSEL) || behandling.harBehandlingÅrsak(RE_SATS_REGULERING)) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        // Manuelt opprettet revurdering
        return BehandleStegResultat.utførtMedAksjonspunkter(List.of(VARSEL_REVURDERING_MANUELL));
    }

    private boolean harUtførtVentRevurdering(Behandling behandling) {
        return behandling.getAksjonspunktMedDefinisjonOptional(AUTO_SATT_PÅ_VENT_REVURDERING).filter(Aksjonspunkt::erUtført).isPresent();
    }
}
