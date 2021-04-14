package no.nav.foreldrepenger.behandling.steg.varselrevurdering.fp;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_MANGLER_FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_MANGLER_FØDSEL_I_PERIODE;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING;

import java.time.LocalDateTime;
import java.util.Collections;

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
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.TransisjonIdentifikator;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;

@BehandlingStegRef(kode = "VRSLREV")
@BehandlingTypeRef
@FagsakYtelseTypeRef("FP")
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

        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.BERØRT_BEHANDLING)) {
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
            return BehandleStegResultat.utførtMedAksjonspunktResultater(Collections.singletonList(resultat));
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();

    }

    private boolean harUtførtVentRevurdering(Behandling behandling) {
        return behandling.getAksjonspunktMedDefinisjonOptional(AUTO_SATT_PÅ_VENT_REVURDERING).map(Aksjonspunkt::erUtført).orElse(Boolean.FALSE);
    }
}
