package no.nav.foreldrepenger.behandling.steg.varselrevurdering.fp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.steg.varselrevurdering.VarselRevurderingSteg;
import no.nav.foreldrepenger.behandling.steg.varselrevurdering.VarselRevurderingStegFeil;
import no.nav.foreldrepenger.behandlingskontroll.*;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.TransisjonIdentifikator;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

import java.time.LocalDateTime;
import java.util.Collections;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.*;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING;

@BehandlingStegRef(BehandlingStegType.VARSEL_REVURDERING)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
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

        if (SpesialBehandling.skalGrunnlagBeholdes(behandling)) {
            var transisjon = TransisjonIdentifikator
                    .forId(FellesTransisjoner.SPOLFREM_PREFIX + BehandlingStegType.KONTROLLER_FAKTA.getKode());
            return BehandleStegResultat.fremoverførtMedAksjonspunktResultater(transisjon, Collections.emptyList());
        }

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
            var resultat = AksjonspunktResultat.opprettForAksjonspunktMedFrist(AUTO_SATT_PÅ_VENT_REVURDERING, Venteårsak.AVV_DOK,
                    LocalDateTime.now().plus(AUTO_SATT_PÅ_VENT_REVURDERING.getFristPeriod()));
            return BehandleStegResultat.utførtMedAksjonspunktResultat(resultat);
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();

    }

    private boolean harUtførtVentRevurdering(Behandling behandling) {
        return behandling.getAksjonspunktMedDefinisjonOptional(AUTO_SATT_PÅ_VENT_REVURDERING).filter(Aksjonspunkt::erUtført).isPresent();
    }
}
