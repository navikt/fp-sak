package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.medlemskap.es;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.medlem.UtledVurderingsdatoerForMedlemskapTjeneste;
import no.nav.foreldrepenger.domene.medlem.VurderMedlemskapTjeneste;
import no.nav.foreldrepenger.domene.medlem.impl.MedlemResultat;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

// TODO medlemskap2 fjerne dette steget. Implementere som et annet Inngangsvilkår under medlemskapssteg
@BehandlingStegRef(BehandlingStegType.VURDER_FORUTGÅENDE_MEDLEMSKAPVILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
@ApplicationScoped
public class VurderForutgåendeMedlemskapvilkårSteg implements BehandlingSteg {
    private static final Logger LOG = LoggerFactory.getLogger(VurderForutgåendeMedlemskapvilkårSteg.class);
    private final UtledVurderingsdatoerForMedlemskapTjeneste vurderingsDatoer;
    private final BehandlingRepository behandlingRepository;
    private final SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private final VurderMedlemskapTjeneste vurderMedlemskapTjeneste;

    @Inject
    public VurderForutgåendeMedlemskapvilkårSteg(BehandlingRepositoryProvider repositoryProvider,
                                                 UtledVurderingsdatoerForMedlemskapTjeneste utledVurderingsdatoerForMedlemskapTjeneste,
                                                 SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                 VurderMedlemskapTjeneste vurderMedlemskapTjeneste) {
        this.vurderingsDatoer = utledVurderingsdatoerForMedlemskapTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.vurderMedlemskapTjeneste = vurderMedlemskapTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(kontekst.getBehandlingId());
        var ref = BehandlingReferanse.fra(behandling);
        var datoer = vurderingsDatoer.finnVurderingsDatoerForutForStpEngangsstønad(ref, skjæringstidspunkter);
        Map<LocalDate, Set<MedlemResultat>> resultat = new TreeMap<>();
        for (var dato : datoer) {
            resultat.put(dato, vurderMedlemskapTjeneste.vurderMedlemskap(ref, skjæringstidspunkter, dato));
        }
        boolean skalAvklares = resultat.values().stream().anyMatch(v -> !v.isEmpty());
        if (skalAvklares) {
            LOG.info("VURDER_FORUTGÅENDE_MEDLEMSKAP: fant behov for avklaring. Resultat for STP {} fra vurderMedlemskapTjeneste: {}",
                skjæringstidspunkter.getUtledetSkjæringstidspunkt(), resultat);
        } else {
            LOG.info("VURDER_FORUTGÅENDE_MEDLEMSKAP: fant ikke behov for avklaring");
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
